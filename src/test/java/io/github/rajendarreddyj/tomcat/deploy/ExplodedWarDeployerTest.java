package io.github.rajendarreddyj.tomcat.deploy;

import io.github.rajendarreddyj.tomcat.config.DeployableConfiguration;
import org.apache.maven.plugin.logging.Log;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExplodedWarDeployerTest {

    @TempDir
    Path tempDir;

    @Mock
    private Log log;

    private ExplodedWarDeployer deployer;
    private Path sourceDir;
    private Path webappsDir;

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

    @Test
    void deployCreatesTargetDirectory() throws IOException {
        DeployableConfiguration config = createConfig("/myapp");

        deployer.deploy(config);

        assertTrue(Files.isDirectory(webappsDir.resolve("myapp")));
    }

    @Test
    void deployCopiesAllFiles() throws IOException {
        DeployableConfiguration config = createConfig("/myapp");

        deployer.deploy(config);

        assertTrue(Files.exists(webappsDir.resolve("myapp").resolve("index.html")));
        assertTrue(Files.exists(webappsDir.resolve("myapp").resolve("WEB-INF").resolve("web.xml")));
        assertTrue(Files.exists(webappsDir.resolve("myapp").resolve("css").resolve("style.css")));
        assertTrue(Files.exists(webappsDir.resolve("myapp").resolve("js").resolve("app.js")));
    }

    @Test
    void deployPreservesFileContent() throws IOException {
        DeployableConfiguration config = createConfig("/myapp");

        deployer.deploy(config);

        assertEquals("<html>Test</html>",
                Files.readString(webappsDir.resolve("myapp").resolve("index.html")));
    }

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

    @Test
    void redeployWorksWithNoExistingDeployment() throws IOException {
        DeployableConfiguration config = createConfig("/newapp");

        deployer.redeploy(config);

        assertTrue(Files.isDirectory(webappsDir.resolve("newapp")));
    }

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

    @Test
    void deployLogsDeploymentInfo() throws IOException {
        DeployableConfiguration config = createConfig("/myapp");

        deployer.deploy(config);

        verify(log).info(contains("Deploying"));
        verify(log).info(contains("Deployment complete"));
    }

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

    private DeployableConfiguration createConfig(String contextPath) {
        return DeployableConfiguration.builder()
                .moduleName("test-module")
                .sourcePath(sourceDir)
                .deployDir(webappsDir)
                .contextPath(contextPath)
                .build();
    }
}
