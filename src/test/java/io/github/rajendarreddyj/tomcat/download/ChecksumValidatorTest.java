package io.github.rajendarreddyj.tomcat.download;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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

    /**
     * Sets up the test environment before each test.
     */
    @BeforeEach
    void setUp() {
        validator = new ChecksumValidator();
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
}
