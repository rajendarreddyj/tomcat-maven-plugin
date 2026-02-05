package io.github.rajendarreddyj.tomcat;

import io.github.rajendarreddyj.tomcat.config.ServerConfiguration;
import io.github.rajendarreddyj.tomcat.deploy.ExplodedWarDeployer;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Deploys or redeploys the webapp to a running Tomcat instance.
 *
 * <p>Usage: {@code mvn tomcat:deploy}</p>
 */
@Mojo(
        name = "deploy",
        requiresDependencyResolution = ResolutionScope.RUNTIME,
        threadSafe = true
)
public class DeployMojo extends AbstractTomcatMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            ServerConfiguration serverConfig = buildServerConfiguration();
            var deployConfig = buildDeployableConfiguration(serverConfig);

            ExplodedWarDeployer deployer = new ExplodedWarDeployer(getLog());
            deployer.redeploy(deployConfig);

            getLog().info("Webapp deployed to: " + deployConfig.getContextPath());

        } catch (Exception e) {
            throw new MojoExecutionException("Deployment failed: " + e.getMessage(), e);
        }
    }
}
