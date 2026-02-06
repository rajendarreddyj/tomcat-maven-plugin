package io.github.rajendarreddyj.tomcat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import io.github.rajendarreddyj.tomcat.config.ServerConfiguration;

/**
 * Stops a running Apache Tomcat instance started by the start goal.
 *
 * <p>
 * This goal attempts to stop Tomcat gracefully using the following strategy:
 * </p>
 * <ol>
 * <li>If a PID file exists in CATALINA_BASE, terminates the process
 * directly</li>
 * <li>If no PID file is found, attempts to stop via the catalina script</li>
 * <li>If graceful shutdown times out, forcibly terminates the process</li>
 * </ol>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code mvn tomcat:stop}</pre>
 *
 * <h2>Configuration</h2>
 * <p>
 * The shutdown timeout can be configured using the
 * {@code tomcat.timeout.shutdown}
 * property (default: 30 seconds).
 * </p>
 *
 * @author rajendarreddyj
 * @see StartMojo for starting Tomcat in background mode
 * @see RunMojo for starting Tomcat in foreground mode
 * @since 1.0.0
 */
@Mojo(name = "stop", defaultPhase = LifecyclePhase.POST_INTEGRATION_TEST, threadSafe = true)
public class StopMojo extends AbstractTomcatMojo {

    /**
     * Executes the stop goal.
     *
     * <p>
     * This method performs the following steps:
     * </p>
     * <ol>
     * <li>Builds the server configuration to locate CATALINA_BASE</li>
     * <li>Checks for a PID file in CATALINA_BASE</li>
     * <li>If PID file exists, terminates the process by PID</li>
     * <li>If no PID file exists, attempts to stop via catalina script</li>
     * <li>Cleans up the PID file after successful stop</li>
     * </ol>
     *
     * @throws MojoExecutionException if an error occurs during execution
     * @throws MojoFailureException   if execution fails due to invalid
     *                                configuration
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            ServerConfiguration serverConfig = buildServerConfiguration();
            Path pidFile = serverConfig.getCatalinaBase().resolve("tomcat.pid");

            if (Files.exists(pidFile)) {
                long pid = Long.parseLong(Files.readString(pidFile).trim());
                stopProcess(pid);
                Files.deleteIfExists(pidFile);
            } else {
                getLog().warn("No PID file found at " + pidFile +
                        ". Attempting to stop via script...");
                stopViaScript(serverConfig);
            }

            getLog().info("Tomcat stopped successfully");

        } catch (Exception e) {
            throw new MojoExecutionException("Failed to stop Tomcat: " + e.getMessage(), e);
        }
    }

    /**
     * Stops the Tomcat process by PID.
     *
     * <p>
     * Attempts graceful shutdown first using {@link ProcessHandle#destroy()}.
     * If the process doesn't terminate within the configured shutdown timeout,
     * it will be forcibly terminated using {@link ProcessHandle#destroyForcibly()}.
     * </p>
     *
     * @param pid the process ID of the Tomcat process to stop
     */
    private void stopProcess(long pid) {
        ProcessHandle.of(pid).ifPresentOrElse(
                handle -> {
                    getLog().info("Stopping Tomcat process (PID: " + pid + ")");
                    handle.destroy();
                    try {
                        handle.onExit().get(shutdownTimeout, TimeUnit.MILLISECONDS);
                    } catch (Exception e) {
                        getLog().warn("Graceful shutdown timed out, forcing termination...");
                        handle.destroyForcibly();
                    }
                },
                () -> getLog().warn("Process " + pid + " not found, may have already stopped"));
    }

    /**
     * Stops Tomcat using the catalina script.
     *
     * <p>
     * This is a fallback method used when no PID file is available.
     * It invokes the catalina.sh (Unix) or catalina.bat (Windows) script
     * with the "stop" command.
     * </p>
     *
     * @param config the server configuration containing CATALINA_HOME and
     *               CATALINA_BASE paths
     * @throws IOException          if the catalina script cannot be executed
     * @throws InterruptedException if the stop command wait is interrupted
     */
    private void stopViaScript(ServerConfiguration config) throws IOException, InterruptedException {
        String scriptName = isWindows() ? "catalina.bat" : "catalina.sh";
        Path script = config.getCatalinaHome().resolve("bin").resolve(scriptName);

        ProcessBuilder pb = new ProcessBuilder();
        if (isWindows()) {
            pb.command("cmd.exe", "/c", script.toString(), "stop");
        } else {
            pb.command(script.toString(), "stop");
        }

        pb.environment().put("CATALINA_HOME", config.getCatalinaHome().toString());
        pb.environment().put("CATALINA_BASE", config.getCatalinaBase().toString());
        pb.inheritIO();

        Process stopProcess = pb.start();
        stopProcess.waitFor(shutdownTimeout, TimeUnit.MILLISECONDS);
    }

    /**
     * Checks if the current OS is Windows.
     *
     * @return {@code true} if running on Windows, {@code false} otherwise
     */
    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }
}
