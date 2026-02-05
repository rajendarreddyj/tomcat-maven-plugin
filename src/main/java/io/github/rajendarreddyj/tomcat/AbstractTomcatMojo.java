package io.github.rajendarreddyj.tomcat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import io.github.rajendarreddyj.tomcat.config.CatalinaBaseGenerator;
import io.github.rajendarreddyj.tomcat.config.DeployableConfiguration;
import io.github.rajendarreddyj.tomcat.config.ServerConfiguration;
import io.github.rajendarreddyj.tomcat.config.TomcatVersion;
import io.github.rajendarreddyj.tomcat.download.TomcatDownloader;

/**
 * Abstract base class for all Tomcat plugin Mojos.
 * Provides common configuration parameters and utility methods.
 *
 * @author rajendarreddyj
 * @since 1.0.0
 */
public abstract class AbstractTomcatMojo extends AbstractMojo {

    // ==================== Tomcat Version & Location ====================

    /**
     * Tomcat version to use. If catalinaHome is not specified or doesn't exist,
     * this version will be downloaded automatically.
     * Supported versions: 10.1.x, 11.x
     */
    @Parameter(property = "tomcat.version", defaultValue = "10.1.52")
    protected String tomcatVersion;

    /**
     * Path to an existing Tomcat installation (CATALINA_HOME).
     * If not specified or doesn't exist, Tomcat will be downloaded based on
     * tomcatVersion.
     */
    @Parameter(property = "tomcat.catalina.home")
    protected File catalinaHome;

    /**
     * Path to Tomcat instance directory (CATALINA_BASE).
     * Defaults to catalinaHome if not specified.
     */
    @Parameter(property = "tomcat.catalina.base")
    protected File catalinaBase;

    /**
     * Directory for caching downloaded Tomcat distributions.
     */
    @Parameter(property = "tomcat.cache.dir", defaultValue = "${user.home}/.m2/tomcat-cache")
    protected File tomcatCacheDir;

    // ==================== Server Configuration ====================

    /**
     * HTTP port for Tomcat to listen on.
     */
    @Parameter(property = "tomcat.http.port", defaultValue = "8080")
    protected int httpPort;

    /**
     * HTTP host/address for Tomcat to bind to.
     */
    @Parameter(property = "tomcat.http.host", defaultValue = "localhost")
    protected String httpHost;

    /**
     * Timeout in milliseconds for Tomcat startup.
     * Default: 120000ms (2 minutes).
     */
    @Parameter(property = "tomcat.timeout.startup", defaultValue = "120000")
    protected long startupTimeout;

    /**
     * Timeout in milliseconds for Tomcat shutdown.
     * Default: 30000ms (30 seconds).
     */
    @Parameter(property = "tomcat.timeout.shutdown", defaultValue = "30000")
    protected long shutdownTimeout;

    /**
     * Skip plugin execution entirely.
     * Useful for CI pipelines where Tomcat should not be started.
     */
    @Parameter(property = "tomcat.skip", defaultValue = "false")
    protected boolean skip;

    // ==================== JVM Configuration ====================

    /**
     * Path to JDK installation (JAVA_HOME).
     * Defaults to the JDK running Maven.
     */
    @Parameter(property = "tomcat.java.home", defaultValue = "${java.home}")
    protected File javaHome;

    /**
     * JVM options to pass to Tomcat (CATALINA_OPTS).
     */
    @Parameter(property = "tomcat.vm.options")
    protected List<String> vmOptions;

    /**
     * Environment variables to set for Tomcat process.
     */
    @Parameter
    protected Map<String, String> environmentVariables;

    // ==================== Deployment Configuration ====================

    /**
     * Context path for the deployed application.
     * Use "/" for ROOT context.
     */
    @Parameter(property = "tomcat.context.path", defaultValue = "/${project.artifactId}")
    protected String contextPath;

    /**
     * Directory containing the exploded WAR to deploy.
     */
    @Parameter(property = "tomcat.war.directory", defaultValue = "${project.build.directory}/${project.build.finalName}")
    protected File warSourceDirectory;

    /**
     * Target directory for deployment within webapps.
     * If not specified, derived from contextPath (e.g., "ROOT" for "/").
     */
    @Parameter(property = "tomcat.deployment.name")
    protected String deploymentOutputName;

    // ==================== Auto-publish Configuration ====================

    /**
     * Enable automatic republishing when source files change.
     */
    @Parameter(property = "tomcat.autopublish.enabled", defaultValue = "false")
    protected boolean autopublishEnabled;

    /**
     * Seconds of inactivity before auto-publish triggers.
     */
    @Parameter(property = "tomcat.autopublish.inactivity", defaultValue = "30")
    protected int autopublishInactivityLimit;

    // ==================== Classpath Configuration ====================

    /**
     * Additional JAR files to add to Tomcat's classpath.
     */
    @Parameter
    protected List<String> classpathAdditions;

    // ==================== Maven Project ====================

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    // ==================== Utility Methods ====================

    /**
     * Resolves the CATALINA_HOME path, downloading Tomcat if necessary.
     *
     * @return the resolved CATALINA_HOME path
     * @throws MojoExecutionException if resolution fails
     */
    protected Path resolveCatalinaHome() throws MojoExecutionException {
        if (catalinaHome != null && catalinaHome.exists() && catalinaHome.isDirectory()) {
            getLog().info("Using existing Tomcat installation: " + catalinaHome);
            validateTomcatInstallation(catalinaHome.toPath());
            return catalinaHome.toPath();
        }

        getLog().info("Tomcat installation not found at: " +
                (catalinaHome != null ? catalinaHome : "<not specified>"));
        getLog().info("Will download Tomcat " + tomcatVersion);

        return downloadTomcat();
    }

    /**
     * Downloads and extracts Tomcat distribution.
     *
     * @return the path to the extracted Tomcat installation
     * @throws MojoExecutionException if download fails
     */
    protected Path downloadTomcat() throws MojoExecutionException {
        validateJavaVersion();

        TomcatDownloader downloader = new TomcatDownloader();
        try {
            Path cacheDir = tomcatCacheDir.toPath();
            return downloader.download(tomcatVersion, cacheDir, getLog());
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to download Tomcat: " + e.getMessage(), e);
        }
    }

    /**
     * Validates that the path contains a valid Tomcat installation.
     *
     * @param tomcatPath the path to validate
     * @throws MojoExecutionException if validation fails
     */
    protected void validateTomcatInstallation(Path tomcatPath) throws MojoExecutionException {
        Path binDir = tomcatPath.resolve("bin");
        Path libDir = tomcatPath.resolve("lib");
        Path catalinaJar = libDir.resolve("catalina.jar");

        if (!Files.isDirectory(binDir)) {
            throw new MojoExecutionException(
                    "Invalid Tomcat installation: bin directory not found at " + binDir);
        }
        if (!Files.exists(catalinaJar)) {
            throw new MojoExecutionException(
                    "Invalid Tomcat installation: catalina.jar not found at " + catalinaJar);
        }

        getLog().debug("Validated Tomcat installation at: " + tomcatPath);
    }

    /**
     * Validates Java version compatibility with Tomcat version.
     *
     * @throws MojoExecutionException if Java version is incompatible
     */
    protected void validateJavaVersion() throws MojoExecutionException {
        TomcatVersion version = TomcatVersion.fromVersionString(tomcatVersion);
        int currentJava = Runtime.version().feature();

        if (currentJava < version.getMinimumJava()) {
            throw new MojoExecutionException(String.format(
                    "Tomcat %s requires Java %d or higher, but current Java version is %d",
                    tomcatVersion, version.getMinimumJava(), currentJava));
        }

        getLog().debug("Java version " + currentJava + " is compatible with Tomcat " + tomcatVersion);
    }

    /**
     * Validates that the HTTP port is available before starting Tomcat.
     *
     * @throws MojoExecutionException if the port is already in use
     */
    protected void validatePortAvailable() throws MojoExecutionException {
        try (ServerSocket socket = new ServerSocket(httpPort, 1, InetAddress.getByName(httpHost))) {
            socket.setReuseAddress(true);
            getLog().debug("Port " + httpPort + " is available on " + httpHost);
        } catch (IOException e) {
            throw new MojoExecutionException(
                    "Port " + httpPort + " is already in use on " + httpHost +
                            ". Stop the existing process or configure a different port with -Dtomcat.http.port=XXXX");
        }
    }

    /**
     * Detects the installed Tomcat version from an existing installation.
     * Reads version from
     * lib/catalina.jar!/org/apache/catalina/util/ServerInfo.properties
     *
     * @param tomcatPath path to the Tomcat installation
     * @return The detected version string (e.g., "10.1.52") or null if detection
     *         fails
     */
    protected String detectInstalledVersion(Path tomcatPath) {
        Path catalinaJar = tomcatPath.resolve("lib/catalina.jar");
        if (!Files.exists(catalinaJar)) {
            return null;
        }

        try (JarFile jar = new JarFile(catalinaJar.toFile())) {
            JarEntry entry = jar.getJarEntry("org/apache/catalina/util/ServerInfo.properties");
            if (entry == null) {
                return null;
            }

            Properties props = new Properties();
            try (InputStream is = jar.getInputStream(entry)) {
                props.load(is);
            }
            String serverNumber = props.getProperty("server.number");

            if (serverNumber != null && !serverNumber.isBlank()) {
                // Format: 10.1.52.0 -> 10.1.52
                String[] parts = serverNumber.split("\\.");
                if (parts.length >= 3) {
                    return parts[0] + "." + parts[1] + "." + parts[2];
                }
            }
        } catch (IOException e) {
            getLog().debug("Could not detect Tomcat version: " + e.getMessage());
        }
        return null;
    }

    /**
     * Builds ServerConfiguration from Mojo parameters.
     * Generates a custom CATALINA_BASE if needed for port/host configuration.
     *
     * @return the ServerConfiguration
     * @throws MojoExecutionException if configuration fails
     */
    protected ServerConfiguration buildServerConfiguration() throws MojoExecutionException {
        Path resolvedHome = resolveCatalinaHome();
        Path resolvedBase = catalinaBase != null ? catalinaBase.toPath() : null;

        // Generate custom CATALINA_BASE for port configuration if not specified
        if (resolvedBase == null && httpPort != 8080) {
            try {
                Path generatedBase = tomcatCacheDir.toPath()
                        .resolve("base-" + tomcatVersion + "-" + httpPort);

                if (!CatalinaBaseGenerator.isValidCatalinaBase(generatedBase)) {
                    CatalinaBaseGenerator.generate(resolvedHome, generatedBase, httpPort, httpHost);
                }
                resolvedBase = generatedBase;
                getLog().info("Using generated CATALINA_BASE: " + resolvedBase);
            } catch (IOException e) {
                throw new MojoExecutionException(
                        "Failed to generate CATALINA_BASE: " + e.getMessage(), e);
            }
        }

        return ServerConfiguration.builder()
                .catalinaHome(resolvedHome)
                .catalinaBase(resolvedBase)
                .httpHost(httpHost)
                .httpPort(httpPort)
                .javaHome(javaHome != null ? javaHome.toPath() : null)
                .vmOptions(vmOptions)
                .environmentVariables(environmentVariables)
                .startupTimeout(startupTimeout)
                .shutdownTimeout(shutdownTimeout)
                .classpathAdditions(classpathAdditions)
                .build();
    }

    /**
     * Builds DeployableConfiguration from Mojo parameters.
     *
     * @param serverConfig the server configuration
     * @return the DeployableConfiguration
     * @throws MojoExecutionException if configuration fails
     */
    protected DeployableConfiguration buildDeployableConfiguration(ServerConfiguration serverConfig)
            throws MojoExecutionException {

        if (warSourceDirectory == null || !warSourceDirectory.exists()) {
            throw new MojoExecutionException(
                    "WAR source directory does not exist: " + warSourceDirectory);
        }

        Path deployDir = serverConfig.getCatalinaBase().resolve("webapps");

        return DeployableConfiguration.builder()
                .moduleName(project.getArtifactId())
                .sourcePath(warSourceDirectory.toPath())
                .contextPath(contextPath)
                .deployDir(deployDir)
                .deploymentOutputName(deploymentOutputName)
                .autopublishEnabled(autopublishEnabled)
                .autopublishInactivityLimit(autopublishInactivityLimit)
                .build();
    }
}
