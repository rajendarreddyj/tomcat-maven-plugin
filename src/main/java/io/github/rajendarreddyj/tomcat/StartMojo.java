package io.github.rajendarreddyj.tomcat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import io.github.rajendarreddyj.tomcat.config.ServerConfiguration;
import io.github.rajendarreddyj.tomcat.deploy.ExplodedWarDeployer;
import io.github.rajendarreddyj.tomcat.lifecycle.TomcatLauncher;

/**
 * Starts Apache Tomcat in background mode with the project's webapp deployed.
 *
 * <p>
 * This goal starts Tomcat as a background process and immediately returns
 * control
 * to the Maven build. The process ID is stored in a file within CATALINA_BASE
 * for
 * later use by the {@link StopMojo} goal.
 * </p>
 *
 * <p>
 * This is useful for integration testing scenarios where Tomcat needs to be
 * started before tests run and stopped after tests complete.
 * </p>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code mvn tomcat:start}</pre>
 *
 * <h2>Integration Test Example</h2>
 *
 * <pre>{@code
 * <plugin>
 *     <groupId>io.github.rajendarreddyj</groupId>
 *     <artifactId>tomcat-maven-plugin</artifactId>
 *     <executions>
 *         <execution>
 *             <id>start-tomcat</id>
 *             <phase>pre-integration-test</phase>
 *             <goals>
 *                 <goal>start</goal>
 *             </goals>
 *         </execution>
 *         <execution>
 *             <id>stop-tomcat</id>
 *             <phase>post-integration-test</phase>
 *             <goals>
 *                 <goal>stop</goal>
 *             </goals>
 *         </execution>
 *     </executions>
 * </plugin>
 * }</pre>
 *
 * @author rajendarreddyj
 * @see RunMojo for foreground mode execution
 * @see StopMojo for stopping a running instance
 * @since 1.0.0
 */
@Mojo(name = "start", defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST, requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true)
public class StartMojo extends AbstractTomcatMojo {

    /**
     * Executes the start goal.
     *
     * <p>
     * This method performs the following steps:
     * </p>
     * <ol>
     * <li>Validates Java version compatibility with the configured Tomcat
     * version</li>
     * <li>Validates that the configured HTTP port is available</li>
     * <li>Builds server and deployment configurations</li>
     * <li>Deploys the webapp to Tomcat's webapps directory</li>
     * <li>Starts Tomcat in background mode</li>
     * <li>Stores the process ID for the stop goal to use</li>
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

        try {
            ServerConfiguration serverConfig = buildServerConfiguration();
            var deployConfig = buildDeployableConfiguration(serverConfig);

            // Deploy webapp
            getLog().info("Deploying webapp to: " + deployConfig.getDeployDir());
            ExplodedWarDeployer deployer = new ExplodedWarDeployer(getLog());
            deployer.deploy(deployConfig);

            // Start Tomcat in background
            TomcatLauncher launcher = new TomcatLauncher(serverConfig, getLog());
            launcher.start();

            // Store PID for stop goal
            storePid(serverConfig.getCatalinaBase(), launcher.getProcess());

            getLog().info("Tomcat started in background on http://" +
                    httpHost + ":" + httpPort + contextPath);

        } catch (Exception e) {
            throw new MojoExecutionException("Failed to start Tomcat: " + e.getMessage(), e);
        }
    }

    /**
     * Stores the process ID to a file for the stop goal to use.
     *
     * <p>
     * The PID is written to a file named {@code tomcat.pid} in the CATALINA_BASE
     * directory. This file is read by the {@link StopMojo} to gracefully terminate
     * the Tomcat process.
     * </p>
     *
     * @param catalinaBase the CATALINA_BASE directory where the PID file will be
     *                     stored
     * @param process      the Tomcat process whose PID should be stored
     * @throws IOException if the PID file cannot be written
     */
    private void storePid(Path catalinaBase, Process process) throws IOException {
        Path pidFile = catalinaBase.resolve("tomcat.pid");
        Files.writeString(pidFile, String.valueOf(process.pid()));
        getLog().debug("Stored PID " + process.pid() + " in " + pidFile);
    }
}
