package io.github.rajendarreddyj.tomcat;

import io.github.rajendarreddyj.tomcat.config.ServerConfiguration;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * Stops a running Apache Tomcat instance started by the start goal.
 *
 * <p>Usage: {@code mvn tomcat:stop}</p>
 */
@Mojo(
        name = "stop",
        defaultPhase = LifecyclePhase.POST_INTEGRATION_TEST,
        threadSafe = true
)
public class StopMojo extends AbstractTomcatMojo {

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
     */
    private void stopProcess(long pid) throws InterruptedException {
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
                () -> getLog().warn("Process " + pid + " not found, may have already stopped")
        );
    }

    /**
     * Stops Tomcat using the catalina script.
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
     */
    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }
}
