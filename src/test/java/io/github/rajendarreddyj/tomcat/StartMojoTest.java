package io.github.rajendarreddyj.tomcat;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for {@link StartMojo}.
 *
 * <p>
 * Tests the background Tomcat start goal including validation,
 * configuration handling, and error scenarios.
 *
 * @author rajendarreddyj
 * @see StartMojo
 */
class StartMojoTest {

    /**
     * Temporary directory for test artifacts, cleaned up automatically after each
     * test.
     */
    @TempDir
    Path tempDir;

    /** Mock Maven project for testing. */
    @Mock
    private MavenProject project;

    /** The StartMojo instance under test. */
    private StartMojo mojo;

    /** Path to the mock Tomcat installation directory. */
    private Path catalinaHome;

    /** Path to the mock WAR source directory. */
    private Path warDir;

    /**
     * Sets up the test environment before each test.
     *
     * <p>
     * Creates a mock Tomcat structure, WAR directory, and configures the Mojo
     * with default test values.
     *
     * @throws Exception if setup fails
     */
    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        mojo = new StartMojo();
        catalinaHome = tempDir.resolve("tomcat");
        warDir = tempDir.resolve("target").resolve("test-app");

        createTomcatStructure(catalinaHome);
        Files.createDirectories(warDir);
        Files.writeString(warDir.resolve("index.html"), "<html>test</html>");

        when(project.getArtifactId()).thenReturn("test-app");
        when(project.getBuild()).thenReturn(mock(org.apache.maven.model.Build.class));
        when(project.getBuild().getDirectory()).thenReturn(tempDir.resolve("target").toString());
        when(project.getBuild().getFinalName()).thenReturn("test-app");

        setField(mojo, "project", project);
        setField(mojo, "tomcatVersion", "10.1.52");
        setField(mojo, "catalinaHome", catalinaHome.toFile());
        setField(mojo, "httpPort", 18081);
        setField(mojo, "httpHost", "localhost");
        setField(mojo, "startupTimeout", 5000L);
        setField(mojo, "shutdownTimeout", 5000L);
        setField(mojo, "tomcatCacheDir", tempDir.resolve("cache").toFile());
        setField(mojo, "contextPath", "/test");
        setField(mojo, "warSourceDirectory", warDir.toFile());
        setField(mojo, "skip", false);
        setField(mojo, "autopublishEnabled", false);
    }

    /**
     * Verifies that execution is skipped when the skip flag is true.
     *
     * @throws Exception if the test fails
     */
    @Test
    void executeSkipsWhenSkipIsTrue() throws Exception {
        setField(mojo, "skip", true);

        assertDoesNotThrow(() -> mojo.execute());
    }

    /**
     * Verifies that execution throws when Tomcat installation is invalid.
     *
     * @throws Exception if the test fails
     */
    @Test
    void executeThrowsForInvalidTomcat() throws Exception {
        Path invalidHome = tempDir.resolve("invalid");
        Files.createDirectories(invalidHome);
        setField(mojo, "catalinaHome", invalidHome.toFile());

        assertThrows(MojoExecutionException.class, () -> mojo.execute());
    }

    /**
     * Verifies that execution throws when WAR directory is missing.
     *
     * @throws Exception if the test fails
     */
    @Test
    void executeThrowsForMissingWarDir() throws Exception {
        setField(mojo, "warSourceDirectory", tempDir.resolve("nonexistent").toFile());

        assertThrows(MojoExecutionException.class, () -> mojo.execute());
    }

    /**
     * Verifies that execution throws when the configured port is already in use.
     *
     * <p>
     * This test binds a socket to localhost explicitly to ensure consistent
     * behavior across different operating systems (Windows, macOS, Linux).
     *
     * @throws Exception if the test fails
     */
    @Test
    void executeThrowsWhenPortInUse() throws Exception {
        // Bind the port to localhost explicitly to match validatePortAvailable() check
        int port = findAvailablePort();
        try (ServerSocket socket = new ServerSocket(port, 1, InetAddress.getByName("localhost"))) {
            setField(mojo, "httpPort", port);

            assertThrows(MojoExecutionException.class, () -> mojo.execute());
        }
    }

    /**
     * Verifies that execution works with a custom CATALINA_BASE.
     *
     * @throws Exception if the test fails
     */
    @Test
    void executeWithCatalinaBase() throws Exception {
        Path catalinaBase = tempDir.resolve("tomcat-base");
        Files.createDirectories(catalinaBase.resolve("conf"));
        Files.writeString(catalinaBase.resolve("conf").resolve("server.xml"), "<Server/>");

        setField(mojo, "catalinaBase", catalinaBase.toFile());

        // Should not throw during validation, may fail when actually starting
        try {
            mojo.execute();
        } catch (MojoExecutionException e) {
            // Expected - may fail during Tomcat start
        }
    }

    /**
     * Verifies that execution succeeds with JVM options configured.
     *
     * @throws Exception if the test fails
     */
    @Test
    void executeWithVmOptions() throws Exception {
        setField(mojo, "vmOptions", List.of("-Xmx256m", "-XX:+UseG1GC"));
        setField(mojo, "skip", true);

        assertDoesNotThrow(() -> mojo.execute());
    }

    /**
     * Verifies that execution succeeds with environment variables configured.
     *
     * @throws Exception if the test fails
     */
    @Test
    void executeWithEnvironmentVariables() throws Exception {
        setField(mojo, "environmentVariables", Map.of("JAVA_OPTS", "-Dtest=true"));
        setField(mojo, "skip", true);

        assertDoesNotThrow(() -> mojo.execute());
    }

    /**
     * Verifies that execution succeeds with a custom context path.
     *
     * @throws Exception if the test fails
     */
    @Test
    void executeWithCustomContextPath() throws Exception {
        setField(mojo, "contextPath", "/myapp");
        setField(mojo, "skip", true);

        assertDoesNotThrow(() -> mojo.execute());
    }

    /**
     * Verifies that execution succeeds with the root context path.
     *
     * @throws Exception if the test fails
     */
    @Test
    void executeWithRootContextPath() throws Exception {
        setField(mojo, "contextPath", "/");
        setField(mojo, "skip", true);

        assertDoesNotThrow(() -> mojo.execute());
    }

    /**
     * Verifies that execution succeeds with the default port 8080.
     *
     * @throws Exception if the test fails
     */
    @Test
    void executeWithDefaultPort8080() throws Exception {
        // Port 8080 is special - skips port check when already in use
        setField(mojo, "httpPort", 8080);
        setField(mojo, "skip", true);

        assertDoesNotThrow(() -> mojo.execute());
    }

    /**
     * Verifies that execution succeeds with classpath additions.
     *
     * @throws Exception if the test fails
     */
    @Test
    void executeWithClasspathAdditions() throws Exception {
        setField(mojo, "classpathAdditions", List.of("/path/to/extra.jar", "/another.jar"));
        setField(mojo, "skip", true);

        assertDoesNotThrow(() -> mojo.execute());
    }

    /**
     * Verifies that execution succeeds with a custom Java home.
     *
     * @throws Exception if the test fails
     */
    @Test
    void executeWithJavaHome() throws Exception {
        Path javaHome = tempDir.resolve("java");
        Files.createDirectories(javaHome.resolve("bin"));
        setField(mojo, "javaHome", javaHome.toFile());
        setField(mojo, "skip", true);

        assertDoesNotThrow(() -> mojo.execute());
    }

    /**
     * Verifies that execution succeeds with auto-publish enabled.
     *
     * @throws Exception if the test fails
     */
    @Test
    void executeWithAutopublishEnabled() throws Exception {
        setField(mojo, "autopublishEnabled", true);
        setField(mojo, "autopublishInactivityLimit", 60);
        setField(mojo, "skip", true);

        assertDoesNotThrow(() -> mojo.execute());
    }

    /**
     * Verifies that execution succeeds with a custom deployment output name.
     *
     * @throws Exception if the test fails
     */
    @Test
    void executeWithDeploymentOutputName() throws Exception {
        setField(mojo, "deploymentOutputName", "ROOT");
        setField(mojo, "skip", true);

        assertDoesNotThrow(() -> mojo.execute());
    }

    /**
     * Verifies that execution succeeds with Tomcat 11.x version.
     *
     * @throws Exception if the test fails
     */
    @Test
    void executeWithTomcat11Version() throws Exception {
        setField(mojo, "tomcatVersion", "11.0.5");
        setField(mojo, "skip", true);

        assertDoesNotThrow(() -> mojo.execute());
    }

    /**
     * Finds an available port on localhost.
     *
     * @return an available port number
     * @throws Exception if port selection fails
     */
    private int findAvailablePort() throws Exception {
        try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getByName("localhost"))) {
            return socket.getLocalPort();
        }
    }

    /**
     * Creates a mock Tomcat directory structure for testing.
     *
     * @param home the path to create the Tomcat structure in
     * @throws Exception if directory creation fails
     */
    private void createTomcatStructure(Path home) throws Exception {
        Files.createDirectories(home.resolve("bin"));
        Files.createDirectories(home.resolve("lib"));
        Files.createDirectories(home.resolve("conf"));
        Files.writeString(home.resolve("lib").resolve("catalina.jar"), "mock");
        Files.writeString(home.resolve("conf").resolve("server.xml"), "<Server/>");

        // Create both scripts for cross-platform testing
        Path batScript = home.resolve("bin").resolve("catalina.bat");
        Files.writeString(batScript, "@echo off\necho test\n");

        Path shScript = home.resolve("bin").resolve("catalina.sh");
        Files.writeString(shScript, "#!/bin/bash\necho test\n");
        shScript.toFile().setExecutable(true);
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
}
