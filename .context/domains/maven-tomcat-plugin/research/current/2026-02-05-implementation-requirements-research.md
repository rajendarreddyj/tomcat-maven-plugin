---
title: Maven Tomcat Plugin Implementation Requirements Research
date: 2026-02-05
author: AI Research Assistant
sources:
  - https://github.com/apache/tomcat-maven-plugin
  - https://tomcat.apache.org/migration-10.1.html
  - https://tomcat.apache.org/migration-11.0.html
  - https://maven.apache.org/guides/plugin/guide-java-plugin-development.html
tags: [research, maven-plugin, tomcat, implementation]
status: complete
---

# Maven Tomcat Plugin Implementation Requirements Research

## Research Question

How to implement a Maven plugin (`io.github.rajendarreddyj:tomcat-maven-plugin`) that supports Apache Tomcat 10.1.x and 11.x with hot deployment, auto-download, and extensive configuration options?

## Summary

This research consolidates findings from the legacy Apache Tomcat Maven Plugin, Tomcat migration guides, and Maven plugin development best practices to guide the implementation of a modern Maven plugin supporting Tomcat 10.1.x and 11.x.

## Requirements Analysis

Based on [requirements.md](../../../requirements.md):

| Requirement | Priority | Implementation Approach |
|-------------|----------|------------------------|
| Support Tomcat 10.1.x and 11.x | High | Version-aware configuration, embedded or standalone |
| Hot code deployment | High | Exploded WAR deployment with file watching |
| Environment variables | High | Map parameter passed to Tomcat process |
| VM options | High | List parameter for JVM arguments |
| JDK home configuration | High | Path parameter with validation |
| Catalina home support | High | Use existing installation |
| Auto-download Tomcat | Medium | Download from Apache mirrors if not present |
| Version specification | Medium | Support specific version strings |
| Context path configuration | High | Parameter with default from artifactId |
| Port configuration | High | HTTP port parameter |
| Auto-publish | Medium | File system watching with inactivity limit |
| Classpath additions | Low | Additional JARs for Tomcat classpath |

## Detailed Findings

### 1. Legacy Plugin Architecture Patterns

The Apache Tomcat Maven Plugin uses these key patterns:

**Class Hierarchy:**
```
AbstractMojo
  └── AbstractTomcatMojo (common config: path, port, config file paths)
        ├── AbstractCatalinaMojo (remote manager operations)
        │     └── Deploy/Undeploy/Redeploy Mojos
        └── AbstractRunMojo (embedded Tomcat execution)
              └── Run/RunWar Mojos
```

**Goals Structure:**
| Goal | Purpose |
|------|---------|
| `run` | Run webapp from exploded directory |
| `run-war` | Run packaged WAR |
| `deploy` | Deploy WAR to remote Tomcat Manager |
| `undeploy` | Remove webapp from Tomcat |
| `shutdown` | Stop embedded Tomcat instance |

**Configuration Parameters (Legacy):**
```xml
<maven.tomcat.path>/${project.artifactId}</maven.tomcat.path>
<maven.tomcat.port>8080</maven.tomcat.port>
<maven.tomcat.url>http://localhost:8080/manager/text</maven.tomcat.url>
<maven.tomcat.contextReloadable>false</maven.tomcat.contextReloadable>
<maven.tomcat.backgroundProcessorDelay>-1</maven.tomcat.backgroundProcessorDelay>
<maven.tomcat.fork>false</maven.tomcat.fork>
```

**Hot Deployment Implementation:**
- Uses `contextReloadable=true` for automatic class reloading
- `backgroundProcessorDelay` controls reload check interval
- `ExternalRepositoriesReloadableWebappLoader` for external classpath monitoring

### 2. Tomcat Version Requirements

**Tomcat 10.1.x:**
- **Minimum Java:** 11
- **Jakarta EE:** 10
- **Specifications:** Servlet 6.0, JSP 3.1, EL 5.0, WebSocket 2.1
- **Current Version:** 10.1.52
- **Download URL Pattern:**
  ```
  https://dlcdn.apache.org/tomcat/tomcat-10/v{VERSION}/bin/apache-tomcat-{VERSION}.zip
  https://dlcdn.apache.org/tomcat/tomcat-10/v{VERSION}/bin/apache-tomcat-{VERSION}.tar.gz
  ```

**Tomcat 11.x:**
- **Minimum Java:** 17
- **Jakarta EE:** 11
- **Specifications:** Servlet 6.1, JSP 4.0, EL 6.0, WebSocket 2.2
- **Current Version:** 11.0.18
- **64-bit only** (no Windows 32-bit)
- **Download URL Pattern:**
  ```
  https://dlcdn.apache.org/tomcat/tomcat-11/v{VERSION}/bin/apache-tomcat-{VERSION}.zip
  https://dlcdn.apache.org/tomcat/tomcat-11/v{VERSION}/bin/apache-tomcat-{VERSION}.tar.gz
  ```

**Archive Fallback:**
```
https://archive.apache.org/dist/tomcat/tomcat-{MAJOR}/v{VERSION}/bin/apache-tomcat-{VERSION}.{ext}
```

### 3. Maven Plugin Development Patterns

**Mojo Annotations:**
```java
@Mojo(
    name = "run",
    defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST,
    requiresDependencyResolution = ResolutionScope.RUNTIME,
    threadSafe = true
)
public class RunMojo extends AbstractTomcatMojo {

    @Parameter(property = "tomcat.http.port", defaultValue = "8080")
    private int httpPort;

    @Parameter(property = "tomcat.catalina.home", required = false)
    private File catalinaHome;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession session;
}
```

**JSR-330 Dependency Injection (Preferred):**
```java
@Named
@Singleton
public class TomcatDownloader {
    private final RepositorySystem repoSystem;

    @Inject
    public TomcatDownloader(RepositorySystem repoSystem) {
        this.repoSystem = repoSystem;
    }
}
```

**Error Handling:**
- `MojoExecutionException` - Unexpected errors (BUILD ERROR)
- `MojoFailureException` - Expected failures (BUILD FAILURE)

**Common Pitfalls to Avoid:**
1. Relative path resolution - always use `File` parameter type
2. File encoding - use explicit charset, not platform default
3. URL to File conversion - use `URI` or utility classes
4. Shutdown hooks - use try-finally instead

### 4. Auto-Download Implementation

**Version Detection from Installed Tomcat:**
```java
// Read from lib/catalina.jar!/org/apache/catalina/util/ServerInfo.properties
// Contains: server.number=11.0.18.0
```

**Checksum Verification:**
```
SHA-512: {DOWNLOAD_URL}.sha512
PGP Key: https://downloads.apache.org/tomcat/tomcat-{MAJOR}/KEYS
```

**Caching Strategy:**
- Cache location: `~/.m2/tomcat-cache/{version}/` or `${localRepository}/tomcat/`
- Validate with SHA-512 before reuse
- Support `offline` mode (skip download if cached)

**Extraction with Zip Slip Protection:**
```java
// Validate entry path doesn't escape target directory
Path targetPath = targetDir.resolve(entry.getName()).normalize();
if (!targetPath.startsWith(targetDir)) {
    throw new IOException("Zip slip attack detected: " + entry.getName());
}
```

## Proposed Architecture

### Project Structure

```
tomcat-maven-plugin/
├── pom.xml
├── requirements.md
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── io/github/rajendarreddyj/tomcat/
│   │   │       ├── AbstractTomcatMojo.java          # Base class with common config
│   │   │       ├── RunMojo.java                     # tomcat:run goal
│   │   │       ├── StartMojo.java                   # tomcat:start goal (background)
│   │   │       ├── StopMojo.java                    # tomcat:stop goal
│   │   │       ├── DeployMojo.java                  # tomcat:deploy goal
│   │   │       ├── config/
│   │   │       │   ├── ServerConfiguration.java     # Server settings DTO
│   │   │       │   ├── DeployableConfiguration.java # Deployment settings DTO
│   │   │       │   └── TomcatVersion.java           # Version enum/class
│   │   │       ├── download/
│   │   │       │   ├── TomcatDownloader.java        # Download and extract
│   │   │       │   ├── TomcatVersionResolver.java   # Resolve latest version
│   │   │       │   └── ChecksumValidator.java       # SHA-512 validation
│   │   │       ├── lifecycle/
│   │   │       │   ├── TomcatLauncher.java          # Start/stop Tomcat process
│   │   │       │   ├── TomcatProcessMonitor.java    # Monitor Tomcat health
│   │   │       │   └── ShutdownHandler.java         # Graceful shutdown
│   │   │       └── deploy/
│   │   │           ├── ExplodedWarDeployer.java     # Deploy exploded WAR
│   │   │           ├── HotDeployWatcher.java        # File system watching
│   │   │           └── ContextReloader.java         # Trigger context reload
│   │   └── resources/
│   │       └── META-INF/
│   │           └── sisu/
│   │               └── javax.inject.Named           # DI index (generated)
│   └── test/
│       └── java/
│           └── io/github/rajendarreddyj/tomcat/
│               ├── AbstractTomcatMojoTest.java
│               ├── RunMojoTest.java
│               └── download/
│                   └── TomcatDownloaderTest.java
```

### Configuration Parameters

```xml
<plugin>
    <groupId>io.github.rajendarreddyj</groupId>
    <artifactId>tomcat-maven-plugin</artifactId>
    <version>1.0.0</version>
    <configuration>
        <!-- Tomcat Version & Location -->
        <tomcatVersion>10.1.52</tomcatVersion>           <!-- or "11.0.18" -->
        <catalinaHome>/path/to/tomcat</catalinaHome>     <!-- optional, auto-download if missing -->
        <catalinaBase>${project.build.directory}/tomcat</catalinaBase>

        <!-- JVM Configuration -->
        <javaHome>${java.home}</javaHome>
        <vmOptions>
            <vmOption>-Xmx1024m</vmOption>
            <vmOption>-Xms512m</vmOption>
        </vmOptions>
        <environmentVariables>
            <JAVA_OPTS>-Dfile.encoding=UTF-8</JAVA_OPTS>
            <CATALINA_OPTS>-Djava.security.egd=file:/dev/./urandom</CATALINA_OPTS>
        </environmentVariables>

        <!-- Server Configuration -->
        <httpPort>8080</httpPort>
        <httpHost>localhost</httpHost>
        <startupTimeout>1800000</startupTimeout>
        <shutdownTimeout>1800000</shutdownTimeout>

        <!-- Deployment Configuration -->
        <contextPath>/</contextPath>                      <!-- ROOT context -->
        <deployDir>${catalinaBase}/webapps</deployDir>
        <warSourceDirectory>${project.build.directory}/${project.build.finalName}</warSourceDirectory>
        <deploymentOutputName>ROOT</deploymentOutputName>

        <!-- Auto-publish (Hot Deploy) -->
        <autopublishEnabled>true</autopublishEnabled>
        <autopublishInactivityLimit>30</autopublishInactivityLimit>

        <!-- Classpath -->
        <classpathAdditions>
            <classpathAddition>/path/to/extra.jar</classpathAddition>
        </classpathAdditions>
    </configuration>
</plugin>
```

### Key Implementation Classes

**AbstractTomcatMojo.java:**
```java
public abstract class AbstractTomcatMojo extends AbstractMojo {
    @Parameter(property = "tomcat.version", defaultValue = "10.1.52")
    protected String tomcatVersion;

    @Parameter(property = "tomcat.catalina.home")
    protected File catalinaHome;

    @Parameter(property = "tomcat.http.port", defaultValue = "8080")
    protected int httpPort;

    @Parameter(property = "tomcat.java.home", defaultValue = "${java.home}")
    protected File javaHome;

    @Parameter
    protected List<String> vmOptions;

    @Parameter
    protected Map<String, String> environmentVariables;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    protected ServerConfiguration buildServerConfiguration() {
        return ServerConfiguration.builder()
            .catalinaHome(resolveCatalinaHome())
            .httpPort(httpPort)
            .javaHome(javaHome.toPath())
            .vmOptions(vmOptions)
            .environmentVariables(environmentVariables)
            .build();
    }

    protected Path resolveCatalinaHome() throws MojoExecutionException {
        if (catalinaHome != null && catalinaHome.exists()) {
            return catalinaHome.toPath();
        }
        // Auto-download Tomcat
        return downloadTomcat(tomcatVersion);
    }
}
```

**TomcatDownloader.java:**
```java
@Named
@Singleton
public class TomcatDownloader {
    private static final String DOWNLOAD_BASE = "https://dlcdn.apache.org/tomcat/tomcat-";
    private static final String ARCHIVE_BASE = "https://archive.apache.org/dist/tomcat/tomcat-";

    public Path download(String version, Path cacheDir) throws IOException {
        int majorVersion = extractMajorVersion(version);
        String fileName = "apache-tomcat-" + version + ".zip";
        Path cachedPath = cacheDir.resolve(version).resolve(fileName);

        if (Files.exists(cachedPath) && verifyChecksum(cachedPath, version)) {
            return extractIfNeeded(cachedPath);
        }

        String url = buildDownloadUrl(majorVersion, version);
        downloadWithRetry(url, cachedPath);
        verifyChecksum(cachedPath, version);
        return extract(cachedPath);
    }

    private String buildDownloadUrl(int major, String version) {
        return DOWNLOAD_BASE + major + "/v" + version + "/bin/apache-tomcat-" + version + ".zip";
    }
}
```

**TomcatLauncher.java:**
```java
public class TomcatLauncher {
    public Process start(ServerConfiguration config) throws IOException {
        ProcessBuilder pb = new ProcessBuilder();

        // Build command
        Path catalinaBat = config.getCatalinaHome().resolve("bin/catalina.bat");
        Path catalinaSh = config.getCatalinaHome().resolve("bin/catalina.sh");
        Path startup = Files.exists(catalinaBat) ? catalinaBat : catalinaSh;

        List<String> command = new ArrayList<>();
        command.add(startup.toString());
        command.add("run");

        // Environment
        Map<String, String> env = pb.environment();
        env.put("JAVA_HOME", config.getJavaHome().toString());
        env.put("CATALINA_HOME", config.getCatalinaHome().toString());
        env.put("CATALINA_BASE", config.getCatalinaBase().toString());

        if (config.getVmOptions() != null) {
            env.put("CATALINA_OPTS", String.join(" ", config.getVmOptions()));
        }

        config.getEnvironmentVariables().forEach(env::put);

        pb.command(command);
        pb.inheritIO();

        return pb.start();
    }
}
```

### Goals Summary

| Goal | Purpose | Phase |
|------|---------|-------|
| `tomcat:run` | Run Tomcat in foreground with webapp deployed | `pre-integration-test` |
| `tomcat:start` | Start Tomcat in background | `pre-integration-test` |
| `tomcat:stop` | Stop running Tomcat | `post-integration-test` |
| `tomcat:deploy` | Deploy/redeploy webapp to running Tomcat | - |
| `tomcat:download` | Download Tomcat distribution only | - |

## Code References

| Component | Reference |
|-----------|-----------|
| Legacy AbstractMojo | `apache/tomcat-maven-plugin/common-tomcat-maven-plugin/src/main/java/org/apache/tomcat/maven/common/run/` |
| Embedded Registry | `apache/tomcat-maven-plugin/.../EmbeddedRegistry.java` |
| TomcatManager | `apache/tomcat-maven-plugin/.../TomcatManager.java` |
| Maven Plugin API | `org.apache.maven:maven-plugin-api:3.9.x` |
| Mojo Annotations | `org.apache.maven.plugin-tools:maven-plugin-annotations:3.11.x` |

## Implementation Phases

### Phase 1: Core Infrastructure
1. Create Maven plugin project structure
2. Implement `AbstractTomcatMojo` with common parameters
3. Implement `ServerConfiguration` and `DeployableConfiguration`
4. Add `TomcatDownloader` with caching and checksum validation

### Phase 2: Basic Goals
1. Implement `RunMojo` for foreground execution
2. Implement `StartMojo` and `StopMojo` for background execution
3. Implement `TomcatLauncher` for process management

### Phase 3: Deployment
1. Implement `DeployMojo` for exploded WAR deployment
2. Add context.xml generation
3. Support `deploymentOutputName` for ROOT deployment

### Phase 4: Hot Deployment
1. Implement `HotDeployWatcher` using `WatchService`
2. Add auto-publish with inactivity detection
3. Implement incremental deployment (changed files only)

### Phase 5: Testing & Documentation
1. Unit tests with Mockito
2. Integration tests with `maven-invoker-plugin`
3. Generate plugin documentation with `maven-plugin-plugin`

## Open Questions

1. **Process vs Embedded:** Should the plugin run Tomcat as an external process (like requirements suggest) or use embedded Tomcat (like legacy plugin)?
   - **Recommendation:** External process better matches the requirements (support existing catalinaHome)

2. **Windows Service Support:** Should the plugin create Windows service configurations?
   - **Recommendation:** Out of scope for initial version

3. **Multiple Webapps:** Should the plugin support deploying multiple webapps to single Tomcat?
   - **Recommendation:** Single webapp focus for simplicity, can extend later

4. **HTTPS Support:** Should the plugin configure HTTPS connectors?
   - **Recommendation:** Configuration property for server.xml customization

## Related Research Documents

- [2026-02-05-apache-tomcat-maven-plugin-legacy-architecture.md](.context/domains/tomcat/research/current/2026-02-05-apache-tomcat-maven-plugin-legacy-architecture.md)
- [2026-02-05-tomcat-10-11-migration-requirements.md](.context/domains/tomcat/research/current/2026-02-05-tomcat-10-11-migration-requirements.md)
- [2026-02-05-maven-plugin-development-patterns.md](.context/domains/maven-plugin/research/current/2026-02-05-maven-plugin-development-patterns.md)
- [2026-02-05-tomcat-auto-download-implementation.md](.context/domains/tomcat/research/current/2026-02-05-tomcat-auto-download-implementation.md)
