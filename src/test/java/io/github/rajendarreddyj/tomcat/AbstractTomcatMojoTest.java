package io.github.rajendarreddyj.tomcat;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for {@link AbstractTomcatMojo}.
 *
 * <p>
 * Tests the base Mojo functionality including Java version validation,
 * Tomcat installation resolution, port availability checks, and configuration
 * building.
 *
 * @author rajendarreddyj
 * @see AbstractTomcatMojo
 */
class AbstractTomcatMojoTest {

    /**
     * Temporary directory for test artifacts, cleaned up automatically after each
     * test.
     */
    @TempDir
    Path tempDir;

    /** Mock Maven project for testing. */
    @Mock
    private MavenProject project;

    /** The Mojo instance under test. */
    private TestTomcatMojo mojo;

    /** Path to the mock Tomcat installation directory. */
    private Path catalinaHome;

    /**
     * Sets up the test environment before each test.
     *
     * <p>
     * Creates a mock Tomcat structure and configures the Mojo with default test
     * values.
     *
     * @throws Exception if setup fails
     */
    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        mojo = new TestTomcatMojo();
        catalinaHome = tempDir.resolve("tomcat");
        createTomcatStructure(catalinaHome);

        when(project.getArtifactId()).thenReturn("test-app");
        when(project.getBuild()).thenReturn(mock(org.apache.maven.model.Build.class));
        when(project.getBuild().getDirectory()).thenReturn(tempDir.resolve("target").toString());
        when(project.getBuild().getFinalName()).thenReturn("test-app");

        setField(mojo, "project", project);
        setField(mojo, "tomcatVersion", "10.1.52");
        setField(mojo, "httpPort", 8080);
        setField(mojo, "httpHost", "localhost");
        setField(mojo, "startupTimeout", 120000L);
        setField(mojo, "shutdownTimeout", 30000L);
        setField(mojo, "tomcatCacheDir", tempDir.resolve("cache").toFile());
        setField(mojo, "contextPath", "/test");
    }

    /**
     * Verifies that Java version validation passes for compatible Tomcat 10.1.x.
     *
     * @throws Exception if the test fails
     */
    @Test
    void validateJavaVersionPassesForCompatibleVersion() throws Exception {
        setField(mojo, "tomcatVersion", "10.1.52");
        assertDoesNotThrow(() -> mojo.validateJavaVersion());
    }

    /**
     * Verifies that Java version validation passes for compatible Tomcat 11.x.
     *
     * @throws Exception if the test fails
     */
    @Test
    void validateJavaVersionPassesForTomcat11() throws Exception {
        setField(mojo, "tomcatVersion", "11.0.5");
        assertDoesNotThrow(() -> mojo.validateJavaVersion());
    }

    /**
     * Verifies that an existing Tomcat installation is used when catalinaHome is
     * specified.
     *
     * @throws Exception if the test fails
     */
    @Test
    void resolveCatalinaHomeUsesExistingInstallation() throws Exception {
        setField(mojo, "catalinaHome", catalinaHome.toFile());

        Path result = mojo.resolveCatalinaHome();

        assertEquals(catalinaHome, result);
    }

    /**
     * Verifies that an exception is thrown when catalinaHome points to an invalid
     * installation.
     *
     * @throws Exception if the test fails
     */
    @Test
    void resolveCatalinaHomeThrowsForInvalidInstallation() throws Exception {
        Path invalidHome = tempDir.resolve("invalid-tomcat");
        Files.createDirectories(invalidHome);
        setField(mojo, "catalinaHome", invalidHome.toFile());

        assertThrows(MojoExecutionException.class, () -> mojo.resolveCatalinaHome());
    }

    /**
     * Verifies behavior when catalinaHome is null - should attempt to download
     * Tomcat.
     *
     * @throws Exception if the test fails
     */
    @Test
    void resolveCatalinaHomeLogsWhenCatalinaHomeIsNull() throws Exception {
        setField(mojo, "catalinaHome", null);
        setField(mojo, "tomcatCacheDir", tempDir.resolve("cache").toFile());

        // When catalinaHome is null, resolveCatalinaHome attempts to download Tomcat
        // This may succeed (download or cache) or fail - we test the flow works
        try {
            Path result = mojo.resolveCatalinaHome();
            // If download succeeds, verify result is valid
            assertTrue(Files.exists(result.resolve("lib").resolve("catalina.jar")));
        } catch (MojoExecutionException e) {
            // Download failed - also acceptable behavior
            assertTrue(e.getMessage().contains("download") || e.getMessage().contains("Tomcat"));
        }
    }

    /**
     * Verifies behavior when catalinaHome path does not exist - should attempt to
     * download.
     *
     * @throws Exception if the test fails
     */
    @Test
    void resolveCatalinaHomeLogsWhenCatalinaHomeDoesNotExist() throws Exception {
        Path nonExistent = tempDir.resolve("nonexistent");
        setField(mojo, "catalinaHome", nonExistent.toFile());

        // When catalinaHome doesn't exist, resolveCatalinaHome attempts to download
        try {
            Path result = mojo.resolveCatalinaHome();
            // If download succeeds, verify result is valid
            assertTrue(Files.exists(result.resolve("lib").resolve("catalina.jar")));
        } catch (MojoExecutionException e) {
            // Download failed - also acceptable behavior
            assertTrue(e.getMessage().contains("download") || e.getMessage().contains("Tomcat"));
        }
    }

    /**
     * Verifies that validation passes for a valid Tomcat installation.
     *
     * @throws Exception if the test fails
     */
    @Test
    void validateTomcatInstallationPassesForValidInstallation() throws Exception {
        assertDoesNotThrow(() -> mojo.validateTomcatInstallation(catalinaHome));
    }

    /**
     * Verifies that validation throws when the bin directory is missing.
     */
    @Test
    void validateTomcatInstallationThrowsForMissingBinDir() {
        Path invalidHome = tempDir.resolve("no-bin");
        assertThrows(MojoExecutionException.class, () -> mojo.validateTomcatInstallation(invalidHome));
    }

    /**
     * Verifies that validation throws when catalina.jar is missing.
     *
     * @throws Exception if the test fails
     */
    @Test
    void validateTomcatInstallationThrowsForMissingCatalinaJar() throws Exception {
        Path invalidHome = tempDir.resolve("no-catalina");
        Files.createDirectories(invalidHome.resolve("bin"));
        assertThrows(MojoExecutionException.class, () -> mojo.validateTomcatInstallation(invalidHome));
    }

    /**
     * Verifies that port validation passes when the port is available.
     *
     * @throws Exception if the test fails
     */
    @Test
    void validatePortAvailablePassesForFreePort() throws Exception {
        setField(mojo, "httpPort", 19999);
        setField(mojo, "httpHost", "localhost");

        assertDoesNotThrow(() -> mojo.validatePortAvailable());
    }

    /**
     * Verifies that port validation throws when the port is already in use.
     *
     * <p>
     * This test binds a socket to localhost explicitly to ensure consistent
     * behavior across different operating systems (Windows, macOS, Linux).
     *
     * @throws Exception if the test fails
     */
    @Test
    void validatePortAvailableThrowsForUsedPort() throws Exception {
        // Use a port that's in use - bind to localhost explicitly to match
        // validatePortAvailable() check
        try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getByName("localhost"))) {
            int usedPort = socket.getLocalPort();
            setField(mojo, "httpPort", usedPort);
            setField(mojo, "httpHost", "localhost");

            assertThrows(MojoExecutionException.class, () -> mojo.validatePortAvailable());
        }
    }

    /**
     * Verifies that buildServerConfiguration creates a valid configuration with all
     * options.
     *
     * @throws Exception if the test fails
     */
    @Test
    void buildServerConfigurationCreatesValidConfig() throws Exception {
        setField(mojo, "catalinaHome", catalinaHome.toFile());
        setField(mojo, "httpPort", 9090);
        setField(mojo, "httpHost", "localhost");
        setField(mojo, "vmOptions", List.of("-Xmx512m"));
        setField(mojo, "environmentVariables", Map.of("TEST_VAR", "value"));
        setField(mojo, "classpathAdditions", List.of("/extra.jar"));

        var config = mojo.buildServerConfiguration();

        assertEquals(catalinaHome, config.getCatalinaHome());
        assertEquals(9090, config.getHttpPort());
        assertEquals("localhost", config.getHttpHost());
        assertEquals(1, config.getVmOptions().size());
        assertEquals(1, config.getEnvironmentVariables().size());
        assertEquals(1, config.getClasspathAdditions().size());
    }

    /**
     * Verifies that a custom CATALINA_BASE is generated for non-default ports.
     *
     * @throws Exception if the test fails
     */
    @Test
    void buildServerConfigurationGeneratesCatalinaBaseForNonDefaultPort() throws Exception {
        setField(mojo, "catalinaHome", catalinaHome.toFile());
        setField(mojo, "catalinaBase", null);
        setField(mojo, "httpPort", 9999);

        var config = mojo.buildServerConfiguration();

        assertNotNull(config.getCatalinaBase());
        assertTrue(config.getCatalinaBase().toString().contains("9999"));
    }

    /**
     * Verifies that default port 8080 does not trigger CATALINA_BASE generation.
     *
     * @throws Exception if the test fails
     */
    @Test
    void buildServerConfigurationUsesDefaultPortWithoutGenerating() throws Exception {
        setField(mojo, "catalinaHome", catalinaHome.toFile());
        setField(mojo, "catalinaBase", null);
        setField(mojo, "httpPort", 8080);

        var config = mojo.buildServerConfiguration();

        // When port is 8080, no separate catalinaBase is generated
        assertEquals(catalinaHome, config.getCatalinaHome());
    }

    /**
     * Verifies that generated CATALINA_BASE is reused on subsequent calls.
     *
     * @throws Exception if the test fails
     */
    @Test
    void buildServerConfigurationReusesExistingGeneratedBase() throws Exception {
        setField(mojo, "catalinaHome", catalinaHome.toFile());
        setField(mojo, "catalinaBase", null);
        setField(mojo, "httpPort", 9998);

        // First build generates catalinaBase
        var config1 = mojo.buildServerConfiguration();
        Path generatedBase = config1.getCatalinaBase();

        // Second build should reuse it
        var config2 = mojo.buildServerConfiguration();

        assertEquals(generatedBase, config2.getCatalinaBase());
    }

    /**
     * Verifies that buildDeployableConfiguration creates a valid configuration.
     *
     * @throws Exception if the test fails
     */
    @Test
    void buildDeployableConfigurationCreatesValidConfig() throws Exception {
        Path warDir = tempDir.resolve("target").resolve("test-app");
        Files.createDirectories(warDir);
        setField(mojo, "catalinaHome", catalinaHome.toFile());
        setField(mojo, "warSourceDirectory", warDir.toFile());
        setField(mojo, "contextPath", "/myapp");
        setField(mojo, "autopublishEnabled", true);
        setField(mojo, "autopublishInactivityLimit", 30);

        var serverConfig = mojo.buildServerConfiguration();
        var deployConfig = mojo.buildDeployableConfiguration(serverConfig);

        assertEquals("/myapp", deployConfig.getContextPath());
        assertEquals("test-app", deployConfig.getModuleName());
        assertTrue(deployConfig.isAutopublishEnabled());
    }

    /**
     * Verifies that buildDeployableConfiguration throws when WAR directory is
     * missing.
     *
     * @throws Exception if the test fails
     */
    @Test
    void buildDeployableConfigurationThrowsForMissingWarDir() throws Exception {
        setField(mojo, "catalinaHome", catalinaHome.toFile());
        setField(mojo, "warSourceDirectory", tempDir.resolve("nonexistent").toFile());

        var serverConfig = mojo.buildServerConfiguration();
        assertThrows(MojoExecutionException.class, () -> mojo.buildDeployableConfiguration(serverConfig));
    }

    /**
     * Verifies that buildDeployableConfiguration throws when WAR directory is null.
     *
     * @throws Exception if the test fails
     */
    @Test
    void buildDeployableConfigurationThrowsForNullWarDir() throws Exception {
        setField(mojo, "catalinaHome", catalinaHome.toFile());
        setField(mojo, "warSourceDirectory", null);

        var serverConfig = mojo.buildServerConfiguration();
        assertThrows(MojoExecutionException.class, () -> mojo.buildDeployableConfiguration(serverConfig));
    }

    /**
     * Verifies that detectInstalledVersion returns null when catalina.jar is
     * missing.
     */
    @Test
    void detectInstalledVersionReturnsNullForMissingJar() {
        Path emptyTomcat = tempDir.resolve("empty");
        assertNull(mojo.detectInstalledVersion(emptyTomcat));
    }

    /**
     * Verifies that detectInstalledVersion returns null for an invalid JAR file.
     *
     * @throws IOException if file creation fails
     */
    @Test
    void detectInstalledVersionReturnsNullForInvalidJar() throws IOException {
        Path invalidTomcat = tempDir.resolve("invalid");
        Files.createDirectories(invalidTomcat.resolve("lib"));
        Files.writeString(invalidTomcat.resolve("lib").resolve("catalina.jar"), "not a jar");
        assertNull(mojo.detectInstalledVersion(invalidTomcat));
    }

    /**
     * Verifies that detectInstalledVersion extracts version from a valid
     * catalina.jar.
     *
     * @throws Exception if the test fails
     */
    @Test
    void detectInstalledVersionReturnsVersionFromValidJar() throws Exception {
        Path tomcat = tempDir.resolve("tomcat-with-version");
        Files.createDirectories(tomcat.resolve("lib"));

        // Create a valid JAR with ServerInfo.properties
        Path catalinaJar = tomcat.resolve("lib").resolve("catalina.jar");
        createCatalinaJarWithVersion(catalinaJar, "10.1.52.0");

        String version = mojo.detectInstalledVersion(tomcat);

        assertEquals("10.1.52", version);
    }

    /**
     * Verifies that detectInstalledVersion handles short version strings
     * gracefully.
     *
     * @throws Exception if the test fails
     */
    @Test
    void detectInstalledVersionHandlesShortVersion() throws Exception {
        Path tomcat = tempDir.resolve("tomcat-short-version");
        Files.createDirectories(tomcat.resolve("lib"));

        // Create a JAR with short version (only 2 parts)
        Path catalinaJar = tomcat.resolve("lib").resolve("catalina.jar");
        createCatalinaJarWithVersion(catalinaJar, "10.1");

        String version = mojo.detectInstalledVersion(tomcat);

        // Should return null since version format is invalid (less than 3 parts)
        assertNull(version);
    }

    /**
     * Verifies that detectInstalledVersion returns null when ServerInfo.properties
     * is missing.
     *
     * @throws Exception if the test fails
     */
    @Test
    void detectInstalledVersionReturnsNullForMissingEntry() throws Exception {
        Path tomcat = tempDir.resolve("tomcat-no-entry");
        Files.createDirectories(tomcat.resolve("lib"));

        // Create a JAR without ServerInfo.properties
        Path catalinaJar = tomcat.resolve("lib").resolve("catalina.jar");
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(catalinaJar.toFile()))) {
            // Add a dummy entry
            jos.putNextEntry(new JarEntry("dummy.txt"));
            jos.write("dummy".getBytes());
            jos.closeEntry();
        }

        String version = mojo.detectInstalledVersion(tomcat);

        assertNull(version);
    }

    /**
     * Verifies that detectInstalledVersion returns null for blank version property.
     *
     * @throws Exception if the test fails
     */
    @Test
    void detectInstalledVersionReturnsNullForBlankVersion() throws Exception {
        Path tomcat = tempDir.resolve("tomcat-blank-version");
        Files.createDirectories(tomcat.resolve("lib"));

        // Create a JAR with blank version
        Path catalinaJar = tomcat.resolve("lib").resolve("catalina.jar");
        createCatalinaJarWithVersion(catalinaJar, "");

        String version = mojo.detectInstalledVersion(tomcat);

        assertNull(version);
    }

    /**
     * Verifies that server configuration includes custom Java home.
     *
     * @throws Exception if the test fails
     */
    @Test
    void buildServerConfigurationWithJavaHome() throws Exception {
        Path javaHome = tempDir.resolve("java");
        Files.createDirectories(javaHome);
        setField(mojo, "catalinaHome", catalinaHome.toFile());
        setField(mojo, "javaHome", javaHome.toFile());

        var config = mojo.buildServerConfiguration();

        assertEquals(javaHome, config.getJavaHome());
    }

    /**
     * Verifies that server configuration uses explicit CATALINA_BASE when provided.
     *
     * @throws Exception if the test fails
     */
    @Test
    void buildServerConfigurationWithCatalinaBase() throws Exception {
        Path catalinaBase = tempDir.resolve("base");
        Files.createDirectories(catalinaBase.resolve("conf"));
        Files.writeString(catalinaBase.resolve("conf").resolve("server.xml"), "<Server/>");
        setField(mojo, "catalinaHome", catalinaHome.toFile());
        setField(mojo, "catalinaBase", catalinaBase.toFile());

        var config = mojo.buildServerConfiguration();

        assertEquals(catalinaBase, config.getCatalinaBase());
    }

    /**
     * Verifies that server configuration handles null optional parameters
     * gracefully.
     *
     * @throws Exception if the test fails
     */
    @Test
    void buildServerConfigurationWithNullVmOptions() throws Exception {
        setField(mojo, "catalinaHome", catalinaHome.toFile());
        setField(mojo, "vmOptions", null);
        setField(mojo, "environmentVariables", null);
        setField(mojo, "classpathAdditions", null);

        var config = mojo.buildServerConfiguration();

        assertNotNull(config);
        assertTrue(config.getVmOptions().isEmpty());
        assertTrue(config.getEnvironmentVariables().isEmpty());
        assertTrue(config.getClasspathAdditions().isEmpty());
    }

    /**
     * Verifies that deployable configuration uses custom deployment output name.
     *
     * @throws Exception if the test fails
     */
    @Test
    void buildDeployableConfigurationWithDeploymentOutputName() throws Exception {
        Path warDir = tempDir.resolve("target").resolve("test-app");
        Files.createDirectories(warDir);
        setField(mojo, "catalinaHome", catalinaHome.toFile());
        setField(mojo, "warSourceDirectory", warDir.toFile());
        setField(mojo, "contextPath", "/myapp");
        setField(mojo, "deploymentOutputName", "custom-name");

        var serverConfig = mojo.buildServerConfiguration();
        var deployConfig = mojo.buildDeployableConfiguration(serverConfig);

        assertEquals("custom-name", deployConfig.getDeploymentOutputName());
    }

    /**
     * Creates a mock Tomcat directory structure for testing.
     *
     * @param home the path to create the Tomcat structure in
     * @throws IOException if directory creation fails
     */
    private void createTomcatStructure(Path home) throws IOException {
        Files.createDirectories(home.resolve("bin"));
        Files.createDirectories(home.resolve("lib"));
        Files.createDirectories(home.resolve("conf"));
        Files.writeString(home.resolve("lib").resolve("catalina.jar"), "mock jar content");
        Files.writeString(home.resolve("conf").resolve("server.xml"), "<Server/>");

        String scriptName = System.getProperty("os.name").toLowerCase().contains("windows")
                ? "catalina.bat"
                : "catalina.sh";
        Path script = home.resolve("bin").resolve(scriptName);
        Files.writeString(script, "echo test");
        if (!scriptName.endsWith(".bat")) {
            script.toFile().setExecutable(true);
        }
    }

    /**
     * Creates a mock catalina.jar with the specified version in
     * ServerInfo.properties.
     *
     * @param jarPath the path to create the JAR file
     * @param version the version string to include
     * @throws IOException if JAR creation fails
     */
    private void createCatalinaJarWithVersion(Path jarPath, String version) throws IOException {
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarPath.toFile()))) {
            // Add ServerInfo.properties
            JarEntry entry = new JarEntry("org/apache/catalina/util/ServerInfo.properties");
            jos.putNextEntry(entry);

            Properties props = new Properties();
            if (version != null && !version.isEmpty()) {
                props.setProperty("server.number", version);
            }
            props.store(jos, null);
            jos.closeEntry();
        }
    }

    /**
     * Sets a field value on the target object using reflection.
     *
     * @param target    the object to modify
     * @param fieldName the name of the field to set
     * @param value     the value to set
     * @throws Exception if reflection fails
     */
    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = findField(target.getClass(), fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    /**
     * Finds a field by name in the class hierarchy.
     *
     * @param clazz     the class to search
     * @param fieldName the name of the field to find
     * @return the Field object
     * @throws NoSuchFieldException if the field is not found
     */
    private Field findField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        while (clazz != null) {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }

    /**
     * Concrete implementation of AbstractTomcatMojo for testing.
     *
     * <p>
     * Provides a no-op execute method to allow testing of base class functionality.
     */
    private static class TestTomcatMojo extends AbstractTomcatMojo {
        @Override
        public void execute() {
            // No-op for testing
        }
    }
}
