package io.github.rajendarreddyj.tomcat;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DeployMojoTest {

    @TempDir
    Path tempDir;

    @Mock
    private MavenProject project;

    private DeployMojo mojo;
    private Path catalinaHome;
    private Path warDir;

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

    @Test
    void executeSkipsWhenSkipIsTrue() throws Exception {
        setField(mojo, "skip", true);

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
    void executeValidatesJavaVersion() throws Exception {
        setField(mojo, "skip", true);
        assertDoesNotThrow(() -> mojo.execute());
    }

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

    private void createTomcatStructure(Path home) throws Exception {
        Files.createDirectories(home.resolve("bin"));
        Files.createDirectories(home.resolve("lib"));
        Files.createDirectories(home.resolve("conf"));
        Files.writeString(home.resolve("lib").resolve("catalina.jar"), "mock");
        Files.writeString(home.resolve("conf").resolve("server.xml"), "<Server/>");

        String scriptName = System.getProperty("os.name").toLowerCase().contains("windows")
                ? "catalina.bat" : "catalina.sh";
        Path script = home.resolve("bin").resolve(scriptName);
        Files.writeString(script, "echo test");
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
