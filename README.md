# Maven Tomcat Plugin

[![License: Apache-2.0](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.rajendarreddyj/maven-tomcat-plugin.svg)](https://central.sonatype.com/artifact/io.github.rajendarreddyj/maven-tomcat-plugin)
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
            <artifactId>maven-tomcat-plugin</artifactId>
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
    <artifactId>maven-tomcat-plugin</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <configuration>
        <!-- Tomcat Version & Location -->
        <tomcatVersion>10.1.52</tomcatVersion>
        <catalinaHome>/path/to/tomcat</catalinaHome>
        <catalinaBase>/path/to/instance</catalinaBase>
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
| `catalinaBase` | `tomcat.catalina.base` | Same as catalinaHome | Tomcat instance directory |
| `tomcatCacheDir` | `tomcat.cache.dir` | `~/.m2/tomcat-cache` | Directory for cached Tomcat downloads |
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

## Hot Deployment (Auto-publish)

Enable auto-publish to automatically redeploy when source files change:

```xml
<configuration>
    <autopublishEnabled>true</autopublishEnabled>
    <autopublishInactivityLimit>30</autopublishInactivityLimit>
</configuration>
```

Changes are detected using file system watching. The plugin waits for the specified inactivity period (no file changes) before republishing to batch rapid changes together.

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
git clone https://github.com/rajendarreddyj/maven-tomcat-plugin.git
cd maven-tomcat-plugin
mvn clean install
```

Run tests:

```bash
mvn test           # Unit tests
mvn verify         # Unit + Integration tests
```

## Community

- Code of Conduct: [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md)
- Contributing: [CONTRIBUTING.md](CONTRIBUTING.md)
- Security Policy: [SECURITY.md](SECURITY.md)
- Support: [SUPPORT.md](SUPPORT.md)
- Architecture: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)

## License

Apache License, Version 2.0

## Contributing

Contributions are welcome! Please see [CONTRIBUTING.md](CONTRIBUTING.md) and submit pull requests against the `main` branch.
