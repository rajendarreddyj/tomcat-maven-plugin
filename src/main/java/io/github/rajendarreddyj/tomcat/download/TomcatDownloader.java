package io.github.rajendarreddyj.tomcat.download;

import io.github.rajendarreddyj.tomcat.config.TomcatVersion;
import org.apache.maven.plugin.logging.Log;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Downloads and extracts Apache Tomcat distributions.
 */
public class TomcatDownloader {

    private static final Duration DOWNLOAD_TIMEOUT = Duration.ofMinutes(10);
    private final ChecksumValidator checksumValidator;
    private final HttpClient httpClient;

    /**
     * Creates a new TomcatDownloader instance.
     */
    public TomcatDownloader() {
        this.checksumValidator = new ChecksumValidator();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * Downloads Tomcat if not cached, then extracts and returns the installation path.
     *
     * @param version  Full Tomcat version (e.g., "10.1.52")
     * @param cacheDir Directory to cache downloads (e.g., ~/.m2/tomcat-cache)
     * @param log      Maven logger
     * @return Path to the extracted Tomcat installation
     * @throws IOException if download or extraction fails
     */
    public Path download(String version, Path cacheDir, Log log) throws IOException {
        TomcatVersion tomcatVersion = TomcatVersion.fromVersionString(version);
        String fileName = "apache-tomcat-" + version + ".zip";

        Path versionDir = cacheDir.resolve(version);
        Path cachedZip = versionDir.resolve(fileName);
        Path extractedDir = versionDir.resolve("apache-tomcat-" + version);

        // Return cached extraction if valid
        if (Files.exists(extractedDir) && isValidTomcatDir(extractedDir)) {
            log.info("Using cached Tomcat installation: " + extractedDir);
            return extractedDir;
        }

        Files.createDirectories(versionDir);

        // Download if not cached or invalid
        if (!Files.exists(cachedZip) || !validateChecksum(cachedZip, tomcatVersion, version, log)) {
            downloadWithFallback(tomcatVersion, version, cachedZip, log);
        }

        // Extract
        log.info("Extracting Tomcat to: " + versionDir);
        extract(cachedZip, versionDir, log);

        if (!Files.exists(extractedDir)) {
            throw new IOException("Extraction failed: " + extractedDir + " not found");
        }

        return extractedDir;
    }

    /**
     * Attempts to download from primary URL with fallback to archive.
     */
    private void downloadWithFallback(TomcatVersion tomcatVersion, String version,
                                      Path targetPath, Log log) throws IOException {
        String primaryUrl = tomcatVersion.getDownloadUrl(version);
        String archiveUrl = tomcatVersion.getArchiveDownloadUrl(version);

        try {
            log.info("Downloading Tomcat " + version + " from: " + primaryUrl);
            downloadFile(primaryUrl, targetPath);

            if (validateChecksum(targetPath, tomcatVersion, version, log)) {
                return;
            }
            log.warn("Checksum validation failed, trying archive...");
        } catch (IOException e) {
            log.warn("Primary download failed: " + e.getMessage());
        }

        // Fallback to archive
        log.info("Downloading from Apache archive: " + archiveUrl);
        downloadFile(archiveUrl, targetPath);

        if (!validateChecksum(targetPath, tomcatVersion, version, log)) {
            throw new IOException("Downloaded file failed checksum validation");
        }
    }

    /**
     * Downloads a file from the given URL.
     */
    private void downloadFile(String url, Path targetPath) throws IOException {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(DOWNLOAD_TIMEOUT)
                    .GET()
                    .build();

            HttpResponse<Path> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofFile(targetPath));

            if (response.statusCode() != 200) {
                Files.deleteIfExists(targetPath);
                throw new IOException("Download failed with status: " + response.statusCode());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Download interrupted", e);
        }
    }

    /**
     * Validates the checksum of a downloaded file.
     */
    private boolean validateChecksum(Path file, TomcatVersion tomcatVersion,
                                     String version, Log log) {
        try {
            String checksumUrl = tomcatVersion.getChecksumUrl(version);
            boolean valid = checksumValidator.validate(file, checksumUrl);
            if (!valid) {
                log.warn("Checksum mismatch for: " + file);
            }
            return valid;
        } catch (IOException e) {
            log.warn("Could not validate checksum: " + e.getMessage());
            return true; // Skip validation if checksum unavailable
        }
    }

    /**
     * Extracts a zip file to the target directory.
     */
    private void extract(Path zipFile, Path targetDir, Log log) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path targetPath = targetDir.resolve(entry.getName()).normalize();

                // Zip slip protection
                if (!targetPath.startsWith(targetDir)) {
                    throw new IOException("Zip slip attack detected: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(targetPath);
                } else {
                    Files.createDirectories(targetPath.getParent());
                    Files.copy(zis, targetPath, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        }

        // Make scripts executable on Unix
        setExecutablePermissions(targetDir, log);
    }

    /**
     * Sets executable permissions on shell scripts in the bin directory.
     */
    private void setExecutablePermissions(Path tomcatDir, Log log) {
        try {
            // The ZIP extracts to tomcatDir/apache-tomcat-X.Y.Z/bin
            try (var dirs = Files.list(tomcatDir)) {
                dirs.filter(p -> p.getFileName().toString().startsWith("apache-tomcat-"))
                        .findFirst()
                        .ifPresent(extractedDir -> {
                            Path binDir = extractedDir.resolve("bin");
                            if (Files.exists(binDir)) {
                                try (var files = Files.list(binDir)) {
                                    files.filter(p -> p.toString().endsWith(".sh"))
                                            .forEach(p -> p.toFile().setExecutable(true, false));
                                } catch (IOException e) {
                                    log.warn("Could not set executable permissions: " + e.getMessage());
                                }
                            }
                        });
            }
        } catch (IOException e) {
            log.warn("Could not set executable permissions: " + e.getMessage());
        }
    }

    /**
     * Checks if a directory contains a valid Tomcat installation.
     */
    private boolean isValidTomcatDir(Path dir) {
        return Files.exists(dir.resolve("bin")) &&
                Files.exists(dir.resolve("lib").resolve("catalina.jar"));
    }
}
