package io.github.rajendarreddyj.tomcat.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CatalinaBaseGeneratorTest {

    @TempDir
    Path tempDir;

    private Path catalinaHome;
    private Path catalinaBase;

    @BeforeEach
    void setUp() throws IOException {
        catalinaHome = tempDir.resolve("tomcat-home");
        catalinaBase = tempDir.resolve("tomcat-base");

        // Create mock CATALINA_HOME structure
        Files.createDirectories(catalinaHome.resolve("bin"));
        Files.createDirectories(catalinaHome.resolve("conf"));
        Files.createDirectories(catalinaHome.resolve("lib"));

        // Create a minimal server.xml
        String serverXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <Server port="8005" shutdown="SHUTDOWN">
                    <Service name="Catalina">
                        <Connector port="8080" protocol="HTTP/1.1"
                                   connectionTimeout="20000"
                                   redirectPort="8443" />
                        <Connector port="8009" protocol="AJP/1.3" redirectPort="8443" />
                        <Engine name="Catalina" defaultHost="localhost">
                            <Host name="localhost"  appBase="webapps">
                            </Host>
                        </Engine>
                    </Service>
                </Server>
                """;
        Files.writeString(catalinaHome.resolve("conf").resolve("server.xml"), serverXml);

        // Create other config files
        Files.writeString(catalinaHome.resolve("conf").resolve("web.xml"), "<web-app/>");
        Files.writeString(catalinaHome.resolve("conf").resolve("context.xml"), "<Context/>");
    }

    @Test
    void generateCreatesRequiredDirectories() throws IOException {
        CatalinaBaseGenerator.generate(catalinaHome, catalinaBase, 9090, "localhost");

        assertTrue(Files.isDirectory(catalinaBase.resolve("conf")));
        assertTrue(Files.isDirectory(catalinaBase.resolve("logs")));
        assertTrue(Files.isDirectory(catalinaBase.resolve("temp")));
        assertTrue(Files.isDirectory(catalinaBase.resolve("webapps")));
        assertTrue(Files.isDirectory(catalinaBase.resolve("work")));
    }

    @Test
    void generateCopiesConfigFiles() throws IOException {
        CatalinaBaseGenerator.generate(catalinaHome, catalinaBase, 9090, "localhost");

        assertTrue(Files.exists(catalinaBase.resolve("conf").resolve("server.xml")));
        assertTrue(Files.exists(catalinaBase.resolve("conf").resolve("web.xml")));
        assertTrue(Files.exists(catalinaBase.resolve("conf").resolve("context.xml")));
    }

    @Test
    void generateModifiesHttpPort() throws IOException {
        CatalinaBaseGenerator.generate(catalinaHome, catalinaBase, 9090, "localhost");

        String serverXml = Files.readString(catalinaBase.resolve("conf").resolve("server.xml"));
        assertTrue(serverXml.contains("port=\"9090\""));
        assertFalse(serverXml.contains("port=\"8080\""));
    }

    @Test
    void generateDisablesShutdownPort() throws IOException {
        CatalinaBaseGenerator.generate(catalinaHome, catalinaBase, 9090, "localhost");

        String serverXml = Files.readString(catalinaBase.resolve("conf").resolve("server.xml"));
        assertTrue(serverXml.contains("port=\"-1\""));
        assertFalse(serverXml.contains("port=\"8005\""));
    }

    @Test
    void generateCommentsOutAjpConnector() throws IOException {
        CatalinaBaseGenerator.generate(catalinaHome, catalinaBase, 9090, "localhost");

        String serverXml = Files.readString(catalinaBase.resolve("conf").resolve("server.xml"));
        assertTrue(serverXml.contains("<!-- Disabled for plugin use:"));
    }

    @Test
    void generateAddsAddressForCustomHost() throws IOException {
        CatalinaBaseGenerator.generate(catalinaHome, catalinaBase, 9090, "192.168.1.100");

        String serverXml = Files.readString(catalinaBase.resolve("conf").resolve("server.xml"));
        assertTrue(serverXml.contains("address=\"192.168.1.100\""));
    }

    @Test
    void generateDoesNotAddAddressForLocalhost() throws IOException {
        CatalinaBaseGenerator.generate(catalinaHome, catalinaBase, 9090, "localhost");

        String serverXml = Files.readString(catalinaBase.resolve("conf").resolve("server.xml"));
        assertFalse(serverXml.contains("address=\"localhost\""));
    }

    @Test
    void generateDoesNotAddAddressForAllInterfaces() throws IOException {
        CatalinaBaseGenerator.generate(catalinaHome, catalinaBase, 9090, "0.0.0.0");

        String serverXml = Files.readString(catalinaBase.resolve("conf").resolve("server.xml"));
        assertFalse(serverXml.contains("address=\"0.0.0.0\""));
    }

    @Test
    void generateHandlesNullHost() throws IOException {
        CatalinaBaseGenerator.generate(catalinaHome, catalinaBase, 9090, null);

        String serverXml = Files.readString(catalinaBase.resolve("conf").resolve("server.xml"));
        // Should still work without throwing
        assertTrue(serverXml.contains("port=\"9090\""));
    }

    @Test
    void generateHandlesEmptyHost() throws IOException {
        CatalinaBaseGenerator.generate(catalinaHome, catalinaBase, 9090, "");

        String serverXml = Files.readString(catalinaBase.resolve("conf").resolve("server.xml"));
        assertTrue(serverXml.contains("port=\"9090\""));
    }

    @Test
    void generateHandlesMissingConfDirectory() throws IOException {
        Path emptyHome = tempDir.resolve("empty-home");
        Files.createDirectories(emptyHome);

        CatalinaBaseGenerator.generate(emptyHome, catalinaBase, 9090, "localhost");

        // Should create empty structure
        assertTrue(Files.isDirectory(catalinaBase.resolve("conf")));
        assertFalse(Files.exists(catalinaBase.resolve("conf").resolve("server.xml")));
    }

    @Test
    void generateCopiesSubdirectories() throws IOException {
        Path subDir = catalinaHome.resolve("conf").resolve("Catalina").resolve("localhost");
        Files.createDirectories(subDir);
        Files.writeString(subDir.resolve("manager.xml"), "<Context/>");

        CatalinaBaseGenerator.generate(catalinaHome, catalinaBase, 9090, "localhost");

        assertTrue(Files.exists(catalinaBase.resolve("conf").resolve("Catalina").resolve("localhost").resolve("manager.xml")));
    }

    @Test
    void isValidCatalinaBaseReturnsTrueForValidBase() throws IOException {
        CatalinaBaseGenerator.generate(catalinaHome, catalinaBase, 9090, "localhost");

        assertTrue(CatalinaBaseGenerator.isValidCatalinaBase(catalinaBase));
    }

    @Test
    void isValidCatalinaBaseReturnsFalseForNonExistentDirectory() {
        assertFalse(CatalinaBaseGenerator.isValidCatalinaBase(tempDir.resolve("nonexistent")));
    }

    @Test
    void isValidCatalinaBaseReturnsFalseForMissingConf() throws IOException {
        Files.createDirectories(catalinaBase);
        assertFalse(CatalinaBaseGenerator.isValidCatalinaBase(catalinaBase));
    }

    @Test
    void isValidCatalinaBaseReturnsFalseForMissingServerXml() throws IOException {
        Files.createDirectories(catalinaBase.resolve("conf"));
        assertFalse(CatalinaBaseGenerator.isValidCatalinaBase(catalinaBase));
    }

    @Test
    void isValidCatalinaBaseReturnsFalseForFile() throws IOException {
        Path file = tempDir.resolve("somefile.txt");
        Files.writeString(file, "not a directory");
        assertFalse(CatalinaBaseGenerator.isValidCatalinaBase(file));
    }

    @Test
    void generateOverwritesExistingBase() throws IOException {
        // Create initial base
        CatalinaBaseGenerator.generate(catalinaHome, catalinaBase, 8080, "localhost");
        String initial = Files.readString(catalinaBase.resolve("conf").resolve("server.xml"));

        // Regenerate with different port
        CatalinaBaseGenerator.generate(catalinaHome, catalinaBase, 9999, "localhost");
        String updated = Files.readString(catalinaBase.resolve("conf").resolve("server.xml"));

        assertNotEquals(initial, updated);
        assertTrue(updated.contains("port=\"9999\""));
    }
}
