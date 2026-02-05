package io.github.rajendarreddyj.tomcat.download;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ChecksumValidatorTest {

    @TempDir
    Path tempDir;

    private ChecksumValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ChecksumValidator();
    }

    @Test
    void calculateChecksumKnownContent() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "Hello, World!");

        String checksum = validator.calculateChecksum(testFile);

        // SHA-512 of "Hello, World!" is known
        assertEquals(
                "374d794a95cdcfd8b35993185fef9ba368f160d8daf432d08ba9f1ed1e5abe6cc69291e0fa2fe0006a52570ef18c19def4e617c33ce52ef0a6e5fbe318cb0387",
                checksum.toLowerCase()
        );
    }

    @Test
    void calculateChecksumEmptyFile() throws IOException {
        Path emptyFile = tempDir.resolve("empty.txt");
        Files.createFile(emptyFile);

        String checksum = validator.calculateChecksum(emptyFile);

        // SHA-512 of empty string
        assertEquals(
                "cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9ce47d0d13c5d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e",
                checksum.toLowerCase()
        );
    }

    @Test
    void calculateChecksumBinaryContent() throws IOException {
        Path testFile = tempDir.resolve("binary.dat");
        Files.write(testFile, new byte[]{0x00, 0x01, 0x02, (byte) 0xFF});

        String checksum = validator.calculateChecksum(testFile);

        // Should produce a valid 128-character hex string (512 bits = 128 hex chars)
        assertNotNull(checksum);
        assertEquals(128, checksum.length());
        assertTrue(checksum.matches("[a-f0-9]+"));
    }

    @Test
    void calculateChecksumNonExistentFile() {
        Path nonExistent = tempDir.resolve("nonexistent.txt");

        assertThrows(IOException.class, () ->
                validator.calculateChecksum(nonExistent)
        );
    }

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
