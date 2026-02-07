---
title: Tomcat Auto-Download Implementation Patterns for Maven Plugin
date: 2026-02-05
author: GitHub Copilot
sources:
  - https://dlcdn.apache.org/tomcat/tomcat-10/
  - https://dlcdn.apache.org/tomcat/tomcat-11/
  - https://archive.apache.org/dist/tomcat/
  - https://central.sonatype.com/artifact/org.apache.tomcat/tomcat
  - https://central.sonatype.com/artifact/org.apache.tomcat.embed/tomcat-embed-core
  - https://maven.apache.org/resolver/
  - https://maven.apache.org/plugins/maven-dependency-plugin/unpack-mojo.html
  - https://tomcat.apache.org/tomcat-11.0-doc/RUNNING.txt
---

# Tomcat Auto-Download Implementation Patterns for Maven Plugin

## 1. Download URL Patterns

### 1.1 Primary Mirror URLs

Apache provides the following download mirrors:

| Mirror Type | Base URL | Purpose |
|-------------|----------|---------|
| Primary CDN | `https://dlcdn.apache.org/tomcat/` | Current releases |
| Archive | `https://archive.apache.org/dist/tomcat/` | Historical releases |
| Downloads | `https://downloads.apache.org/tomcat/` | Current releases (alternative) |

### 1.2 URL Templates for Tomcat 10.1.x

```
# Base directory listing
https://dlcdn.apache.org/tomcat/tomcat-10/

# Version directory (current: v10.1.52)
https://dlcdn.apache.org/tomcat/tomcat-10/v{VERSION}/

# Binary distributions
https://dlcdn.apache.org/tomcat/tomcat-10/v{VERSION}/bin/apache-tomcat-{VERSION}.zip
https://dlcdn.apache.org/tomcat/tomcat-10/v{VERSION}/bin/apache-tomcat-{VERSION}.tar.gz

# Windows distributions
https://dlcdn.apache.org/tomcat/tomcat-10/v{VERSION}/bin/apache-tomcat-{VERSION}-windows-x64.zip
https://dlcdn.apache.org/tomcat/tomcat-10/v{VERSION}/bin/apache-tomcat-{VERSION}-windows-x86.zip

# Windows Service Installer
https://dlcdn.apache.org/tomcat/tomcat-10/v{VERSION}/bin/apache-tomcat-{VERSION}.exe

# Embedded distribution
https://dlcdn.apache.org/tomcat/tomcat-10/v{VERSION}/bin/embed/apache-tomcat-{VERSION}-embed.zip
https://dlcdn.apache.org/tomcat/tomcat-10/v{VERSION}/bin/embed/apache-tomcat-{VERSION}-embed.tar.gz
```

### 1.3 URL Templates for Tomcat 11.x

```
# Base directory listing
https://dlcdn.apache.org/tomcat/tomcat-11/

# Version directory (current: v11.0.18)
https://dlcdn.apache.org/tomcat/tomcat-11/v{VERSION}/

# Binary distributions
https://dlcdn.apache.org/tomcat/tomcat-11/v{VERSION}/bin/apache-tomcat-{VERSION}.zip
https://dlcdn.apache.org/tomcat/tomcat-11/v{VERSION}/bin/apache-tomcat-{VERSION}.tar.gz

# Windows distribution (64-bit only - no 32-bit support)
https://dlcdn.apache.org/tomcat/tomcat-11/v{VERSION}/bin/apache-tomcat-{VERSION}-windows-x64.zip

# Windows Service Installer
https://dlcdn.apache.org/tomcat/tomcat-11/v{VERSION}/bin/apache-tomcat-{VERSION}.exe

# Embedded distribution
https://dlcdn.apache.org/tomcat/tomcat-11/v{VERSION}/bin/embed/apache-tomcat-{VERSION}-embed.zip
https://dlcdn.apache.org/tomcat/tomcat-11/v{VERSION}/bin/embed/apache-tomcat-{VERSION}-embed.tar.gz
```

### 1.4 Generic URL Pattern Formula

```
https://dlcdn.apache.org/tomcat/tomcat-{MAJOR}/v{VERSION}/bin/apache-tomcat-{VERSION}.{EXTENSION}

Where:
- {MAJOR} = "10" or "11" (extracted from first part of version)
- {VERSION} = Full version string (e.g., "10.1.52", "11.0.18")
- {EXTENSION} = "zip" for Windows, "tar.gz" for Unix/Linux/macOS
```

---

## 2. Distribution Archive Formats

### 2.1 Available Formats

| Format | Extension | Size (typical) | Use Case |
|--------|-----------|----------------|----------|
| Core zip | `.zip` | ~14MB | Windows, cross-platform |
| Core tar.gz | `.tar.gz` | ~14MB | Linux/Unix/macOS |
| Windows 64-bit | `-windows-x64.zip` | ~16MB | Windows with service wrapper + APR |
| Windows 32-bit | `-windows-x86.zip` | ~15MB | Legacy 32-bit Windows (10.1.x only) |
| Windows Installer | `.exe` | ~14-15MB | Windows service installation |
| Embedded zip | `-embed.zip` | ~9.6MB | Programmatic embedding |
| Embedded tar.gz | `-embed.tar.gz` | ~9.6MB | Programmatic embedding |

### 2.2 Archive Selection Logic

```
if (os == Windows) {
    if (requiresServiceWrapper || requiresAPR) {
        return "apache-tomcat-{VERSION}-windows-x64.zip"  // or x86 for 10.1.x
    } else {
        return "apache-tomcat-{VERSION}.zip"
    }
} else {
    return "apache-tomcat-{VERSION}.tar.gz"
}
```

### 2.3 Important Notes on tar.gz

From official documentation:
> NOTE: The tar files in this distribution use GNU tar extensions, and must be untarred with a GNU compatible version of tar. The version of `tar` on Solaris and Mac OS X will not work with these files.

---

## 3. Checksum Verification

### 3.1 Available Verification Files

Each distribution file has corresponding verification files:

| Verification Type | Extension | Algorithm |
|-------------------|-----------|-----------|
| SHA-512 Checksum | `.sha512` | SHA-512 |
| PGP Signature | `.asc` | PGP/GPG |

### 3.2 Checksum File URLs

```
# SHA-512 checksum
{DOWNLOAD_URL}.sha512

# PGP signature
{DOWNLOAD_URL}.asc

# Public keys for signature verification
https://downloads.apache.org/tomcat/tomcat-10/KEYS
https://downloads.apache.org/tomcat/tomcat-11/KEYS
```

### 3.3 Checksum File Format

SHA-512 checksum files contain a single line:
```
<64-character-hex-hash>  apache-tomcat-{VERSION}.zip
```

Example content of `apache-tomcat-11.0.18.zip.sha512`:
```
abc123def456...  apache-tomcat-11.0.18.zip
```

### 3.4 Verification Process

1. Download the archive file
2. Download the `.sha512` file
3. Compute SHA-512 hash of downloaded archive
4. Compare with hash in `.sha512` file
5. Optionally verify PGP signature with KEYS file

---

## 4. Tomcat Distribution Directory Structure

### 4.1 Standard Directory Layout

After extraction, a Tomcat distribution contains:

```
apache-tomcat-{VERSION}/
├── bin/                    # Startup/shutdown scripts and executables
│   ├── catalina.sh         # Main script (Unix)
│   ├── catalina.bat        # Main script (Windows)
│   ├── startup.sh / .bat   # Convenience startup
│   ├── shutdown.sh / .bat  # Convenience shutdown
│   ├── setenv.sh / .bat    # Environment configuration (user-created)
│   ├── bootstrap.jar       # Bootstrap classloader
│   ├── tomcat-juli.jar     # Logging implementation
│   └── commons-daemon-*.jar # Service wrapper support
├── conf/                   # Configuration files
│   ├── server.xml          # Main server configuration
│   ├── web.xml             # Default web application configuration
│   ├── context.xml         # Default context configuration
│   ├── tomcat-users.xml    # User/role authentication
│   ├── logging.properties  # Logging configuration
│   └── catalina.properties # Catalina system properties
├── lib/                    # Server libraries
│   ├── catalina.jar        # Core Catalina classes
│   ├── servlet-api.jar     # Servlet API
│   ├── jsp-api.jar         # JSP API
│   ├── el-api.jar          # Expression Language API
│   ├── websocket-api.jar   # WebSocket API
│   └── annotations-api.jar # Annotations API
├── logs/                   # Log file output directory
├── temp/                   # Temporary files directory
├── webapps/                # Web application deployment directory
│   ├── ROOT/               # Default root application
│   ├── manager/            # Manager application
│   ├── host-manager/       # Host manager application
│   └── docs/               # Documentation webapp
├── work/                   # Working directory for compiled JSPs
├── LICENSE                 # Apache License
├── NOTICE                  # Legal notices
├── RELEASE-NOTES           # Release notes
├── RUNNING.txt             # Running instructions
└── BUILDING.txt            # Build instructions
```

### 4.2 Key Files for Version Detection

| File/Location | Content |
|---------------|---------|
| `lib/catalina.jar!/org/apache/catalina/util/ServerInfo.properties` | Contains `server.info`, `server.number`, `server.built` |
| `RELEASE-NOTES` | Contains version in header |
| `bin/version.sh` or `bin/version.bat` | Script to output version info |

---

## 5. Version Detection Methods

### 5.1 From ServerInfo.properties

The file `lib/catalina.jar!/org/apache/catalina/util/ServerInfo.properties` contains:

```properties
server.info=Apache Tomcat/11.0.18
server.number=11.0.18.0
server.built=Jan 23 2026 11:05:15 UTC
```

### 5.2 Programmatic Detection (Java)

```java
import org.apache.catalina.util.ServerInfo;

// Method 1: Using ServerInfo class
String serverInfo = ServerInfo.getServerInfo();        // "Apache Tomcat/11.0.18"
String serverNumber = ServerInfo.getServerNumber();    // "11.0.18.0"
String serverBuilt = ServerInfo.getServerBuilt();      // "Jan 23 2026 11:05:15 UTC"

// Method 2: Parsing properties file from catalina.jar
try (JarFile jar = new JarFile(new File(catalinaHome, "lib/catalina.jar"))) {
    JarEntry entry = jar.getJarEntry("org/apache/catalina/util/ServerInfo.properties");
    Properties props = new Properties();
    props.load(jar.getInputStream(entry));
    String version = props.getProperty("server.number");  // "11.0.18.0"
}
```

### 5.3 Detection via Script

```bash
# Unix/Linux/macOS
$CATALINA_HOME/bin/version.sh

# Windows
%CATALINA_HOME%\bin\version.bat
```

Output format:
```
Server version: Apache Tomcat/11.0.18
Server built:   Jan 23 2026 11:05:15 UTC
Server number:  11.0.18.0
OS Name:        ...
OS Version:     ...
Architecture:   ...
Java Home:      ...
JVM Version:    ...
JVM Vendor:     ...
```

### 5.4 Detection via Directory Name

After extraction, the directory is named `apache-tomcat-{VERSION}`:
```java
String dirname = extractedDir.getName();  // "apache-tomcat-11.0.18"
String version = dirname.replaceFirst("apache-tomcat-", "");  // "11.0.18"
```

---

## 6. Maven Artifact Coordinates

### 6.1 Binary Distribution Artifact

```xml
<!-- Full Tomcat binary distribution (POM packaging) -->
<dependency>
    <groupId>org.apache.tomcat</groupId>
    <artifactId>tomcat</artifactId>
    <version>11.0.18</version>
    <type>pom</type>
</dependency>
```

**Note**: This is a POM artifact, not a distributable archive. It defines dependencies on individual Tomcat modules.

### 6.2 Embedded Tomcat Artifacts

```xml
<!-- Core embedded Tomcat -->
<dependency>
    <groupId>org.apache.tomcat.embed</groupId>
    <artifactId>tomcat-embed-core</artifactId>
    <version>11.0.18</version>
</dependency>

<!-- JSP support for embedded -->
<dependency>
    <groupId>org.apache.tomcat.embed</groupId>
    <artifactId>tomcat-embed-jasper</artifactId>
    <version>11.0.18</version>
</dependency>

<!-- WebSocket support for embedded -->
<dependency>
    <groupId>org.apache.tomcat.embed</groupId>
    <artifactId>tomcat-embed-websocket</artifactId>
    <version>11.0.18</version>
</dependency>

<!-- EL support for embedded -->
<dependency>
    <groupId>org.apache.tomcat.embed</groupId>
    <artifactId>tomcat-embed-el</artifactId>
    <version>11.0.18</version>
</dependency>
```

### 6.3 Individual Tomcat Module Artifacts

```xml
<!-- Core Catalina -->
<dependency>
    <groupId>org.apache.tomcat</groupId>
    <artifactId>tomcat-catalina</artifactId>
    <version>11.0.18</version>
</dependency>

<!-- Servlet API -->
<dependency>
    <groupId>org.apache.tomcat</groupId>
    <artifactId>tomcat-servlet-api</artifactId>
    <version>11.0.18</version>
</dependency>

<!-- Annotations API -->
<dependency>
    <groupId>org.apache.tomcat</groupId>
    <artifactId>tomcat-annotations-api</artifactId>
    <version>11.0.18</version>
</dependency>
```

### 6.4 Important Note on Maven Artifacts

The Maven Central artifacts for Tomcat are **JAR libraries**, not the full distribution archive. For a full Tomcat installation with:
- Startup scripts
- Configuration files  
- Manager applications
- Documentation

You **must download from Apache mirrors** using the URL patterns documented in Section 1.

---

## 7. Archive Extraction Approaches

### 7.1 Java ZIP Extraction

```java
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;

public void extractZip(File zipFile, File destDir) throws IOException {
    try (ZipFile zip = new ZipFile(zipFile)) {
        Enumeration<? extends ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            File entryFile = new File(destDir, entry.getName());
            
            // Security: Validate path to prevent zip slip vulnerability
            if (!entryFile.getCanonicalPath().startsWith(destDir.getCanonicalPath())) {
                throw new IOException("Zip entry outside target dir: " + entry.getName());
            }
            
            if (entry.isDirectory()) {
                entryFile.mkdirs();
            } else {
                entryFile.getParentFile().mkdirs();
                try (InputStream is = zip.getInputStream(entry);
                     OutputStream os = new FileOutputStream(entryFile)) {
                    is.transferTo(os);
                }
            }
        }
    }
}
```

### 7.2 Java tar.gz Extraction

```java
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

public void extractTarGz(File tarGzFile, File destDir) throws IOException {
    try (FileInputStream fis = new FileInputStream(tarGzFile);
         GzipCompressorInputStream gzis = new GzipCompressorInputStream(fis);
         TarArchiveInputStream tais = new TarArchiveInputStream(gzis)) {
        
        TarArchiveEntry entry;
        while ((entry = tais.getNextTarEntry()) != null) {
            File entryFile = new File(destDir, entry.getName());
            
            // Security: Validate path
            if (!entryFile.getCanonicalPath().startsWith(destDir.getCanonicalPath())) {
                throw new IOException("Tar entry outside target dir: " + entry.getName());
            }
            
            if (entry.isDirectory()) {
                entryFile.mkdirs();
            } else {
                entryFile.getParentFile().mkdirs();
                try (OutputStream os = new FileOutputStream(entryFile)) {
                    IOUtils.copy(tais, os);
                }
                // Preserve executable permissions on Unix
                if (entry.getMode() != 0) {
                    entryFile.setExecutable((entry.getMode() & 0100) != 0);
                }
            }
        }
    }
}
```

### 7.3 Maven Dependency Plugin Approach

For Maven plugins, use `maven-dependency-plugin` to download and unpack:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-dependency-plugin</artifactId>
    <version>3.9.0</version>
    <executions>
        <execution>
            <id>unpack-tomcat</id>
            <phase>prepare-package</phase>
            <goals>
                <goal>unpack</goal>
            </goals>
            <configuration>
                <artifactItems>
                    <artifactItem>
                        <groupId>org.apache.tomcat</groupId>
                        <artifactId>tomcat</artifactId>
                        <version>11.0.18</version>
                        <type>zip</type>
                        <outputDirectory>${project.build.directory}/tomcat</outputDirectory>
                    </artifactItem>
                </artifactItems>
            </configuration>
        </execution>
    </executions>
</plugin>
```

**Important**: The `org.apache.tomcat:tomcat` artifact in Maven Central is a POM, not a ZIP. For downloading the actual distribution, use direct URL download.

### 7.4 Maven Artifact Resolver API

For programmatic download within a Maven plugin:

```java
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResult;

@Component
private RepositorySystem repoSystem;

@Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
private RepositorySystemSession repoSession;

public File resolveArtifact(String groupId, String artifactId, String version, String type) 
        throws ArtifactResolutionException {
    Artifact artifact = new DefaultArtifact(groupId, artifactId, type, version);
    ArtifactRequest request = new ArtifactRequest(artifact, remoteRepositories, null);
    ArtifactResult result = repoSystem.resolveArtifact(repoSession, request);
    return result.getArtifact().getFile();
}
```

---

## 8. Caching Downloaded Distributions

### 8.1 Recommended Cache Location

```
~/.m2/repository/org/apache/tomcat/distribution/
    └── {version}/
        ├── apache-tomcat-{version}.zip
        ├── apache-tomcat-{version}.zip.sha512
        └── apache-tomcat-{version}.zip.lastModified
```

Or in a dedicated plugin cache:

```
~/.m2/tomcat-cache/
    └── {version}/
        └── apache-tomcat-{version}/   (extracted)
```

### 8.2 Cache Key Strategy

```java
public String getCacheKey(String version, String archiveType) {
    return String.format("tomcat-%s-%s", version, archiveType);
    // e.g., "tomcat-11.0.18-zip"
}
```

### 8.3 Cache Validation

```java
public boolean isCacheValid(File cachedFile, String expectedSha512) {
    if (!cachedFile.exists()) {
        return false;
    }
    
    // Verify checksum
    String actualSha512 = computeSha512(cachedFile);
    return expectedSha512.equals(actualSha512);
}

public String computeSha512(File file) throws IOException, NoSuchAlgorithmException {
    MessageDigest digest = MessageDigest.getInstance("SHA-512");
    try (InputStream is = new FileInputStream(file);
         DigestInputStream dis = new DigestInputStream(is, digest)) {
        byte[] buffer = new byte[8192];
        while (dis.read(buffer) != -1) {
            // Reading updates digest
        }
    }
    byte[] hash = digest.digest();
    return HexFormat.of().formatHex(hash);
}
```

### 8.4 Cache Cleanup Strategy

Options:
1. **LRU (Least Recently Used)**: Track access time, remove oldest when cache exceeds size limit
2. **Version-based**: Keep only last N versions of each major release
3. **Time-based**: Remove distributions not accessed in X days
4. **Size-based**: Limit total cache size (e.g., 500MB)

---

## 9. Download Implementation Pattern

### 9.1 Complete Download Flow

```java
public class TomcatDownloader {
    
    private static final String MIRROR_URL = "https://dlcdn.apache.org/tomcat";
    private static final String ARCHIVE_URL = "https://archive.apache.org/dist/tomcat";
    
    public File downloadAndExtract(String version, File cacheDir, File destDir) 
            throws IOException {
        
        // 1. Parse version to get major version
        int majorVersion = getMajorVersion(version);  // "11.0.18" -> 11
        
        // 2. Determine archive format
        String archiveExt = isWindows() ? ".zip" : ".tar.gz";
        String archiveName = "apache-tomcat-" + version + archiveExt;
        
        // 3. Build download URL
        String downloadUrl = buildDownloadUrl(majorVersion, version, archiveName);
        
        // 4. Check cache
        File cachedArchive = new File(cacheDir, archiveName);
        if (!cachedArchive.exists()) {
            // 5. Download archive
            downloadFile(downloadUrl, cachedArchive);
            
            // 6. Download and verify checksum
            String checksumUrl = downloadUrl + ".sha512";
            String expectedChecksum = downloadChecksum(checksumUrl);
            String actualChecksum = computeSha512(cachedArchive);
            if (!actualChecksum.equalsIgnoreCase(expectedChecksum)) {
                cachedArchive.delete();
                throw new IOException("Checksum verification failed");
            }
        }
        
        // 7. Extract archive
        File extractedDir = new File(destDir, "apache-tomcat-" + version);
        if (!extractedDir.exists()) {
            if (archiveExt.equals(".zip")) {
                extractZip(cachedArchive, destDir);
            } else {
                extractTarGz(cachedArchive, destDir);
            }
        }
        
        return extractedDir;
    }
    
    private String buildDownloadUrl(int major, String version, String archiveName) {
        // Try primary mirror first
        return String.format("%s/tomcat-%d/v%s/bin/%s", 
            MIRROR_URL, major, version, archiveName);
    }
    
    private int getMajorVersion(String version) {
        return Integer.parseInt(version.split("\\.")[0]);
    }
}
```

### 9.2 Fallback URL Strategy

```java
public File downloadWithFallback(String version, String archiveName) throws IOException {
    List<String> urls = Arrays.asList(
        // Primary CDN
        String.format("https://dlcdn.apache.org/tomcat/tomcat-%d/v%s/bin/%s",
            getMajorVersion(version), version, archiveName),
        // Archive mirror (for older versions)
        String.format("https://archive.apache.org/dist/tomcat/tomcat-%d/v%s/bin/%s",
            getMajorVersion(version), version, archiveName),
        // Alternative downloads mirror
        String.format("https://downloads.apache.org/tomcat/tomcat-%d/v%s/bin/%s",
            getMajorVersion(version), version, archiveName)
    );
    
    IOException lastException = null;
    for (String url : urls) {
        try {
            return downloadFile(url);
        } catch (IOException e) {
            lastException = e;
            // Log and try next mirror
        }
    }
    throw new IOException("All download mirrors failed", lastException);
}
```

---

## 10. Version Discovery

### 10.1 Parsing Directory Listing

To discover available versions, parse the Apache directory listing:

```java
public List<String> getAvailableVersions(int majorVersion) throws IOException {
    String url = String.format("https://dlcdn.apache.org/tomcat/tomcat-%d/", majorVersion);
    String html = fetchUrl(url);
    
    // Parse HTML for version directories (format: v{VERSION}/)
    Pattern pattern = Pattern.compile("href=\"v([\\d.]+)/\"");
    Matcher matcher = pattern.matcher(html);
    
    List<String> versions = new ArrayList<>();
    while (matcher.find()) {
        versions.add(matcher.group(1));
    }
    
    // Sort versions (semantic versioning)
    versions.sort(Comparator.comparing(Version::parse).reversed());
    return versions;
}
```

### 10.2 Latest Version Detection

```java
public String getLatestVersion(int majorVersion) throws IOException {
    List<String> versions = getAvailableVersions(majorVersion);
    if (versions.isEmpty()) {
        throw new IOException("No versions found for Tomcat " + majorVersion);
    }
    return versions.get(0);  // Already sorted by version, newest first
}
```

---

## 11. Environment Variables

### 11.1 Required Environment Variables

| Variable | Description | Usage |
|----------|-------------|-------|
| `CATALINA_HOME` | Points to Tomcat installation directory | Required |
| `CATALINA_BASE` | Points to active configuration (defaults to CATALINA_HOME) | Optional |
| `JRE_HOME` or `JAVA_HOME` | Points to Java installation | Required |

### 11.2 Optional Environment Variables

| Variable | Description |
|----------|-------------|
| `CATALINA_OPTS` | JVM options for Tomcat process |
| `JAVA_OPTS` | JVM options for all commands (start/stop) |
| `CATALINA_PID` | File path to store Tomcat PID (*nix only) |
| `CATALINA_TMPDIR` | Temp directory for Tomcat |
| `JPDA_TRANSPORT` | Debug transport (default: `dt_socket`) |
| `JPDA_ADDRESS` | Debug address (default: `localhost:8000`) |

### 11.3 Setting Environment Programmatically

```java
public ProcessBuilder createTomcatProcess(File catalinaHome, File javaHome) {
    ProcessBuilder pb = new ProcessBuilder();
    Map<String, String> env = pb.environment();
    
    env.put("CATALINA_HOME", catalinaHome.getAbsolutePath());
    env.put("CATALINA_BASE", catalinaHome.getAbsolutePath());
    env.put("JAVA_HOME", javaHome.getAbsolutePath());
    
    // Optional: Add custom options
    env.put("CATALINA_OPTS", "-Xmx1024m -Dfile.encoding=UTF-8");
    
    String script = isWindows() ? "catalina.bat" : "catalina.sh";
    pb.command(new File(catalinaHome, "bin/" + script).getAbsolutePath(), "run");
    
    return pb;
}
```

---

## 12. Java Version Requirements

| Tomcat Version | Minimum Java | Notes |
|----------------|--------------|-------|
| 10.1.x | Java 11 | Jakarta EE 10 |
| 11.x | Java 17 | Jakarta EE 11, SecurityManager removed |

### 12.1 Pre-flight Java Version Check

```java
public void validateJavaVersion(String tomcatVersion, String javaVersion) 
        throws IncompatibleVersionException {
    int tomcatMajor = getMajorVersion(tomcatVersion);
    int javaMajor = getJavaMajorVersion(javaVersion);
    
    int requiredJava = switch (tomcatMajor) {
        case 10 -> 11;
        case 11 -> 17;
        default -> throw new UnsupportedOperationException(
            "Unknown Tomcat major version: " + tomcatMajor);
    };
    
    if (javaMajor < requiredJava) {
        throw new IncompatibleVersionException(
            String.format("Tomcat %s requires Java %d or later, but Java %s was found",
                tomcatVersion, requiredJava, javaVersion));
    }
}
```

---

## 13. Summary Tables

### 13.1 Current Version Summary (as of Feb 2026)

| Tomcat Line | Current Version | Java Requirement | Jakarta EE |
|-------------|-----------------|------------------|------------|
| 10.1.x | 10.1.52 | Java 11+ | Jakarta EE 10 |
| 11.x | 11.0.18 | Java 17+ | Jakarta EE 11 |

### 13.2 Archive Selection Matrix

| OS | Architecture | Tomcat 10.1.x | Tomcat 11.x |
|----|--------------|---------------|-------------|
| Windows | 64-bit | `.zip` or `-windows-x64.zip` | `.zip` or `-windows-x64.zip` |
| Windows | 32-bit | `.zip` or `-windows-x86.zip` | Not supported |
| Linux/Unix | Any | `.tar.gz` | `.tar.gz` |
| macOS | Any | `.tar.gz` (with GNU tar) | `.tar.gz` (with GNU tar) |

### 13.3 Important Files for Validation

| Purpose | File Location |
|---------|---------------|
| Version detection | `lib/catalina.jar!/org/apache/catalina/util/ServerInfo.properties` |
| Existence validation | `bin/catalina.sh` or `bin/catalina.bat` |
| Configuration check | `conf/server.xml` |
| Public keys | `https://downloads.apache.org/tomcat/tomcat-{MAJOR}/KEYS` |

