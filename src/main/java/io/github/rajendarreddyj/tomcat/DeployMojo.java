package io.github.rajendarreddyj.tomcat;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import io.github.rajendarreddyj.tomcat.config.ServerConfiguration;
import io.github.rajendarreddyj.tomcat.deploy.ExplodedWarDeployer;

/**
 * Deploys or redeploys the webapp to a running Tomcat instance.
 *
 * <p>
 * This goal copies the exploded WAR directory to Tomcat's webapps directory.
 * If a deployment already exists at the target location, it is removed before
 * the new deployment is copied. This provides a quick way to update a running
 * application without restarting Tomcat.
 * </p>
 *
 * <p>
 * <strong>Note:</strong> Tomcat must be configured for automatic deployment
 * (autoDeploy="true" in Host element) for the changes to take effect
 * immediately.
 * Otherwise, a Tomcat restart may be required.
 * </p>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code mvn tomcat:deploy}</pre>
 *
 * <h2>Configuration Example</h2>
 *
 * <pre>{@code
 * <plugin>
 *     <groupId>io.github.rajendarreddyj</groupId>
 *     <artifactId>tomcat-maven-plugin</artifactId>
 *     <configuration>
 *         <contextPath>/myapp</contextPath>
 *         <warSourceDirectory>${project.build.directory}/${project.build.finalName}</warSourceDirectory>
 *     </configuration>
 * </plugin>
 * }</pre>
 *
 * @author rajendarreddyj
 * @see RunMojo for running Tomcat with auto-publish support
 * @since 1.0.0
 */
@Mojo(name = "deploy", requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true)
public class DeployMojo extends AbstractTomcatMojo {

    /**
     * Executes the deploy goal.
     *
     * <p>
     * This method performs the following steps:
     * </p>
     * <ol>
     * <li>Builds server and deployment configurations</li>
     * <li>Removes any existing deployment at the target location</li>
     * <li>Copies the exploded WAR to Tomcat's webapps directory</li>
     * </ol>
     *
     * @throws MojoExecutionException if an error occurs during deployment
     * @throws MojoFailureException   if deployment fails due to invalid
     *                                configuration
     */
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
