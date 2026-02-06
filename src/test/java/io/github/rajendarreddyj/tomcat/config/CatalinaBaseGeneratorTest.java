package io.github.rajendarreddyj.tomcat.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for {@link CatalinaBaseGenerator}.
 *
 * <p>
 * Tests the CATALINA_BASE generation including directory structure creation,
 * server.xml modification, and port configuration.
 *
 * @author rajendarreddyj
 * @see CatalinaBaseGenerator
 */
class CatalinaBaseGeneratorTest {

    /**
     * Temporary directory for test artifacts, cleaned up automatically after each
     * test.
     */
    @TempDir
    Path tempDir;

    /** Path to the mock CATALINA_HOME directory. */
    private Path catalinaHome;

    /** Path to the generated CATALINA_BASE directory. */
    private Path catalinaBase;

    /**
     * Sets up the test environment before each test.
     *
     * <p>
     * Creates a mock CATALINA_HOME structure with minimal configuration files.
     *
     * @throws IOException if setup fails
     */
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

    /**
     * Verifies that generate creates all required directories in CATALINA_BASE.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void generateCreatesRequiredDirectories() throws IOException {
        CatalinaBaseGenerator.generate(catalinaHome, catalinaBase, 9090, "localhost");

        assertTrue(Files.isDirectory(catalinaBase.resolve("conf")));
        assertTrue(Files.isDirectory(catalinaBase.resolve("logs")));
        assertTrue(Files.isDirectory(catalinaBase.resolve("temp")));
        assertTrue(Files.isDirectory(catalinaBase.resolve("webapps")));
        assertTrue(Files.isDirectory(catalinaBase.resolve("work")));
    }

    /**
     * Verifies that generate copies configuration files from CATALINA_HOME to
     * CATALINA_BASE.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void generateCopiesConfigFiles() throws IOException {
        CatalinaBaseGenerator.generate(catalinaHome, catalinaBase, 9090, "localhost");

        assertTrue(Files.exists(catalinaBase.resolve("conf").resolve("server.xml")));
        assertTrue(Files.exists(catalinaBase.resolve("conf").resolve("web.xml")));
        assertTrue(Files.exists(catalinaBase.resolve("conf").resolve("context.xml")));
    }

    /**
     * Verifies that generate modifies the HTTP connector port in server.xml.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void generateModifiesHttpPort() throws IOException {
        CatalinaBaseGenerator.generate(catalinaHome, catalinaBase, 9090, "localhost");

        String serverXml = Files.readString(catalinaBase.resolve("conf").resolve("server.xml"));
        assertTrue(serverXml.contains("port=\"9090\""));
        assertFalse(serverXml.contains("port=\"8080\""));
    }

    /**
     * Verifies that generate disables the shutdown port in server.xml.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void generateDisablesShutdownPort() throws IOException {
        CatalinaBaseGenerator.generate(catalinaHome, catalinaBase, 9090, "localhost");

        String serverXml = Files.readString(catalinaBase.resolve("conf").resolve("server.xml"));
        assertTrue(serverXml.contains("port=\"-1\""));
        assertFalse(serverXml.contains("port=\"8005\""));
    }

    /**
     * Verifies that generate comments out the AJP connector in server.xml.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void generateCommentsOutAjpConnector() throws IOException {
        CatalinaBaseGenerator.generate(catalinaHome, catalinaBase, 9090, "localhost");

        String serverXml = Files.readString(catalinaBase.resolve("conf").resolve("server.xml"));
        assertTrue(serverXml.contains("<!-- Disabled for plugin use:"));
    }

    /**
     * Verifies that generate adds address attribute for custom hosts.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void generateAddsAddressForCustomHost() throws IOException {
        CatalinaBaseGenerator.generate(catalinaHome, catalinaBase, 9090, "192.168.1.100");

        String serverXml = Files.readString(catalinaBase.resolve("conf").resolve("server.xml"));
        assertTrue(serverXml.contains("address=\"192.168.1.100\""));
    }

    /**
     * Verifies that generate does not add address attribute for localhost.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void generateDoesNotAddAddressForLocalhost() throws IOException {
        CatalinaBaseGenerator.generate(catalinaHome, catalinaBase, 9090, "localhost");

        String serverXml = Files.readString(catalinaBase.resolve("conf").resolve("server.xml"));
        assertFalse(serverXml.contains("address=\"localhost\""));
    }

    /**
     * Verifies that generate does not add address attribute for all interfaces
     * (0.0.0.0).
     *
     * @throws IOException if file operations fail
     */
    @Test
    void generateDoesNotAddAddressForAllInterfaces() throws IOException {
        CatalinaBaseGenerator.generate(catalinaHome, catalinaBase, 9090, "0.0.0.0");

        String serverXml = Files.readString(catalinaBase.resolve("conf").resolve("server.xml"));
        assertFalse(serverXml.contains("address=\"0.0.0.0\""));
    }

    /**
     * Verifies that generate handles null host parameter gracefully.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void generateHandlesNullHost() throws IOException {
        CatalinaBaseGenerator.generate(catalinaHome, catalinaBase, 9090, null);

        String serverXml = Files.readString(catalinaBase.resolve("conf").resolve("server.xml"));
        // Should still work without throwing
        assertTrue(serverXml.contains("port=\"9090\""));
    }

    /**
     * Verifies that generate handles empty host parameter gracefully.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void generateHandlesEmptyHost() throws IOException {
        CatalinaBaseGenerator.generate(catalinaHome, catalinaBase, 9090, "");

        String serverXml = Files.readString(catalinaBase.resolve("conf").resolve("server.xml"));
        assertTrue(serverXml.contains("port=\"9090\""));
    }

    /**
     * Verifies that generate handles missing conf directory in CATALINA_HOME.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void generateHandlesMissingConfDirectory() throws IOException {
        Path emptyHome = tempDir.resolve("empty-home");
        Files.createDirectories(emptyHome);

        CatalinaBaseGenerator.generate(emptyHome, catalinaBase, 9090, "localhost");

        // Should create empty structure
        assertTrue(Files.isDirectory(catalinaBase.resolve("conf")));
        assertFalse(Files.exists(catalinaBase.resolve("conf").resolve("server.xml")));
    }

    /**
     * Verifies that generate copies subdirectories from CATALINA_HOME conf.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void generateCopiesSubdirectories() throws IOException {
        Path subDir = catalinaHome.resolve("conf").resolve("Catalina").resolve("localhost");
        Files.createDirectories(subDir);
        Files.writeString(subDir.resolve("manager.xml"), "<Context/>");

        CatalinaBaseGenerator.generate(catalinaHome, catalinaBase, 9090, "localhost");

        assertTrue(Files
                .exists(catalinaBase.resolve("conf").resolve("Catalina").resolve("localhost").resolve("manager.xml")));
    }

    /**
     * Verifies that isValidCatalinaBase returns true for a valid CATALINA_BASE.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void isValidCatalinaBaseReturnsTrueForValidBase() throws IOException {
        CatalinaBaseGenerator.generate(catalinaHome, catalinaBase, 9090, "localhost");

        assertTrue(CatalinaBaseGenerator.isValidCatalinaBase(catalinaBase));
    }

    /**
     * Verifies that isValidCatalinaBase returns false for non-existent directory.
     */
    @Test
    void isValidCatalinaBaseReturnsFalseForNonExistentDirectory() {
        assertFalse(CatalinaBaseGenerator.isValidCatalinaBase(tempDir.resolve("nonexistent")));
    }

    /**
     * Verifies that isValidCatalinaBase returns false when conf directory is
     * missing.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void isValidCatalinaBaseReturnsFalseForMissingConf() throws IOException {
        Files.createDirectories(catalinaBase);
        assertFalse(CatalinaBaseGenerator.isValidCatalinaBase(catalinaBase));
    }

    /**
     * Verifies that isValidCatalinaBase returns false when server.xml is missing.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void isValidCatalinaBaseReturnsFalseForMissingServerXml() throws IOException {
        Files.createDirectories(catalinaBase.resolve("conf"));
        assertFalse(CatalinaBaseGenerator.isValidCatalinaBase(catalinaBase));
    }

    /**
     * Verifies that isValidCatalinaBase returns false when path is a file, not
     * directory.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void isValidCatalinaBaseReturnsFalseForFile() throws IOException {
        Path file = tempDir.resolve("somefile.txt");
        Files.writeString(file, "not a directory");
        assertFalse(CatalinaBaseGenerator.isValidCatalinaBase(file));
    }

    /**
     * Verifies that generate can overwrite an existing CATALINA_BASE.
     *
     * @throws IOException if file operations fail
     */
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

    /**
     * Verifies that generate modifies HTTP port when protocol attribute comes
     * before port.
     * This tests the alternate attribute ordering that some Tomcat versions may
     * use.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void generateModifiesHttpPortWithProtocolBeforePort() throws IOException {
        // Create a server.xml with protocol before port (alternate ordering)
        String serverXmlWithProtocolFirst = """
                <?xml version="1.0" encoding="UTF-8"?>
                <Server port="8005" shutdown="SHUTDOWN">
                    <Service name="Catalina">
                        <Connector protocol="HTTP/1.1" port="8080"
                                   connectionTimeout="20000"
                                   redirectPort="8443" />
                        <Engine name="Catalina" defaultHost="localhost">
                            <Host name="localhost" appBase="webapps">
                            </Host>
                        </Engine>
                    </Service>
                </Server>
                """;
        Files.writeString(catalinaHome.resolve("conf").resolve("server.xml"), serverXmlWithProtocolFirst);

        CatalinaBaseGenerator.generate(catalinaHome, catalinaBase, 9090, "localhost");

        String serverXml = Files.readString(catalinaBase.resolve("conf").resolve("server.xml"));
        assertTrue(serverXml.contains("port=\"9090\""), "Port should be changed to 9090");
        assertFalse(serverXml.contains("port=\"8080\""), "Default port 8080 should be replaced");
    }

    /**
     * Verifies that generate handles multi-line connector elements correctly.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void generateHandlesMultiLineConnector() throws IOException {
        // Create a server.xml with connector spread across multiple lines
        String multiLineServerXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <Server port="8005" shutdown="SHUTDOWN">
                    <Service name="Catalina">
                        <Connector
                            port="8080"
                            protocol="HTTP/1.1"
                            connectionTimeout="20000"
                            redirectPort="8443"
                            maxParameterCount="1000" />
                        <Engine name="Catalina" defaultHost="localhost">
                            <Host name="localhost" appBase="webapps">
                            </Host>
                        </Engine>
                    </Service>
                </Server>
                """;
        Files.writeString(catalinaHome.resolve("conf").resolve("server.xml"), multiLineServerXml);

        CatalinaBaseGenerator.generate(catalinaHome, catalinaBase, 9090, "localhost");

        String serverXml = Files.readString(catalinaBase.resolve("conf").resolve("server.xml"));
        assertTrue(serverXml.contains("port=\"9090\""), "Port should be changed to 9090");
        assertFalse(serverXml.contains("port=\"8080\""), "Default port 8080 should be replaced");
    }

    /**
     * Verifies that generate handles Tomcat 11 style server.xml correctly.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void generateHandlesTomcat11ServerXml() throws IOException {
        // Create a server.xml similar to Tomcat 11 format
        String tomcat11ServerXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <Server port="8005" shutdown="SHUTDOWN">
                    <Listener className="org.apache.catalina.startup.VersionLoggerListener" />
                    <Service name="Catalina">
                        <Connector port="8080" protocol="HTTP/1.1"
                                   connectionTimeout="20000"
                                   redirectPort="8443"
                                   maxParameterCount="1000" />
                        <Connector protocol="AJP/1.3"
                                   address="::1"
                                   port="8009"
                                   redirectPort="8443"
                                   maxParameterCount="1000"
                                   secretRequired="false" />
                        <Engine name="Catalina" defaultHost="localhost">
                            <Realm className="org.apache.catalina.realm.LockOutRealm">
                            </Realm>
                            <Host name="localhost"  appBase="webapps"
                                  unpackWARs="true" autoDeploy="true">
                            </Host>
                        </Engine>
                    </Service>
                </Server>
                """;
        Files.writeString(catalinaHome.resolve("conf").resolve("server.xml"), tomcat11ServerXml);

        CatalinaBaseGenerator.generate(catalinaHome, catalinaBase, 18080, "localhost");

        String serverXml = Files.readString(catalinaBase.resolve("conf").resolve("server.xml"));
        assertTrue(serverXml.contains("port=\"18080\""), "HTTP port should be changed to 18080");
        assertFalse(serverXml.contains("port=\"8080\""), "Default HTTP port should be replaced");
        // AJP port should be commented out, not changed
        assertTrue(serverXml.contains("<!-- Disabled for plugin use:"), "AJP connector should be commented out");
    }

    /**
     * Verifies that hasCorrectPort returns true when the configured port matches.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void hasCorrectPortReturnsTrueWhenPortMatches() throws IOException {
        CatalinaBaseGenerator.generate(catalinaHome, catalinaBase, 9090, "localhost");

        assertTrue(CatalinaBaseGenerator.hasCorrectPort(catalinaBase, 9090));
    }

    /**
     * Verifies that hasCorrectPort returns false when the configured port does not
     * match.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void hasCorrectPortReturnsFalseWhenPortDoesNotMatch() throws IOException {
        CatalinaBaseGenerator.generate(catalinaHome, catalinaBase, 9090, "localhost");

        assertFalse(CatalinaBaseGenerator.hasCorrectPort(catalinaBase, 8080));
    }

    /**
     * Verifies that hasCorrectPort returns false when server.xml does not exist.
     */
    @Test
    void hasCorrectPortReturnsFalseWhenServerXmlMissing() {
        assertFalse(CatalinaBaseGenerator.hasCorrectPort(catalinaBase, 8080));
    }

    /**
     * Verifies that hasCorrectPort returns false when CATALINA_BASE does not exist.
     */
    @Test
    void hasCorrectPortReturnsFalseWhenCatalinaBaseMissing() {
        Path nonExistent = tempDir.resolve("nonexistent");
        assertFalse(CatalinaBaseGenerator.hasCorrectPort(nonExistent, 8080));
    }

    /**
     * Verifies that hasCorrectPort returns false when server.xml has invalid
     * content.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void hasCorrectPortReturnsFalseForInvalidServerXml() throws IOException {
        Files.createDirectories(catalinaBase.resolve("conf"));
        Files.writeString(catalinaBase.resolve("conf").resolve("server.xml"), "invalid xml content");

        assertFalse(CatalinaBaseGenerator.hasCorrectPort(catalinaBase, 8080));
    }

    /**
     * Verifies that hasCorrectPort returns false when server.xml has no HTTP
     * connector.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void hasCorrectPortReturnsFalseWhenNoHttpConnector() throws IOException {
        Files.createDirectories(catalinaBase.resolve("conf"));
        String serverXmlNoHttp = """
                <?xml version="1.0" encoding="UTF-8"?>
                <Server port="8005" shutdown="SHUTDOWN">
                    <Service name="Catalina">
                        <Connector port="8009" protocol="AJP/1.3" redirectPort="8443" />
                        <Engine name="Catalina" defaultHost="localhost">
                            <Host name="localhost" appBase="webapps">
                            </Host>
                        </Engine>
                    </Service>
                </Server>
                """;
        Files.writeString(catalinaBase.resolve("conf").resolve("server.xml"), serverXmlNoHttp);

        assertFalse(CatalinaBaseGenerator.hasCorrectPort(catalinaBase, 8080));
    }

    /**
     * Verifies that hasCorrectPort handles HTTP connector with missing port
     * attribute.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void hasCorrectPortReturnsFalseWhenPortAttributeMissing() throws IOException {
        Files.createDirectories(catalinaBase.resolve("conf"));
        String serverXmlNoPort = """
                <?xml version="1.0" encoding="UTF-8"?>
                <Server port="8005" shutdown="SHUTDOWN">
                    <Service name="Catalina">
                        <Connector protocol="HTTP/1.1" connectionTimeout="20000" />
                        <Engine name="Catalina" defaultHost="localhost">
                            <Host name="localhost" appBase="webapps">
                            </Host>
                        </Engine>
                    </Service>
                </Server>
                """;
        Files.writeString(catalinaBase.resolve("conf").resolve("server.xml"), serverXmlNoPort);

        assertFalse(CatalinaBaseGenerator.hasCorrectPort(catalinaBase, 8080));
    }

    /**
     * Verifies that hasCorrectPort correctly identifies different port values.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void hasCorrectPortWorksWithVariousPorts() throws IOException {
        // Test with standard port
        CatalinaBaseGenerator.generate(catalinaHome, catalinaBase, 8080, "localhost");
        assertTrue(CatalinaBaseGenerator.hasCorrectPort(catalinaBase, 8080));
        assertFalse(CatalinaBaseGenerator.hasCorrectPort(catalinaBase, 8081));

        // Test with high port number
        Path catalinaBase2 = tempDir.resolve("tomcat-base-2");
        CatalinaBaseGenerator.generate(catalinaHome, catalinaBase2, 49999, "localhost");
        assertTrue(CatalinaBaseGenerator.hasCorrectPort(catalinaBase2, 49999));
        assertFalse(CatalinaBaseGenerator.hasCorrectPort(catalinaBase2, 49998));
    }
}
