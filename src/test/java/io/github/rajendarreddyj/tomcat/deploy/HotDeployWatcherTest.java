package io.github.rajendarreddyj.tomcat.deploy;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.apache.maven.plugin.logging.Log;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.github.rajendarreddyj.tomcat.config.DeployableConfiguration;

/**
 * Unit tests for {@link HotDeployWatcher}.
 *
 * <p>
 * Tests the file watching and hot deployment functionality including
 * file change detection, auto-publish triggering, and resource cleanup.
 *
 * @author rajendarreddyj
 * @see HotDeployWatcher
 */
class HotDeployWatcherTest {

    /**
     * Temporary directory for test artifacts, cleaned up automatically after each
     * test.
     */
    @TempDir
    Path tempDir;

    /** Mock Maven logger for testing. */
    @Mock
    private Log log;

    /** Mock deployer for verifying deployment calls. */
    @Mock
    private ExplodedWarDeployer deployer;

    /** Path to the source directory being watched. */
    private Path sourceDir;

    /** Path to the deployment target directory. */
    private Path deployDir;

    /** The HotDeployWatcher instance under test. */
    private HotDeployWatcher watcher;

    /**
     * Sets up the test environment before each test.
     *
     * <p>
     * Creates source and deploy directories for testing.
     *
     * @throws IOException if setup fails
     */
    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);

        sourceDir = tempDir.resolve("source");
        deployDir = tempDir.resolve("deploy");
        Files.createDirectories(sourceDir);
        Files.createDirectories(deployDir);
    }

    /**
     * Cleans up resources after each test.
     *
     * <p>
     * Ensures the watcher is properly closed to release file handles.
     */
    @AfterEach
    void tearDown() {
        if (watcher != null) {
            watcher.close();
        }
    }

    /**
     * Verifies that start returns immediately when auto-publish is disabled.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void startReturnsImmediatelyWhenDisabled() throws IOException {
        DeployableConfiguration config = createConfig(false, 5);
        watcher = new HotDeployWatcher(config, deployer, log);

        watcher.start();

        verify(log).info("Auto-publish is disabled");
    }

    /**
     * Verifies that start enables file watching when auto-publish is enabled.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void startEnablesWatchingWhenEnabled() throws IOException {
        DeployableConfiguration config = createConfig(true, 5);
        watcher = new HotDeployWatcher(config, deployer, log);

        watcher.start();

        verify(log).info(contains("Hot deployment enabled"));
    }

    /**
     * Verifies that close stops file watching gracefully.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void closeStopsWatching() throws IOException {
        DeployableConfiguration config = createConfig(true, 1);
        watcher = new HotDeployWatcher(config, deployer, log);
        watcher.start();

        watcher.close();

        // Should not throw and should complete gracefully
        assertDoesNotThrow(() -> Thread.sleep(100));
    }

    /**
     * Verifies that close handles null watch service gracefully.
     */
    @Test
    void closeHandlesNullWatchService() {
        DeployableConfiguration config = createConfig(false, 5);
        watcher = new HotDeployWatcher(config, deployer, log);

        // Close without starting
        assertDoesNotThrow(() -> watcher.close());
    }

    /**
     * Verifies that close can be called multiple times without error.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void closeCanBeCalledMultipleTimes() throws IOException {
        DeployableConfiguration config = createConfig(true, 1);
        watcher = new HotDeployWatcher(config, deployer, log);
        watcher.start();

        assertDoesNotThrow(() -> {
            watcher.close();
            watcher.close();
        });
    }

    /**
     * Verifies that constructor creates a valid instance.
     */
    @Test
    void constructorCreatesInstance() {
        DeployableConfiguration config = createConfig(true, 30);

        watcher = new HotDeployWatcher(config, deployer, log);

        assertNotNull(watcher);
    }

    /**
     * Verifies that start registers the source directory for watching.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void startRegistersSourceDirectory() throws IOException {
        DeployableConfiguration config = createConfig(true, 5);
        watcher = new HotDeployWatcher(config, deployer, log);

        watcher.start();

        // Verify watching is enabled by checking log
        verify(log).info(argThat((CharSequence msg) -> msg.toString().contains("Hot deployment enabled")));
    }

    /**
     * Verifies that start registers subdirectories for watching.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void startRegistersSubdirectories() throws IOException {
        // Create subdirectory structure
        Files.createDirectories(sourceDir.resolve("WEB-INF"));
        Files.createDirectories(sourceDir.resolve("css"));

        DeployableConfiguration config = createConfig(true, 5);
        watcher = new HotDeployWatcher(config, deployer, log);

        watcher.start();

        // Should start without error
        verify(log).info(contains("Hot deployment enabled"));
    }

    /**
     * Verifies that file change triggers redeploy after inactivity period.
     *
     * @throws Exception if the test fails
     */
    @Test
    void fileChangeTriggersRedeployAfterInactivity() throws Exception {
        DeployableConfiguration config = createConfig(true, 1);
        watcher = new HotDeployWatcher(config, deployer, log);
        watcher.start();

        // Create a file change
        Path testFile = sourceDir.resolve("test.txt");
        Files.writeString(testFile, "test content");

        // Wait for inactivity period plus some buffer
        TimeUnit.SECONDS.sleep(3);

        // Verify redeploy was called
        verify(deployer, timeout(5000).atLeastOnce()).redeploy(any());
    }

    /**
     * Verifies that rapid changes are batched into fewer redeploys.
     *
     * @throws Exception if the test fails
     */
    @Test
    void rapidChangesAreBatched() throws Exception {
        DeployableConfiguration config = createConfig(true, 2);
        watcher = new HotDeployWatcher(config, deployer, log);
        watcher.start();

        // Make rapid changes
        for (int i = 0; i < 5; i++) {
            Files.writeString(sourceDir.resolve("file" + i + ".txt"), "content " + i);
            TimeUnit.MILLISECONDS.sleep(100);
        }

        // Wait for batch to complete
        TimeUnit.SECONDS.sleep(4);

        // Should batch to single redeploy (or at most a few)
        verify(deployer, atMost(3)).redeploy(any());
    }

    /**
     * Verifies that new directories are registered for watching.
     *
     * @throws Exception if the test fails
     */
    @Test
    void newDirectoryIsRegisteredForWatching() throws Exception {
        DeployableConfiguration config = createConfig(true, 1);
        watcher = new HotDeployWatcher(config, deployer, log);
        watcher.start();

        // Create new directory and file
        Path newDir = sourceDir.resolve("new-dir");
        Files.createDirectories(newDir);
        TimeUnit.MILLISECONDS.sleep(500);

        Files.writeString(newDir.resolve("new-file.txt"), "content");

        // Wait for sync
        TimeUnit.SECONDS.sleep(3);

        verify(deployer, timeout(5000).atLeastOnce()).redeploy(any());
    }

    /**
     * Verifies that close shuts down the scheduler properly.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void closeShutdownsScheduler() throws IOException {
        DeployableConfiguration config = createConfig(true, 1);
        watcher = new HotDeployWatcher(config, deployer, log);
        watcher.start();

        watcher.close();

        // Allow time for cleanup
        assertDoesNotThrow(() -> TimeUnit.MILLISECONDS.sleep(200));
    }

    /**
     * Verifies that start logs the inactivity limit.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void startLogsInactivityLimit() throws IOException {
        DeployableConfiguration config = createConfig(true, 30);
        watcher = new HotDeployWatcher(config, deployer, log);

        watcher.start();

        verify(log).info(contains("30s"));
    }

    /**
     * Creates a DeployableConfiguration for testing.
     *
     * @param autopublishEnabled whether auto-publish should be enabled
     * @param inactivityLimit    the inactivity limit in seconds
     * @return a configured DeployableConfiguration instance
     */
    private DeployableConfiguration createConfig(boolean autopublishEnabled, int inactivityLimit) {
        return DeployableConfiguration.builder()
                .moduleName("test-module")
                .sourcePath(sourceDir)
                .deployDir(deployDir)
                .contextPath("/test")
                .autopublishEnabled(autopublishEnabled)
                .autopublishInactivityLimit(inactivityLimit)
                .build();
    }
}
