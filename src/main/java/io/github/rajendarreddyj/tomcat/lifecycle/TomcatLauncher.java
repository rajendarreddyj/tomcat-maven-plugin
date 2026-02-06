package io.github.rajendarreddyj.tomcat.lifecycle;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.maven.plugin.logging.Log;

import io.github.rajendarreddyj.tomcat.config.ServerConfiguration;

/**
 * Manages Tomcat process lifecycle.
 *
 * @author rajendarreddyj
 * @since 1.0.0
 */
public class TomcatLauncher {

    /** The server configuration containing Tomcat paths and settings. */
    private final ServerConfiguration config;

    /** The Maven logger for status and debug messages. */
    private final Log log;

    /** The underlying Tomcat process, null until started. */
    private Process tomcatProcess;

    /**
     * Creates a new TomcatLauncher with the given configuration.
     *
     * @param config the server configuration
     * @param log    the Maven logger
     */
    public TomcatLauncher(ServerConfiguration config, Log log) {
        this.config = config;
        this.log = log;
    }

    /**
     * Starts Tomcat in foreground mode (blocks until shutdown).
     *
     * @throws IOException          if process cannot be started
     * @throws InterruptedException if wait is interrupted
     */
    public void run() throws IOException, InterruptedException {
        tomcatProcess = startProcess("run");

        // Add shutdown hook for graceful termination
        Thread shutdownHook = new Thread(() -> {
            try {
                stop();
            } catch (Exception e) {
                log.error("Error during shutdown: " + e.getMessage());
            }
        });
        Runtime.getRuntime().addShutdownHook(shutdownHook);

        int exitCode = tomcatProcess.waitFor();
        if (exitCode != 0) {
            log.warn("Tomcat exited with code: " + exitCode);
        }

        // Remove shutdown hook if we get here normally
        try {
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
        } catch (IllegalStateException e) {
            // JVM is already shutting down, ignore
        }
    }

    /**
     * Starts Tomcat in background mode.
     *
     * @throws IOException if process cannot be started or startup fails
     */
    public void start() throws IOException {
        tomcatProcess = startProcess("start");
        waitForStartup();
    }

    /**
     * Stops running Tomcat instance.
     *
     * @throws IOException          if stop fails
     * @throws InterruptedException if wait is interrupted
     */
    public void stop() throws IOException, InterruptedException {
        if (tomcatProcess != null && tomcatProcess.isAlive()) {
            log.info("Stopping Tomcat process...");
            tomcatProcess.destroy();

            if (!tomcatProcess.waitFor(config.getShutdownTimeout(), TimeUnit.MILLISECONDS)) {
                log.warn("Tomcat did not stop gracefully, forcing termination...");
                tomcatProcess.destroyForcibly();
            }
        } else {
            // Try to stop via catalina script
            stopViaScript();
        }
    }

    /**
     * Starts the Tomcat process with the specified command.
     *
     * <p>
     * Creates a new process using the catalina script with the given command
     * (e.g., "run", "start"). Configures the process environment and inherits
     * I/O streams from the parent process.
     * </p>
     *
     * @param command the catalina command to execute ("run", "start", "stop")
     * @return the started Process instance
     * @throws IOException if the process cannot be started or the catalina script is not found
     */
    private Process startProcess(String command) throws IOException {
        Path catalinaScript = resolveCatalinaScript();

        ProcessBuilder pb = new ProcessBuilder();
        List<String> cmd = new ArrayList<>();

        if (isWindows()) {
            cmd.add("cmd.exe");
            cmd.add("/c");
            cmd.add(catalinaScript.toString());
        } else {
            cmd.add(catalinaScript.toString());
        }
        cmd.add(command);

        pb.command(cmd);
        configureEnvironment(pb.environment());
        pb.directory(config.getCatalinaHome().toFile());
        pb.inheritIO();

        log.info("Starting Tomcat with command: " + String.join(" ", cmd));
        log.info("CATALINA_HOME: " + config.getCatalinaHome());
        log.info("CATALINA_BASE: " + config.getCatalinaBase());
        log.info("HTTP Port: " + config.getHttpPort());

        return pb.start();
    }

    /**
     * Stops Tomcat using the catalina script.
     *
     * <p>
     * Invokes the catalina script with the "stop" command. This method is used
     * as a fallback when the Tomcat process reference is not available or has
     * already terminated. Respects the configured shutdown timeout.
     * </p>
     *
     * @throws IOException          if the catalina script cannot be executed
     * @throws InterruptedException if the stop command wait is interrupted
     */
    private void stopViaScript() throws IOException, InterruptedException {
        Path catalinaScript = resolveCatalinaScript();

        ProcessBuilder pb = new ProcessBuilder();
        List<String> cmd = new ArrayList<>();

        if (isWindows()) {
            cmd.add("cmd.exe");
            cmd.add("/c");
            cmd.add(catalinaScript.toString());
        } else {
            cmd.add(catalinaScript.toString());
        }
        cmd.add("stop");

        pb.command(cmd);
        configureEnvironment(pb.environment());
        pb.inheritIO();

        Process stopProcess = pb.start();
        boolean stopped = stopProcess.waitFor(config.getShutdownTimeout(), TimeUnit.MILLISECONDS);

        if (!stopped) {
            log.warn("Stop command timed out");
            stopProcess.destroyForcibly();
        }
    }

    /**
     * Resolves the path to the catalina script based on the current operating system.
     *
     * <p>
     * Returns the path to catalina.bat on Windows or catalina.sh on Unix-like
     * systems. Verifies that the script exists before returning.
     * </p>
     *
     * @return the path to the catalina script
     * @throws IOException if the catalina script is not found at the expected location
     */
    private Path resolveCatalinaScript() throws IOException {
        String scriptName = isWindows() ? "catalina.bat" : "catalina.sh";
        Path script = config.getCatalinaHome().resolve("bin").resolve(scriptName);

        if (!Files.exists(script)) {
            throw new IOException("Catalina script not found: " + script);
        }

        return script;
    }

    /**
     * Configures the process environment variables for the Tomcat process.
     *
     * <p>
     * Sets up the following environment variables:
     * </p>
     * <ul>
     * <li>CATALINA_HOME - the Tomcat installation directory</li>
     * <li>CATALINA_BASE - the Tomcat instance directory</li>
     * <li>JAVA_HOME - the Java installation directory (if configured)</li>
     * <li>CATALINA_OPTS - JVM options from configuration</li>
     * <li>CLASSPATH - additional classpath entries</li>
     * <li>Custom environment variables from configuration</li>
     * </ul>
     *
     * @param env the environment map to configure
     */
    private void configureEnvironment(Map<String, String> env) {
        // Core Tomcat environment
        env.put("CATALINA_HOME", config.getCatalinaHome().toString());
        env.put("CATALINA_BASE", config.getCatalinaBase().toString());

        if (config.getJavaHome() != null) {
            env.put("JAVA_HOME", config.getJavaHome().toString());
        }

        // VM options
        if (!config.getVmOptions().isEmpty()) {
            String existingOpts = env.getOrDefault("CATALINA_OPTS", "");
            String newOpts = String.join(" ", config.getVmOptions());
            env.put("CATALINA_OPTS", (existingOpts + " " + newOpts).trim());
        }

        // Classpath additions
        if (!config.getClasspathAdditions().isEmpty()) {
            String pathSeparator = File.pathSeparator;
            String additions = String.join(pathSeparator, config.getClasspathAdditions());
            String existing = env.getOrDefault("CLASSPATH", "");
            env.put("CLASSPATH", existing.isEmpty() ? additions : existing + pathSeparator + additions);
            log.debug("Added to CLASSPATH: " + additions);
        }

        // Custom environment variables
        env.putAll(config.getEnvironmentVariables());
    }

    /**
     * Waits for Tomcat to start and become ready to accept connections.
     *
     * <p>
     * Polls the configured HTTP port at one-second intervals until the server
     * accepts connections or the startup timeout is exceeded. Logs progress
     * and success/failure status.
     * </p>
     *
     * @throws IOException if the startup timeout is exceeded or the wait is interrupted
     */
    private void waitForStartup() throws IOException {
        log.info("Waiting for Tomcat to start (timeout: " + config.getStartupTimeout() + "ms)...");

        long startTime = System.currentTimeMillis();
        long timeout = config.getStartupTimeout();

        while (System.currentTimeMillis() - startTime < timeout) {
            if (isServerReady()) {
                log.info("Tomcat started successfully on port " + config.getHttpPort());
                return;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Startup wait interrupted", e);
            }
        }

        throw new IOException("Tomcat startup timed out after " + timeout + "ms");
    }

    /**
     * Checks if the server is ready by attempting to connect to the HTTP port.
     *
     * <p>
     * Opens a TCP connection to the configured HTTP host and port. A successful
     * connection indicates the server is ready to accept requests.
     * </p>
     *
     * @return {@code true} if the server accepts connections, {@code false} otherwise
     */
    private boolean isServerReady() {
        try (Socket socket = new Socket(config.getHttpHost(), config.getHttpPort())) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Checks if the current operating system is Windows.
     *
     * <p>
     * Used to determine which catalina script to execute (catalina.bat vs catalina.sh)
     * and how to construct the process command line.
     * </p>
     *
     * @return {@code true} if running on Windows, {@code false} otherwise
     */
    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }

    /**
     * Gets the underlying Tomcat process.
     *
     * @return the Tomcat process, or null if not started
     */
    public Process getProcess() {
        return tomcatProcess;
    }
}
