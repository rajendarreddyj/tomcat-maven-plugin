package io.github.rajendarreddyj.tomcat;

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
 * Runs Apache Tomcat in foreground mode with the project's webapp deployed.
 *
 * <p>
 * This goal starts Tomcat and blocks until the process is terminated (Ctrl+C).
 * The webapp is deployed to Tomcat's webapps directory before startup. If
 * auto-publish
 * is enabled, file changes in the source directory will trigger automatic
 * redeployment.
 * </p>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code mvn tomcat:run}</pre>
 *
 * <h2>Configuration Example</h2>
 *
 * <pre>{@code
 * <plugin>
 *     <groupId>io.github.rajendarreddyj</groupId>
 *     <artifactId>tomcat-maven-plugin</artifactId>
 *     <configuration>
 *         <httpPort>8080</httpPort>
 *         <contextPath>/myapp</contextPath>
 *         <autopublishEnabled>true</autopublishEnabled>
 *     </configuration>
 * </plugin>
 * }</pre>
 *
 * @author rajendarreddyj
 * @see StartMojo for background mode execution
 * @see StopMojo for stopping a running instance
 * @since 1.0.0
 */
@Mojo(name = "run", defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST, requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true)
public class RunMojo extends AbstractTomcatMojo {

    /**
     * Executes the run goal.
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
     * <li>Starts the hot deploy watcher if auto-publish is enabled</li>
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

        try {
            ServerConfiguration serverConfig = buildServerConfiguration();
            var deployConfig = buildDeployableConfiguration(serverConfig);

            // Deploy webapp
            getLog().info("Deploying webapp to: " + deployConfig.getDeployDir());
            ExplodedWarDeployer deployer = new ExplodedWarDeployer(getLog());
            deployer.deploy(deployConfig);

            // Start hot deploy watcher if enabled
            HotDeployWatcher watcher = new HotDeployWatcher(deployConfig, deployer, getLog());

            try {
                watcher.start();

                // Start Tomcat
                TomcatLauncher launcher = new TomcatLauncher(serverConfig, getLog());
                getLog().info("Starting Tomcat " + tomcatVersion + " on http://" +
                        httpHost + ":" + httpPort + contextPath);

                launcher.run();

            } finally {
                watcher.close();
            }

        } catch (Exception e) {
            throw new MojoExecutionException("Failed to run Tomcat: " + e.getMessage(), e);
        }
    }
}
