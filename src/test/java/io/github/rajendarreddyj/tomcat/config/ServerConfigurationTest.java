package io.github.rajendarreddyj.tomcat.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ServerConfigurationTest {

    @Test
    void builderRequiredFields() {
        ServerConfiguration config = ServerConfiguration.builder()
                .catalinaHome(Path.of("/tomcat"))
                .build();

        assertEquals(Path.of("/tomcat"), config.getCatalinaHome());
        // catalinaBase defaults to catalinaHome when not set
        assertEquals(Path.of("/tomcat"), config.getCatalinaBase());
    }

    @Test
    void builderAllFields() {
        ServerConfiguration config = ServerConfiguration.builder()
                .catalinaHome(Path.of("/tomcat"))
                .catalinaBase(Path.of("/tomcat-base"))
                .httpHost("0.0.0.0")
                .httpPort(9090)
                .javaHome(Path.of("/java"))
                .vmOptions(List.of("-Xmx2g", "-Xms512m"))
                .environmentVariables(Map.of("JAVA_OPTS", "-Dfile.encoding=UTF-8"))
                .startupTimeout(60000)
                .shutdownTimeout(15000)
                .classpathAdditions(List.of("/extra.jar"))
                .build();

        assertEquals(Path.of("/tomcat"), config.getCatalinaHome());
        assertEquals(Path.of("/tomcat-base"), config.getCatalinaBase());
        assertEquals("0.0.0.0", config.getHttpHost());
        assertEquals(9090, config.getHttpPort());
        assertEquals(Path.of("/java"), config.getJavaHome());
        assertEquals(List.of("-Xmx2g", "-Xms512m"), config.getVmOptions());
        assertEquals(Map.of("JAVA_OPTS", "-Dfile.encoding=UTF-8"), config.getEnvironmentVariables());
        assertEquals(60000, config.getStartupTimeout());
        assertEquals(15000, config.getShutdownTimeout());
        assertEquals(List.of("/extra.jar"), config.getClasspathAdditions());
    }

    @Test
    void builderDefaultValues() {
        ServerConfiguration config = ServerConfiguration.builder()
                .catalinaHome(Path.of("/tomcat"))
                .build();

        assertEquals("localhost", config.getHttpHost());
        assertEquals(8080, config.getHttpPort());
        assertNull(config.getJavaHome());
        assertTrue(config.getVmOptions().isEmpty());
        assertTrue(config.getEnvironmentVariables().isEmpty());
        assertEquals(120000, config.getStartupTimeout());
        assertEquals(30000, config.getShutdownTimeout());
        assertTrue(config.getClasspathAdditions().isEmpty());
    }

    @Test
    void catalinaHomeIsRequired() {
        assertThrows(NullPointerException.class, () ->
                ServerConfiguration.builder().build()
        );
    }

    @Test
    void immutabilityVmOptions() {
        ServerConfiguration config = ServerConfiguration.builder()
                .catalinaHome(Path.of("/tomcat"))
                .vmOptions(List.of("-Xmx1g"))
                .build();

        assertThrows(UnsupportedOperationException.class, () ->
                config.getVmOptions().add("-Xms256m")
        );
    }

    @Test
    void immutabilityEnvironmentVariables() {
        ServerConfiguration config = ServerConfiguration.builder()
                .catalinaHome(Path.of("/tomcat"))
                .environmentVariables(Map.of("KEY", "VALUE"))
                .build();

        assertThrows(UnsupportedOperationException.class, () ->
                config.getEnvironmentVariables().put("NEW", "VALUE")
        );
    }

    @Test
    void immutabilityClasspathAdditions() {
        ServerConfiguration config = ServerConfiguration.builder()
                .catalinaHome(Path.of("/tomcat"))
                .classpathAdditions(List.of("/lib.jar"))
                .build();

        assertThrows(UnsupportedOperationException.class, () ->
                config.getClasspathAdditions().add("/other.jar")
        );
    }

    @Test
    void toStringContainsAllFields() {
        ServerConfiguration config = ServerConfiguration.builder()
                .catalinaHome(Path.of("/tomcat"))
                .httpPort(8080)
                .build();

        String str = config.toString();
        assertTrue(str.contains("ServerConfiguration"));
        assertTrue(str.contains("catalinaHome"));
        assertTrue(str.contains("httpPort"));
    }
}
