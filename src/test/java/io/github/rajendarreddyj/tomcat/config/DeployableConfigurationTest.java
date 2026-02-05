package io.github.rajendarreddyj.tomcat.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link DeployableConfiguration}.
 *
 * <p>
 * Tests the deployment configuration builder including context path
 * normalization, target directory name derivation, and auto-publish settings.
 *
 * @author rajendarreddyj
 * @see DeployableConfiguration
 */
class DeployableConfigurationTest {

    /** Test module name constant. */
    private static final String MODULE_NAME = "test-module";

    /**
     * Verifies that target directory name uses deploymentOutputName when specified.
     */
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

    /**
     * Verifies that target directory name is ROOT for root context path.
     */
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

    /**
     * Verifies that target directory name is derived from context path.
     */
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

    /**
     * Verifies that context path normalization adds leading slash when missing.
     */
    @Test
    void normalizeContextPathAddsLeadingSlash() {
        DeployableConfiguration config = DeployableConfiguration.builder()
                .moduleName(MODULE_NAME)
                .sourcePath(Path.of("/source"))
                .deployDir(Path.of("/target"))
                .contextPath("myapp") // missing leading /
                .build();

        assertEquals("/myapp", config.getContextPath());
    }

    /**
     * Verifies that context path normalization preserves existing leading slash.
     */
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

    /**
     * Verifies that empty context path is normalized to root.
     */
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

    /**
     * Verifies that null context path is normalized to root.
     */
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

    /**
     * Verifies that auto-publish is disabled by default with correct inactivity
     * limit.
     */
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

    /**
     * Verifies that auto-publish settings can be configured.
     */
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

    /**
     * Verifies that nested context paths are converted to Tomcat format using hash
     * separator.
     */
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

    /**
     * Verifies that builder throws NullPointerException when moduleName is not set.
     */
    @Test
    void moduleNameIsRequired() {
        assertThrows(NullPointerException.class, () -> DeployableConfiguration.builder()
                .sourcePath(Path.of("/source"))
                .build());
    }

    /**
     * Verifies that builder throws NullPointerException when sourcePath is not set.
     */
    @Test
    void sourcePathIsRequired() {
        assertThrows(NullPointerException.class, () -> DeployableConfiguration.builder()
                .moduleName(MODULE_NAME)
                .build());
    }
}
