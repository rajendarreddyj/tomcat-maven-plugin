package io.github.rajendarreddyj.tomcat.lifecycle;

import io.github.rajendarreddyj.tomcat.config.ServerConfiguration;
import org.apache.maven.plugin.logging.Log;

import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Manages Tomcat process lifecycle.
 */
public class TomcatLauncher {

    private final ServerConfiguration config;
    private final Log log;
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
     * Starts the Tomcat process with the given command.
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
     * Resolves the path to the catalina script.
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
     * Configures the process environment variables.
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
            String pathSeparator = System.getProperty("path.separator");
            String additions = String.join(pathSeparator, config.getClasspathAdditions());
            String existing = env.getOrDefault("CLASSPATH", "");
            env.put("CLASSPATH", existing.isEmpty() ? additions : existing + pathSeparator + additions);
            log.debug("Added to CLASSPATH: " + additions);
        }

        // Custom environment variables
        env.putAll(config.getEnvironmentVariables());
    }

    /**
     * Waits for Tomcat to start and become ready.
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
     */
    private boolean isServerReady() {
        try (Socket socket = new Socket(config.getHttpHost(), config.getHttpPort())) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Checks if the current OS is Windows.
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
