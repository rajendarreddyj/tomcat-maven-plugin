package io.github.rajendarreddyj.tomcat;

import io.github.rajendarreddyj.tomcat.config.ServerConfiguration;
import io.github.rajendarreddyj.tomcat.deploy.ExplodedWarDeployer;
import io.github.rajendarreddyj.tomcat.deploy.HotDeployWatcher;
import io.github.rajendarreddyj.tomcat.lifecycle.TomcatLauncher;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Runs Apache Tomcat in foreground mode with the project's webapp deployed.
 *
 * <p>Usage: {@code mvn tomcat:run}</p>
 */
@Mojo(
        name = "run",
        defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST,
        requiresDependencyResolution = ResolutionScope.RUNTIME,
        threadSafe = true
)
public class RunMojo extends AbstractTomcatMojo {

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
