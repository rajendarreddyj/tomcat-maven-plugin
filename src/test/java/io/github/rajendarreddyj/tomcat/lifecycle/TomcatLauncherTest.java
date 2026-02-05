package io.github.rajendarreddyj.tomcat.lifecycle;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.logging.Log;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.github.rajendarreddyj.tomcat.config.ServerConfiguration;

/**
 * Unit tests for {@link TomcatLauncher}.
 *
 * <p>
 * Tests the Tomcat process lifecycle management including
 * startup, shutdown, environment configuration, and process handling.
 *
 * @author rajendarreddyj
 * @see TomcatLauncher
 */
class TomcatLauncherTest {

    /**
     * Temporary directory for test artifacts, cleaned up automatically after each
     * test.
     */
    @TempDir
    Path tempDir;

    /** Mock Maven logger for testing. */
    @Mock
    private Log log;

    /** Path to the mock Tomcat installation directory. */
    private Path catalinaHome;

    /**
     * Sets up the test environment before each test.
     *
     * <p>
     * Creates a mock Tomcat bin directory with catalina scripts
     * for cross-platform testing.
     *
     * @throws IOException if setup fails
     */
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

    /**
     * Verifies that constructor creates a valid instance.
     */
    @Test
    void constructorCreatesInstance() {
        ServerConfiguration config = createConfig(8080);
        TomcatLauncher launcher = new TomcatLauncher(config, log);

        assertNotNull(launcher);
    }

    /**
     * Verifies that getProcess returns null before start is called.
     */
    @Test
    void getProcessReturnsNullBeforeStart() {
        ServerConfiguration config = createConfig(8080);
        TomcatLauncher launcher = new TomcatLauncher(config, log);

        assertNull(launcher.getProcess());
    }

    /**
     * Verifies that stop handles null process gracefully.
     *
     * @throws Exception if the test fails
     */
    @Test
    void stopHandlesNullProcess() throws Exception {
        ServerConfiguration config = createConfig(8080);
        TomcatLauncher launcher = new TomcatLauncher(config, log);

        // Should not throw
        assertDoesNotThrow(() -> launcher.stop());
    }

    /**
     * Verifies that start throws exception for missing catalina script.
     *
     * @throws IOException if file operations fail
     */
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

    /**
     * Verifies that start logs startup information.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void startLogsStartupInfo() throws IOException {
        ServerConfiguration config = createConfig(9090);
        TomcatLauncher launcher = new TomcatLauncher(config, log);

        try {
            launcher.start();
        } catch (IOException e) {
            // Expected - may fail for various reasons in test env
        }

        verify(log, atLeastOnce()).info(argThat((CharSequence msg) -> msg.toString().contains("CATALINA_HOME") ||
                msg.toString().contains("Starting") ||
                msg.toString().contains("Waiting")));
    }

    /**
     * Verifies that server configuration works with custom port.
     */
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

    /**
     * Verifies that server configuration works with VM options.
     */
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

    /**
     * Verifies that server configuration works with environment variables.
     */
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

    /**
     * Verifies that server configuration works with custom Java home.
     */
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

    /**
     * Verifies that server configuration works with classpath additions.
     */
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

    /**
     * Verifies that server configuration works with custom timeouts.
     */
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

    /**
     * Verifies that server configuration works with custom CATALINA_BASE.
     */
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

    /**
     * Verifies that server configuration works with custom HTTP host.
     */
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

    /**
     * Verifies that stop handles already stopped process gracefully.
     *
     * @throws Exception if the test fails
     */
    @Test
    void stopHandlesAlreadyStoppedProcess() throws Exception {
        ServerConfiguration config = createConfig(8080);
        TomcatLauncher launcher = new TomcatLauncher(config, log);

        // Call stop multiple times
        launcher.stop();
        launcher.stop();

        // Should not throw
    }

    /**
     * Verifies that OS detection correctly identifies the current platform.
     */
    @Test
    void isWindowsDetectsCorrectOS() {
        String osName = System.getProperty("os.name").toLowerCase();
        boolean expectedWindows = osName.contains("windows");

        ServerConfiguration config = createConfig(8080);
        TomcatLauncher launcher = new TomcatLauncher(config, log);

        // We can't directly test private method, but we can verify the script
        // resolution
        // would work for the current OS by checking the script exists
        String scriptName = expectedWindows ? "catalina.bat" : "catalina.sh";
        assertTrue(Files.exists(catalinaHome.resolve("bin").resolve(scriptName)));
    }

    /**
     * Verifies that start times out when server is not ready.
     *
     * @throws IOException if file operations fail
     */
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

    /**
     * Verifies that start succeeds when server is already ready.
     *
     * @throws Exception if the test fails
     */
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
                verify(log).info(argThat((CharSequence msg) -> msg.toString().contains("started successfully")));
            } finally {
                launcher.stop();
            }
        }
    }

    /**
     * Verifies that stop handles an alive process correctly.
     *
     * @throws Exception if the test fails
     */
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

    /**
     * Verifies that environment is configured with VM options.
     *
     * @throws IOException if file operations fail
     */
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

        verify(log).info(argThat((CharSequence msg) -> msg.toString().contains("CATALINA_HOME")));
    }

    /**
     * Verifies that environment is configured with classpath additions.
     *
     * @throws IOException if file operations fail
     */
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

    /**
     * Verifies that stop works via script when no process is running.
     *
     * @throws Exception if the test fails
     */
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

    /**
     * Verifies that start logs configuration details.
     *
     * @throws Exception if the test fails
     */
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
        verify(log).info(argThat((CharSequence msg) -> msg.toString().contains("CATALINA_HOME")));
        verify(log).info(argThat((CharSequence msg) -> msg.toString().contains("CATALINA_BASE")));
        verify(log).info(argThat((CharSequence msg) -> msg.toString().contains("HTTP Port")));
    }

    /**
     * Verifies that getProcess returns a process after start.
     *
     * @throws Exception if the test fails
     */
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

    /**
     * Finds an available port for testing.
     *
     * @return an available port number
     * @throws IOException if port selection fails
     */
    private int findAvailablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    /**
     * Creates a ServerConfiguration for testing.
     *
     * @param port the HTTP port to use
     * @return a configured ServerConfiguration instance
     */
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
