package io.github.rajendarreddyj.tomcat.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class DeployableConfigurationTest {

    private static final String MODULE_NAME = "test-module";

    @Test
    void getTargetDirectoryNameWithDeploymentOutputName() {
        DeployableConfiguration config = DeployableConfiguration.builder()
                .moduleName(MODULE_NAME)
                .sourcePath(Path.of("/source"))
                .deployDir(Path.of("/target"))
                .deploymentOutputName("ROOT")
                .build();

        assertEquals("ROOT", config.getTargetDirectoryName());
    }

    @Test
    void getTargetDirectoryNameRootContext() {
        DeployableConfiguration config = DeployableConfiguration.builder()
                .moduleName(MODULE_NAME)
                .sourcePath(Path.of("/source"))
                .deployDir(Path.of("/target"))
                .contextPath("/")
                .build();

        assertEquals("ROOT", config.getTargetDirectoryName());
    }

    @Test
    void getTargetDirectoryNameNamedContext() {
        DeployableConfiguration config = DeployableConfiguration.builder()
                .moduleName(MODULE_NAME)
                .sourcePath(Path.of("/source"))
                .deployDir(Path.of("/target"))
                .contextPath("/myapp")
                .build();

        assertEquals("myapp", config.getTargetDirectoryName());
    }

    @Test
    void normalizeContextPathAddsLeadingSlash() {
        DeployableConfiguration config = DeployableConfiguration.builder()
                .moduleName(MODULE_NAME)
                .sourcePath(Path.of("/source"))
                .deployDir(Path.of("/target"))
                .contextPath("myapp")  // missing leading /
                .build();

        assertEquals("/myapp", config.getContextPath());
    }

    @Test
    void normalizeContextPathPreservesLeadingSlash() {
        DeployableConfiguration config = DeployableConfiguration.builder()
                .moduleName(MODULE_NAME)
                .sourcePath(Path.of("/source"))
                .deployDir(Path.of("/target"))
                .contextPath("/existing")
                .build();

        assertEquals("/existing", config.getContextPath());
    }

    @Test
    void normalizeContextPathEmptyBecomesRoot() {
        DeployableConfiguration config = DeployableConfiguration.builder()
                .moduleName(MODULE_NAME)
                .sourcePath(Path.of("/source"))
                .deployDir(Path.of("/target"))
                .contextPath("")
                .build();

        assertEquals("/", config.getContextPath());
    }

    @Test
    void normalizeContextPathNullBecomesRoot() {
        DeployableConfiguration config = DeployableConfiguration.builder()
                .moduleName(MODULE_NAME)
                .sourcePath(Path.of("/source"))
                .deployDir(Path.of("/target"))
                .contextPath(null)
                .build();

        assertEquals("/", config.getContextPath());
    }

    @Test
    void autopublishDefaults() {
        DeployableConfiguration config = DeployableConfiguration.builder()
                .moduleName(MODULE_NAME)
                .sourcePath(Path.of("/source"))
                .deployDir(Path.of("/target"))
                .build();

        assertFalse(config.isAutopublishEnabled());
        // Default of 30 applies when not explicitly set
        assertEquals(30, config.getAutopublishInactivityLimit());
    }

    @Test
    void autopublishEnabled() {
        DeployableConfiguration config = DeployableConfiguration.builder()
                .moduleName(MODULE_NAME)
                .sourcePath(Path.of("/source"))
                .deployDir(Path.of("/target"))
                .autopublishEnabled(true)
                .autopublishInactivityLimit(60)
                .build();

        assertTrue(config.isAutopublishEnabled());
        assertEquals(60, config.getAutopublishInactivityLimit());
    }

    @Test
    void getTargetDirectoryNameNestedContext() {
        DeployableConfiguration config = DeployableConfiguration.builder()
                .moduleName(MODULE_NAME)
                .sourcePath(Path.of("/source"))
                .deployDir(Path.of("/target"))
                .contextPath("/api/v1")
                .build();

        // Should be "api#v1" for nested paths in Tomcat
        assertEquals("api#v1", config.getTargetDirectoryName());
    }

    @Test
    void moduleNameIsRequired() {
        assertThrows(NullPointerException.class, () ->
                DeployableConfiguration.builder()
                        .sourcePath(Path.of("/source"))
                        .build()
        );
    }

    @Test
    void sourcePathIsRequired() {
        assertThrows(NullPointerException.class, () ->
                DeployableConfiguration.builder()
                        .moduleName(MODULE_NAME)
                        .build()
        );
    }
}
