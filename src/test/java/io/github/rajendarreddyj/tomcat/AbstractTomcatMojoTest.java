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

class AbstractTomcatMojoTest {

    @TempDir
    Path tempDir;

    @Mock
    private MavenProject project;

    private TestTomcatMojo mojo;
    private Path catalinaHome;

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

    @Test
    void validateJavaVersionPassesForCompatibleVersion() throws Exception {
        setField(mojo, "tomcatVersion", "10.1.52");
        assertDoesNotThrow(() -> mojo.validateJavaVersion());
    }

    @Test
    void validateJavaVersionPassesForTomcat11() throws Exception {
        setField(mojo, "tomcatVersion", "11.0.5");
        assertDoesNotThrow(() -> mojo.validateJavaVersion());
    }

    @Test
    void resolveCatalinaHomeUsesExistingInstallation() throws Exception {
        setField(mojo, "catalinaHome", catalinaHome.toFile());

        Path result = mojo.resolveCatalinaHome();

        assertEquals(catalinaHome, result);
    }

    @Test
    void resolveCatalinaHomeThrowsForInvalidInstallation() throws Exception {
        Path invalidHome = tempDir.resolve("invalid-tomcat");
        Files.createDirectories(invalidHome);
        setField(mojo, "catalinaHome", invalidHome.toFile());

        assertThrows(MojoExecutionException.class, () -> mojo.resolveCatalinaHome());
    }

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

    @Test
    void validateTomcatInstallationPassesForValidInstallation() throws Exception {
        assertDoesNotThrow(() -> mojo.validateTomcatInstallation(catalinaHome));
    }

    @Test
    void validateTomcatInstallationThrowsForMissingBinDir() {
        Path invalidHome = tempDir.resolve("no-bin");
        assertThrows(MojoExecutionException.class, () -> mojo.validateTomcatInstallation(invalidHome));
    }

    @Test
    void validateTomcatInstallationThrowsForMissingCatalinaJar() throws Exception {
        Path invalidHome = tempDir.resolve("no-catalina");
        Files.createDirectories(invalidHome.resolve("bin"));
        assertThrows(MojoExecutionException.class, () -> mojo.validateTomcatInstallation(invalidHome));
    }

    @Test
    void validatePortAvailablePassesForFreePort() throws Exception {
        setField(mojo, "httpPort", 19999);
        setField(mojo, "httpHost", "localhost");

        assertDoesNotThrow(() -> mojo.validatePortAvailable());
    }

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

    @Test
    void buildServerConfigurationGeneratesCatalinaBaseForNonDefaultPort() throws Exception {
        setField(mojo, "catalinaHome", catalinaHome.toFile());
        setField(mojo, "catalinaBase", null);
        setField(mojo, "httpPort", 9999);

        var config = mojo.buildServerConfiguration();

        assertNotNull(config.getCatalinaBase());
        assertTrue(config.getCatalinaBase().toString().contains("9999"));
    }

    @Test
    void buildServerConfigurationUsesDefaultPortWithoutGenerating() throws Exception {
        setField(mojo, "catalinaHome", catalinaHome.toFile());
        setField(mojo, "catalinaBase", null);
        setField(mojo, "httpPort", 8080);

        var config = mojo.buildServerConfiguration();

        // When port is 8080, no separate catalinaBase is generated
        assertEquals(catalinaHome, config.getCatalinaHome());
    }

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

    @Test
    void buildDeployableConfigurationThrowsForMissingWarDir() throws Exception {
        setField(mojo, "catalinaHome", catalinaHome.toFile());
        setField(mojo, "warSourceDirectory", tempDir.resolve("nonexistent").toFile());

        var serverConfig = mojo.buildServerConfiguration();
        assertThrows(MojoExecutionException.class, () -> mojo.buildDeployableConfiguration(serverConfig));
    }

    @Test
    void buildDeployableConfigurationThrowsForNullWarDir() throws Exception {
        setField(mojo, "catalinaHome", catalinaHome.toFile());
        setField(mojo, "warSourceDirectory", null);

        var serverConfig = mojo.buildServerConfiguration();
        assertThrows(MojoExecutionException.class, () -> mojo.buildDeployableConfiguration(serverConfig));
    }

    @Test
    void detectInstalledVersionReturnsNullForMissingJar() {
        Path emptyTomcat = tempDir.resolve("empty");
        assertNull(mojo.detectInstalledVersion(emptyTomcat));
    }

    @Test
    void detectInstalledVersionReturnsNullForInvalidJar() throws IOException {
        Path invalidTomcat = tempDir.resolve("invalid");
        Files.createDirectories(invalidTomcat.resolve("lib"));
        Files.writeString(invalidTomcat.resolve("lib").resolve("catalina.jar"), "not a jar");
        assertNull(mojo.detectInstalledVersion(invalidTomcat));
    }

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

    @Test
    void buildServerConfigurationWithJavaHome() throws Exception {
        Path javaHome = tempDir.resolve("java");
        Files.createDirectories(javaHome);
        setField(mojo, "catalinaHome", catalinaHome.toFile());
        setField(mojo, "javaHome", javaHome.toFile());

        var config = mojo.buildServerConfiguration();

        assertEquals(javaHome, config.getJavaHome());
    }

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

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = findField(target.getClass(), fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

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

    // Concrete implementation for testing
    private static class TestTomcatMojo extends AbstractTomcatMojo {
        @Override
        public void execute() {
            // No-op for testing
        }
    }
}
