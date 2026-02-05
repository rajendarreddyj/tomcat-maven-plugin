package io.github.rajendarreddyj.tomcat.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TomcatVersionTest {

    @Test
    void fromVersionStringTomcat101() {
        assertEquals(TomcatVersion.TOMCAT_10_1, TomcatVersion.fromVersionString("10.1.52"));
        assertEquals(TomcatVersion.TOMCAT_10_1, TomcatVersion.fromVersionString("10.1.0"));
    }

    @Test
    void fromVersionStringTomcat11() {
        assertEquals(TomcatVersion.TOMCAT_11, TomcatVersion.fromVersionString("11.0.18"));
        assertEquals(TomcatVersion.TOMCAT_11, TomcatVersion.fromVersionString("11.0.0"));
    }

    @Test
    void fromVersionStringInvalid() {
        assertThrows(IllegalArgumentException.class,
                () -> TomcatVersion.fromVersionString("9.0.50"));
        assertThrows(IllegalArgumentException.class,
                () -> TomcatVersion.fromVersionString(null));
        assertThrows(IllegalArgumentException.class,
                () -> TomcatVersion.fromVersionString(""));
    }

    @Test
    void fromVersionStringTomcat100NotSupported() {
        // Tomcat 10.0.x is NOT supported (different from 10.1.x)
        assertThrows(IllegalArgumentException.class,
                () -> TomcatVersion.fromVersionString("10.0.27"));
    }

    @Test
    void getDownloadUrl() {
        String url = TomcatVersion.TOMCAT_10_1.getDownloadUrl("10.1.52");
        assertEquals(
                "https://dlcdn.apache.org/tomcat/tomcat-10/v10.1.52/bin/apache-tomcat-10.1.52.zip",
                url
        );
    }

    @Test
    void getArchiveDownloadUrl() {
        String url = TomcatVersion.TOMCAT_11.getArchiveDownloadUrl("11.0.5");
        assertEquals(
                "https://archive.apache.org/dist/tomcat/tomcat-11/v11.0.5/bin/apache-tomcat-11.0.5.zip",
                url
        );
    }

    @Test
    void getChecksumUrl() {
        String url = TomcatVersion.TOMCAT_10_1.getChecksumUrl("10.1.52");
        assertEquals(
                "https://dlcdn.apache.org/tomcat/tomcat-10/v10.1.52/bin/apache-tomcat-10.1.52.zip.sha512",
                url
        );
    }

    @Test
    void getMinimumJava() {
        assertEquals(11, TomcatVersion.TOMCAT_10_1.getMinimumJava());
        assertEquals(17, TomcatVersion.TOMCAT_11.getMinimumJava());
    }

    @Test
    void getMajorMinor() {
        assertEquals("10.1", TomcatVersion.TOMCAT_10_1.getMajorMinor());
        assertEquals("11", TomcatVersion.TOMCAT_11.getMajorMinor());
    }

    @Test
    void validateJavaVersionCurrentJdkMeetsTomcat101() {
        // Current JDK (21) should pass validation for Tomcat 10.1 (requires 11+)
        assertDoesNotThrow(() -> TomcatVersion.TOMCAT_10_1.validateJavaVersion());
    }

    @Test
    void validateJavaVersionCurrentJdkMeetsTomcat11() {
        // Current JDK (21) should pass validation for Tomcat 11 (requires 17+)
        assertDoesNotThrow(() -> TomcatVersion.TOMCAT_11.validateJavaVersion());
    }

    @Test
    void getArchiveChecksumUrl() {
        String url = TomcatVersion.TOMCAT_10_1.getArchiveChecksumUrl("10.1.52");
        assertEquals(
                "https://archive.apache.org/dist/tomcat/tomcat-10/v10.1.52/bin/apache-tomcat-10.1.52.zip.sha512",
                url
        );
    }
}
