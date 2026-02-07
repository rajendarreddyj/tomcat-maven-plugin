# Tomcat Maven Plugin

Welcome to the **Tomcat Maven Plugin** documentation wiki!

A modern Maven plugin for deploying and managing web applications on Apache Tomcat 10.1.x and 11.x.

## Quick Navigation

| Page | Description |
|------|-------------|
| [Getting Started](Getting-Started) | Installation and first steps |
| [Configuration](Configuration) | All configuration parameters |
| [Goals Reference](Goals-Reference) | Detailed goal documentation |
| [Debugging](Debugging) | Debug your application |
| [Hot Deployment](Hot-Deployment) | Auto-publish on file changes |
| [Tomcat Versions](Tomcat-Versions) | Version compatibility guide |
| [Troubleshooting](Troubleshooting) | Common issues and solutions |
| [FAQ](FAQ) | Frequently asked questions |

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
- **Debug mode** with JDWP support

## Requirements

| Requirement | Version |
|-------------|---------|
| Java | JDK 21+ |
| Maven | 3.9.6+ |
| Tomcat | 10.1.x or 11.x |

## Quick Start

```xml
<plugin>
    <groupId>io.github.rajendarreddyj</groupId>
    <artifactId>tomcat-maven-plugin</artifactId>
    <version>1.0.0</version>
</plugin>
```

```bash
mvn tomcat:run
```

## Links

- [GitHub Repository](https://github.com/rajendarreddyj/tomcat-maven-plugin)
- [Maven Central](https://central.sonatype.com/artifact/io.github.rajendarreddyj/tomcat-maven-plugin)
- [API Documentation](https://rajendarreddyj.github.io/tomcat-maven-plugin/apidocs/)
- [Issue Tracker](https://github.com/rajendarreddyj/tomcat-maven-plugin/issues)
