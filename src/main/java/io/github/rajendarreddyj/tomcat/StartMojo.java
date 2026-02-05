package io.github.rajendarreddyj.tomcat;

import io.github.rajendarreddyj.tomcat.config.ServerConfiguration;
import io.github.rajendarreddyj.tomcat.deploy.ExplodedWarDeployer;
import io.github.rajendarreddyj.tomcat.lifecycle.TomcatLauncher;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Starts Apache Tomcat in background mode with the project's webapp deployed.
 * The process ID is stored for later use by the stop goal.
 *
 * <p>Usage: {@code mvn tomcat:start}</p>
 */
@Mojo(
        name = "start",
        defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST,
        requiresDependencyResolution = ResolutionScope.RUNTIME,
        threadSafe = true
)
public class StartMojo extends AbstractTomcatMojo {

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
     */
    private void storePid(Path catalinaBase, Process process) throws IOException {
        Path pidFile = catalinaBase.resolve("tomcat.pid");
        Files.writeString(pidFile, String.valueOf(process.pid()));
        getLog().debug("Stored PID " + process.pid() + " in " + pidFile);
    }
}
