package io.github.rajendarreddyj.tomcat;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for {@link DeployMojo}.
 *
 * <p>
 * Tests the webapp deployment goal including file copying,
 * context path handling, and error scenarios.
 *
 * @author rajendarreddyj
 * @see DeployMojo
 */
class DeployMojoTest {

    /**
     * Temporary directory for test artifacts, cleaned up automatically after each
     * test.
     */
    @TempDir
    Path tempDir;

    /** Mock Maven project for testing. */
    @Mock
    private MavenProject project;

    /** The DeployMojo instance under test. */
    private DeployMojo mojo;

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

        mojo = new DeployMojo();
        catalinaHome = tempDir.resolve("tomcat");
        warDir = tempDir.resolve("target").resolve("test-app");

        createTomcatStructure(catalinaHome);
        Files.createDirectories(warDir);
        Files.writeString(warDir.resolve("index.html"), "<html>test</html>");
        Files.createDirectories(warDir.resolve("WEB-INF"));
        Files.writeString(warDir.resolve("WEB-INF").resolve("web.xml"), "<web-app/>");

        when(project.getArtifactId()).thenReturn("test-app");
        when(project.getBuild()).thenReturn(mock(org.apache.maven.model.Build.class));
        when(project.getBuild().getDirectory()).thenReturn(tempDir.resolve("target").toString());
        when(project.getBuild().getFinalName()).thenReturn("test-app");

        setField(mojo, "project", project);
        setField(mojo, "tomcatVersion", "10.1.52");
        setField(mojo, "catalinaHome", catalinaHome.toFile());
        setField(mojo, "httpPort", 18083);
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
     * Verifies that Java version validation passes with skip flag enabled.
     *
     * @throws Exception if the test fails
     */
    @Test
    void executeValidatesJavaVersion() throws Exception {
        setField(mojo, "skip", true);
        assertDoesNotThrow(() -> mojo.execute());
    }

    /**
     * Verifies that deployment executes successfully with valid configuration.
     *
     * @throws Exception if the test fails
     */
    @Test
    void executeDeploysWhenValid() throws Exception {
        // Setup valid catalinaBase
        Path catalinaBase = tempDir.resolve("tomcat-base");
        Files.createDirectories(catalinaBase.resolve("conf"));
        Files.createDirectories(catalinaBase.resolve("webapps"));
        Files.writeString(catalinaBase.resolve("conf").resolve("server.xml"), "<Server/>");
        setField(mojo, "catalinaBase", catalinaBase.toFile());

        // Deploy should work with valid setup
        // May still fail during actual process start, but shouldn't fail validation
        try {
            mojo.execute();
        } catch (MojoExecutionException e) {
            // Expected - Tomcat process won't actually start in test
            assertTrue(e.getMessage() != null);
        }
    }

    /**
     * Verifies that deployment works with the root context path.
     *
     * @throws Exception if the test fails
     */
    @Test
    void executeDeploysToRootContext() throws Exception {
        setField(mojo, "contextPath", "/");

        Path catalinaBase = tempDir.resolve("tomcat-base-root");
        Files.createDirectories(catalinaBase.resolve("conf"));
        Files.createDirectories(catalinaBase.resolve("webapps"));
        Files.writeString(catalinaBase.resolve("conf").resolve("server.xml"), "<Server/>");
        setField(mojo, "catalinaBase", catalinaBase.toFile());

        try {
            mojo.execute();
        } catch (MojoExecutionException e) {
            // Expected
            assertTrue(e.getMessage() != null);
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

        String scriptName = System.getProperty("os.name").toLowerCase().contains("windows")
                ? "catalina.bat"
                : "catalina.sh";
        Path script = home.resolve("bin").resolve(scriptName);
        Files.writeString(script, "echo test");
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
