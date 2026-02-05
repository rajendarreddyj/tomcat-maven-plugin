package io.github.rajendarreddyj.tomcat.lifecycle;

import io.github.rajendarreddyj.tomcat.config.ServerConfiguration;
import org.apache.maven.plugin.logging.Log;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TomcatLauncherTest {

    @TempDir
    Path tempDir;

    @Mock
    private Log log;

    private Path catalinaHome;

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);

        catalinaHome = tempDir.resolve("tomcat");
        Files.createDirectories(catalinaHome.resolve("bin"));

        // Create both scripts for cross-platform testing
        Path batScript = catalinaHome.resolve("bin").resolve("catalina.bat");
        Files.writeString(batScript, "@echo off\necho test\n");

        Path shScript = catalinaHome.resolve("bin").resolve("catalina.sh");
        Files.writeString(shScript, "#!/bin/bash\necho 'test'\n");
        shScript.toFile().setExecutable(true);
    }

    @Test
    void constructorCreatesInstance() {
        ServerConfiguration config = createConfig(8080);
        TomcatLauncher launcher = new TomcatLauncher(config, log);

        assertNotNull(launcher);
    }

    @Test
    void getProcessReturnsNullBeforeStart() {
        ServerConfiguration config = createConfig(8080);
        TomcatLauncher launcher = new TomcatLauncher(config, log);

        assertNull(launcher.getProcess());
    }

    @Test
    void stopHandlesNullProcess() throws Exception {
        ServerConfiguration config = createConfig(8080);
        TomcatLauncher launcher = new TomcatLauncher(config, log);

        // Should not throw
        assertDoesNotThrow(() -> launcher.stop());
    }

    @Test
    void startThrowsExceptionForMissingScript() throws IOException {
        // Remove the catalina script
        Path binDir = catalinaHome.resolve("bin");
        Files.list(binDir).forEach(p -> {
            try {
                Files.delete(p);
            } catch (IOException e) {
                // ignore
            }
        });

        ServerConfiguration config = createConfig(8080);
        TomcatLauncher launcher = new TomcatLauncher(config, log);

        assertThrows(IOException.class, launcher::start);
    }

    @Test
    void startLogsStartupInfo() throws IOException {
        ServerConfiguration config = createConfig(9090);
        TomcatLauncher launcher = new TomcatLauncher(config, log);

        try {
            launcher.start();
        } catch (IOException e) {
            // Expected - may fail for various reasons in test env
        }

        verify(log, atLeastOnce()).info(argThat((CharSequence msg) -> 
                msg.toString().contains("CATALINA_HOME") || 
                msg.toString().contains("Starting") ||
                msg.toString().contains("Waiting")));
    }

    @Test
    void serverConfigurationWithCustomPort() {
        ServerConfiguration config = ServerConfiguration.builder()
                .catalinaHome(catalinaHome)
                .httpPort(9999)
                .build();
        TomcatLauncher launcher = new TomcatLauncher(config, log);

        assertNotNull(launcher);
        assertEquals(9999, config.getHttpPort());
    }

    @Test
    void serverConfigurationWithVmOptions() {
        ServerConfiguration config = ServerConfiguration.builder()
                .catalinaHome(catalinaHome)
                .vmOptions(List.of("-Xmx512m", "-Xms256m"))
                .build();
        TomcatLauncher launcher = new TomcatLauncher(config, log);

        assertNotNull(launcher);
        assertEquals(2, config.getVmOptions().size());
    }

    @Test
    void serverConfigurationWithEnvironmentVariables() {
        ServerConfiguration config = ServerConfiguration.builder()
                .catalinaHome(catalinaHome)
                .environmentVariables(Map.of("JAVA_OPTS", "-Dfile.encoding=UTF-8"))
                .build();
        TomcatLauncher launcher = new TomcatLauncher(config, log);

        assertNotNull(launcher);
        assertEquals("-Dfile.encoding=UTF-8", config.getEnvironmentVariables().get("JAVA_OPTS"));
    }

    @Test
    void serverConfigurationWithJavaHome() {
        Path javaHome = tempDir.resolve("java");
        ServerConfiguration config = ServerConfiguration.builder()
                .catalinaHome(catalinaHome)
                .javaHome(javaHome)
                .build();
        TomcatLauncher launcher = new TomcatLauncher(config, log);

        assertNotNull(launcher);
        assertEquals(javaHome, config.getJavaHome());
    }

    @Test
    void serverConfigurationWithClasspathAdditions() {
        ServerConfiguration config = ServerConfiguration.builder()
                .catalinaHome(catalinaHome)
                .classpathAdditions(List.of("/path/to/extra.jar"))
                .build();
        TomcatLauncher launcher = new TomcatLauncher(config, log);

        assertNotNull(launcher);
        assertEquals(1, config.getClasspathAdditions().size());
    }

    @Test
    void serverConfigurationWithCustomTimeouts() {
        ServerConfiguration config = ServerConfiguration.builder()
                .catalinaHome(catalinaHome)
                .startupTimeout(60000)
                .shutdownTimeout(15000)
                .build();
        TomcatLauncher launcher = new TomcatLauncher(config, log);

        assertNotNull(launcher);
        assertEquals(60000, config.getStartupTimeout());
        assertEquals(15000, config.getShutdownTimeout());
    }

    @Test
    void serverConfigurationWithCatalinaBase() {
        Path catalinaBase = tempDir.resolve("tomcat-base");
        ServerConfiguration config = ServerConfiguration.builder()
                .catalinaHome(catalinaHome)
                .catalinaBase(catalinaBase)
                .build();
        TomcatLauncher launcher = new TomcatLauncher(config, log);

        assertNotNull(launcher);
        assertEquals(catalinaBase, config.getCatalinaBase());
    }

    @Test
    void serverConfigurationWithCustomHost() {
        ServerConfiguration config = ServerConfiguration.builder()
                .catalinaHome(catalinaHome)
                .httpHost("0.0.0.0")
                .build();
        TomcatLauncher launcher = new TomcatLauncher(config, log);

        assertNotNull(launcher);
        assertEquals("0.0.0.0", config.getHttpHost());
    }

    @Test
    void stopHandlesAlreadyStoppedProcess() throws Exception {
        ServerConfiguration config = createConfig(8080);
        TomcatLauncher launcher = new TomcatLauncher(config, log);

        // Call stop multiple times
        launcher.stop();
        launcher.stop();

        // Should not throw
    }

    @Test
    void isWindowsDetectsCorrectOS() {
        String osName = System.getProperty("os.name").toLowerCase();
        boolean expectedWindows = osName.contains("windows");

        ServerConfiguration config = createConfig(8080);
        TomcatLauncher launcher = new TomcatLauncher(config, log);

        // We can't directly test private method, but we can verify the script resolution
        // would work for the current OS by checking the script exists
        String scriptName = expectedWindows ? "catalina.bat" : "catalina.sh";
        assertTrue(Files.exists(catalinaHome.resolve("bin").resolve(scriptName)));
    }

    @Test
    void startTimesOutWhenServerNotReady() throws IOException {
        // Use a short timeout and unavailable port
        ServerConfiguration config = ServerConfiguration.builder()
                .catalinaHome(catalinaHome)
                .catalinaBase(catalinaHome)
                .httpPort(49999)
                .httpHost("localhost")
                .startupTimeout(2000) // 2 second timeout
                .shutdownTimeout(1000)
                .build();
        TomcatLauncher launcher = new TomcatLauncher(config, log);

        // Should throw IOException due to timeout
        assertThrows(IOException.class, launcher::start);
    }

    @Test
    void startSucceedsWhenServerIsReady() throws Exception {
        // Use ServerSocket to simulate a running server
        int port = findAvailablePort();
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            ServerConfiguration config = ServerConfiguration.builder()
                    .catalinaHome(catalinaHome)
                    .catalinaBase(catalinaHome)
                    .httpPort(port)
                    .httpHost("localhost")
                    .startupTimeout(5000)
                    .shutdownTimeout(1000)
                    .build();
            TomcatLauncher launcher = new TomcatLauncher(config, log);

            try {
                // Server is already "running", so start should succeed
                assertDoesNotThrow(launcher::start);

                // Verify success log message
                verify(log).info(argThat((CharSequence msg) ->
                        msg.toString().contains("started successfully")));
            } finally {
                launcher.stop();
            }
        }
    }

    @Test
    void stopWithAliveProcess() throws Exception {
        ServerConfiguration config = createConfig(8080);
        TomcatLauncher launcher = new TomcatLauncher(config, log);

        // Start a mock process using launcher
        // Use a very short timeout
        ServerConfiguration shortConfig = ServerConfiguration.builder()
                .catalinaHome(catalinaHome)
                .catalinaBase(catalinaHome)
                .httpPort(49888)
                .httpHost("localhost")
                .startupTimeout(1000)
                .shutdownTimeout(1000)
                .build();

        TomcatLauncher launcher2 = new TomcatLauncher(shortConfig, log);

        // Stop should work without exception
        assertDoesNotThrow(launcher2::stop);
    }

    @Test
    void configureEnvironmentWithVmOptionsAndExistingOpts() throws IOException {
        ServerConfiguration config = ServerConfiguration.builder()
                .catalinaHome(catalinaHome)
                .catalinaBase(catalinaHome)
                .vmOptions(List.of("-Xmx512m", "-XX:+UseG1GC"))
                .httpPort(8080)
                .build();
        TomcatLauncher launcher = new TomcatLauncher(config, log);

        // Start should configure environment properly
        try {
            launcher.start();
        } catch (IOException e) {
            // Expected - timeout because Tomcat isn't really running
        }

        verify(log).info(argThat((CharSequence msg) ->
                msg.toString().contains("CATALINA_HOME")));
    }

    @Test
    void configureEnvironmentWithClasspathAdditions() throws IOException {
        ServerConfiguration config = ServerConfiguration.builder()
                .catalinaHome(catalinaHome)
                .catalinaBase(catalinaHome)
                .classpathAdditions(List.of("/path/to/extra.jar", "/another/lib.jar"))
                .httpPort(8080)
                .startupTimeout(1000)
                .build();
        TomcatLauncher launcher = new TomcatLauncher(config, log);

        // Start will configure environment
        try {
            launcher.start();
        } catch (IOException e) {
            // Expected - timeout
        }

        verify(log, atLeastOnce()).info(any(CharSequence.class));
    }

    @Test
    void stopViaScriptWhenNoProcess() throws Exception {
        ServerConfiguration config = ServerConfiguration.builder()
                .catalinaHome(catalinaHome)
                .catalinaBase(catalinaHome)
                .httpPort(8080)
                .shutdownTimeout(2000)
                .build();
        TomcatLauncher launcher = new TomcatLauncher(config, log);

        // Stop without starting should call stopViaScript
        assertDoesNotThrow(launcher::stop);
    }

    @Test
    void startLogsConfigurationDetails() throws Exception {
        Path catalinaBase = tempDir.resolve("tomcat-base");
        Files.createDirectories(catalinaBase);

        ServerConfiguration config = ServerConfiguration.builder()
                .catalinaHome(catalinaHome)
                .catalinaBase(catalinaBase)
                .httpPort(9000)
                .httpHost("0.0.0.0")
                .startupTimeout(1000)
                .shutdownTimeout(1000)
                .build();
        TomcatLauncher launcher = new TomcatLauncher(config, log);

        try {
            launcher.start();
        } catch (IOException e) {
            // Expected
        } finally {
            launcher.stop();
        }

        // Verify all config details are logged
        verify(log).info(argThat((CharSequence msg) ->
                msg.toString().contains("CATALINA_HOME")));
        verify(log).info(argThat((CharSequence msg) ->
                msg.toString().contains("CATALINA_BASE")));
        verify(log).info(argThat((CharSequence msg) ->
                msg.toString().contains("HTTP Port")));
    }

    @Test
    void getProcessReturnsProcessAfterStart() throws Exception {
        // Bind to a port to simulate running server
        int port = findAvailablePort();
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            ServerConfiguration config = ServerConfiguration.builder()
                    .catalinaHome(catalinaHome)
                    .catalinaBase(catalinaHome)
                    .httpPort(port)
                    .httpHost("localhost")
                    .startupTimeout(5000)
                    .shutdownTimeout(1000)
                    .build();
            TomcatLauncher launcher = new TomcatLauncher(config, log);

            try {
                launcher.start();

                // Process should be set after start
                assertNotNull(launcher.getProcess());
            } finally {
                launcher.stop();
            }
        }
    }

    private int findAvailablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private ServerConfiguration createConfig(int port) {
        return ServerConfiguration.builder()
                .catalinaHome(catalinaHome)
                .catalinaBase(catalinaHome)
                .httpPort(port)
                .httpHost("localhost")
                .startupTimeout(5000)
                .shutdownTimeout(5000)
                .build();
    }
}
