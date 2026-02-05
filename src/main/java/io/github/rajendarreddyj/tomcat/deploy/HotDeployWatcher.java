package io.github.rajendarreddyj.tomcat.deploy;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.maven.plugin.logging.Log;

import io.github.rajendarreddyj.tomcat.config.DeployableConfiguration;

/**
 * Watches for file changes and triggers hot deployment after an inactivity
 * period.
 *
 * <p>
 * This class monitors the source webapp directory for file changes using the
 * Java NIO WatchService. When changes are detected, it waits for a configurable
 * period of inactivity before triggering a redeployment. This batching approach
 * prevents multiple rapid redeployments when many files change in quick
 * succession.
 * </p>
 *
 * <h2>Thread Model</h2>
 * <p>
 * Uses two threads:
 * </p>
 * <ul>
 * <li><strong>hot-deploy-watcher</strong>: Monitors the WatchService for file
 * events</li>
 * <li><strong>hot-deploy-sync</strong>: Periodically checks if redeployment is
 * needed</li>
 * </ul>
 *
 * <h2>Resource Management</h2>
 * <p>
 * Implements {@link AutoCloseable} and should be used in a try-with-resources
 * statement or explicitly closed when no longer needed.
 * </p>
 *
 * @author rajendarreddyj
 * @see DeployableConfiguration#isAutopublishEnabled()
 * @see DeployableConfiguration#getAutopublishInactivityLimit()
 * @since 1.0.0
 */
public class HotDeployWatcher implements AutoCloseable {

    /** The deployment configuration with source path and auto-publish settings. */
    private final DeployableConfiguration config;

    /** The deployer used for redeployment operations. */
    private final ExplodedWarDeployer deployer;

    /** The Maven logger for status messages. */
    private final Log log;

    /** Scheduler for periodic sync check tasks. */
    private final ScheduledExecutorService scheduler;

    /** Flag indicating whether the watcher is running. */
    private final AtomicBoolean running = new AtomicBoolean(false);

    /** Timestamp of the last detected file change. */
    private final AtomicLong lastChangeTime = new AtomicLong(0);
    /** The WatchService for monitoring file system events. */
    private WatchService watchService;

    /** Background thread that polls the WatchService. */
    private Thread watchThread;

    /** Scheduled task that checks for redeployment trigger. */
    private ScheduledFuture<?> syncTask;

    /**
     * Creates a new HotDeployWatcher.
     *
     * @param config   the deployment configuration containing source path and
     *                 auto-publish settings
     * @param deployer the deployer to use for redeployment operations
     * @param log      the Maven logger for status messages
     */
    public HotDeployWatcher(DeployableConfiguration config, ExplodedWarDeployer deployer, Log log) {
        this.config = config;
        this.deployer = deployer;
        this.log = log;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "hot-deploy-sync");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Starts watching for file changes.
     *
     * <p>
     * If auto-publish is disabled in the configuration, this method returns
     * immediately without starting any watchers.
     * </p>
     *
     * <p>
     * This method performs the following steps:
     * </p>
     * <ol>
     * <li>Creates a new WatchService</li>
     * <li>Recursively registers the source directory and all subdirectories</li>
     * <li>Starts the background watcher thread</li>
     * <li>Schedules the periodic sync check task</li>
     * </ol>
     *
     * @throws IOException if the WatchService cannot be created or directories
     *                     cannot be registered
     */
    public void start() throws IOException {
        if (!config.isAutopublishEnabled()) {
            log.info("Auto-publish is disabled");
            return;
        }

        running.set(true);
        watchService = FileSystems.getDefault().newWatchService();

        // Register source directory and subdirectories
        registerRecursive(config.getSourcePath());

        // Start watching thread
        watchThread = new Thread(this::watch, "hot-deploy-watcher");
        watchThread.setDaemon(true);
        watchThread.start();

        // Schedule periodic sync check
        int inactivitySeconds = config.getAutopublishInactivityLimit();
        syncTask = scheduler.scheduleAtFixedRate(
                this::checkAndSync,
                inactivitySeconds,
                inactivitySeconds,
                TimeUnit.SECONDS);

        log.info("Hot deployment enabled (inactivity limit: " + inactivitySeconds + "s)");
    }

    /**
     * Recursively registers a directory and all its subdirectories with the
     * WatchService.
     *
     * @param path the root path to register
     * @throws IOException if directory registration fails
     */
    private void registerRecursive(Path path) throws IOException {
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                    throws IOException {
                dir.register(watchService,
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_MODIFY,
                        StandardWatchEventKinds.ENTRY_DELETE);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Main watch loop that polls for file system events.
     *
     * <p>
     * This method runs in a separate thread and continuously polls the WatchService
     * for file events. When events are detected, it updates the last change time
     * and registers any newly created directories.
     * </p>
     */
    private void watch() {
        log.debug("Watch thread started for: " + config.getSourcePath());

        while (running.get()) {
            try {
                WatchKey key = watchService.poll(1, TimeUnit.SECONDS);
                if (key != null) {
                    for (WatchEvent<?> event : key.pollEvents()) {
                        @SuppressWarnings("unchecked")
                        WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                        Path changed = ((Path) key.watchable()).resolve(pathEvent.context());

                        log.debug("File changed: " + changed + " (" + event.kind() + ")");
                        lastChangeTime.set(System.currentTimeMillis());

                        // Register new directories
                        if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE
                                && Files.isDirectory(changed)) {
                            try {
                                registerRecursive(changed);
                            } catch (IOException e) {
                                log.warn("Could not watch new directory: " + changed);
                            }
                        }
                    }
                    key.reset();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (ClosedWatchServiceException e) {
                break;
            }
        }

        log.debug("Watch thread stopped");
    }

    /**
     * Checks if sufficient inactivity time has passed and triggers sync if needed.
     *
     * <p>
     * This method is called periodically by the scheduler. It compares the time
     * elapsed since the last file change against the configured inactivity limit.
     * If enough time has passed, it triggers a redeployment.
     * </p>
     */
    private void checkAndSync() {
        long lastChange = lastChangeTime.get();
        if (lastChange == 0) {
            return;
        }

        long inactivityMs = config.getAutopublishInactivityLimit() * 1000L;
        long elapsed = System.currentTimeMillis() - lastChange;

        if (elapsed >= inactivityMs) {
            lastChangeTime.set(0);
            performSync();
        }
    }

    /**
     * Performs the actual synchronization by redeploying the webapp.
     *
     * <p>
     * Invokes the deployer to copy changed files from the source directory
     * to the deployed webapp location.
     * </p>
     */
    private void performSync() {
        try {
            log.info("Auto-publishing changes...");
            deployer.redeploy(config);
            log.info("Auto-publish complete");
        } catch (IOException e) {
            log.error("Auto-publish failed: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        running.set(false);

        if (syncTask != null) {
            syncTask.cancel(false);
        }

        scheduler.shutdown();

        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                log.warn("Error closing watch service: " + e.getMessage());
            }
        }

        if (watchThread != null) {
            watchThread.interrupt();
        }
    }
}
