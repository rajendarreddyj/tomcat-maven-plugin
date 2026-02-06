package io.github.rajendarreddyj.tomcat.download;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.sun.net.httpserver.HttpServer;

/**
 * Unit tests for {@link ChecksumValidator}.
 *
 * <p>
 * Tests the SHA-512 checksum calculation and validation functionality
 * including known content verification and error handling.
 *
 * @author rajendarreddyj
 * @see ChecksumValidator
 */
class ChecksumValidatorTest {

    /**
     * Temporary directory for test artifacts, cleaned up automatically after each
     * test.
     */
    @TempDir
    Path tempDir;

    /** The ChecksumValidator instance under test. */
    private ChecksumValidator validator;

    /** HTTP server for serving mock checksum files. */
    private HttpServer httpServer;

    /** Port for the test HTTP server. */
    private int serverPort;

    /**
     * Sets up the test environment before each test.
     *
     * @throws IOException if server setup fails
     */
    @BeforeEach
    void setUp() throws IOException {
        validator = new ChecksumValidator();
        // Start a local HTTP server for testing validate() method
        httpServer = HttpServer.create(new InetSocketAddress(0), 0);
        serverPort = httpServer.getAddress().getPort();
        httpServer.start();
    }

    /**
     * Cleans up resources after each test.
     */
    @AfterEach
    void tearDown() {
        if (httpServer != null) {
            httpServer.stop(0);
        }
    }

    /**
     * Verifies that calculateChecksum produces the expected SHA-512 hash for known
     * content.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void calculateChecksumKnownContent() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "Hello, World!");

        String checksum = validator.calculateChecksum(testFile);

        // SHA-512 of "Hello, World!" is known
        assertEquals(
                "374d794a95cdcfd8b35993185fef9ba368f160d8daf432d08ba9f1ed1e5abe6cc69291e0fa2fe0006a52570ef18c19def4e617c33ce52ef0a6e5fbe318cb0387",
                checksum.toLowerCase());
    }

    /**
     * Verifies that calculateChecksum produces the expected SHA-512 hash for empty
     * files.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void calculateChecksumEmptyFile() throws IOException {
        Path emptyFile = tempDir.resolve("empty.txt");
        Files.createFile(emptyFile);

        String checksum = validator.calculateChecksum(emptyFile);

        // SHA-512 of empty string
        assertEquals(
                "cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9ce47d0d13c5d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e",
                checksum.toLowerCase());
    }

    /**
     * Verifies that calculateChecksum handles binary content correctly.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void calculateChecksumBinaryContent() throws IOException {
        Path testFile = tempDir.resolve("binary.dat");
        Files.write(testFile, new byte[] { 0x00, 0x01, 0x02, (byte) 0xFF });

        String checksum = validator.calculateChecksum(testFile);

        // Should produce a valid 128-character hex string (512 bits = 128 hex chars)
        assertNotNull(checksum);
        assertEquals(128, checksum.length());
        assertTrue(checksum.matches("[a-f0-9]+"));
    }

    /**
     * Verifies that calculateChecksum throws IOException for non-existent files.
     */
    @Test
    void calculateChecksumNonExistentFile() {
        Path nonExistent = tempDir.resolve("nonexistent.txt");

        assertThrows(IOException.class, () -> validator.calculateChecksum(nonExistent));
    }

    /**
     * Verifies that same content produces same checksum hash.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void calculateChecksumSameContentSameHash() throws IOException {
        Path file1 = tempDir.resolve("file1.txt");
        Path file2 = tempDir.resolve("file2.txt");
        String content = "Same content";

        Files.writeString(file1, content);
        Files.writeString(file2, content);

        String checksum1 = validator.calculateChecksum(file1);
        String checksum2 = validator.calculateChecksum(file2);

        assertEquals(checksum1, checksum2);
    }

    /**
     * Verifies that different content produces different checksum hashes.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void calculateChecksumDifferentContentDifferentHash() throws IOException {
        Path file1 = tempDir.resolve("file1.txt");
        Path file2 = tempDir.resolve("file2.txt");

        Files.writeString(file1, "Content A");
        Files.writeString(file2, "Content B");

        String checksum1 = validator.calculateChecksum(file1);
        String checksum2 = validator.calculateChecksum(file2);

        assertNotEquals(checksum1, checksum2);
    }

    /**
     * Verifies that validate returns true when checksum matches.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void validateReturnsTrueWhenChecksumMatches() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "Hello, World!");

        // SHA-512 of "Hello, World!"
        String expectedChecksum = "374d794a95cdcfd8b35993185fef9ba368f160d8daf432d08ba9f1ed1e5abe6cc69291e0fa2fe0006a52570ef18c19def4e617c33ce52ef0a6e5fbe318cb0387";

        // Set up HTTP server to return the checksum
        httpServer.createContext("/checksum.sha512", exchange -> {
            byte[] response = expectedChecksum.getBytes();
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });

        String checksumUrl = "http://localhost:" + serverPort + "/checksum.sha512";
        assertTrue(validator.validate(testFile, checksumUrl));
    }

    /**
     * Verifies that validate returns false when checksum does not match.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void validateReturnsFalseWhenChecksumDoesNotMatch() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "Hello, World!");

        // Different checksum
        String wrongChecksum = "0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000";

        httpServer.createContext("/wrong-checksum.sha512", exchange -> {
            byte[] response = wrongChecksum.getBytes();
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });

        String checksumUrl = "http://localhost:" + serverPort + "/wrong-checksum.sha512";
        assertFalse(validator.validate(testFile, checksumUrl));
    }

    /**
     * Verifies that validate handles checksum with filename suffix (Apache format).
     *
     * <p>
     * Apache checksum files typically contain the hash followed by the filename:
     * {@code 374d794a...  apache-tomcat-10.1.52.zip}
     *
     * @throws IOException if file operations fail
     */
    @Test
    void validateHandlesChecksumWithFilename() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "Hello, World!");

        // Apache format: checksum followed by filename
        String checksumWithFilename = "374d794a95cdcfd8b35993185fef9ba368f160d8daf432d08ba9f1ed1e5abe6cc69291e0fa2fe0006a52570ef18c19def4e617c33ce52ef0a6e5fbe318cb0387  apache-tomcat-10.1.52.zip";

        httpServer.createContext("/apache-format.sha512", exchange -> {
            byte[] response = checksumWithFilename.getBytes();
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });

        String checksumUrl = "http://localhost:" + serverPort + "/apache-format.sha512";
        assertTrue(validator.validate(testFile, checksumUrl));
    }

    /**
     * Verifies that validate handles uppercase checksum correctly.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void validateHandlesUppercaseChecksum() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "Hello, World!");

        // Same checksum in uppercase
        String uppercaseChecksum = "374D794A95CDCFD8B35993185FEF9BA368F160D8DAF432D08BA9F1ED1E5ABE6CC69291E0FA2FE0006A52570EF18C19DEF4E617C33CE52EF0A6E5FBE318CB0387";

        httpServer.createContext("/uppercase.sha512", exchange -> {
            byte[] response = uppercaseChecksum.getBytes();
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });

        String checksumUrl = "http://localhost:" + serverPort + "/uppercase.sha512";
        assertTrue(validator.validate(testFile, checksumUrl));
    }

    /**
     * Verifies that validate throws IOException when checksum URL is unreachable.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void validateThrowsIOExceptionWhenUrlUnreachable() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "test content");

        // Use a non-existent URL
        String unreachableUrl = "http://localhost:" + serverPort + "/nonexistent.sha512";

        assertThrows(IOException.class, () -> validator.validate(testFile, unreachableUrl));
    }

    /**
     * Verifies that validate throws IOException for invalid URL format.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void validateThrowsExceptionForInvalidUrl() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "test content");

        assertThrows(Exception.class, () -> validator.validate(testFile, "not-a-valid-url"));
    }

    /**
     * Verifies that validate handles empty file correctly.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void validateHandlesEmptyFile() throws IOException {
        Path emptyFile = tempDir.resolve("empty.txt");
        Files.createFile(emptyFile);

        // SHA-512 of empty file
        String emptyFileChecksum = "cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9ce47d0d13c5d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e";

        httpServer.createContext("/empty.sha512", exchange -> {
            byte[] response = emptyFileChecksum.getBytes();
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });

        String checksumUrl = "http://localhost:" + serverPort + "/empty.sha512";
        assertTrue(validator.validate(emptyFile, checksumUrl));
    }
}
