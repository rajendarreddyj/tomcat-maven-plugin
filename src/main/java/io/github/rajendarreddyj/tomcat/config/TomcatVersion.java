package io.github.rajendarreddyj.tomcat.config;

/**
 * Represents supported Tomcat versions with their download URLs and Java requirements.
 */
public enum TomcatVersion {
    /**
     * Tomcat 10.1.x - Jakarta EE 10, requires Java 11+.
     */
    TOMCAT_10_1("10.1", 11, "https://dlcdn.apache.org/tomcat/tomcat-10/"),

    /**
     * Tomcat 11.x - Jakarta EE 11, requires Java 17+.
     */
    TOMCAT_11("11", 17, "https://dlcdn.apache.org/tomcat/tomcat-11/");

    private static final String ARCHIVE_BASE_URL_10 = "https://archive.apache.org/dist/tomcat/tomcat-10/";
    private static final String ARCHIVE_BASE_URL_11 = "https://archive.apache.org/dist/tomcat/tomcat-11/";

    private final String majorMinor;
    private final int minimumJava;
    private final String downloadBaseUrl;

    TomcatVersion(String majorMinor, int minimumJava, String downloadBaseUrl) {
        this.majorMinor = majorMinor;
        this.minimumJava = minimumJava;
        this.downloadBaseUrl = downloadBaseUrl;
    }

    /**
     * Gets the major.minor version string.
     *
     * @return the major.minor version (e.g., "10.1" or "11")
     */
    public String getMajorMinor() {
        return majorMinor;
    }

    /**
     * Gets the minimum Java version required.
     *
     * @return the minimum Java version
     */
    public int getMinimumJava() {
        return minimumJava;
    }

    /**
     * Gets the primary download URL for a specific version.
     *
     * @param fullVersion the full version string (e.g., "10.1.52")
     * @return the download URL
     */
    public String getDownloadUrl(String fullVersion) {
        return downloadBaseUrl + "v" + fullVersion + "/bin/apache-tomcat-" + fullVersion + ".zip";
    }

    /**
     * Gets the fallback (archive) download URL for a specific version.
     *
     * @param fullVersion the full version string (e.g., "10.1.52")
     * @return the archive download URL
     */
    public String getArchiveDownloadUrl(String fullVersion) {
        String archiveBase = this == TOMCAT_10_1 ? ARCHIVE_BASE_URL_10 : ARCHIVE_BASE_URL_11;
        return archiveBase + "v" + fullVersion + "/bin/apache-tomcat-" + fullVersion + ".zip";
    }

    /**
     * Gets the checksum URL for a specific version.
     *
     * @param fullVersion the full version string (e.g., "10.1.52")
     * @return the SHA-512 checksum URL
     */
    public String getChecksumUrl(String fullVersion) {
        return getDownloadUrl(fullVersion) + ".sha512";
    }

    /**
     * Gets the archive checksum URL for a specific version.
     *
     * @param fullVersion the full version string (e.g., "10.1.52")
     * @return the archive SHA-512 checksum URL
     */
    public String getArchiveChecksumUrl(String fullVersion) {
        return getArchiveDownloadUrl(fullVersion) + ".sha512";
    }

    /**
     * Determines the TomcatVersion from a version string.
     *
     * @param version the version string (e.g., "10.1.52" or "11.0.2")
     * @return the corresponding TomcatVersion
     * @throws IllegalArgumentException if the version is not supported
     */
    public static TomcatVersion fromVersionString(String version) {
        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("Version string cannot be null or blank");
        }

        // Explicitly reject Tomcat 10.0.x
        if (version.startsWith("10.0")) {
            throw new IllegalArgumentException(
                    "Tomcat 10.0.x is not supported. Use 10.1.x or later.");
        }

        if (version.startsWith("10.1")) {
            return TOMCAT_10_1;
        } else if (version.startsWith("11.")) {
            return TOMCAT_11;
        }

        throw new IllegalArgumentException(
                "Unsupported Tomcat version: " + version + ". Supported versions are 10.1.x and 11.x");
    }

    /**
     * Validates that the current Java runtime meets the minimum requirements.
     *
     * @throws IllegalStateException if Java version is insufficient
     */
    public void validateJavaVersion() {
        int currentJava = Runtime.version().feature();
        if (currentJava < minimumJava) {
            throw new IllegalStateException(
                    String.format("Tomcat %s requires Java %d or later, but current Java is %d",
                            majorMinor, minimumJava, currentJava));
        }
    }
}
