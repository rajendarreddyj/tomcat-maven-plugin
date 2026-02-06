package io.github.rajendarreddyj.tomcat;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import io.github.rajendarreddyj.tomcat.config.ServerConfiguration;
import io.github.rajendarreddyj.tomcat.deploy.ExplodedWarDeployer;
import io.github.rajendarreddyj.tomcat.deploy.HotDeployWatcher;
import io.github.rajendarreddyj.tomcat.lifecycle.TomcatLauncher;

/**
 * Runs Apache Tomcat in debug mode with JDWP agent enabled.
 *
 * <p>
 * This goal starts Tomcat with Java debugging enabled, allowing IDEs
 * (VS Code, IntelliJ, Eclipse) to attach and debug the running application.
 * The goal blocks until the process is terminated (Ctrl+C).
 * </p>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code mvn tomcat:debug}</pre>
 *
 * <h2>IDE Connection</h2>
 *
 * <p>Connect your IDE debugger to localhost:{debugPort} (default: 5005)</p>
 *
 * <h3>VS Code</h3>
 * <p>Add to launch.json:</p>
 * <pre>{@code
 * {
 * "type": "java",
 * "name": "Attach to Tomcat",
 * "request": "attach",
 * "hostName": "localhost",
 * "port": 5005
 * }
 * }</pre>
 *
 * <h3>IntelliJ IDEA</h3>
 * <p>Run → Edit Configurations → Add → Remote JVM Debug → Port: 5005</p>
 *
 * <h3>Eclipse</h3>
 * <p>Run → Debug Configurations → Remote Java Application → Port: 5005</p>
 *
 * <h2>Configuration Example</h2>
 *
 * <pre>{@code
 * <plugin>
 * <groupId>io.github.rajendarreddyj</groupId>
 * <artifactId>tomcat-maven-plugin</artifactId>
 * <configuration>
 * <httpPort>8080</httpPort>
 * <debugPort>5005</debugPort>
 * <debugSuspend>false</debugSuspend>
 * <contextPath>/myapp</contextPath>
 * </configuration>
 * </plugin>
 * }</pre>
 *
 * @author rajendarreddyj
 * @see RunMojo for non-debug foreground execution
 * @see StartMojo for background mode execution
 * @since 1.0.0
 */
@Mojo(name = "debug", defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST, requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true)
public class DebugMojo extends AbstractTomcatMojo {

    /** Empty line constant for debug instructions banner. */
    private static final String EMPTY_LINE = "║                                                                  ║";

    /**
     * Executes the debug goal.
     *
     * <p>
     * This method performs the following steps:
     * </p>
     * <ol>
     * <li>Validates Java version compatibility with the configured Tomcat
     * version</li>
     * <li>Validates that the HTTP port is available</li>
     * <li>Validates that the debug port is available</li>
     * <li>Builds server configuration with JDWP debug options</li>
     * <li>Deploys the webapp to Tomcat's webapps directory</li>
     * <li>Starts the hot deploy watcher if auto-publish is enabled</li>
     * <li>Prints debug connection instructions</li>
     * <li>Starts Tomcat in foreground mode and blocks until shutdown</li>
     * </ol>
     *
     * @throws MojoExecutionException if an error occurs during execution
     * @throws MojoFailureException   if execution fails due to invalid
     *                                configuration
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Skipping Tomcat execution (tomcat.skip=true)");
            return;
        }

        validateJavaVersion();
        validatePortAvailable();
        validateDebugPortAvailable();

        try {
            ServerConfiguration serverConfig = buildDebugServerConfiguration();
            var deployConfig = buildDeployableConfiguration(serverConfig);

            // Deploy webapp
            getLog().info("Deploying webapp to: " + deployConfig.getDeployDir());
            ExplodedWarDeployer deployer = new ExplodedWarDeployer(getLog());
            deployer.deploy(deployConfig);

            // Start hot deploy watcher if enabled
            try (HotDeployWatcher watcher = new HotDeployWatcher(deployConfig, deployer, getLog())) {
                watcher.start();

                // Print debug connection instructions
                printDebugInstructions();

                // Start Tomcat
                TomcatLauncher launcher = new TomcatLauncher(serverConfig, getLog());
                getLog().info("Starting Tomcat " + tomcatVersion + " in DEBUG mode on http://" +
                        httpHost + ":" + httpPort + contextPath);

                launcher.run();
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MojoExecutionException("Tomcat debug execution was interrupted", e);
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to run Tomcat in debug mode: " + e.getMessage(), e);
        }
    }

    /**
     * Builds ServerConfiguration with JDWP debug options included in vmOptions.
     *
     * @return the ServerConfiguration with debug settings
     * @throws MojoExecutionException if configuration fails
     */
    private ServerConfiguration buildDebugServerConfiguration() throws MojoExecutionException {
        // Prepend JDWP agent to vmOptions
        List<String> debugVmOptions = new ArrayList<>();
        debugVmOptions.add(buildJdwpAgentArg());

        if (vmOptions != null) {
            debugVmOptions.addAll(vmOptions);
        }

        // Temporarily set vmOptions with debug agent
        List<String> originalVmOptions = vmOptions;
        vmOptions = debugVmOptions;

        try {
            return buildServerConfiguration();
        } finally {
            // Restore original vmOptions
            vmOptions = originalVmOptions;
        }
    }

    /**
     * Prints debug connection instructions to the console.
     */
    private void printDebugInstructions() {
        getLog().info("");
        getLog().info("╔══════════════════════════════════════════════════════════════════╗");
        getLog().info("║                    DEBUG MODE ENABLED                            ║");
        getLog().info("╠══════════════════════════════════════════════════════════════════╣");
        getLog().info("║ Listening for debugger on port: " + padRight(String.valueOf(debugPort), 31) + "║");
        getLog().info("║ Suspend on startup: " + padRight(String.valueOf(debugSuspend), 43) + "║");
        getLog().info("╠══════════════════════════════════════════════════════════════════╣");
        getLog().info("║ IDE Connection Instructions:                                     ║");
        getLog().info(EMPTY_LINE);
        getLog().info("║ VS Code:                                                         ║");
        getLog().info("║   1. Open Run and Debug (Ctrl+Shift+D)                          ║");
        getLog().info("║   2. Add \"Attach\" configuration with port " + padRight(String.valueOf(debugPort), 20) + "║");
        getLog().info("║   3. Start debugging (F5)                                        ║");
        getLog().info(EMPTY_LINE);
        getLog().info("║ IntelliJ IDEA:                                                   ║");
        getLog().info("║   Run → Edit Configurations → Remote JVM Debug → Port: "
                + padRight(String.valueOf(debugPort), 8) + "║");
        getLog().info(EMPTY_LINE);
        getLog().info("║ Eclipse:                                                         ║");
        getLog().info("║   Run → Debug Configurations → Remote Java Application          ║");
        getLog().info("║   Connection Type: Standard (Socket Attach), Port: " + padRight(String.valueOf(debugPort), 13)
                + "║");
        getLog().info("╚══════════════════════════════════════════════════════════════════╝");
        getLog().info("");

        if (debugSuspend) {
            getLog().info(">>> JVM is SUSPENDED - waiting for debugger to attach...");
            getLog().info("");
        }
    }

    /**
     * Right-pads a string with spaces to the specified length.
     *
     * @param str    the string to pad
     * @param length the desired total length
     * @return the padded string
     */
    private String padRight(String str, int length) {
        if (str.length() >= length) {
            return str;
        }
        StringBuilder sb = new StringBuilder(str);
        while (sb.length() < length) {
            sb.append(' ');
        }
        return sb.toString();
    }
}
