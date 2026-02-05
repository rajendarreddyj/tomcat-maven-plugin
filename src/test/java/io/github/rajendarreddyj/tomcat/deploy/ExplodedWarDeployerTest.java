package io.github.rajendarreddyj.tomcat.deploy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.plugin.logging.Log;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.github.rajendarreddyj.tomcat.config.DeployableConfiguration;

/**
 * Unit tests for {@link ExplodedWarDeployer}.
 *
 * <p>
 * Tests the exploded WAR deployment functionality including
 * file copying, directory creation, and redeployment scenarios.
 *
 * @author rajendarreddyj
 * @see ExplodedWarDeployer
 */
class ExplodedWarDeployerTest {

    /**
     * Temporary directory for test artifacts, cleaned up automatically after each
     * test.
     */
    @TempDir
    Path tempDir;

    /** Mock Maven logger for testing. */
    @Mock
    private Log log;

    /** The ExplodedWarDeployer instance under test. */
    private ExplodedWarDeployer deployer;

    /** Path to the source webapp directory. */
    private Path sourceDir;

    /** Path to Tomcat's webapps directory. */
    private Path webappsDir;

    /**
     * Sets up the test environment before each test.
     *
     * <p>
     * Creates a mock source webapp structure with typical files.
     *
     * @throws IOException if setup fails
     */
    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);
        deployer = new ExplodedWarDeployer(log);

        sourceDir = tempDir.resolve("source-webapp");
        webappsDir = tempDir.resolve("webapps");

        // Create source webapp structure
        Files.createDirectories(sourceDir.resolve("WEB-INF"));
        Files.createDirectories(sourceDir.resolve("css"));
        Files.createDirectories(sourceDir.resolve("js"));
        Files.writeString(sourceDir.resolve("index.html"), "<html>Test</html>");
        Files.writeString(sourceDir.resolve("WEB-INF").resolve("web.xml"), "<web-app/>");
        Files.writeString(sourceDir.resolve("css").resolve("style.css"), "body {}");
        Files.writeString(sourceDir.resolve("js").resolve("app.js"), "console.log('test');");
    }

    /**
     * Verifies that deploy creates the target directory.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void deployCreatesTargetDirectory() throws IOException {
        DeployableConfiguration config = createConfig("/myapp");

        deployer.deploy(config);

        assertTrue(Files.isDirectory(webappsDir.resolve("myapp")));
    }

    /**
     * Verifies that deploy copies all source files to target directory.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void deployCopiesAllFiles() throws IOException {
        DeployableConfiguration config = createConfig("/myapp");

        deployer.deploy(config);

        assertTrue(Files.exists(webappsDir.resolve("myapp").resolve("index.html")));
        assertTrue(Files.exists(webappsDir.resolve("myapp").resolve("WEB-INF").resolve("web.xml")));
        assertTrue(Files.exists(webappsDir.resolve("myapp").resolve("css").resolve("style.css")));
        assertTrue(Files.exists(webappsDir.resolve("myapp").resolve("js").resolve("app.js")));
    }

    /**
     * Verifies that deploy preserves file content correctly.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void deployPreservesFileContent() throws IOException {
        DeployableConfiguration config = createConfig("/myapp");

        deployer.deploy(config);

        assertEquals("<html>Test</html>",
                Files.readString(webappsDir.resolve("myapp").resolve("index.html")));
    }

    /**
     * Verifies that deploy throws exception for non-existent source directory.
     */
    @Test
    void deployThrowsExceptionForNonExistentSource() {
        DeployableConfiguration config = DeployableConfiguration.builder()
                .moduleName("test")
                .sourcePath(tempDir.resolve("nonexistent"))
                .deployDir(webappsDir)
                .contextPath("/myapp")
                .build();

        assertThrows(IOException.class, () -> deployer.deploy(config));
    }

    /**
     * Verifies that deploy removes existing deployment before deploying.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void deployRemovesExistingDeployment() throws IOException {
        DeployableConfiguration config = createConfig("/myapp");

        // Create existing deployment
        Path existingFile = webappsDir.resolve("myapp").resolve("old.txt");
        Files.createDirectories(existingFile.getParent());
        Files.writeString(existingFile, "old content");

        deployer.deploy(config);

        assertFalse(Files.exists(existingFile));
        assertTrue(Files.exists(webappsDir.resolve("myapp").resolve("index.html")));
    }

    /**
     * Verifies that deploy creates ROOT directory for root context path.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void deployDeploysAsRoot() throws IOException {
        DeployableConfiguration config = DeployableConfiguration.builder()
                .moduleName("test")
                .sourcePath(sourceDir)
                .deployDir(webappsDir)
                .contextPath("/")
                .build();

        deployer.deploy(config);

        assertTrue(Files.isDirectory(webappsDir.resolve("ROOT")));
        assertTrue(Files.exists(webappsDir.resolve("ROOT").resolve("index.html")));
    }

    /**
     * Verifies that deploy uses custom deployment output name when specified.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void deployUsesDeploymentOutputName() throws IOException {
        DeployableConfiguration config = DeployableConfiguration.builder()
                .moduleName("test")
                .sourcePath(sourceDir)
                .deployDir(webappsDir)
                .contextPath("/myapp")
                .deploymentOutputName("custom-name")
                .build();

        deployer.deploy(config);

        assertTrue(Files.isDirectory(webappsDir.resolve("custom-name")));
    }

    /**
     * Verifies that redeploy removes existing deployment and recreates it.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void redeployRemovesAndRecreates() throws IOException {
        DeployableConfiguration config = createConfig("/myapp");

        // Initial deployment
        deployer.deploy(config);
        Path markerFile = webappsDir.resolve("myapp").resolve("marker.txt");
        Files.writeString(markerFile, "marker");

        // Redeploy
        deployer.redeploy(config);

        assertFalse(Files.exists(markerFile));
        assertTrue(Files.exists(webappsDir.resolve("myapp").resolve("index.html")));
    }

    /**
     * Verifies that redeploy works when no existing deployment exists.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void redeployWorksWithNoExistingDeployment() throws IOException {
        DeployableConfiguration config = createConfig("/newapp");

        deployer.redeploy(config);

        assertTrue(Files.isDirectory(webappsDir.resolve("newapp")));
    }

    /**
     * Verifies that syncChanges copies changed file to deployment.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void syncChangesCopiesChangedFile() throws IOException {
        DeployableConfiguration config = createConfig("/myapp");
        deployer.deploy(config);

        // Modify source
        Path changedFile = sourceDir.resolve("index.html");
        Files.writeString(changedFile, "<html>Updated</html>");

        deployer.syncChanges(config, changedFile);

        assertEquals("<html>Updated</html>",
                Files.readString(webappsDir.resolve("myapp").resolve("index.html")));
    }

    /**
     * Verifies that syncChanges creates parent directories for new files.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void syncChangesCreatesParentDirectories() throws IOException {
        DeployableConfiguration config = createConfig("/myapp");
        deployer.deploy(config);

        // Add new file in new directory
        Path newDir = sourceDir.resolve("new-dir");
        Files.createDirectories(newDir);
        Path newFile = newDir.resolve("new.txt");
        Files.writeString(newFile, "new content");

        deployer.syncChanges(config, newFile);

        assertTrue(Files.exists(webappsDir.resolve("myapp").resolve("new-dir").resolve("new.txt")));
    }

    /**
     * Verifies that deploy creates webapps directory if it doesn't exist.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void deployCreatesWebappsDirectory() throws IOException {
        Path newWebapps = tempDir.resolve("new-webapps");
        DeployableConfiguration config = DeployableConfiguration.builder()
                .moduleName("test")
                .sourcePath(sourceDir)
                .deployDir(newWebapps)
                .contextPath("/myapp")
                .build();

        deployer.deploy(config);

        assertTrue(Files.isDirectory(newWebapps));
        assertTrue(Files.isDirectory(newWebapps.resolve("myapp")));
    }

    /**
     * Verifies that deploy handles nested context paths using Tomcat hash format.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void deployHandlesNestedContextPath() throws IOException {
        DeployableConfiguration config = DeployableConfiguration.builder()
                .moduleName("test")
                .sourcePath(sourceDir)
                .deployDir(webappsDir)
                .contextPath("/api/v1")
                .build();

        deployer.deploy(config);

        assertTrue(Files.isDirectory(webappsDir.resolve("api#v1")));
    }

    /**
     * Verifies that deploy logs deployment information.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void deployLogsDeploymentInfo() throws IOException {
        DeployableConfiguration config = createConfig("/myapp");

        deployer.deploy(config);

        verify(log).info(contains("Deploying"));
        verify(log).info(contains("Deployment complete"));
    }

    /**
     * Verifies that deploy handles empty source directory correctly.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void deployHandlesEmptySourceDirectory() throws IOException {
        Path emptySource = tempDir.resolve("empty-source");
        Files.createDirectories(emptySource);

        DeployableConfiguration config = DeployableConfiguration.builder()
                .moduleName("test")
                .sourcePath(emptySource)
                .deployDir(webappsDir)
                .contextPath("/empty")
                .build();

        deployer.deploy(config);

        assertTrue(Files.isDirectory(webappsDir.resolve("empty")));
    }

    /**
     * Verifies that deploy handles deeply nested directory structures.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void deployHandlesDeepNestedStructure() throws IOException {
        Path deepPath = sourceDir.resolve("a").resolve("b").resolve("c").resolve("d");
        Files.createDirectories(deepPath);
        Files.writeString(deepPath.resolve("deep.txt"), "deep content");

        DeployableConfiguration config = createConfig("/myapp");
        deployer.deploy(config);

        assertTrue(Files.exists(webappsDir.resolve("myapp").resolve("a").resolve("b")
                .resolve("c").resolve("d").resolve("deep.txt")));
    }

    /**
     * Creates a DeployableConfiguration for testing.
     *
     * @param contextPath the context path for the deployment
     * @return a configured DeployableConfiguration instance
     */
    private DeployableConfiguration createConfig(String contextPath) {
        return DeployableConfiguration.builder()
                .moduleName("test-module")
                .sourcePath(sourceDir)
                .deployDir(webappsDir)
                .contextPath(contextPath)
                .build();
    }
}
