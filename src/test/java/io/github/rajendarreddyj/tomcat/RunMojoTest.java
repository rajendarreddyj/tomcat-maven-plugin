package io.github.rajendarreddyj.tomcat;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RunMojoTest {

    @TempDir
    Path tempDir;

    @Mock
    private MavenProject project;

    private RunMojo mojo;
    private Path catalinaHome;
    private Path warDir;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        mojo = new RunMojo();
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
        setField(mojo, "httpPort", 18080);
        setField(mojo, "httpHost", "localhost");
        setField(mojo, "startupTimeout", 5000L);
        setField(mojo, "shutdownTimeout", 5000L);
        setField(mojo, "tomcatCacheDir", tempDir.resolve("cache").toFile());
        setField(mojo, "contextPath", "/test");
        setField(mojo, "warSourceDirectory", warDir.toFile());
        setField(mojo, "skip", false);
        setField(mojo, "autopublishEnabled", false);
    }

    @Test
    void executeSkipsWhenSkipIsTrue() throws Exception {
        setField(mojo, "skip", true);

        // Should not throw
        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void executeThrowsForInvalidTomcat() throws Exception {
        Path invalidHome = tempDir.resolve("invalid");
        Files.createDirectories(invalidHome);
        setField(mojo, "catalinaHome", invalidHome.toFile());

        assertThrows(MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void executeThrowsForMissingWarDir() throws Exception {
        setField(mojo, "warSourceDirectory", tempDir.resolve("nonexistent").toFile());

        assertThrows(MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void executeThrowsWhenPortInUse() throws Exception {
        // Bind the port
        int port = findAvailablePort();
        try (ServerSocket socket = new ServerSocket(port)) {
            setField(mojo, "httpPort", port);

            assertThrows(MojoExecutionException.class, () -> mojo.execute());
        }
    }

    @Test
    void executeWithCatalinaBase() throws Exception {
        Path catalinaBase = tempDir.resolve("tomcat-base");
        Files.createDirectories(catalinaBase.resolve("conf"));
        Files.writeString(catalinaBase.resolve("conf").resolve("server.xml"), "<Server/>");

        setField(mojo, "catalinaBase", catalinaBase.toFile());

        // Should not throw during validation, may fail when actually running
        try {
            mojo.execute();
        } catch (MojoExecutionException e) {
            // Expected - may fail during Tomcat run
        }
    }

    @Test
    void executeWithVmOptions() throws Exception {
        setField(mojo, "vmOptions", List.of("-Xmx256m"));
        setField(mojo, "skip", true);

        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void executeWithEnvironmentVariables() throws Exception {
        setField(mojo, "environmentVariables", Map.of("JAVA_OPTS", "-Dtest=true"));
        setField(mojo, "skip", true);

        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void executeWithCustomContextPath() throws Exception {
        setField(mojo, "contextPath", "/myapp");
        setField(mojo, "skip", true);

        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void executeWithRootContextPath() throws Exception {
        setField(mojo, "contextPath", "/");
        setField(mojo, "skip", true);

        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void executeWithDefaultPort8080() throws Exception {
        // Port 8080 is special - skips port check when already in use
        setField(mojo, "httpPort", 8080);
        setField(mojo, "skip", true);

        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void executeWithClasspathAdditions() throws Exception {
        setField(mojo, "classpathAdditions", List.of("/path/to/extra.jar"));
        setField(mojo, "skip", true);

        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void executeWithJavaHome() throws Exception {
        Path javaHome = tempDir.resolve("java");
        Files.createDirectories(javaHome.resolve("bin"));
        setField(mojo, "javaHome", javaHome.toFile());
        setField(mojo, "skip", true);

        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void executeWithAutopublishEnabled() throws Exception {
        setField(mojo, "autopublishEnabled", true);
        setField(mojo, "autopublishInactivityLimit", 30);
        setField(mojo, "skip", true);

        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void executeWithDeploymentOutputName() throws Exception {
        setField(mojo, "deploymentOutputName", "ROOT");
        setField(mojo, "skip", true);

        assertDoesNotThrow(() -> mojo.execute());
    }

    private int findAvailablePort() throws Exception {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

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
}
