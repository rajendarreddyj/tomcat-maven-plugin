# Tomcat Maven Plugin

[![License: Apache-2.0](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.rajendarreddyj/tomcat-maven-plugin.svg)](https://central.sonatype.com/artifact/io.github.rajendarreddyj/tomcat-maven-plugin)
[![Java](https://img.shields.io/badge/Java-21%2B-brightgreen.svg)](https://www.oracle.com/java/technologies/downloads/)

A Maven plugin for deploying and managing web applications on Apache Tomcat 10.1.x and 11.x.

## Features

- **Hot code deployment** via Maven
- **Auto-download Tomcat** if not available locally
- **Environment variable configuration**
- **JVM options support**
- **JDK home configuration**
- **Catalina home support** for existing Tomcat installations
- **Context path configuration**
- **Port configuration**
- **Auto-publish** with inactivity detection
- **Classpath additions support**

## Requirements

- **Java:** JDK 21 or later
- **Maven:** 3.9.6 or later
- **Tomcat:** 10.1.x (Jakarta EE 10) or 11.x (Jakarta EE 11)

## Installation

Add the plugin to your project's `pom.xml`:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>io.github.rajendarreddyj</groupId>
            <artifactId>tomcat-maven-plugin</artifactId>
            <version>1.0.0-SNAPSHOT</version>
            <configuration>
                <tomcatVersion>10.1.52</tomcatVersion>
                <httpPort>8080</httpPort>
                <contextPath>/myapp</contextPath>
            </configuration>
        </plugin>
    </plugins>
</build>
```

## Goals

| Goal | Description |
|------|-------------|
| `tomcat:run` | Runs Tomcat in the foreground with your webapp deployed |
| `tomcat:debug` | Runs Tomcat in debug mode with JDWP agent enabled |
| `tomcat:start` | Starts Tomcat in the background |
| `tomcat:stop` | Stops a running Tomcat instance |
| `tomcat:deploy` | Deploys/redeploys the webapp to a running Tomcat |
| `tomcat:help` | Displays help information on the plugin goals |

## Usage

### Run in Foreground

Start Tomcat and deploy your webapp in the foreground (Ctrl+C to stop):

```bash
mvn tomcat:run
```

### Background Mode

Start Tomcat in the background:

```bash
mvn tomcat:start
```

Stop the background Tomcat:

```bash
mvn tomcat:stop
```

### Hot Deployment

Redeploy changes to a running Tomcat:

```bash
mvn tomcat:deploy
```

## Configuration

### Full Configuration Example

```xml
<plugin>
    <groupId>io.github.rajendarreddyj</groupId>
    <artifactId>tomcat-maven-plugin</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <configuration>
        <!-- Tomcat Version & Location -->
        <tomcatVersion>10.1.52</tomcatVersion>
        <catalinaHome>/path/to/tomcat</catalinaHome>
        <!-- catalinaBase: Optional. If omitted and httpPort!=8080,
             auto-generates at {tomcatCacheDir}/base-{version}-{port} -->
        <catalinaBase>/path/to/instance</catalinaBase>
        <!-- tomcatCacheDir: Used for Tomcat downloads AND generated CATALINA_BASE -->
        <tomcatCacheDir>${user.home}/.m2/tomcat-cache</tomcatCacheDir>

        <!-- Server Configuration -->
        <httpPort>8080</httpPort>
        <httpHost>localhost</httpHost>

        <!-- JVM Configuration -->
        <javaHome>${java.home}</javaHome>
        <vmOptions>
            <vmOption>-Xmx1024m</vmOption>
            <vmOption>-Xms256m</vmOption>
        </vmOptions>
        <environmentVariables>
            <JAVA_OPTS>-Dfile.encoding=UTF-8</JAVA_OPTS>
        </environmentVariables>

        <!-- Deployment Configuration -->
        <contextPath>/myapp</contextPath>
        <warSourceDirectory>${project.build.directory}/${project.build.finalName}</warSourceDirectory>
        <deploymentOutputName>myapp</deploymentOutputName>

        <!-- Auto-publish -->
        <autopublishEnabled>true</autopublishEnabled>
        <autopublishInactivityLimit>30</autopublishInactivityLimit>

        <!-- Timeouts -->
        <startupTimeout>120000</startupTimeout>
        <shutdownTimeout>30000</shutdownTimeout>

        <!-- Classpath -->
        <classpathAdditions>
            <classpathAddition>/path/to/extra.jar</classpathAddition>
        </classpathAdditions>

        <!-- Skip execution -->
        <skip>false</skip>
    </configuration>
</plugin>
```

### Configuration Parameters

| Parameter | Property | Default | Description |
|-----------|----------|---------|-------------|
| `tomcatVersion` | `tomcat.version` | `10.1.52` | Tomcat version to use |
| `catalinaHome` | `tomcat.catalina.home` | Auto-download | Tomcat installation directory |
| `catalinaBase` | `tomcat.catalina.base` | Auto-generated | Tomcat instance directory (CATALINA_BASE). If not specified and `httpPort` differs from 8080, a custom base is generated at `{tomcatCacheDir}/base-{version}-{port}` with modified `server.xml` |
| `tomcatCacheDir` | `tomcat.cache.dir` | `~/.m2/tomcat-cache` | Directory for cached Tomcat downloads and auto-generated CATALINA_BASE directories |
| `httpPort` | `tomcat.http.port` | `8080` | HTTP port |
| `httpHost` | `tomcat.http.host` | `localhost` | HTTP host to bind to |
| `javaHome` | `tomcat.java.home` | `${java.home}` | Java installation directory |
| `contextPath` | `tomcat.context.path` | `/${project.artifactId}` | Context path for webapp |
| `warSourceDirectory` | `tomcat.war.directory` | `${project.build.directory}/${project.build.finalName}` | Directory containing exploded WAR |
| `deploymentOutputName` | `tomcat.deployment.name` | Derived from contextPath | Target directory name in webapps |
| `vmOptions` | `tomcat.vm.options` | Empty | JVM options (CATALINA_OPTS) |
| `environmentVariables` | - | Empty | Environment variables for Tomcat process |
| `autopublishEnabled` | `tomcat.autopublish.enabled` | `false` | Enable auto-publish on file changes |
| `autopublishInactivityLimit` | `tomcat.autopublish.inactivity` | `30` | Seconds of inactivity before publish |
| `startupTimeout` | `tomcat.timeout.startup` | `120000` | Startup timeout in ms |
| `shutdownTimeout` | `tomcat.timeout.shutdown` | `30000` | Shutdown timeout in ms |
| `skip` | `tomcat.skip` | `false` | Skip plugin execution |

## Tomcat Version Compatibility

| Tomcat Version | Jakarta EE | Minimum Java | Servlet | JSP | EL | WebSocket |
|----------------|------------|--------------|---------|-----|-----|-----------|
| 10.1.x | 10 | 11 | 6.0 | 3.1 | 5.0 | 2.1 |
| 11.x | 11 | 17 | 6.1 | 4.0 | 6.0 | 2.2 |

## Auto-Download

If `catalinaHome` is not specified or doesn't exist, the plugin automatically downloads Tomcat:

1. Attempts download from Apache CDN: `https://dlcdn.apache.org/tomcat/...`
2. Falls back to Apache Archive: `https://archive.apache.org/dist/tomcat/...`
3. Validates download using SHA-512 checksum
4. Extracts to `~/.m2/tomcat-cache/apache-tomcat-{version}` (configurable via `tomcatCacheDir`)

## Auto-Generated CATALINA_BASE

When `httpPort` differs from the default (8080) and `catalinaBase` is not specified, the plugin automatically generates a custom CATALINA_BASE directory:

1. Creates `{tomcatCacheDir}/base-{version}-{port}` (e.g., `~/.m2/tomcat-cache/base-10.1.52-9080`)
2. Copies configuration files from CATALINA_HOME
3. Modifies `server.xml` to use the configured HTTP port and host
4. Disables the shutdown port (set to -1) for security
5. Comments out the AJP connector

This allows running multiple Tomcat instances with different ports without modifying the original installation. The generated base is cached and reused if the port configuration matches.

## Hot Deployment (Auto-publish)

Enable auto-publish to automatically redeploy when source files change:

```xml
<configuration>
    <autopublishEnabled>true</autopublishEnabled>
    <autopublishInactivityLimit>30</autopublishInactivityLimit>
</configuration>
```

Changes are detected using file system watching. The plugin waits for the specified inactivity period (no file changes) before republishing to batch rapid changes together.

## Debugging Your Application

Start Tomcat with debugging enabled:

```bash
mvn tomcat:debug
```

The debug goal automatically configures the JDWP agent and prints connection instructions.

### Debug Configuration Parameters

| Parameter | Property | Default | Description |
|-----------|----------|---------|-------------|
| `debugPort` | `tomcat.debug.port` | `5005` | Port for debugger to connect |
| `debugSuspend` | `tomcat.debug.suspend` | `false` | Wait for debugger before starting |
| `debugHost` | `tomcat.debug.host` | `*` | Host/interface to bind debug agent |

### Command Line Examples

```bash
# Default debug mode (port 5005, no suspend)
mvn tomcat:debug

# Custom debug port
mvn tomcat:debug -Dtomcat.debug.port=8000

# Suspend until debugger attaches (for debugging startup)
mvn tomcat:debug -Dtomcat.debug.suspend=true

# Local connections only
mvn tomcat:debug -Dtomcat.debug.host=localhost
```

### POM Configuration Example

```xml
<plugin>
    <groupId>io.github.rajendarreddyj</groupId>
    <artifactId>tomcat-maven-plugin</artifactId>
    <configuration>
        <httpPort>8080</httpPort>
        <debugPort>5005</debugPort>
        <debugSuspend>false</debugSuspend>
        <contextPath>/myapp</contextPath>
    </configuration>
</plugin>
```

### VS Code Setup

1. Add to `.vscode/launch.json`:

```json
{
    "type": "java",
    "name": "Attach to Tomcat",
    "request": "attach",
    "hostName": "localhost",
    "port": 5005
}
```

2. Run `mvn tomcat:debug` in terminal
3. Set breakpoints in your code
4. Press F5 or select "Attach to Tomcat" in Run and Debug panel

### IntelliJ IDEA Setup

1. Run → Edit Configurations → Add → Remote JVM Debug
2. Configure:
   - Name: `Attach to Tomcat`
   - Host: `localhost`
   - Port: `5005`
3. Run `mvn tomcat:debug` in terminal
4. Set breakpoints and click Debug

### Eclipse Setup

1. Run → Debug Configurations → Remote Java Application → New
2. Configure:
   - Name: `Attach to Tomcat`
   - Connection Type: Standard (Socket Attach)
   - Host: `localhost`
   - Port: `5005`
3. Run `mvn tomcat:debug` in terminal
4. Set breakpoints and click Debug

## Deploying as ROOT

To deploy your application as the ROOT context (accessible at `/`):

```xml
<configuration>
    <contextPath>/</contextPath>
    <!-- OR -->
    <deploymentOutputName>ROOT</deploymentOutputName>
</configuration>
```

## Skipping Execution

Skip plugin execution in CI/CD or other environments:

```bash
mvn tomcat:run -Dtomcat.skip=true
```

Or in pom.xml:

```xml
<configuration>
    <skip>${skipTomcat}</skip>
</configuration>
```

## Troubleshooting

### Port Already in Use

If port 8080 is in use, specify a different port:

```bash
mvn tomcat:run -Dtomcat.http.port=9080
```

### Java Version Mismatch

Tomcat 10.1.x requires Java 11+, Tomcat 11.x requires Java 17+. Ensure your JDK meets the requirements:

```bash
java -version
```

### Download Failures

If Tomcat download fails:
1. Check network connectivity
2. Verify firewall allows HTTPS connections
3. Try specifying a local `catalinaHome` with a pre-installed Tomcat

## Building from Source

```bash
git clone https://github.com/rajendarreddyj/tomcat-maven-plugin.git
cd tomcat-maven-plugin
mvn clean install
```

Run tests:

```bash
mvn test           # Unit tests
mvn verify         # Unit + Integration tests
```

## Community

- [Documentation](https://rajendarreddyj.github.io/tomcat-maven-plugin/) - GitHub Pages site
- [Wiki](https://github.com/rajendarreddyj/tomcat-maven-plugin/wiki) - User guide and tutorials
- [API Docs](https://rajendarreddyj.github.io/tomcat-maven-plugin/apidocs/) - Javadoc reference
- Code of Conduct: [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md)
- Contributing: [CONTRIBUTING.md](CONTRIBUTING.md)
- Security Policy: [SECURITY.md](SECURITY.md)
- Support: [SUPPORT.md](SUPPORT.md)
- Architecture: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)

## License

Apache License, Version 2.0

## Contributing

Contributions are welcome! Please see [CONTRIBUTING.md](CONTRIBUTING.md) and submit pull requests against the `main` branch.
