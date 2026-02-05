package io.github.rajendarreddyj.tomcat.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TomcatVersion}.
 *
 * <p>
 * Tests the Tomcat version enumeration including version parsing,
 * download URL generation, and Java version requirements.
 *
 * @author rajendarreddyj
 * @see TomcatVersion
 */
class TomcatVersionTest {

    /**
     * Verifies that Tomcat 10.1.x versions are correctly parsed.
     */
    @Test
    void fromVersionStringTomcat101() {
        assertEquals(TomcatVersion.TOMCAT_10_1, TomcatVersion.fromVersionString("10.1.52"));
        assertEquals(TomcatVersion.TOMCAT_10_1, TomcatVersion.fromVersionString("10.1.0"));
    }

    /**
     * Verifies that Tomcat 11.x versions are correctly parsed.
     */
    @Test
    void fromVersionStringTomcat11() {
        assertEquals(TomcatVersion.TOMCAT_11, TomcatVersion.fromVersionString("11.0.18"));
        assertEquals(TomcatVersion.TOMCAT_11, TomcatVersion.fromVersionString("11.0.0"));
    }

    /**
     * Verifies that invalid version strings throw IllegalArgumentException.
     */
    @Test
    void fromVersionStringInvalid() {
        assertThrows(IllegalArgumentException.class,
                () -> TomcatVersion.fromVersionString("9.0.50"));
        assertThrows(IllegalArgumentException.class,
                () -> TomcatVersion.fromVersionString(null));
        assertThrows(IllegalArgumentException.class,
                () -> TomcatVersion.fromVersionString(""));
    }

    /**
     * Verifies that Tomcat 10.0.x is not supported (different from 10.1.x).
     */
    @Test
    void fromVersionStringTomcat100NotSupported() {
        // Tomcat 10.0.x is NOT supported (different from 10.1.x)
        assertThrows(IllegalArgumentException.class,
                () -> TomcatVersion.fromVersionString("10.0.27"));
    }

    /**
     * Verifies that download URL is correctly generated for a version.
     */
    @Test
    void getDownloadUrl() {
        String url = TomcatVersion.TOMCAT_10_1.getDownloadUrl("10.1.52");
        assertEquals(
                "https://dlcdn.apache.org/tomcat/tomcat-10/v10.1.52/bin/apache-tomcat-10.1.52.zip",
                url);
    }

    /**
     * Verifies that archive download URL (fallback) is correctly generated.
     */
    @Test
    void getArchiveDownloadUrl() {
        String url = TomcatVersion.TOMCAT_11.getArchiveDownloadUrl("11.0.5");
        assertEquals(
                "https://archive.apache.org/dist/tomcat/tomcat-11/v11.0.5/bin/apache-tomcat-11.0.5.zip",
                url);
    }

    /**
     * Verifies that checksum URL is correctly generated for a version.
     */
    @Test
    void getChecksumUrl() {
        String url = TomcatVersion.TOMCAT_10_1.getChecksumUrl("10.1.52");
        assertEquals(
                "https://dlcdn.apache.org/tomcat/tomcat-10/v10.1.52/bin/apache-tomcat-10.1.52.zip.sha512",
                url);
    }

    /**
     * Verifies that minimum Java requirements are correct for each Tomcat version.
     */
    @Test
    void getMinimumJava() {
        assertEquals(11, TomcatVersion.TOMCAT_10_1.getMinimumJava());
        assertEquals(17, TomcatVersion.TOMCAT_11.getMinimumJava());
    }

    /**
     * Verifies that major.minor version strings are correct for each Tomcat
     * version.
     */
    @Test
    void getMajorMinor() {
        assertEquals("10.1", TomcatVersion.TOMCAT_10_1.getMajorMinor());
        assertEquals("11", TomcatVersion.TOMCAT_11.getMajorMinor());
    }

    /**
     * Verifies that current JDK (21) meets Tomcat 10.1 Java requirements (11+).
     */
    @Test
    void validateJavaVersionCurrentJdkMeetsTomcat101() {
        // Current JDK (21) should pass validation for Tomcat 10.1 (requires 11+)
        assertDoesNotThrow(() -> TomcatVersion.TOMCAT_10_1.validateJavaVersion());
    }

    /**
     * Verifies that current JDK (21) meets Tomcat 11 Java requirements (17+).
     */
    @Test
    void validateJavaVersionCurrentJdkMeetsTomcat11() {
        // Current JDK (21) should pass validation for Tomcat 11 (requires 17+)
        assertDoesNotThrow(() -> TomcatVersion.TOMCAT_11.validateJavaVersion());
    }

    /**
     * Verifies that archive checksum URL is correctly generated for a version.
     */
    @Test
    void getArchiveChecksumUrl() {
        String url = TomcatVersion.TOMCAT_10_1.getArchiveChecksumUrl("10.1.52");
        assertEquals(
                "https://archive.apache.org/dist/tomcat/tomcat-10/v10.1.52/bin/apache-tomcat-10.1.52.zip.sha512",
                url);
    }
}
