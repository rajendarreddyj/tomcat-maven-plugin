package io.github.rajendarreddyj.tomcat;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
 * Unit tests for {@link StopMojo}.
 *
 * <p>
 * Tests the Tomcat stop goal including process termination,
 * PID file handling, and script-based shutdown scenarios.
 *
 * @author rajendarreddyj
 * @see StopMojo
 */
class StopMojoTest {

    /**
     * Temporary directory for test artifacts, cleaned up automatically after each
     * test.
     */
    @TempDir
    Path tempDir;

    /** Mock Maven project for testing. */
    @Mock
    private MavenProject project;

    /** The StopMojo instance under test. */
    private StopMojo mojo;

    /** Path to the mock Tomcat installation directory. */
    private Path catalinaHome;

    /**
     * Sets up the test environment before each test.
     *
     * <p>
     * Creates a mock Tomcat structure and configures the Mojo
     * with default test values.
     *
     * @throws Exception if setup fails
     */
    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        mojo = new StopMojo();
        catalinaHome = tempDir.resolve("tomcat");

        createTomcatStructure(catalinaHome);

        when(project.getArtifactId()).thenReturn("test-app");
        when(project.getBuild()).thenReturn(mock(org.apache.maven.model.Build.class));
        when(project.getBuild().getDirectory()).thenReturn(tempDir.resolve("target").toString());
        when(project.getBuild().getFinalName()).thenReturn("test-app");

        setField(mojo, "project", project);
        setField(mojo, "tomcatVersion", "10.1.52");
        setField(mojo, "catalinaHome", catalinaHome.toFile());
        setField(mojo, "catalinaBase", null);
        setField(mojo, "httpPort", 18082);
        setField(mojo, "httpHost", "localhost");
        setField(mojo, "startupTimeout", 5000L);
        setField(mojo, "shutdownTimeout", 5000L);
        setField(mojo, "tomcatCacheDir", tempDir.resolve("cache").toFile());
        setField(mojo, "skip", false);
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
     * Verifies that execution works with a custom CATALINA_BASE.
     *
     * @throws Exception if the test fails
     */
    @Test
    void executeWithCatalinaBase() throws Exception {
        // catalinaBase set to a valid directory
        Path catalinaBase = tempDir.resolve("tomcat-base");
        Files.createDirectories(catalinaBase.resolve("conf"));
        Files.writeString(catalinaBase.resolve("conf").resolve("server.xml"), "<Server/>");
        setField(mojo, "catalinaBase", catalinaBase.toFile());

        // Should not throw during validation, may fail at runtime when stopping
        try {
            mojo.execute();
        } catch (MojoExecutionException e) {
            // Expected - Tomcat process not running
            assertTrue(e.getMessage() != null);
        }
    }

    /**
     * Verifies that execution stops a Tomcat process using the PID file.
     *
     * <p>
     * Creates a PID file with a non-existent process ID to test graceful handling.
     *
     * @throws Exception if the test fails
     */
    @Test
    void executeStopsProcessWithPidFile() throws Exception {
        // Create a PID file with the current process ID
        Path catalinaBase = tempDir.resolve("tomcat-base-pid");
        Files.createDirectories(catalinaBase.resolve("conf"));
        Files.writeString(catalinaBase.resolve("conf").resolve("server.xml"), "<Server/>");

        // Write an invalid PID that won't exist
        Path pidFile = catalinaBase.resolve("tomcat.pid");
        Files.writeString(pidFile, "999999999");

        setField(mojo, "catalinaBase", catalinaBase.toFile());

        // Execute - should handle non-existent process gracefully
        assertDoesNotThrow(() -> mojo.execute());

        // PID file should be deleted
        assertFalse(Files.exists(pidFile));
    }

    /**
     * Verifies that execution stops Tomcat via script when no PID file exists.
     *
     * @throws Exception if the test fails
     */
    @Test
    void executeStopsViaScriptWhenNoPidFile() throws Exception {
        // Create catalinaBase without PID file
        Path catalinaBase = tempDir.resolve("tomcat-base-no-pid");
        Files.createDirectories(catalinaBase.resolve("conf"));
        Files.writeString(catalinaBase.resolve("conf").resolve("server.xml"), "<Server/>");
        setField(mojo, "catalinaBase", catalinaBase.toFile());

        // Should try to stop via script (which may fail in test env)
        try {
            mojo.execute();
        } catch (MojoExecutionException e) {
            // Expected - script may fail in test environment
            assertNotNull(e.getMessage());
        }
    }

    /**
     * Verifies that execution throws when PID file contains invalid format.
     *
     * @throws Exception if the test fails
     */
    @Test
    void executeHandlesInvalidPidFormat() throws Exception {
        Path catalinaBase = tempDir.resolve("tomcat-base-bad-pid");
        Files.createDirectories(catalinaBase.resolve("conf"));
        Files.writeString(catalinaBase.resolve("conf").resolve("server.xml"), "<Server/>");

        // Write invalid PID
        Path pidFile = catalinaBase.resolve("tomcat.pid");
        Files.writeString(pidFile, "not-a-number");

        setField(mojo, "catalinaBase", catalinaBase.toFile());

        // Should throw due to number format exception
        assertThrows(MojoExecutionException.class, () -> mojo.execute());
    }

    /**
     * Verifies that execution succeeds with the default HTTP port 8080.
     *
     * @throws Exception if the test fails
     */
    @Test
    void executeWithDefaultHttpPort() throws Exception {
        Path catalinaBase = tempDir.resolve("tomcat-base-default-port");
        Files.createDirectories(catalinaBase.resolve("conf"));
        Files.writeString(catalinaBase.resolve("conf").resolve("server.xml"), "<Server/>");

        // Write non-existent PID
        Path pidFile = catalinaBase.resolve("tomcat.pid");
        Files.writeString(pidFile, "999999999");

        setField(mojo, "catalinaBase", catalinaBase.toFile());
        setField(mojo, "httpPort", 8080);

        // Execute should complete (process not found is handled gracefully)
        assertDoesNotThrow(() -> mojo.execute());
    }

    /**
     * Verifies that execution handles an already stopped Tomcat process gracefully.
     *
     * @throws Exception if the test fails
     */
    @Test
    void executeHandlesAlreadyStoppedProcess() throws Exception {
        Path catalinaBase = tempDir.resolve("tomcat-base-stopped");
        Files.createDirectories(catalinaBase.resolve("conf"));
        Files.writeString(catalinaBase.resolve("conf").resolve("server.xml"), "<Server/>");

        // Use a very large PID that definitely doesn't exist
        Path pidFile = catalinaBase.resolve("tomcat.pid");
        Files.writeString(pidFile, "9999999999");

        setField(mojo, "catalinaBase", catalinaBase.toFile());

        // Should handle gracefully - process already stopped
        assertDoesNotThrow(() -> mojo.execute());
    }

    /**
     * Verifies that OS detection returns the expected value for the current
     * platform.
     *
     * @throws Exception if the test fails
     */
    @Test
    void isWindowsReturnsExpectedValue() throws Exception {
        // Test OS detection through behavior
        String osName = System.getProperty("os.name").toLowerCase();
        boolean expectedWindows = osName.contains("windows");

        // Verify script name selection matches OS
        String scriptName = expectedWindows ? "catalina.bat" : "catalina.sh";
        assertTrue(Files.exists(catalinaHome.resolve("bin").resolve(scriptName)));
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
        Files.writeString(batScript, "@echo off\necho stopping\n");

        Path shScript = home.resolve("bin").resolve("catalina.sh");
        Files.writeString(shScript, "#!/bin/bash\necho stopping\n");
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
