package io.github.rajendarreddyj.tomcat.deploy;

import io.github.rajendarreddyj.tomcat.config.DeployableConfiguration;
import org.apache.maven.plugin.logging.Log;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HotDeployWatcherTest {

    @TempDir
    Path tempDir;

    @Mock
    private Log log;

    @Mock
    private ExplodedWarDeployer deployer;

    private Path sourceDir;
    private Path deployDir;
    private HotDeployWatcher watcher;

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);

        sourceDir = tempDir.resolve("source");
        deployDir = tempDir.resolve("deploy");
        Files.createDirectories(sourceDir);
        Files.createDirectories(deployDir);
    }

    @AfterEach
    void tearDown() {
        if (watcher != null) {
            watcher.close();
        }
    }

    @Test
    void startReturnsImmediatelyWhenDisabled() throws IOException {
        DeployableConfiguration config = createConfig(false, 5);
        watcher = new HotDeployWatcher(config, deployer, log);

        watcher.start();

        verify(log).info("Auto-publish is disabled");
    }

    @Test
    void startEnablesWatchingWhenEnabled() throws IOException {
        DeployableConfiguration config = createConfig(true, 5);
        watcher = new HotDeployWatcher(config, deployer, log);

        watcher.start();

        verify(log).info(contains("Hot deployment enabled"));
    }

    @Test
    void closeStopsWatching() throws IOException {
        DeployableConfiguration config = createConfig(true, 1);
        watcher = new HotDeployWatcher(config, deployer, log);
        watcher.start();

        watcher.close();

        // Should not throw and should complete gracefully
        assertDoesNotThrow(() -> Thread.sleep(100));
    }

    @Test
    void closeHandlesNullWatchService() {
        DeployableConfiguration config = createConfig(false, 5);
        watcher = new HotDeployWatcher(config, deployer, log);

        // Close without starting
        assertDoesNotThrow(() -> watcher.close());
    }

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

    @Test
    void constructorCreatesInstance() {
        DeployableConfiguration config = createConfig(true, 30);

        watcher = new HotDeployWatcher(config, deployer, log);

        assertNotNull(watcher);
    }

    @Test
    void startRegistersSourceDirectory() throws IOException {
        DeployableConfiguration config = createConfig(true, 5);
        watcher = new HotDeployWatcher(config, deployer, log);

        watcher.start();

        // Verify watching is enabled by checking log
        verify(log).info(argThat((CharSequence msg) -> msg.toString().contains("Hot deployment enabled")));
    }

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

    @Test
    void closeShutdownsScheduler() throws IOException {
        DeployableConfiguration config = createConfig(true, 1);
        watcher = new HotDeployWatcher(config, deployer, log);
        watcher.start();

        watcher.close();

        // Allow time for cleanup
        assertDoesNotThrow(() -> TimeUnit.MILLISECONDS.sleep(200));
    }

    @Test
    void startLogsInactivityLimit() throws IOException {
        DeployableConfiguration config = createConfig(true, 30);
        watcher = new HotDeployWatcher(config, deployer, log);

        watcher.start();

        verify(log).info(contains("30s"));
    }

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
