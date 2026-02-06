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

    /**
     * Verifies that context path normalization removes trailing slash.
     *
     * <p>
     * Context path "/myapp/" should be normalized to "/myapp".
     * </p>
     */
    @Test
    void normalizeContextPathRemovesTrailingSlash() {
        DeployableConfiguration config = DeployableConfiguration.builder()
                .moduleName(MODULE_NAME)
                .sourcePath(Path.of("/source"))
                .deployDir(Path.of("/target"))
                .contextPath("/myapp/")
                .build();

        assertEquals("/myapp", config.getContextPath());
    }

    /**
     * Verifies that blank context path (whitespace only) is normalized to root.
     *
     * <p>
     * Context path with spaces should be treated as empty and normalized to "/".
     * </p>
     */
    @Test
    void normalizeContextPathBlankBecomesRoot() {
        DeployableConfiguration config = DeployableConfiguration.builder()
                .moduleName(MODULE_NAME)
                .sourcePath(Path.of("/source"))
                .deployDir(Path.of("/target"))
                .contextPath("   ")
                .build();

        assertEquals("/", config.getContextPath());
    }

    /**
     * Verifies that context path with leading/trailing whitespace is trimmed.
     *
     * <p>
     * Whitespace around the context path should be removed.
     * </p>
     */
    @Test
    void normalizeContextPathTrimsWhitespace() {
        DeployableConfiguration config = DeployableConfiguration.builder()
                .moduleName(MODULE_NAME)
                .sourcePath(Path.of("/source"))
                .deployDir(Path.of("/target"))
                .contextPath("  /myapp  ")
                .build();

        assertEquals("/myapp", config.getContextPath());
    }

    /**
     * Verifies that target directory name returns empty string when
     * deploymentOutputName is blank.
     *
     * <p>
     * When deploymentOutputName is blank (but not null), it should fall back
     * to deriving the name from context path.
     * </p>
     */
    @Test
    void getTargetDirectoryNameWithBlankDeploymentOutputName() {
        DeployableConfiguration config = DeployableConfiguration.builder()
                .moduleName(MODULE_NAME)
                .sourcePath(Path.of("/source"))
                .deployDir(Path.of("/target"))
                .contextPath("/myapp")
                .deploymentOutputName("   ")
                .build();

        // Blank deploymentOutputName should fall back to context path derivation
        assertEquals("myapp", config.getTargetDirectoryName());
    }

    /**
     * Verifies that toString() contains all relevant field information.
     */
    @Test
    void toStringContainsAllFields() {
        DeployableConfiguration config = DeployableConfiguration.builder()
                .moduleName(MODULE_NAME)
                .sourcePath(Path.of("/source"))
                .deployDir(Path.of("/target"))
                .contextPath("/myapp")
                .autopublishEnabled(true)
                .autopublishInactivityLimit(45)
                .deploymentOutputName("custom-output")
                .build();

        String str = config.toString();
        assertTrue(str.contains("DeployableConfiguration"));
        assertTrue(str.contains("moduleName"));
        assertTrue(str.contains("sourcePath"));
        assertTrue(str.contains("contextPath"));
        assertTrue(str.contains("deployDir"));
        assertTrue(str.contains("autopublishEnabled"));
        assertTrue(str.contains("autopublishInactivityLimit"));
        assertTrue(str.contains("deploymentOutputName"));
    }

    /**
     * Verifies that getModuleName() returns the configured module name.
     */
    @Test
    void getModuleNameReturnsConfiguredValue() {
        DeployableConfiguration config = DeployableConfiguration.builder()
                .moduleName(MODULE_NAME)
                .sourcePath(Path.of("/source"))
                .build();

        assertEquals(MODULE_NAME, config.getModuleName());
    }

    /**
     * Verifies that getSourcePath() returns the configured source path.
     */
    @Test
    void getSourcePathReturnsConfiguredValue() {
        Path sourcePath = Path.of("/my/source/path");
        DeployableConfiguration config = DeployableConfiguration.builder()
                .moduleName(MODULE_NAME)
                .sourcePath(sourcePath)
                .build();

        assertEquals(sourcePath, config.getSourcePath());
    }

    /**
     * Verifies that getDeployDir() returns the configured deploy directory.
     */
    @Test
    void getDeployDirReturnsConfiguredValue() {
        Path deployDir = Path.of("/deploy/webapps");
        DeployableConfiguration config = DeployableConfiguration.builder()
                .moduleName(MODULE_NAME)
                .sourcePath(Path.of("/source"))
                .deployDir(deployDir)
                .build();

        assertEquals(deployDir, config.getDeployDir());
    }

    /**
     * Verifies that getDeployDir() returns null when not configured.
     */
    @Test
    void getDeployDirReturnsNullWhenNotConfigured() {
        DeployableConfiguration config = DeployableConfiguration.builder()
                .moduleName(MODULE_NAME)
                .sourcePath(Path.of("/source"))
                .build();

        assertEquals(null, config.getDeployDir());
    }

    /**
     * Verifies that getDeploymentOutputName() returns the configured value.
     */
    @Test
    void getDeploymentOutputNameReturnsConfiguredValue() {
        DeployableConfiguration config = DeployableConfiguration.builder()
                .moduleName(MODULE_NAME)
                .sourcePath(Path.of("/source"))
                .deploymentOutputName("custom-webapp")
                .build();

        assertEquals("custom-webapp", config.getDeploymentOutputName());
    }

    /**
     * Verifies that getDeploymentOutputName() returns null when not configured.
     */
    @Test
    void getDeploymentOutputNameReturnsNullWhenNotConfigured() {
        DeployableConfiguration config = DeployableConfiguration.builder()
                .moduleName(MODULE_NAME)
                .sourcePath(Path.of("/source"))
                .build();

        assertEquals(null, config.getDeploymentOutputName());
    }

    /**
     * Verifies that autopublishInactivityLimit defaults to 30 when set to zero.
     */
    @Test
    void autopublishInactivityLimitDefaultsToThirtyWhenZero() {
        DeployableConfiguration config = DeployableConfiguration.builder()
                .moduleName(MODULE_NAME)
                .sourcePath(Path.of("/source"))
                .autopublishInactivityLimit(0)
                .build();

        assertEquals(30, config.getAutopublishInactivityLimit());
    }

    /**
     * Verifies that autopublishInactivityLimit defaults to 30 when set to negative.
     */
    @Test
    void autopublishInactivityLimitDefaultsToThirtyWhenNegative() {
        DeployableConfiguration config = DeployableConfiguration.builder()
                .moduleName(MODULE_NAME)
                .sourcePath(Path.of("/source"))
                .autopublishInactivityLimit(-10)
                .build();

        assertEquals(30, config.getAutopublishInactivityLimit());
    }
}
