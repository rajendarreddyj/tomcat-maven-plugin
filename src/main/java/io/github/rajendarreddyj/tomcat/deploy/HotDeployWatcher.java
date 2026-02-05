package io.github.rajendarreddyj.tomcat.deploy;

import io.github.rajendarreddyj.tomcat.config.DeployableConfiguration;
import org.apache.maven.plugin.logging.Log;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Watches for file changes and triggers hot deployment after inactivity period.
 */
public class HotDeployWatcher implements AutoCloseable {

    private final DeployableConfiguration config;
    private final ExplodedWarDeployer deployer;
    private final Log log;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicLong lastChangeTime = new AtomicLong(0);
    private WatchService watchService;
    private Thread watchThread;
    private ScheduledFuture<?> syncTask;

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
                TimeUnit.SECONDS
        );

        log.info("Hot deployment enabled (inactivity limit: " + inactivitySeconds + "s)");
    }

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
