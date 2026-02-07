# Getting Started

This guide will help you get started with the Tomcat Maven Plugin.

## Prerequisites

Before using the plugin, ensure you have:

- **Java Development Kit (JDK) 21** or later
- **Apache Maven 3.9.6** or later
- A Maven-based web application project

## Installation

Add the plugin to your project's `pom.xml`:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>io.github.rajendarreddyj</groupId>
            <artifactId>tomcat-maven-plugin</artifactId>
            <version>1.0.0</version>
        </plugin>
    </plugins>
</build>
```

## Your First Run

### Step 1: Run Your Web Application

From your project directory, run:

```bash
mvn tomcat:run
```

This will:
1. Download Tomcat automatically (if not already cached)
2. Deploy your web application
3. Start Tomcat on port 8080
4. Display startup logs in your terminal

### Step 2: Access Your Application

Open your browser and navigate to:

```
http://localhost:8080/your-artifact-id
```

The context path defaults to `/${project.artifactId}`.

### Step 3: Stop the Server

Press `Ctrl+C` in the terminal to stop Tomcat.

## Basic Configuration

### Custom Port

```xml
<configuration>
    <httpPort>9080</httpPort>
</configuration>
```

Or via command line:

```bash
mvn tomcat:run -Dtomcat.http.port=9080
```

### Custom Context Path

```xml
<configuration>
    <contextPath>/myapp</contextPath>
</configuration>
```

Access at: `http://localhost:8080/myapp`

### Deploy as ROOT

```xml
<configuration>
    <contextPath>/</contextPath>
</configuration>
```

Access at: `http://localhost:8080/`

## Background Mode

Start Tomcat in the background:

```bash
mvn tomcat:start
```

Stop background Tomcat:

```bash
mvn tomcat:stop
```

## Next Steps

- [Configuration](Configuration) - All available configuration options
- [Goals Reference](Goals-Reference) - Detailed goal documentation
- [Debugging](Debugging) - Set up debugging for your application
- [Hot Deployment](Hot-Deployment) - Enable auto-publish on changes
