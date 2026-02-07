---
title: Maven Tomcat Plugin Implementation Plan
date: 2026-02-05
author: AI Implementation Assistant
status: active
related_research:
  - .context/domains/tomcat-maven-plugin/research/current/2026-02-05-implementation-requirements-research.md
  - .context/domains/tomcat/research/current/2026-02-05-apache-tomcat-maven-plugin-legacy-architecture.md
  - .context/domains/tomcat/research/current/2026-02-05-tomcat-10-11-migration-requirements.md
  - .context/domains/maven-plugin/research/current/2026-02-05-maven-plugin-development-patterns.md
  - .context/domains/tomcat/research/current/2026-02-05-tomcat-auto-download-implementation.md
---

# Maven Tomcat Plugin Implementation Plan

## Overview

Implement a Maven plugin (`io.github.rajendarreddyj:tomcat-maven-plugin`) that supports Apache Tomcat 10.1.x and 11.x with hot deployment, auto-download capabilities, and extensive configuration options for running web applications during development.

## Current State Analysis

- **Project Status:** Greenfield - no existing code
- **Available:** `requirements.md` with feature requirements
- **Research Completed:** Legacy plugin architecture, Tomcat version requirements, Maven plugin patterns, auto-download implementation

### Key Discoveries:
- Legacy plugin uses embedded Tomcat, but requirements suggest external process management
- Tomcat 10.1.x requires Java 11+, Tomcat 11.x requires Java 17+
- Download URLs: `https://dlcdn.apache.org/tomcat/tomcat-{MAJOR}/v{VERSION}/bin/apache-tomcat-{VERSION}.zip`
- SHA-512 checksum available at `{DOWNLOAD_URL}.sha512`
- Version detection via `lib/catalina.jar!/org/apache/catalina/util/ServerInfo.properties`

## Desired End State

A fully functional Maven plugin that:
1. Downloads Tomcat automatically if `catalinaHome` is not specified or doesn't exist
2. Runs Tomcat as an external process with configurable JVM options and environment variables
3. Deploys exploded WAR files with hot-reload capabilities
4. Supports both foreground (`run`) and background (`start`/`stop`) execution modes
5. Validates Java version compatibility with selected Tomcat version

### How to Verify:
```bash
# Plugin builds successfully
mvn clean install

# Run goal works with auto-download
mvn io.github.rajendarreddyj:tomcat-maven-plugin:run -Dtomcat.version=10.1.52

# Run goal works with existing Tomcat installation
mvn io.github.rajendarreddyj:tomcat-maven-plugin:run -Dtomcat.catalina.home=/path/to/tomcat

# Hot deployment detects file changes
# (modify a JSP/class file while running, verify automatic reload)
```

## What We're NOT Doing

- Windows Service installation support
- Multiple webapp deployment to single Tomcat instance
- HTTPS/SSL configuration (use external server.xml)
- Remote deployment to Tomcat Manager webapp
- Embedded Tomcat mode (use external process only)
- WAR packaging (use `maven-war-plugin`)
- Tomcat 9.x or earlier support
- Tomcat 10.0.x support (different Jakarta EE version from 10.1.x; use 10.1.x instead)

---

## Implementation Approach

Use external process management (not embedded Tomcat) to:
- Support existing Tomcat installations via `catalinaHome`
- Leverage Tomcat's native startup scripts for proper environment setup
- Align with requirements for exploded WAR deployment to existing webapps directory

---

## Phase 1: Project Setup and Core Infrastructure

### Overview
Create the Maven plugin project structure with POM configuration, base Mojo class, and configuration DTOs.

### Changes Required:

#### 1. Create pom.xml
**File:** `pom.xml`
**Changes:** Create Maven plugin POM with all required dependencies

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>io.github.rajendarreddyj</groupId>
    <artifactId>tomcat-maven-plugin</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>maven-plugin</packaging>

    <name>Maven Tomcat Plugin</name>
    <description>Maven plugin for deploying and managing web applications on Apache Tomcat 10.1.x and 11.x</description>
    <url>https://github.com/rajendarreddyj/tomcat-maven-plugin</url>

    <licenses>
        <license>
            <name>Apache License, Version 2.0</name>
            <url>https://www.apache.org/licenses/LICENSE-2.0.txt</url>
        </license>
    </licenses>

    <developers>
        <developer>
            <id>rajendarreddyj</id>
            <name>Rajendar Reddy</name>
            <email>rajendarreddyj@gmail.com</email>
        </developer>
    </developers>

    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <maven.compiler.release>21</maven.compiler.release>
        <maven.version>3.9.6</maven.version>
        <maven-plugin-api.version>3.9.6</maven-plugin-api.version>
        <maven-plugin-annotations.version>3.11.0</maven-plugin-annotations.version>
        <junit.version>5.10.2</junit.version>
        <mockito.version>5.10.0</mockito.version>
    </properties>

    <prerequisites>
        <maven>${maven.version}</maven>
    </prerequisites>

    <dependencies>
        <!-- Maven Plugin API -->
        <dependency>
            <groupId>org.apache.maven</groupId>
            <artifactId>maven-plugin-api</artifactId>
            <version>${maven-plugin-api.version}</version>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.apache.maven.plugin-tools</groupId>
            <artifactId>maven-plugin-annotations</artifactId>
            <version>${maven-plugin-annotations.version}</version>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.apache.maven</groupId>
            <artifactId>maven-core</artifactId>
            <version>${maven-plugin-api.version}</version>
            <scope>provided</scope>
        </dependency>

        <!-- JSR-330 for Dependency Injection -->
        <dependency>
            <groupId>javax.inject</groupId>
            <artifactId>javax.inject</artifactId>
            <version>1</version>
            <scope>provided</scope>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>${junit.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-core</artifactId>
            <version>${mockito.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-junit-jupiter</artifactId>
            <version>${mockito.version}</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.12.1</version>
                <configuration>
                    <release>${maven.compiler.release}</release>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-plugin-plugin</artifactId>
                <version>${maven-plugin-annotations.version}</version>
                <executions>
                    <execution>
                        <id>default-descriptor</id>
                        <phase>process-classes</phase>
                    </execution>
                    <execution>
                        <id>help-goal</id>
                        <goals>
                            <goal>helpmojo</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
            <plugin>
                <groupId>org.eclipse.sisu</groupId>
                <artifactId>sisu-maven-plugin</artifactId>
                <version>0.9.0.M2</version>
                <executions>
                    <execution>
                        <id>generate-index</id>
                        <goals>
                            <goal>main-index</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.2.5</version>
            </plugin>
        </plugins>
    </build>
</project>
```

#### 2. Create TomcatVersion enum
**File:** `src/main/java/io/github/rajendarreddyj/tomcat/config/TomcatVersion.java`
**Changes:** Version enumeration with download URLs and Java requirements

```java
package io.github.rajendarreddyj.tomcat.config;

/**
 * Represents supported Tomcat major versions with their download URLs and requirements.
 * Note: TOMCAT_10_1 specifically for 10.1.x series (Jakarta EE 10), not 10.0.x
 */
public enum TomcatVersion {
    TOMCAT_10_1("10", 11, "https://dlcdn.apache.org/tomcat/tomcat-10/"),
    TOMCAT_11("11", 17, "https://dlcdn.apache.org/tomcat/tomcat-11/");

    private static final String ARCHIVE_BASE = "https://archive.apache.org/dist/tomcat/tomcat-";

    private final String majorVersion;
    private final int minimumJavaVersion;
    private final String downloadBaseUrl;

    TomcatVersion(String majorVersion, int minimumJavaVersion, String downloadBaseUrl) {
        this.majorVersion = majorVersion;
        this.minimumJavaVersion = minimumJavaVersion;
        this.downloadBaseUrl = downloadBaseUrl;
    }

    public String getMajorVersion() {
        return majorVersion;
    }

    public int getMinimumJavaVersion() {
        return minimumJavaVersion;
    }

    public String getDownloadUrl(String fullVersion) {
        return downloadBaseUrl + "v" + fullVersion + "/bin/apache-tomcat-" + fullVersion + ".zip";
    }

    public String getChecksumUrl(String fullVersion) {
        return getDownloadUrl(fullVersion) + ".sha512";
    }

    public String getArchiveUrl(String fullVersion) {
        return ARCHIVE_BASE + majorVersion + "/v" + fullVersion + "/bin/apache-tomcat-" + fullVersion + ".zip";
    }

    /**
     * Determines the TomcatVersion from a version string.
     * @param version Full version string (e.g., "10.1.52" or "11.0.18")
     * @return The corresponding TomcatVersion enum
     * @throws IllegalArgumentException if version is not supported
     */
    public static TomcatVersion fromVersionString(String version) {
        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("Version string cannot be null or empty");
        }

        if (version.startsWith("10.1.")) {
            return TOMCAT_10_1;
        } else if (version.startsWith("11.")) {
            return TOMCAT_11;
        }

        throw new IllegalArgumentException(
            "Unsupported Tomcat version: " + version + ". Supported versions: 10.1.x, 11.x (10.0.x not supported)");
    }
}
```

#### 3. Create ServerConfiguration DTO
**File:** `src/main/java/io/github/rajendarreddyj/tomcat/config/ServerConfiguration.java`
**Changes:** Immutable configuration class for server settings

```java
package io.github.rajendarreddyj.tomcat.config;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable configuration for Tomcat server settings.
 */
public final class ServerConfiguration {

    private final Path catalinaHome;
    private final Path catalinaBase;
    private final String httpHost;
    private final int httpPort;
    private final Path javaHome;
    private final List<String> vmOptions;
    private final Map<String, String> environmentVariables;
    private final long startupTimeout;
    private final long shutdownTimeout;
    private final List<String> classpathAdditions;

    private ServerConfiguration(Builder builder) {
        this.catalinaHome = Objects.requireNonNull(builder.catalinaHome, "catalinaHome is required");
        this.catalinaBase = builder.catalinaBase != null ? builder.catalinaBase : builder.catalinaHome;
        this.httpHost = builder.httpHost != null ? builder.httpHost : "localhost";
        this.httpPort = builder.httpPort > 0 ? builder.httpPort : 8080;
        this.javaHome = builder.javaHome;
        this.vmOptions = builder.vmOptions != null
            ? Collections.unmodifiableList(builder.vmOptions)
            : Collections.emptyList();
        this.environmentVariables = builder.environmentVariables != null
            ? Collections.unmodifiableMap(builder.environmentVariables)
            : Collections.emptyMap();
        this.startupTimeout = builder.startupTimeout > 0 ? builder.startupTimeout : 120000L;
        this.shutdownTimeout = builder.shutdownTimeout > 0 ? builder.shutdownTimeout : 30000L;
        this.classpathAdditions = builder.classpathAdditions != null
            ? Collections.unmodifiableList(builder.classpathAdditions)
            : Collections.emptyList();
    }

    public Path getCatalinaHome() { return catalinaHome; }
    public Path getCatalinaBase() { return catalinaBase; }
    public String getHttpHost() { return httpHost; }
    public int getHttpPort() { return httpPort; }
    public Path getJavaHome() { return javaHome; }
    public List<String> getVmOptions() { return vmOptions; }
    public Map<String, String> getEnvironmentVariables() { return environmentVariables; }
    public long getStartupTimeout() { return startupTimeout; }
    public long getShutdownTimeout() { return shutdownTimeout; }
    public List<String> getClasspathAdditions() { return classpathAdditions; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Path catalinaHome;
        private Path catalinaBase;
        private String httpHost;
        private int httpPort;
        private Path javaHome;
        private List<String> vmOptions;
        private Map<String, String> environmentVariables;
        private long startupTimeout;
        private long shutdownTimeout;
        private List<String> classpathAdditions;

        public Builder catalinaHome(Path catalinaHome) {
            this.catalinaHome = catalinaHome;
            return this;
        }

        public Builder catalinaBase(Path catalinaBase) {
            this.catalinaBase = catalinaBase;
            return this;
        }

        public Builder httpHost(String httpHost) {
            this.httpHost = httpHost;
            return this;
        }

        public Builder httpPort(int httpPort) {
            this.httpPort = httpPort;
            return this;
        }

        public Builder javaHome(Path javaHome) {
            this.javaHome = javaHome;
            return this;
        }

        public Builder vmOptions(List<String> vmOptions) {
            this.vmOptions = vmOptions;
            return this;
        }

        public Builder environmentVariables(Map<String, String> environmentVariables) {
            this.environmentVariables = environmentVariables;
            return this;
        }

        public Builder startupTimeout(long startupTimeout) {
            this.startupTimeout = startupTimeout;
            return this;
        }

        public Builder shutdownTimeout(long shutdownTimeout) {
            this.shutdownTimeout = shutdownTimeout;
            return this;
        }

        public Builder classpathAdditions(List<String> classpathAdditions) {
            this.classpathAdditions = classpathAdditions;
            return this;
        }

        public ServerConfiguration build() {
            return new ServerConfiguration(this);
        }
    }
}
```

#### 4. Create DeployableConfiguration DTO
**File:** `src/main/java/io/github/rajendarreddyj/tomcat/config/DeployableConfiguration.java`
**Changes:** Immutable configuration class for deployment settings

```java
package io.github.rajendarreddyj.tomcat.config;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Immutable configuration for webapp deployment settings.
 */
public final class DeployableConfiguration {

    private final String moduleName;
    private final Path sourcePath;
    private final String contextPath;
    private final Path deployDir;
    private final String deploymentOutputName;
    private final boolean autopublishEnabled;
    private final int autopublishInactivityLimit;

    private DeployableConfiguration(Builder builder) {
        this.moduleName = builder.moduleName;
        this.sourcePath = Objects.requireNonNull(builder.sourcePath, "sourcePath is required");
        this.contextPath = builder.contextPath != null ? normalizeContextPath(builder.contextPath) : "/";
        this.deployDir = Objects.requireNonNull(builder.deployDir, "deployDir is required");
        this.deploymentOutputName = builder.deploymentOutputName;
        this.autopublishEnabled = builder.autopublishEnabled;
        this.autopublishInactivityLimit = builder.autopublishInactivityLimit > 0
            ? builder.autopublishInactivityLimit : 30;
    }

    private static String normalizeContextPath(String contextPath) {
        if (contextPath == null || contextPath.isBlank() || "/".equals(contextPath)) {
            return "/";
        }
        String normalized = contextPath.startsWith("/") ? contextPath : "/" + contextPath;
        return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
    }

    public String getModuleName() { return moduleName; }
    public Path getSourcePath() { return sourcePath; }
    public String getContextPath() { return contextPath; }
    public Path getDeployDir() { return deployDir; }
    public String getDeploymentOutputName() { return deploymentOutputName; }
    public boolean isAutopublishEnabled() { return autopublishEnabled; }
    public int getAutopublishInactivityLimit() { return autopublishInactivityLimit; }

    /**
     * Determines the target directory name for deployment.
     * Uses deploymentOutputName if set, otherwise derives from contextPath.
     */
    public String getTargetDirectoryName() {
        if (deploymentOutputName != null && !deploymentOutputName.isBlank()) {
            return deploymentOutputName;
        }
        if ("/".equals(contextPath)) {
            return "ROOT";
        }
        return contextPath.substring(1).replace('/', '#');
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String moduleName;
        private Path sourcePath;
        private String contextPath;
        private Path deployDir;
        private String deploymentOutputName;
        private boolean autopublishEnabled;
        private int autopublishInactivityLimit;

        public Builder moduleName(String moduleName) {
            this.moduleName = moduleName;
            return this;
        }

        public Builder sourcePath(Path sourcePath) {
            this.sourcePath = sourcePath;
            return this;
        }

        public Builder contextPath(String contextPath) {
            this.contextPath = contextPath;
            return this;
        }

        public Builder deployDir(Path deployDir) {
            this.deployDir = deployDir;
            return this;
        }

        public Builder deploymentOutputName(String deploymentOutputName) {
            this.deploymentOutputName = deploymentOutputName;
            return this;
        }

        public Builder autopublishEnabled(boolean autopublishEnabled) {
            this.autopublishEnabled = autopublishEnabled;
            return this;
        }

        public Builder autopublishInactivityLimit(int autopublishInactivityLimit) {
            this.autopublishInactivityLimit = autopublishInactivityLimit;
            return this;
        }

        public DeployableConfiguration build() {
            return new DeployableConfiguration(this);
        }
    }
}
```

#### 5. Create CatalinaBaseGenerator
**File:** `src/main/java/io/github/rajendarreddyj/tomcat/config/CatalinaBaseGenerator.java`
**Changes:** Generates a custom CATALINA_BASE with modified server.xml for port/host configuration

```java
package io.github.rajendarreddyj.tomcat.config;

import org.apache.maven.plugin.logging.Log;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generates a CATALINA_BASE directory with customized server.xml.
 * This allows port/host configuration without modifying the original Tomcat installation.
 */
public class CatalinaBaseGenerator {

    private static final Pattern CONNECTOR_PORT_PATTERN = Pattern.compile(
        "(<Connector[^>]*port=\")8080(\"[^>]*)", Pattern.DOTALL);
    private static final Pattern SHUTDOWN_PORT_PATTERN = Pattern.compile(
        "(<Server[^>]*port=\")8005(\"[^>]*)", Pattern.DOTALL);
    private static final Pattern AJP_PORT_PATTERN = Pattern.compile(
        "(<Connector[^>]*port=\")8009(\"[^>]*)", Pattern.DOTALL);

    private final Log log;

    public CatalinaBaseGenerator(Log log) {
        this.log = log;
    }

    /**
     * Generates a CATALINA_BASE directory with custom configuration.
     *
     * @param catalinaHome Source Tomcat installation (CATALINA_HOME)
     * @param targetBase   Target directory for CATALINA_BASE
     * @param httpPort     HTTP port for the Connector
     * @param httpHost     HTTP host/address to bind to (not used in server.xml, but for validation)
     * @param shutdownPort Shutdown port (defaults to httpPort + 1000 - 8080 + 8005 = httpPort - 75)
     * @return Path to the generated CATALINA_BASE
     * @throws IOException if generation fails
     */
    public Path generate(Path catalinaHome, Path targetBase, int httpPort, String httpHost,
                         int shutdownPort) throws IOException {

        log.info("Generating CATALINA_BASE at: " + targetBase);
        log.debug("HTTP Port: " + httpPort + ", Shutdown Port: " + shutdownPort);

        // Create required directories
        createDirectoryStructure(targetBase);

        // Copy conf directory with modifications
        copyAndModifyConf(catalinaHome.resolve("conf"), targetBase.resolve("conf"),
                          httpPort, shutdownPort);

        log.info("CATALINA_BASE generated successfully");
        return targetBase;
    }

    /**
     * Generates with auto-calculated shutdown port.
     */
    public Path generate(Path catalinaHome, Path targetBase, int httpPort, String httpHost)
            throws IOException {
        // Calculate shutdown port: same offset from 8005 as httpPort from 8080
        int shutdownPort = 8005 + (httpPort - 8080);
        if (shutdownPort <= 0 || shutdownPort > 65535) {
            shutdownPort = 8005; // Fallback to default
        }
        return generate(catalinaHome, targetBase, httpPort, httpHost, shutdownPort);
    }

    private void createDirectoryStructure(Path targetBase) throws IOException {
        Files.createDirectories(targetBase.resolve("conf"));
        Files.createDirectories(targetBase.resolve("logs"));
        Files.createDirectories(targetBase.resolve("temp"));
        Files.createDirectories(targetBase.resolve("webapps"));
        Files.createDirectories(targetBase.resolve("work"));
    }

    private void copyAndModifyConf(Path sourceConf, Path targetConf, int httpPort,
                                    int shutdownPort) throws IOException {
        Files.walkFileTree(sourceConf, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                Path targetFile = targetConf.resolve(sourceConf.relativize(file));
                Files.createDirectories(targetFile.getParent());

                if (file.getFileName().toString().equals("server.xml")) {
                    // Modify server.xml with custom ports
                    String content = Files.readString(file);
                    content = modifyServerXml(content, httpPort, shutdownPort);
                    Files.writeString(targetFile, content);
                    log.debug("Modified server.xml with httpPort=" + httpPort);
                } else {
                    // Copy other files as-is
                    Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                    throws IOException {
                Path targetDir = targetConf.resolve(sourceConf.relativize(dir));
                Files.createDirectories(targetDir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private String modifyServerXml(String content, int httpPort, int shutdownPort) {
        // Modify HTTP Connector port (8080 -> httpPort)
        Matcher httpMatcher = CONNECTOR_PORT_PATTERN.matcher(content);
        if (httpMatcher.find()) {
            content = httpMatcher.replaceFirst("$1" + httpPort + "$2");
        }

        // Modify shutdown port (8005 -> shutdownPort)
        Matcher shutdownMatcher = SHUTDOWN_PORT_PATTERN.matcher(content);
        if (shutdownMatcher.find()) {
            content = shutdownMatcher.replaceFirst("$1" + shutdownPort + "$2");
        }

        // Optionally disable AJP connector by changing to a high port or leaving as-is
        // For now, keep AJP at default 8009 (usually commented out in modern Tomcat)

        return content;
    }

    /**
     * Checks if a valid CATALINA_BASE already exists.
     */
    public boolean isValidBase(Path targetBase) {
        return Files.exists(targetBase.resolve("conf/server.xml")) &&
               Files.isDirectory(targetBase.resolve("webapps"));
    }
}
```

#### 6. Create AbstractTomcatMojo base class
**File:** `src/main/java/io/github/rajendarreddyj/tomcat/AbstractTomcatMojo.java`
**Changes:** Base Mojo class with all common parameters

```java
package io.github.rajendarreddyj.tomcat;

import io.github.rajendarreddyj.tomcat.config.CatalinaBaseGenerator;
import io.github.rajendarreddyj.tomcat.config.DeployableConfiguration;
import io.github.rajendarreddyj.tomcat.config.ServerConfiguration;
import io.github.rajendarreddyj.tomcat.config.TomcatVersion;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Abstract base class for all Tomcat plugin Mojos.
 * Provides common configuration parameters and utility methods.
 */
public abstract class AbstractTomcatMojo extends AbstractMojo {

    // ==================== Tomcat Version & Location ====================

    /**
     * Tomcat version to use. If catalinaHome is not specified or doesn't exist,
     * this version will be downloaded automatically.
     * Supported versions: 10.1.x, 11.x
     */
    @Parameter(property = "tomcat.version", defaultValue = "10.1.52")
    protected String tomcatVersion;

    /**
     * Path to an existing Tomcat installation (CATALINA_HOME).
     * If not specified or doesn't exist, Tomcat will be downloaded based on tomcatVersion.
     */
    @Parameter(property = "tomcat.catalina.home")
    protected File catalinaHome;

    /**
     * Path to Tomcat instance directory (CATALINA_BASE).
     * Defaults to catalinaHome if not specified.
     */
    @Parameter(property = "tomcat.catalina.base")
    protected File catalinaBase;

    // ==================== Server Configuration ====================

    /**
     * HTTP port for Tomcat to listen on.
     */
    @Parameter(property = "tomcat.http.port", defaultValue = "8080")
    protected int httpPort;

    /**
     * HTTP host/address for Tomcat to bind to.
     */
    @Parameter(property = "tomcat.http.host", defaultValue = "localhost")
    protected String httpHost;

    /**
     * Timeout in milliseconds for Tomcat startup.
     * Default: 120000ms (2 minutes).
     */
    @Parameter(property = "tomcat.timeout.startup", defaultValue = "120000")
    protected long startupTimeout;

    /**
     * Timeout in milliseconds for Tomcat shutdown.
     * Default: 30000ms (30 seconds).
     */
    @Parameter(property = "tomcat.timeout.shutdown", defaultValue = "30000")
    protected long shutdownTimeout;

    /**
     * Skip plugin execution entirely.
     * Useful for CI pipelines where Tomcat should not be started.
     */
    @Parameter(property = "tomcat.skip", defaultValue = "false")
    protected boolean skip;

    // ==================== JVM Configuration ====================

    /**
     * Path to JDK installation (JAVA_HOME).
     * Defaults to the JDK running Maven.
     */
    @Parameter(property = "tomcat.java.home", defaultValue = "${java.home}")
    protected File javaHome;

    /**
     * JVM options to pass to Tomcat (CATALINA_OPTS).
     */
    @Parameter(property = "tomcat.vm.options")
    protected List<String> vmOptions;

    /**
     * Environment variables to set for Tomcat process.
     */
    @Parameter
    protected Map<String, String> environmentVariables;

    // ==================== Deployment Configuration ====================

    /**
     * Context path for the deployed application.
     * Use "/" for ROOT context.
     */
    @Parameter(property = "tomcat.context.path", defaultValue = "/${project.artifactId}")
    protected String contextPath;

    /**
     * Directory containing the exploded WAR to deploy.
     */
    @Parameter(property = "tomcat.war.directory",
               defaultValue = "${project.build.directory}/${project.build.finalName}")
    protected File warSourceDirectory;

    /**
     * Target directory for deployment within webapps.
     * If not specified, derived from contextPath (e.g., "ROOT" for "/").
     */
    @Parameter(property = "tomcat.deployment.name")
    protected String deploymentOutputName;

    // ==================== Auto-publish Configuration ====================

    /**
     * Enable automatic republishing when source files change.
     */
    @Parameter(property = "tomcat.autopublish.enabled", defaultValue = "false")
    protected boolean autopublishEnabled;

    /**
     * Seconds of inactivity before auto-publish triggers.
     */
    @Parameter(property = "tomcat.autopublish.inactivity", defaultValue = "30")
    protected int autopublishInactivityLimit;

    // ==================== Classpath Configuration ====================

    /**
     * Additional JAR files to add to Tomcat's classpath.
     */
    @Parameter
    protected List<String> classpathAdditions;

    // ==================== Maven Project ====================

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    // ==================== Utility Methods ====================

    /**
     * Resolves the CATALINA_HOME path, downloading Tomcat if necessary.
     */
    protected Path resolveCatalinaHome() throws MojoExecutionException {
        if (catalinaHome != null && catalinaHome.exists() && catalinaHome.isDirectory()) {
            getLog().info("Using existing Tomcat installation: " + catalinaHome);
            validateTomcatInstallation(catalinaHome.toPath());
            return catalinaHome.toPath();
        }

        getLog().info("Tomcat installation not found at: " +
            (catalinaHome != null ? catalinaHome : "<not specified>"));
        getLog().info("Will download Tomcat " + tomcatVersion);

        return downloadTomcat();
    }

    /**
     * Downloads and extracts Tomcat distribution.
     */
    protected Path downloadTomcat() throws MojoExecutionException {
        // TODO: Implement in Phase 2 with TomcatDownloader
        throw new MojoExecutionException(
            "Auto-download not yet implemented. Please specify a valid catalinaHome.");
    }

    /**
     * Validates that the path contains a valid Tomcat installation.
     */
    protected void validateTomcatInstallation(Path tomcatPath) throws MojoExecutionException {
        Path binDir = tomcatPath.resolve("bin");
        Path libDir = tomcatPath.resolve("lib");
        Path catalinaJar = libDir.resolve("catalina.jar");

        if (!Files.isDirectory(binDir)) {
            throw new MojoExecutionException(
                "Invalid Tomcat installation: bin directory not found at " + binDir);
        }
        if (!Files.exists(catalinaJar)) {
            throw new MojoExecutionException(
                "Invalid Tomcat installation: catalina.jar not found at " + catalinaJar);
        }

        getLog().debug("Validated Tomcat installation at: " + tomcatPath);
    }

    /**
     * Validates Java version compatibility with Tomcat version.
     */
    protected void validateJavaVersion() throws MojoExecutionException {
        TomcatVersion version = TomcatVersion.fromVersionString(tomcatVersion);
        int currentJava = Runtime.version().feature();

        if (currentJava < version.getMinimumJavaVersion()) {
            throw new MojoExecutionException(String.format(
                "Tomcat %s requires Java %d or higher, but current Java version is %d",
                tomcatVersion, version.getMinimumJavaVersion(), currentJava));
        }

        getLog().debug("Java version " + currentJava + " is compatible with Tomcat " + tomcatVersion);
    }

    /**
     * Validates that the HTTP port is available before starting Tomcat.
     */
    protected void validatePortAvailable() throws MojoExecutionException {
        try (java.net.ServerSocket socket = new java.net.ServerSocket(
                httpPort, 1, java.net.InetAddress.getByName(httpHost))) {
            socket.setReuseAddress(true);
            getLog().debug("Port " + httpPort + " is available on " + httpHost);
        } catch (IOException e) {
            throw new MojoExecutionException(
                "Port " + httpPort + " is already in use on " + httpHost +
                ". Stop the existing process or configure a different port with -Dtomcat.http.port=XXXX");
        }
    }

    /**
     * Detects the installed Tomcat version from an existing installation.
     * Reads version from lib/catalina.jar!/org/apache/catalina/util/ServerInfo.properties
     * @return The detected version string (e.g., "10.1.52") or null if detection fails
     */
    protected String detectInstalledVersion(Path tomcatPath) {
        Path catalinaJar = tomcatPath.resolve("lib/catalina.jar");
        if (!Files.exists(catalinaJar)) {
            return null;
        }

        try (java.util.jar.JarFile jar = new java.util.jar.JarFile(catalinaJar.toFile())) {
            java.util.jar.JarEntry entry = jar.getJarEntry(
                "org/apache/catalina/util/ServerInfo.properties");
            if (entry == null) {
                return null;
            }

            java.util.Properties props = new java.util.Properties();
            props.load(jar.getInputStream(entry));
            String serverNumber = props.getProperty("server.number");

            if (serverNumber != null && !serverNumber.isBlank()) {
                // Format: 10.1.52.0 -> 10.1.52
                String[] parts = serverNumber.split("\\.");
                if (parts.length >= 3) {
                    return parts[0] + "." + parts[1] + "." + parts[2];
                }
            }
        } catch (IOException e) {
            getLog().debug("Could not detect Tomcat version: " + e.getMessage());
        }
        return null;
    }

    /**
     * Builds ServerConfiguration from Mojo parameters.
     * Generates a custom CATALINA_BASE if needed for port/host configuration.
     */
    protected ServerConfiguration buildServerConfiguration() throws MojoExecutionException {
        Path resolvedHome = resolveCatalinaHome();
        Path resolvedBase = catalinaBase != null ? catalinaBase.toPath() : null;

        // Generate custom CATALINA_BASE for port configuration if not specified
        if (resolvedBase == null && httpPort != 8080) {
            try {
                CatalinaBaseGenerator generator = new CatalinaBaseGenerator(getLog());
                Path generatedBase = tomcatCacheDir.toPath()
                    .resolve("base-" + tomcatVersion + "-" + httpPort);

                if (!generator.isValidBase(generatedBase)) {
                    generator.generate(resolvedHome, generatedBase, httpPort, httpHost);
                }
                resolvedBase = generatedBase;
                getLog().info("Using generated CATALINA_BASE: " + resolvedBase);
            } catch (IOException e) {
                throw new MojoExecutionException(
                    "Failed to generate CATALINA_BASE: " + e.getMessage(), e);
            }
        }

        return ServerConfiguration.builder()
            .catalinaHome(resolvedHome)
            .catalinaBase(resolvedBase)
            .httpHost(httpHost)
            .httpPort(httpPort)
            .javaHome(javaHome != null ? javaHome.toPath() : null)
            .vmOptions(vmOptions)
            .environmentVariables(environmentVariables)
            .startupTimeout(startupTimeout)
            .shutdownTimeout(shutdownTimeout)
            .classpathAdditions(classpathAdditions)
            .build();
    }

    /**
     * Builds DeployableConfiguration from Mojo parameters.
     */
    protected DeployableConfiguration buildDeployableConfiguration(
            ServerConfiguration serverConfig) throws MojoExecutionException {

        if (warSourceDirectory == null || !warSourceDirectory.exists()) {
            throw new MojoExecutionException(
                "WAR source directory does not exist: " + warSourceDirectory);
        }

        Path deployDir = serverConfig.getCatalinaBase().resolve("webapps");

        return DeployableConfiguration.builder()
            .moduleName(project.getArtifactId())
            .sourcePath(warSourceDirectory.toPath())
            .contextPath(contextPath)
            .deployDir(deployDir)
            .deploymentOutputName(deploymentOutputName)
            .autopublishEnabled(autopublishEnabled)
            .autopublishInactivityLimit(autopublishInactivityLimit)
            .build();
    }
}
```

### Success Criteria:

#### Automated Verification:
- [ ] Build compiles: `mvn clean compile`
- [ ] Unit tests pass: `mvn test`
- [ ] Plugin descriptor generated: Check `target/classes/META-INF/maven/plugin.xml` exists

#### Manual Verification:
- [ ] TomcatVersion enum correctly parses "10.1.52" and "11.0.18"
- [ ] TomcatVersion rejects "10.0.x" versions (not supported)
- [ ] ServerConfiguration builder creates valid immutable objects
- [ ] DeployableConfiguration correctly normalizes context paths
- [ ] CatalinaBaseGenerator creates valid server.xml with custom port/host

**Implementation Note:** After completing Phase 1, verify all automated criteria before proceeding.

---

## Phase 2: Tomcat Download and Extraction

### Overview
Implement auto-download capability to fetch Tomcat distributions from Apache mirrors with checksum verification and caching.

### Changes Required:

#### 1. Create ChecksumValidator
**File:** `src/main/java/io/github/rajendarreddyj/tomcat/download/ChecksumValidator.java`
**Changes:** SHA-512 checksum validation

```java
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
 */
public class ChecksumValidator {

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
```

#### 2. Create TomcatDownloader
**File:** `src/main/java/io/github/rajendarreddyj/tomcat/download/TomcatDownloader.java`
**Changes:** Download and extract Tomcat distributions

```java
package io.github.rajendarreddyj.tomcat.download;

import io.github.rajendarreddyj.tomcat.config.TomcatVersion;
import org.apache.maven.plugin.logging.Log;

import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.time.Duration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Downloads and extracts Apache Tomcat distributions.
 */
@Named
@Singleton
public class TomcatDownloader {

    private static final Duration DOWNLOAD_TIMEOUT = Duration.ofMinutes(10);
    private final ChecksumValidator checksumValidator;
    private final HttpClient httpClient;

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

    private void downloadWithFallback(TomcatVersion tomcatVersion, String version,
                                       Path targetPath, Log log) throws IOException {
        String primaryUrl = tomcatVersion.getDownloadUrl(version);
        String archiveUrl = tomcatVersion.getArchiveUrl(version);

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

    private void setExecutablePermissions(Path tomcatDir, Log log) {
        try {
            Path binDir = tomcatDir.resolve(
                tomcatDir.getFileName().toString()).resolve("bin");
            if (Files.exists(binDir)) {
                try (var files = Files.list(binDir)) {
                    files.filter(p -> p.toString().endsWith(".sh"))
                         .forEach(p -> p.toFile().setExecutable(true, false));
                }
            }
        } catch (IOException e) {
            log.warn("Could not set executable permissions: " + e.getMessage());
        }
    }

    private boolean isValidTomcatDir(Path dir) {
        return Files.exists(dir.resolve("bin")) &&
               Files.exists(dir.resolve("lib/catalina.jar"));
    }
}
```

#### 3. Update AbstractTomcatMojo to use TomcatDownloader
**File:** `src/main/java/io/github/rajendarreddyj/tomcat/AbstractTomcatMojo.java`
**Changes:** Implement downloadTomcat() method

```java
// Add to AbstractTomcatMojo class - replace the downloadTomcat() stub:

    /**
     * Cache directory for downloaded Tomcat distributions.
     */
    @Parameter(property = "tomcat.cache.dir",
               defaultValue = "${user.home}/.m2/tomcat-cache")
    protected File tomcatCacheDir;

    /**
     * Downloads and extracts Tomcat distribution.
     */
    protected Path downloadTomcat() throws MojoExecutionException {
        validateJavaVersion();

        TomcatDownloader downloader = new TomcatDownloader();
        try {
            Path cacheDir = tomcatCacheDir.toPath();
            return downloader.download(tomcatVersion, cacheDir, getLog());
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to download Tomcat: " + e.getMessage(), e);
        }
    }
```

### Success Criteria:

#### Automated Verification:
- [ ] Build compiles: `mvn clean compile`
- [ ] Unit tests pass: `mvn test`
- [ ] ChecksumValidator correctly validates SHA-512 hashes

#### Manual Verification:
- [ ] Download works: Delete cache dir, run plugin, verify Tomcat downloads
- [ ] Caching works: Run plugin again, verify it uses cached version
- [ ] Fallback works: Block primary URL, verify archive fallback
- [ ] Extraction works: Verify bin/catalina.sh or .bat exists after extraction

**Implementation Note:** After completing Phase 2, verify download functionality before proceeding.

---

## Phase 3: Tomcat Lifecycle Management

### Overview
Implement process-based Tomcat lifecycle management for start, run, and stop operations.

### Changes Required:

#### 1. Create TomcatLauncher
**File:** `src/main/java/io/github/rajendarreddyj/tomcat/lifecycle/TomcatLauncher.java`
**Changes:** Start and manage Tomcat process

```java
package io.github.rajendarreddyj.tomcat.lifecycle;

import io.github.rajendarreddyj.tomcat.config.ServerConfiguration;
import org.apache.maven.plugin.logging.Log;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Manages Tomcat process lifecycle.
 */
public class TomcatLauncher {

    private final ServerConfiguration config;
    private final Log log;
    private Process tomcatProcess;

    public TomcatLauncher(ServerConfiguration config, Log log) {
        this.config = config;
        this.log = log;
    }

    /**
     * Starts Tomcat in foreground mode (blocks until shutdown).
     */
    public void run() throws IOException, InterruptedException {
        tomcatProcess = startProcess("run");

        // Add shutdown hook for graceful termination
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                stop();
            } catch (Exception e) {
                log.error("Error during shutdown: " + e.getMessage());
            }
        }));

        int exitCode = tomcatProcess.waitFor();
        if (exitCode != 0) {
            log.warn("Tomcat exited with code: " + exitCode);
        }
    }

    /**
     * Starts Tomcat in background mode.
     */
    public void start() throws IOException {
        tomcatProcess = startProcess("start");
        waitForStartup();
    }

    /**
     * Stops running Tomcat instance.
     */
    public void stop() throws IOException, InterruptedException {
        if (tomcatProcess != null && tomcatProcess.isAlive()) {
            log.info("Stopping Tomcat process...");
            tomcatProcess.destroy();

            if (!tomcatProcess.waitFor(config.getShutdownTimeout(), TimeUnit.MILLISECONDS)) {
                log.warn("Tomcat did not stop gracefully, forcing termination...");
                tomcatProcess.destroyForcibly();
            }
        } else {
            // Try to stop via catalina script
            stopViaScript();
        }
    }

    private Process startProcess(String command) throws IOException {
        Path catalinaScript = resolveCatalinaScript();

        ProcessBuilder pb = new ProcessBuilder();
        List<String> cmd = new ArrayList<>();

        if (isWindows()) {
            cmd.add("cmd.exe");
            cmd.add("/c");
            cmd.add(catalinaScript.toString());
        } else {
            cmd.add(catalinaScript.toString());
        }
        cmd.add(command);

        pb.command(cmd);
        configureEnvironment(pb.environment());
        pb.directory(config.getCatalinaHome().toFile());
        pb.inheritIO();

        log.info("Starting Tomcat with command: " + String.join(" ", cmd));
        log.info("CATALINA_HOME: " + config.getCatalinaHome());
        log.info("CATALINA_BASE: " + config.getCatalinaBase());
        log.info("HTTP Port: " + config.getHttpPort());

        return pb.start();
    }

    private void stopViaScript() throws IOException, InterruptedException {
        Path catalinaScript = resolveCatalinaScript();

        ProcessBuilder pb = new ProcessBuilder();
        List<String> cmd = new ArrayList<>();

        if (isWindows()) {
            cmd.add("cmd.exe");
            cmd.add("/c");
            cmd.add(catalinaScript.toString());
        } else {
            cmd.add(catalinaScript.toString());
        }
        cmd.add("stop");

        pb.command(cmd);
        configureEnvironment(pb.environment());
        pb.inheritIO();

        Process stopProcess = pb.start();
        boolean stopped = stopProcess.waitFor(config.getShutdownTimeout(), TimeUnit.MILLISECONDS);

        if (!stopped) {
            log.warn("Stop command timed out");
            stopProcess.destroyForcibly();
        }
    }

    private Path resolveCatalinaScript() throws IOException {
        String scriptName = isWindows() ? "catalina.bat" : "catalina.sh";
        Path script = config.getCatalinaHome().resolve("bin").resolve(scriptName);

        if (!Files.exists(script)) {
            throw new IOException("Catalina script not found: " + script);
        }

        return script;
    }

    private void configureEnvironment(Map<String, String> env) {
        // Core Tomcat environment
        env.put("CATALINA_HOME", config.getCatalinaHome().toString());
        env.put("CATALINA_BASE", config.getCatalinaBase().toString());

        if (config.getJavaHome() != null) {
            env.put("JAVA_HOME", config.getJavaHome().toString());
        }

        // VM options
        if (!config.getVmOptions().isEmpty()) {
            String existingOpts = env.getOrDefault("CATALINA_OPTS", "");
            String newOpts = String.join(" ", config.getVmOptions());
            env.put("CATALINA_OPTS", (existingOpts + " " + newOpts).trim());
        }

        // Classpath additions
        if (!config.getClasspathAdditions().isEmpty()) {
            String pathSeparator = System.getProperty("path.separator");
            String additions = String.join(pathSeparator, config.getClasspathAdditions());
            String existing = env.getOrDefault("CLASSPATH", "");
            env.put("CLASSPATH", existing.isEmpty() ? additions : existing + pathSeparator + additions);
            log.debug("Added to CLASSPATH: " + additions);
        }

        // Custom environment variables
        env.putAll(config.getEnvironmentVariables());
    }

    private void waitForStartup() throws IOException {
        log.info("Waiting for Tomcat to start (timeout: " + config.getStartupTimeout() + "ms)...");

        long startTime = System.currentTimeMillis();
        long timeout = config.getStartupTimeout();

        while (System.currentTimeMillis() - startTime < timeout) {
            if (isServerReady()) {
                log.info("Tomcat started successfully on port " + config.getHttpPort());
                return;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Startup wait interrupted", e);
            }
        }

        throw new IOException("Tomcat startup timed out after " + timeout + "ms");
    }

    private boolean isServerReady() {
        try {
            java.net.Socket socket = new java.net.Socket(config.getHttpHost(), config.getHttpPort());
            socket.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }

    public Process getProcess() {
        return tomcatProcess;
    }
}
```

#### 2. Create RunMojo
**File:** `src/main/java/io/github/rajendarreddyj/tomcat/RunMojo.java`
**Changes:** Implement tomcat:run goal

```java
package io.github.rajendarreddyj.tomcat;

import io.github.rajendarreddyj.tomcat.config.ServerConfiguration;
import io.github.rajendarreddyj.tomcat.deploy.ExplodedWarDeployer;
import io.github.rajendarreddyj.tomcat.lifecycle.TomcatLauncher;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Runs Apache Tomcat in foreground mode with the project's webapp deployed.
 *
 * <p>Usage: {@code mvn tomcat:run}</p>
 */
@Mojo(
    name = "run",
    defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST,
    requiresDependencyResolution = ResolutionScope.RUNTIME,
    threadSafe = true
)
public class RunMojo extends AbstractTomcatMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Skipping Tomcat execution (tomcat.skip=true)");
            return;
        }

        validateJavaVersion();
        validatePortAvailable();

        try {
            ServerConfiguration serverConfig = buildServerConfiguration();
            var deployConfig = buildDeployableConfiguration(serverConfig);

            // Deploy webapp
            getLog().info("Deploying webapp to: " + deployConfig.getDeployDir());
            ExplodedWarDeployer deployer = new ExplodedWarDeployer(getLog());
            deployer.deploy(deployConfig);

            // Start Tomcat
            TomcatLauncher launcher = new TomcatLauncher(serverConfig, getLog());
            getLog().info("Starting Tomcat " + tomcatVersion + " on http://" +
                httpHost + ":" + httpPort + contextPath);

            launcher.run();

        } catch (Exception e) {
            throw new MojoExecutionException("Failed to run Tomcat: " + e.getMessage(), e);
        }
    }
}
```

#### 3. Create StartMojo
**File:** `src/main/java/io/github/rajendarreddyj/tomcat/StartMojo.java`
**Changes:** Implement tomcat:start goal (background)

```java
package io.github.rajendarreddyj.tomcat;

import io.github.rajendarreddyj.tomcat.config.ServerConfiguration;
import io.github.rajendarreddyj.tomcat.deploy.ExplodedWarDeployer;
import io.github.rajendarreddyj.tomcat.lifecycle.TomcatLauncher;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Starts Apache Tomcat in background mode with the project's webapp deployed.
 * The process ID is stored for later use by the stop goal.
 *
 * <p>Usage: {@code mvn tomcat:start}</p>
 */
@Mojo(
    name = "start",
    defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST,
    requiresDependencyResolution = ResolutionScope.RUNTIME,
    threadSafe = true
)
public class StartMojo extends AbstractTomcatMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Skipping Tomcat execution (tomcat.skip=true)");
            return;
        }

        validateJavaVersion();
        validatePortAvailable();

        try {
            ServerConfiguration serverConfig = buildServerConfiguration();
            var deployConfig = buildDeployableConfiguration(serverConfig);

            // Deploy webapp
            getLog().info("Deploying webapp to: " + deployConfig.getDeployDir());
            ExplodedWarDeployer deployer = new ExplodedWarDeployer(getLog());
            deployer.deploy(deployConfig);

            // Start Tomcat in background
            TomcatLauncher launcher = new TomcatLauncher(serverConfig, getLog());
            launcher.start();

            // Store PID for stop goal
            storePid(serverConfig.getCatalinaBase(), launcher.getProcess());

            getLog().info("Tomcat started in background on http://" +
                httpHost + ":" + httpPort + contextPath);

        } catch (Exception e) {
            throw new MojoExecutionException("Failed to start Tomcat: " + e.getMessage(), e);
        }
    }

    private void storePid(Path catalinaBase, Process process) throws IOException {
        Path pidFile = catalinaBase.resolve("tomcat.pid");
        Files.writeString(pidFile, String.valueOf(process.pid()));
        getLog().debug("Stored PID " + process.pid() + " in " + pidFile);
    }
}
```

#### 4. Create StopMojo
**File:** `src/main/java/io/github/rajendarreddyj/tomcat/StopMojo.java`
**Changes:** Implement tomcat:stop goal

```java
package io.github.rajendarreddyj.tomcat;

import io.github.rajendarreddyj.tomcat.config.ServerConfiguration;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * Stops a running Apache Tomcat instance started by the start goal.
 *
 * <p>Usage: {@code mvn tomcat:stop}</p>
 */
@Mojo(
    name = "stop",
    defaultPhase = LifecyclePhase.POST_INTEGRATION_TEST,
    threadSafe = true
)
public class StopMojo extends AbstractTomcatMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            ServerConfiguration serverConfig = buildServerConfiguration();
            Path pidFile = serverConfig.getCatalinaBase().resolve("tomcat.pid");

            if (Files.exists(pidFile)) {
                long pid = Long.parseLong(Files.readString(pidFile).trim());
                stopProcess(pid);
                Files.deleteIfExists(pidFile);
            } else {
                getLog().warn("No PID file found at " + pidFile +
                    ". Attempting to stop via script...");
                stopViaScript(serverConfig);
            }

            getLog().info("Tomcat stopped successfully");

        } catch (Exception e) {
            throw new MojoExecutionException("Failed to stop Tomcat: " + e.getMessage(), e);
        }
    }

    private void stopProcess(long pid) throws IOException, InterruptedException {
        ProcessHandle.of(pid).ifPresentOrElse(
            handle -> {
                getLog().info("Stopping Tomcat process (PID: " + pid + ")");
                handle.destroy();
                try {
                    handle.onExit().get(shutdownTimeout, TimeUnit.MILLISECONDS);
                } catch (Exception e) {
                    getLog().warn("Graceful shutdown timed out, forcing termination...");
                    handle.destroyForcibly();
                }
            },
            () -> getLog().warn("Process " + pid + " not found, may have already stopped")
        );
    }

    private void stopViaScript(ServerConfiguration config) throws IOException, InterruptedException {
        String scriptName = isWindows() ? "catalina.bat" : "catalina.sh";
        Path script = config.getCatalinaHome().resolve("bin").resolve(scriptName);

        ProcessBuilder pb = new ProcessBuilder();
        if (isWindows()) {
            pb.command("cmd.exe", "/c", script.toString(), "stop");
        } else {
            pb.command(script.toString(), "stop");
        }

        pb.environment().put("CATALINA_HOME", config.getCatalinaHome().toString());
        pb.environment().put("CATALINA_BASE", config.getCatalinaBase().toString());
        pb.inheritIO();

        Process stopProcess = pb.start();
        stopProcess.waitFor(shutdownTimeout, TimeUnit.MILLISECONDS);
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }
}
```

### Success Criteria:

#### Automated Verification:
- [ ] Build compiles: `mvn clean compile`
- [ ] Plugin descriptor contains run, start, stop goals
- [ ] Unit tests pass: `mvn test`

#### Manual Verification:
- [ ] `mvn tomcat:run` starts Tomcat in foreground and blocks
- [ ] Ctrl+C gracefully stops Tomcat during `run`
- [ ] `mvn tomcat:start` starts Tomcat and returns
- [ ] `mvn tomcat:stop` stops the background instance
- [ ] PID file created during start, deleted during stop

**Implementation Note:** After completing Phase 3, verify lifecycle management before proceeding.

---

## Phase 4: Webapp Deployment

### Overview
Implement exploded WAR deployment with support for deployment output naming and context path configuration.

### Changes Required:

#### 1. Create ExplodedWarDeployer
**File:** `src/main/java/io/github/rajendarreddyj/tomcat/deploy/ExplodedWarDeployer.java`
**Changes:** Copy exploded WAR to webapps directory

```java
package io.github.rajendarreddyj.tomcat.deploy;

import io.github.rajendarreddyj.tomcat.config.DeployableConfiguration;
import org.apache.maven.plugin.logging.Log;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Deploys exploded WAR directories to Tomcat webapps.
 */
public class ExplodedWarDeployer {

    private final Log log;

    public ExplodedWarDeployer(Log log) {
        this.log = log;
    }

    /**
     * Deploys the webapp to the configured location.
     *
     * @param config Deployment configuration
     * @throws IOException if deployment fails
     */
    public void deploy(DeployableConfiguration config) throws IOException {
        Path sourcePath = config.getSourcePath();
        Path deployDir = config.getDeployDir();
        String targetName = config.getTargetDirectoryName();
        Path targetPath = deployDir.resolve(targetName);

        if (!Files.exists(sourcePath)) {
            throw new IOException("Source path does not exist: " + sourcePath);
        }

        log.info("Deploying " + config.getModuleName() + " to " + targetPath);
        log.debug("Source: " + sourcePath);
        log.debug("Context path: " + config.getContextPath());

        // Clean existing deployment
        if (Files.exists(targetPath)) {
            log.info("Removing existing deployment: " + targetPath);
            deleteDirectory(targetPath);
        }

        // Create webapps directory if needed
        Files.createDirectories(deployDir);

        // Copy webapp
        copyDirectory(sourcePath, targetPath);

        log.info("Deployment complete: " + targetName);
    }

    /**
     * Redeploys the webapp (remove and recreate).
     */
    public void redeploy(DeployableConfiguration config) throws IOException {
        Path targetPath = config.getDeployDir().resolve(config.getTargetDirectoryName());

        if (Files.exists(targetPath)) {
            log.info("Undeploying existing application...");
            deleteDirectory(targetPath);
        }

        deploy(config);
    }

    /**
     * Synchronizes changed files from source to deployed webapp.
     * Used for hot deployment.
     */
    public void syncChanges(DeployableConfiguration config, Path changedFile) throws IOException {
        Path sourcePath = config.getSourcePath();
        Path targetDir = config.getDeployDir().resolve(config.getTargetDirectoryName());

        // Calculate relative path
        Path relativePath = sourcePath.relativize(changedFile);
        Path targetFile = targetDir.resolve(relativePath);

        log.debug("Syncing changed file: " + relativePath);

        Files.createDirectories(targetFile.getParent());
        Files.copy(changedFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
    }

    private void copyDirectory(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                    throws IOException {
                Path targetDir = target.resolve(source.relativize(dir));
                Files.createDirectories(targetDir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                Path targetFile = target.resolve(source.relativize(file));
                Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void deleteDirectory(Path directory) throws IOException {
        if (Files.exists(directory)) {
            try (Stream<Path> walk = Files.walk(directory)) {
                walk.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            log.warn("Could not delete: " + path);
                        }
                    });
            }
        }
    }
}
```

#### 2. Create DeployMojo
**File:** `src/main/java/io/github/rajendarreddyj/tomcat/DeployMojo.java`
**Changes:** Implement tomcat:deploy goal for redeployment

```java
package io.github.rajendarreddyj.tomcat;

import io.github.rajendarreddyj.tomcat.config.ServerConfiguration;
import io.github.rajendarreddyj.tomcat.deploy.ExplodedWarDeployer;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Deploys or redeploys the webapp to a running Tomcat instance.
 *
 * <p>Usage: {@code mvn tomcat:deploy}</p>
 */
@Mojo(
    name = "deploy",
    requiresDependencyResolution = ResolutionScope.RUNTIME,
    threadSafe = true
)
public class DeployMojo extends AbstractTomcatMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            ServerConfiguration serverConfig = buildServerConfiguration();
            var deployConfig = buildDeployableConfiguration(serverConfig);

            ExplodedWarDeployer deployer = new ExplodedWarDeployer(getLog());
            deployer.redeploy(deployConfig);

            getLog().info("Webapp deployed to: " + deployConfig.getContextPath());

        } catch (Exception e) {
            throw new MojoExecutionException("Deployment failed: " + e.getMessage(), e);
        }
    }
}
```

### Success Criteria:

#### Automated Verification:
- [ ] Build compiles: `mvn clean compile`
- [ ] Plugin descriptor contains deploy goal
- [ ] Unit tests pass: `mvn test`

#### Manual Verification:
- [ ] `mvn tomcat:deploy` copies exploded WAR to webapps
- [ ] ROOT deployment works with `<deploymentOutputName>ROOT</deploymentOutputName>`
- [ ] Redeploy removes old deployment before copying new one
- [ ] Context path `/myapp` creates `webapps/myapp` directory

**Implementation Note:** After completing Phase 4, verify deployment before proceeding.

---

## Phase 5: Hot Deployment (Auto-publish)

### Overview
Implement file system watching to detect changes and automatically sync to deployed webapp.

### Changes Required:

#### 1. Create HotDeployWatcher
**File:** `src/main/java/io/github/rajendarreddyj/tomcat/deploy/HotDeployWatcher.java`
**Changes:** Watch for file changes with inactivity detection

```java
package io.github.rajendarreddyj.tomcat.deploy;

import io.github.rajendarreddyj.tomcat.config.DeployableConfiguration;
import org.apache.maven.plugin.logging.Log;

import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Watches for file changes and triggers hot deployment after inactivity period.
 */
public class HotDeployWatcher implements AutoCloseable {

    private final DeployableConfiguration config;
    private final ExplodedWarDeployer deployer;
    private final Log log;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicLong lastChangeTime = new AtomicLong(0);
    private WatchService watchService;
    private Thread watchThread;
    private ScheduledFuture<?> syncTask;

    public HotDeployWatcher(DeployableConfiguration config, ExplodedWarDeployer deployer, Log log) {
        this.config = config;
        this.deployer = deployer;
        this.log = log;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "hot-deploy-sync");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Starts watching for file changes.
     */
    public void start() throws IOException {
        if (!config.isAutopublishEnabled()) {
            log.info("Auto-publish is disabled");
            return;
        }

        running.set(true);
        watchService = FileSystems.getDefault().newWatchService();

        // Register source directory and subdirectories
        registerRecursive(config.getSourcePath());

        // Start watching thread
        watchThread = new Thread(this::watch, "hot-deploy-watcher");
        watchThread.setDaemon(true);
        watchThread.start();

        // Schedule periodic sync check
        int inactivitySeconds = config.getAutopublishInactivityLimit();
        syncTask = scheduler.scheduleAtFixedRate(
            this::checkAndSync,
            inactivitySeconds,
            inactivitySeconds,
            TimeUnit.SECONDS
        );

        log.info("Hot deployment enabled (inactivity limit: " + inactivitySeconds + "s)");
    }

    private void registerRecursive(Path path) throws IOException {
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, java.nio.file.attribute.BasicFileAttributes attrs)
                    throws IOException {
                dir.register(watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void watch() {
        log.debug("Watch thread started for: " + config.getSourcePath());

        while (running.get()) {
            try {
                WatchKey key = watchService.poll(1, TimeUnit.SECONDS);
                if (key != null) {
                    for (WatchEvent<?> event : key.pollEvents()) {
                        @SuppressWarnings("unchecked")
                        WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                        Path changed = ((Path) key.watchable()).resolve(pathEvent.context());

                        log.debug("File changed: " + changed + " (" + event.kind() + ")");
                        lastChangeTime.set(System.currentTimeMillis());

                        // Register new directories
                        if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE
                                && Files.isDirectory(changed)) {
                            try {
                                registerRecursive(changed);
                            } catch (IOException e) {
                                log.warn("Could not watch new directory: " + changed);
                            }
                        }
                    }
                    key.reset();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (ClosedWatchServiceException e) {
                break;
            }
        }

        log.debug("Watch thread stopped");
    }

    private void checkAndSync() {
        long lastChange = lastChangeTime.get();
        if (lastChange == 0) {
            return;
        }

        long inactivityMs = config.getAutopublishInactivityLimit() * 1000L;
        long elapsed = System.currentTimeMillis() - lastChange;

        if (elapsed >= inactivityMs) {
            lastChangeTime.set(0);
            performSync();
        }
    }

    private void performSync() {
        try {
            log.info("Auto-publishing changes...");
            deployer.redeploy(config);
            log.info("Auto-publish complete");
        } catch (IOException e) {
            log.error("Auto-publish failed: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        running.set(false);

        if (syncTask != null) {
            syncTask.cancel(false);
        }

        scheduler.shutdown();

        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                log.warn("Error closing watch service: " + e.getMessage());
            }
        }

        if (watchThread != null) {
            watchThread.interrupt();
        }
    }
}
```

#### 2. Update RunMojo to use HotDeployWatcher
**File:** `src/main/java/io/github/rajendarreddyj/tomcat/RunMojo.java`
**Changes:** Add hot deployment support

```java
// Modify RunMojo.execute() method to include hot deployment:

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        validateJavaVersion();

        try {
            ServerConfiguration serverConfig = buildServerConfiguration();
            var deployConfig = buildDeployableConfiguration(serverConfig);

            // Deploy webapp
            getLog().info("Deploying webapp to: " + deployConfig.getDeployDir());
            ExplodedWarDeployer deployer = new ExplodedWarDeployer(getLog());
            deployer.deploy(deployConfig);

            // Start hot deploy watcher if enabled
            HotDeployWatcher watcher = new HotDeployWatcher(deployConfig, deployer, getLog());

            try {
                watcher.start();

                // Start Tomcat
                TomcatLauncher launcher = new TomcatLauncher(serverConfig, getLog());
                getLog().info("Starting Tomcat " + tomcatVersion + " on http://" +
                    httpHost + ":" + httpPort + contextPath);

                launcher.run();

            } finally {
                watcher.close();
            }

        } catch (Exception e) {
            throw new MojoExecutionException("Failed to run Tomcat: " + e.getMessage(), e);
        }
    }
```

Add import:
```java
import io.github.rajendarreddyj.tomcat.deploy.HotDeployWatcher;
```

### Success Criteria:

#### Automated Verification:
- [ ] Build compiles: `mvn clean compile`
- [ ] Unit tests pass: `mvn test`

#### Manual Verification:
- [ ] With `<autopublishEnabled>true</autopublishEnabled>`:
  - [ ] Modifying a JSP triggers republish after inactivity limit
  - [ ] Adding a new file triggers republish
  - [ ] Rapid changes are batched (no republish until inactivity)
- [ ] With `<autopublishEnabled>false</autopublishEnabled>`:
  - [ ] No file watching occurs
  - [ ] Changes require manual `mvn tomcat:deploy`

**Implementation Note:** After completing Phase 5, verify hot deployment before finalizing.

---

## Phase 6: Testing and Documentation

### Overview
Add comprehensive tests and generate plugin documentation.

### Changes Required:

#### 1. Create Unit Tests for TomcatVersion
**File:** `src/test/java/io/github/rajendarreddyj/tomcat/config/TomcatVersionTest.java`

```java
package io.github.rajendarreddyj.tomcat.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TomcatVersionTest {

    @Test
    void fromVersionString_tomcat10_1() {
        assertEquals(TomcatVersion.TOMCAT_10_1, TomcatVersion.fromVersionString("10.1.52"));
        assertEquals(TomcatVersion.TOMCAT_10_1, TomcatVersion.fromVersionString("10.1.0"));
    }

    @Test
    void fromVersionString_tomcat11() {
        assertEquals(TomcatVersion.TOMCAT_11, TomcatVersion.fromVersionString("11.0.18"));
        assertEquals(TomcatVersion.TOMCAT_11, TomcatVersion.fromVersionString("11.0.0"));
    }

    @Test
    void fromVersionString_invalid() {
        assertThrows(IllegalArgumentException.class,
            () -> TomcatVersion.fromVersionString("9.0.50"));
        assertThrows(IllegalArgumentException.class,
            () -> TomcatVersion.fromVersionString(null));
        assertThrows(IllegalArgumentException.class,
            () -> TomcatVersion.fromVersionString(""));
    }

    @Test
    void fromVersionString_tomcat10_0_notSupported() {
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
    void getMinimumJavaVersion() {
        assertEquals(11, TomcatVersion.TOMCAT_10_1.getMinimumJavaVersion());
        assertEquals(17, TomcatVersion.TOMCAT_11.getMinimumJavaVersion());
    }
}
```

#### 2. Create Unit Tests for Configuration classes
**File:** `src/test/java/io/github/rajendarreddyj/tomcat/config/DeployableConfigurationTest.java`

```java
package io.github.rajendarreddyj.tomcat.config;

import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class DeployableConfigurationTest {

    @Test
    void getTargetDirectoryName_withDeploymentOutputName() {
        DeployableConfiguration config = DeployableConfiguration.builder()
            .sourcePath(Path.of("/source"))
            .deployDir(Path.of("/target"))
            .deploymentOutputName("ROOT")
            .build();

        assertEquals("ROOT", config.getTargetDirectoryName());
    }

    @Test
    void getTargetDirectoryName_rootContext() {
        DeployableConfiguration config = DeployableConfiguration.builder()
            .sourcePath(Path.of("/source"))
            .deployDir(Path.of("/target"))
            .contextPath("/")
            .build();

        assertEquals("ROOT", config.getTargetDirectoryName());
    }

    @Test
    void getTargetDirectoryName_namedContext() {
        DeployableConfiguration config = DeployableConfiguration.builder()
            .sourcePath(Path.of("/source"))
            .deployDir(Path.of("/target"))
            .contextPath("/myapp")
            .build();

        assertEquals("myapp", config.getTargetDirectoryName());
    }

    @Test
    void normalizeContextPath() {
        DeployableConfiguration config = DeployableConfiguration.builder()
            .sourcePath(Path.of("/source"))
            .deployDir(Path.of("/target"))
            .contextPath("myapp")  // missing leading /
            .build();

        assertEquals("/myapp", config.getContextPath());
    }
}
```

#### 3. Update pom.xml with integration test configuration
Add to pom.xml:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-invoker-plugin</artifactId>
    <version>3.6.0</version>
    <configuration>
        <projectsDirectory>src/it</projectsDirectory>
        <cloneProjectsTo>${project.build.directory}/it</cloneProjectsTo>
        <postBuildHookScript>verify</postBuildHookScript>
        <localRepositoryPath>${project.build.directory}/local-repo</localRepositoryPath>
        <goals>
            <goal>install</goal>
        </goals>
    </configuration>
    <executions>
        <execution>
            <id>integration-test</id>
            <goals>
                <goal>install</goal>
                <goal>run</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

#### 4. Create Integration Test Project
**Directory:** `src/it/basic-webapp/`

**File:** `src/it/basic-webapp/pom.xml`
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                             http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>io.github.rajendarreddyj.it</groupId>
    <artifactId>basic-webapp</artifactId>
    <version>1.0-SNAPSHOT</version>
    <packaging>war</packaging>

    <properties>
        <maven.compiler.release>21</maven.compiler.release>
    </properties>

    <dependencies>
        <dependency>
            <groupId>jakarta.servlet</groupId>
            <artifactId>jakarta.servlet-api</artifactId>
            <version>6.0.0</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-war-plugin</artifactId>
                <version>3.4.0</version>
            </plugin>
            <plugin>
                <groupId>@project.groupId@</groupId>
                <artifactId>@project.artifactId@</artifactId>
                <version>@project.version@</version>
                <configuration>
                    <tomcatVersion>10.1.52</tomcatVersion>
                    <httpPort>18080</httpPort>
                    <contextPath>/test</contextPath>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

**File:** `src/it/basic-webapp/src/main/webapp/WEB-INF/web.xml`
```xml
<?xml version="1.0" encoding="UTF-8"?>
<web-app xmlns="https://jakarta.ee/xml/ns/jakartaee"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee
                             https://jakarta.ee/xml/ns/jakartaee/web-app_6_0.xsd"
         version="6.0">
    <display-name>Integration Test Webapp</display-name>
</web-app>
```

**File:** `src/it/basic-webapp/src/main/webapp/index.jsp`
```jsp
<%@ page contentType="text/html;charset=UTF-8" %>
<!DOCTYPE html>
<html>
<head><title>IT Test</title></head>
<body>
<h1>Integration Test Success</h1>
<p>Timestamp: <%= new java.util.Date() %></p>
</body>
</html>
```

**File:** `src/it/basic-webapp/verify.groovy`
```groovy
import java.net.URL
import java.net.HttpURLConnection

// Check that the build log contains expected output
File buildLog = new File(basedir, "build.log")
assert buildLog.exists()
assert buildLog.text.contains("Downloading Tomcat") || buildLog.text.contains("Using cached Tomcat")
assert buildLog.text.contains("Deploying webapp")

// Note: For full IT testing, you would start Tomcat in background,
// verify the app is accessible, then stop it. This requires more complex setup.
println "Integration test verification completed successfully"
```

### Success Criteria:

#### Automated Verification:
- [ ] All unit tests pass: `mvn test`
- [ ] Integration tests pass: `mvn verify`
- [ ] Plugin documentation generated: `mvn site`
- [ ] No compiler warnings

#### Manual Verification:
- [ ] README.md has usage examples
- [ ] Plugin help goal works: `mvn tomcat:help`
- [ ] All parameters documented in generated site

---

## Testing Strategy

### Unit Tests:
- TomcatVersion parsing and URL generation
- ServerConfiguration and DeployableConfiguration builders
- ChecksumValidator hashing
- ExplodedWarDeployer file operations (mock filesystem)

### Integration Tests:
- Full plugin execution with real Tomcat download
- Deploy/redeploy lifecycle
- Start/stop lifecycle

### Manual Testing Steps:
1. Create a sample WAR project
2. Run `mvn tomcat:run` with auto-download
3. Verify webapp accessible at configured URL
4. Modify a JSP file, verify hot deployment
5. Stop with Ctrl+C, verify graceful shutdown
6. Run `mvn tomcat:start`, then `mvn tomcat:stop`

---

## Risks and Mitigations

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Apache download URL structure changes | Medium | High | Fallback to archive.apache.org; consider making URLs configurable via plugin parameter |
| Windows file locking during hot deploy | High | Medium | Use `Files.move()` atomic operations with retry logic; add exponential backoff (100ms, 200ms, 400ms) for up to 5 attempts |
| Port conflict not detected early | Low | Low | Added `validatePortAvailable()` check before starting - port is validated at plugin execution time |
| Race condition in HotDeployWatcher | Low | Medium | Use `AtomicLong` for change tracking; synchronize on redeploy trigger; debounce changes with inactivity timer |
| Timeout too short in slow CI environments | Medium | Medium | Defaults: 120s startup, 30s shutdown; users can override with `-Dtomcat.timeout.startup=X` for slow environments |
| CatalinaBase not regenerated after port change | Low | Low | Use port number in cache path (`base-10.1.52-8020`); add `isValidBase()` check |
| Tomcat version mismatch with existing installation | Medium | Medium | Added `detectInstalledVersion()` to warn if configured version differs from installed; log warning but proceed |
| Plugin blocks CI pipelines | Medium | Low | Added `tomcat.skip` property for easy disabling; document usage in README |

### Known Limitations

1. **10.0.x not supported** - Only Tomcat 10.1.x and 11.x are supported. Tomcat 10.0.x uses different Jakarta EE version.
2. **Single webapp only** - Each plugin execution deploys one webapp. For multiple apps, run multiple Maven modules.
3. **No embedded mode** - Always uses external process; no in-JVM Tomcat for faster startup.
4. **No SSL auto-config** - HTTPS requires manual server.xml configuration or existing catalinaBase.

## Performance Considerations

- Cache downloaded Tomcat distributions to avoid repeated downloads
- Use NIO file operations for efficient copying
- Batch file change notifications during hot deployment
- Use daemon threads for file watching to not block shutdown

## Migration Notes

N/A - Greenfield project

## References

- Research: [2026-02-05-implementation-requirements-research.md](../research/current/2026-02-05-implementation-requirements-research.md)
- Requirements: [requirements.md](../../../../requirements.md)
- Legacy Plugin: https://github.com/apache/tomcat-maven-plugin
- Maven Plugin Development: https://maven.apache.org/guides/plugin/guide-java-plugin-development.html
