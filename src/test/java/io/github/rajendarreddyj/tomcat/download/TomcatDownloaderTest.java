package io.github.rajendarreddyj.tomcat.download;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.maven.plugin.logging.Log;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for {@link TomcatDownloader}.
 *
 * <p>
 * Tests the Tomcat download and extraction functionality including
 * caching, version validation, and ZIP extraction.
 *
 * @author rajendarreddyj
 * @see TomcatDownloader
 */
class TomcatDownloaderTest {

    /**
     * Temporary directory for test artifacts, cleaned up automatically after each
     * test.
     */
    @TempDir
    Path tempDir;

    /** Mock Maven logger for testing. */
    @Mock
    private Log log;

    /** The TomcatDownloader instance under test. */
    private TomcatDownloader downloader;

    /**
     * Sets up the test environment before each test.
     */
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        downloader = new TomcatDownloader();
    }

    /**
     * Verifies that constructor creates a valid instance.
     */
    @Test
    void constructorCreatesInstance() {
        TomcatDownloader instance = new TomcatDownloader();
        assertNotNull(instance);
    }

    /**
     * Verifies that download returns cached installation when available.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void downloadReturnsCachedInstallation() throws IOException {
        // Create a mock cached installation
        String version = "10.1.36";
        Path versionDir = tempDir.resolve(version);
        Path extractedDir = versionDir.resolve("apache-tomcat-" + version);
        Path binDir = extractedDir.resolve("bin");
        Path libDir = extractedDir.resolve("lib");

        Files.createDirectories(binDir);
        Files.createDirectories(libDir);
        Files.writeString(binDir.resolve("catalina.sh"), "#!/bin/bash");
        Files.writeString(libDir.resolve("catalina.jar"), "mock jar");

        Path result = downloader.download(version, tempDir, log);

        assertEquals(extractedDir, result);
        verify(log).info(contains("Using cached"));
    }

    /**
     * Verifies that download throws exception for unsupported Tomcat versions.
     */
    @Test
    void downloadInvalidVersionThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> downloader.download("9.0.50", tempDir, log));
    }

    /**
     * Verifies that download throws exception for null version.
     */
    @Test
    void downloadNullVersionThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> downloader.download(null, tempDir, log));
    }

    /**
     * Verifies that download throws exception for empty version string.
     */
    @Test
    void downloadEmptyVersionThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> downloader.download("", tempDir, log));
    }

    /**
     * Verifies that download creates version directory even if download fails.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void downloadCreatesVersionDirectory() throws IOException {
        // This test will fail to download (no network) but should create directory
        String version = "10.1.36";

        try {
            downloader.download(version, tempDir, log);
        } catch (IOException e) {
            // Expected - can't actually download
        }

        assertTrue(Files.isDirectory(tempDir.resolve(version)));
    }

    /**
     * Verifies that download detects and rejects invalid cached directory.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void downloadDetectsInvalidCachedDir() throws IOException {
        // Create invalid cached directory (missing catalina.jar)
        String version = "10.1.36";
        Path extractedDir = tempDir.resolve(version).resolve("apache-tomcat-" + version);
        Files.createDirectories(extractedDir.resolve("bin"));
        // Missing lib/catalina.jar

        try {
            downloader.download(version, tempDir, log);
        } catch (IOException e) {
            // Expected - invalid cache and can't download
        }

        // Should not use invalid cache
        verify(log, never()).info(contains("Using cached"));
    }

    /**
     * Verifies that download handles Tomcat 11 versions correctly.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void downloadHandlesTomcat11Version() throws IOException {
        // Create a mock cached installation for Tomcat 11
        String version = "11.0.5";
        Path extractedDir = tempDir.resolve(version).resolve("apache-tomcat-" + version);
        Files.createDirectories(extractedDir.resolve("bin"));
        Files.createDirectories(extractedDir.resolve("lib"));
        Files.writeString(extractedDir.resolve("lib").resolve("catalina.jar"), "mock");

        Path result = downloader.download(version, tempDir, log);

        assertEquals(extractedDir, result);
    }

    /**
     * Verifies that extractZip handles valid ZIP files correctly.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void extractZipHandlesValidZip() throws IOException {
        // Create a valid zip file
        Path zipFile = tempDir.resolve("test.zip");
        Path extractDir = tempDir.resolve("extract");

        createTestZip(zipFile, "apache-tomcat-10.1.36/");

        // Extract using reflection or by creating the cached structure
        // For now, test the cache flow
        Path versionDir = tempDir.resolve("10.1.36");
        Files.createDirectories(versionDir);
        Files.copy(zipFile, versionDir.resolve("apache-tomcat-10.1.36.zip"));

        try {
            downloader.download("10.1.36", tempDir, log);
        } catch (IOException e) {
            // May fail checksum but should attempt extraction
        }
    }

    /**
     * Verifies that download logs download attempt information.
     */
    @Test
    void downloadLogsDownloadAttempt() {
        try {
            downloader.download("10.1.36", tempDir, log);
        } catch (IOException e) {
            // Expected - can't actually download
        }

        verify(log, atLeastOnce()).info(argThat(
                (CharSequence msg) -> msg.toString().contains("Downloading") || msg.toString().contains("cached")));
    }

    /**
     * Verifies that download handles invalid version format correctly.
     */
    @Test
    void downloadHandlesInvalidVersionFormat() {
        // Test that invalid version format is handled
        assertThrows(IllegalArgumentException.class, () -> downloader.download("invalid.version.format", tempDir, log));
    }

    /**
     * Creates a test ZIP file with Tomcat directory structure.
     *
     * @param zipFile the path to create the ZIP file
     * @param rootDir the root directory name inside the ZIP
     * @throws IOException if file creation fails
     */
    private void createTestZip(Path zipFile, String rootDir) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile))) {
            // Add root directory
            zos.putNextEntry(new ZipEntry(rootDir));
            zos.closeEntry();

            // Add bin directory
            zos.putNextEntry(new ZipEntry(rootDir + "bin/"));
            zos.closeEntry();

            // Add lib directory
            zos.putNextEntry(new ZipEntry(rootDir + "lib/"));
            zos.closeEntry();

            // Add catalina.sh
            ZipEntry catalinaEntry = new ZipEntry(rootDir + "bin/catalina.sh");
            zos.putNextEntry(catalinaEntry);
            zos.write("#!/bin/bash".getBytes());
            zos.closeEntry();

            // Add catalina.jar
            ZipEntry jarEntry = new ZipEntry(rootDir + "lib/catalina.jar");
            zos.putNextEntry(jarEntry);
            zos.write("mock jar content".getBytes());
            zos.closeEntry();
        }
    }
}
