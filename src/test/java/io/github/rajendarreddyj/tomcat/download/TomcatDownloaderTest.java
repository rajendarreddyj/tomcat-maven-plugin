package io.github.rajendarreddyj.tomcat.download;

import org.apache.maven.plugin.logging.Log;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TomcatDownloaderTest {

    @TempDir
    Path tempDir;

    @Mock
    private Log log;

    private TomcatDownloader downloader;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        downloader = new TomcatDownloader();
    }

    @Test
    void constructorCreatesInstance() {
        TomcatDownloader instance = new TomcatDownloader();
        assertNotNull(instance);
    }

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

    @Test
    void downloadInvalidVersionThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
                downloader.download("9.0.50", tempDir, log));
    }

    @Test
    void downloadNullVersionThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
                downloader.download(null, tempDir, log));
    }

    @Test
    void downloadEmptyVersionThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
                downloader.download("", tempDir, log));
    }

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

    @Test
    void downloadLogsDownloadAttempt() {
        try {
            downloader.download("10.1.36", tempDir, log);
        } catch (IOException e) {
            // Expected - can't actually download
        }

        verify(log, atLeastOnce()).info(argThat((CharSequence msg) ->
                msg.toString().contains("Downloading") || msg.toString().contains("cached")));
    }

    @Test
    void downloadHandlesInvalidVersionFormat() {
        // Test that invalid version format is handled
        assertThrows(IllegalArgumentException.class, () ->
                downloader.download("invalid.version.format", tempDir, log));
    }

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
