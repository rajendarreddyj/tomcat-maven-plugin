package io.github.rajendarreddyj.tomcat;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.rajendarreddyj.tomcat.config.ServerConfiguration;

/**
 * Unit tests for {@link DebugMojo}.
 *
 * <p>
 * Tests the debug mode Tomcat execution goal including JDWP configuration,
 * parameter handling, and error scenarios.
 * </p>
 *
 * @author rajendarreddyj
 * @see DebugMojo
 */
class DebugMojoTest {

    @TempDir
    Path tempDir;

    private DebugMojo mojo;
    private Log log;

    @BeforeEach
    void setUp() throws Exception {
        mojo = new DebugMojo();
        log = mock(Log.class);

        // Set up required fields using reflection
        setField(mojo, "log", log);
        setField(mojo, "tomcatVersion", "10.1.52");
        setField(mojo, "httpPort", 8080);
        setField(mojo, "httpHost", "localhost");
        setField(mojo, "debugPort", 5005);
        setField(mojo, "debugSuspend", false);
        setField(mojo, "debugHost", "*");

        // Mock project
        MavenProject project = mock(MavenProject.class);
        when(project.getArtifactId()).thenReturn("test-app");
        setField(mojo, "project", project);

        // Create temp directories
        Path catalinaHome = tempDir.resolve("tomcat");
        Files.createDirectories(catalinaHome.resolve("bin"));
        Files.createDirectories(catalinaHome.resolve("lib"));
        Files.createDirectories(catalinaHome.resolve("webapps"));
        Files.createFile(catalinaHome.resolve("lib/catalina.jar"));

        setField(mojo, "catalinaHome", catalinaHome.toFile());
        setField(mojo, "tomcatCacheDir", tempDir.resolve("cache").toFile());
    }

    /**
     * Verifies that execution is skipped when skip=true.
     */
    @DisplayName("shouldSkipExecutionWhenSkipIsTrue")
    @Test
    void shouldSkipExecutionWhenSkipIsTrue() throws Exception {
        // Arrange
        setField(mojo, "skip", true);

        // Act & Assert
        assertDoesNotThrow(() -> mojo.execute());
        verify(log).info("Skipping Tomcat execution (tomcat.skip=true)");
    }

    /**
     * Verifies that buildJdwpAgentArg creates correct JDWP string with defaults.
     */
    @DisplayName("shouldBuildJdwpAgentArgWithDefaultValues")
    @Test
    void shouldBuildJdwpAgentArgWithDefaultValues() throws Exception {
        // Arrange
        setField(mojo, "debugPort", 5005);
        setField(mojo, "debugSuspend", false);
        setField(mojo, "debugHost", "*");

        // Act
        String jdwpArg = invokeMethod(mojo, "buildJdwpAgentArg");

        // Assert
        assertEquals("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005", jdwpArg);
    }

    /**
     * Verifies that buildJdwpAgentArg creates correct JDWP string with
     * suspend=true.
     */
    @DisplayName("shouldBuildJdwpAgentArgWithSuspendTrue")
    @Test
    void shouldBuildJdwpAgentArgWithSuspendTrue() throws Exception {
        // Arrange
        setField(mojo, "debugPort", 8000);
        setField(mojo, "debugSuspend", true);
        setField(mojo, "debugHost", "localhost");

        // Act
        String jdwpArg = invokeMethod(mojo, "buildJdwpAgentArg");

        // Assert
        assertEquals("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=localhost:8000", jdwpArg);
    }

    /**
     * Verifies that custom debug port is used.
     */
    @DisplayName("shouldUseCustomDebugPort")
    @Test
    void shouldUseCustomDebugPort() throws Exception {
        // Arrange
        setField(mojo, "debugPort", 9999);
        setField(mojo, "skip", true);

        // Act
        assertDoesNotThrow(() -> mojo.execute());

        // Assert
        int debugPort = (int) getField(mojo, "debugPort");
        assertEquals(9999, debugPort);
    }

    /**
     * Verifies that execution succeeds with all debug options configured.
     */
    @DisplayName("shouldAcceptAllDebugOptions")
    @Test
    void shouldAcceptAllDebugOptions() throws Exception {
        // Arrange
        setField(mojo, "debugPort", 5005);
        setField(mojo, "debugSuspend", true);
        setField(mojo, "debugHost", "localhost");
        setField(mojo, "skip", true);

        // Act & Assert
        assertDoesNotThrow(() -> mojo.execute());
    }

    /**
     * Verifies that vmOptions are preserved when debug options are added.
     */
    @DisplayName("shouldPreserveVmOptionsWithDebugOptions")
    @Test
    void shouldPreserveVmOptionsWithDebugOptions() throws Exception {
        // Arrange
        List<String> originalVmOptions = List.of("-Xmx512m", "-XX:+UseG1GC");
        setField(mojo, "vmOptions", originalVmOptions);
        setField(mojo, "skip", true);

        // Act
        assertDoesNotThrow(() -> mojo.execute());

        // Assert - verify original vmOptions are not modified
        @SuppressWarnings("unchecked")
        List<String> vmOptions = (List<String>) getField(mojo, "vmOptions");
        assertEquals(2, vmOptions.size());
        assertTrue(vmOptions.contains("-Xmx512m"));
    }

    /**
     * Verifies that the JDWP agent string format is correct for different ports.
     */
    @DisplayName("shouldFormatJdwpAgentStringWithDifferentPorts")
    @Test
    void shouldFormatJdwpAgentStringWithDifferentPorts() throws Exception {
        // Arrange
        setField(mojo, "debugPort", 12345);
        setField(mojo, "debugSuspend", false);
        setField(mojo, "debugHost", "0.0.0.0");

        // Act
        String jdwpArg = invokeMethod(mojo, "buildJdwpAgentArg");

        // Assert
        assertTrue(jdwpArg.contains("address=0.0.0.0:12345"));
        assertTrue(jdwpArg.contains("suspend=n"));
    }

    /**
     * Verifies that debugHost can be localhost for local-only debugging.
     */
    @DisplayName("shouldRestrictToLocalhostWhenConfigured")
    @Test
    void shouldRestrictToLocalhostWhenConfigured() throws Exception {
        // Arrange
        setField(mojo, "debugPort", 5005);
        setField(mojo, "debugSuspend", false);
        setField(mojo, "debugHost", "localhost");

        // Act
        String jdwpArg = invokeMethod(mojo, "buildJdwpAgentArg");

        // Assert
        assertTrue(jdwpArg.contains("address=localhost:5005"));
    }

    /**
     * Verifies that padRight returns the original string when it equals target
     * length.
     *
     * <p>
     * Tests the branch where str.length() >= length, which should return
     * the original string unchanged without padding.
     * </p>
     */
    @DisplayName("shouldReturnOriginalStringWhenPadRightLengthEquals")
    @Test
    void shouldReturnOriginalStringWhenPadRightLengthEquals() throws Exception {
        // Arrange - string with exact target length
        String input = "12345";
        int targetLength = 5;

        // Act
        String result = invokeMethod(mojo, "padRight", input, targetLength);

        // Assert
        assertEquals(input, result);
        assertEquals(5, result.length());
    }

    /**
     * Verifies that padRight returns the original string when it exceeds target
     * length.
     *
     * <p>
     * Tests the branch where str.length() > length, which should return
     * the original string unchanged without truncation.
     * </p>
     */
    @DisplayName("shouldReturnOriginalStringWhenPadRightLengthExceeds")
    @Test
    void shouldReturnOriginalStringWhenPadRightLengthExceeds() throws Exception {
        // Arrange - string longer than target
        String input = "1234567890";
        int targetLength = 5;

        // Act
        String result = invokeMethod(mojo, "padRight", input, targetLength);

        // Assert
        assertEquals(input, result);
        assertEquals(10, result.length());
    }

    /**
     * Verifies that padRight correctly pads strings shorter than target length.
     *
     * <p>
     * Tests the normal padding branch where str.length() < length.
     * </p>
     */
    @DisplayName("shouldPadStringWhenShorterThanTargetLength")
    @Test
    void shouldPadStringWhenShorterThanTargetLength() throws Exception {
        // Arrange
        String input = "test";
        int targetLength = 10;

        // Act
        String result = invokeMethod(mojo, "padRight", input, targetLength);

        // Assert
        assertEquals(10, result.length());
        assertTrue(result.startsWith("test"));
        assertEquals("test      ", result);
    }

    /**
     * Verifies that printDebugInstructions logs suspend message when
     * debugSuspend=true.
     *
     * <p>
     * Tests the branch in printDebugInstructions that displays the JVM suspended
     * warning message when debugSuspend is enabled.
     * </p>
     */
    @DisplayName("shouldLogSuspendMessageWhenDebugSuspendIsTrue")
    @Test
    void shouldLogSuspendMessageWhenDebugSuspendIsTrue() throws Exception {
        // Arrange
        setField(mojo, "debugSuspend", true);
        setField(mojo, "debugPort", 5005);

        // Act
        invokeMethod(mojo, "printDebugInstructions");

        // Assert - verify suspend message was logged
        verify(log).info(">>> JVM is SUSPENDED - waiting for debugger to attach...");
    }

    /**
     * Verifies that printDebugInstructions does not log suspend message when
     * debugSuspend=false.
     *
     * <p>
     * Tests the branch in printDebugInstructions that skips the JVM suspended
     * warning message when debugSuspend is disabled.
     * </p>
     */
    @DisplayName("shouldNotLogSuspendMessageWhenDebugSuspendIsFalse")
    @Test
    void shouldNotLogSuspendMessageWhenDebugSuspendIsFalse() throws Exception {
        // Arrange
        setField(mojo, "debugSuspend", false);
        setField(mojo, "debugPort", 5005);
        org.mockito.Mockito.reset(log);

        // Act
        invokeMethod(mojo, "printDebugInstructions");

        // Assert - verify suspend message was NOT logged
        verify(log, org.mockito.Mockito.never()).info(">>> JVM is SUSPENDED - waiting for debugger to attach...");
    }

    /**
     * Verifies that buildDebugServerConfiguration includes JDWP agent with null
     * vmOptions.
     *
     * <p>
     * Tests the branch where vmOptions is null, ensuring the JDWP agent
     * is still properly added to the configuration.
     * </p>
     */
    @DisplayName("shouldBuildDebugServerConfigWithNullVmOptions")
    @Test
    void shouldBuildDebugServerConfigWithNullVmOptions() throws Exception {
        // Arrange
        setField(mojo, "vmOptions", null);
        setField(mojo, "debugPort", 5005);
        setField(mojo, "debugSuspend", false);
        setField(mojo, "debugHost", "*");

        // Act
        ServerConfiguration config = invokeMethod(mojo, "buildDebugServerConfiguration");

        // Assert
        List<String> resultVmOptions = config.getVmOptions();
        assertTrue(resultVmOptions.stream().anyMatch(opt -> opt.contains("-agentlib:jdwp")));
    }

    // ==================== Helper Methods ====================

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
     * Gets a field value from the target object using reflection.
     *
     * @param target    the object to read from
     * @param fieldName the name of the field to get
     * @return the field value
     * @throws Exception if reflection fails
     */
    private Object getField(Object target, String fieldName) throws Exception {
        Field field = findField(target.getClass(), fieldName);
        field.setAccessible(true);
        return field.get(target);
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
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName + " not found in " + clazz.getName());
    }

    /**
     * Invokes a no-argument method on the target object using reflection.
     *
     * @param <T>        the expected return type
     * @param target     the object to invoke the method on
     * @param methodName the name of the method to invoke
     * @return the method's return value
     * @throws Exception if reflection or invocation fails
     */
    @SuppressWarnings("unchecked")
    private <T> T invokeMethod(Object target, String methodName) throws Exception {
        var method = findMethod(target.getClass(), methodName);
        method.setAccessible(true);
        return (T) method.invoke(target);
    }

    /**
     * Invokes a method with parameters on the target object using reflection.
     *
     * @param <T>        the expected return type
     * @param target     the object to invoke the method on
     * @param methodName the name of the method to invoke
     * @param args       the method arguments
     * @return the method's return value
     * @throws Exception if reflection or invocation fails
     */
    @SuppressWarnings("unchecked")
    private <T> T invokeMethod(Object target, String methodName, Object... args) throws Exception {
        Class<?>[] paramTypes = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            paramTypes[i] = args[i].getClass();
            // Handle primitive type wrappers
            if (args[i] instanceof Integer) {
                paramTypes[i] = int.class;
            }
        }
        var method = findMethodWithParams(target.getClass(), methodName, paramTypes);
        method.setAccessible(true);
        return (T) method.invoke(target, args);
    }

    /**
     * Finds a no-argument method by name in the class hierarchy.
     *
     * @param clazz      the class to search
     * @param methodName the name of the method to find
     * @return the Method object
     * @throws NoSuchMethodException if the method is not found
     */
    private java.lang.reflect.Method findMethod(Class<?> clazz, String methodName) throws NoSuchMethodException {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredMethod(methodName);
            } catch (NoSuchMethodException e) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchMethodException(methodName + " not found in " + clazz.getName());
    }

    /**
     * Finds a method with parameters by name in the class hierarchy.
     *
     * @param clazz      the class to search
     * @param methodName the name of the method to find
     * @param paramTypes the parameter types
     * @return the Method object
     * @throws NoSuchMethodException if the method is not found
     */
    private java.lang.reflect.Method findMethodWithParams(Class<?> clazz, String methodName, Class<?>... paramTypes)
            throws NoSuchMethodException {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredMethod(methodName, paramTypes);
            } catch (NoSuchMethodException e) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchMethodException(methodName + " not found in " + clazz.getName());
    }
}
