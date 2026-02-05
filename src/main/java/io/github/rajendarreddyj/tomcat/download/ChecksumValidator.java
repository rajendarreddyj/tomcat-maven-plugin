package io.github.rajendarreddyj.tomcat.download;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Validates file integrity using SHA-512 checksums.
 *
 * @author rajendarreddyj
 * @since 1.0.0
 */
public class ChecksumValidator {

    /** The algorithm used for checksum calculation. */
    private static final String SHA_512 = "SHA-512";

    /**
     * Validates file checksum against remote SHA-512 file.
     *
     * @param file        The file to validate
     * @param checksumUrl URL to the .sha512 checksum file
     * @return true if checksum matches, false otherwise
     * @throws IOException if checksum cannot be read or calculated
     */
    public boolean validate(Path file, String checksumUrl) throws IOException {
        String expectedChecksum = fetchChecksum(checksumUrl);
        String actualChecksum = calculateChecksum(file);

        // Apache checksum files may contain filename after hash
        String expectedHash = expectedChecksum.split("\\s+")[0].toLowerCase();
        String actualHash = actualChecksum.toLowerCase();

        return expectedHash.equals(actualHash);
    }

    /**
     * Fetches checksum from remote URL.
     *
     * <p>
     * Reads the first line from the checksum file, which typically contains
     * the hash followed optionally by the filename.
     * </p>
     *
     * @param checksumUrl URL to the checksum file
     * @return the checksum string from the remote file
     * @throws IOException if fetch fails or URL is invalid
     */
    private String fetchChecksum(String checksumUrl) throws IOException {
        try (InputStream is = URI.create(checksumUrl).toURL().openStream();
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(is, StandardCharsets.UTF_8))) {
            return reader.readLine();
        }
    }

    /**
     * Calculates SHA-512 checksum of a file.
     *
     * @param file the file to calculate checksum for
     * @return the hex-encoded checksum
     * @throws IOException if file cannot be read or algorithm unavailable
     */
    public String calculateChecksum(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA_512);
            byte[] fileBytes = Files.readAllBytes(file);
            byte[] hashBytes = digest.digest(fileBytes);
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-512 algorithm not available", e);
        }
    }
}
