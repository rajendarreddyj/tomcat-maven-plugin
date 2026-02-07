# Configuration

Complete reference for all configuration parameters.

## Configuration Example

```xml
<plugin>
    <groupId>io.github.rajendarreddyj</groupId>
    <artifactId>tomcat-maven-plugin</artifactId>
    <version>1.0.0</version>
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

        <!-- Debug Configuration -->
        <debugPort>5005</debugPort>
        <debugSuspend>false</debugSuspend>
        <debugHost>*</debugHost>

        <!-- Classpath & Skip -->
        <classpathAdditions>
            <classpathAddition>/path/to/extra.jar</classpathAddition>
        </classpathAdditions>
        <skip>false</skip>
    </configuration>
</plugin>
```

## Parameter Reference

### Tomcat Location

| Parameter | Property | Default | Description |
|-----------|----------|---------|-------------|
| `tomcatVersion` | `tomcat.version` | `10.1.52` | Tomcat version to download/use |
| `catalinaHome` | `tomcat.catalina.home` | Auto-download | Path to Tomcat installation (CATALINA_HOME) |
| `catalinaBase` | `tomcat.catalina.base` | Auto-generated | Path to Tomcat instance directory (CATALINA_BASE) |
| `tomcatCacheDir` | `tomcat.cache.dir` | `~/.m2/tomcat-cache` | Cache directory for downloads and generated bases |

### Server Configuration

| Parameter | Property | Default | Description |
|-----------|----------|---------|-------------|
| `httpPort` | `tomcat.http.port` | `8080` | HTTP port for Tomcat |
| `httpHost` | `tomcat.http.host` | `localhost` | Host/interface to bind HTTP connector |

### JVM Configuration

| Parameter | Property | Default | Description |
|-----------|----------|---------|-------------|
| `javaHome` | `tomcat.java.home` | `${java.home}` | Path to JDK installation |
| `vmOptions` | `tomcat.vm.options` | Empty | List of JVM options (CATALINA_OPTS) |
| `environmentVariables` | - | Empty | Environment variables for Tomcat process |

### Deployment Configuration

| Parameter | Property | Default | Description |
|-----------|----------|---------|-------------|
| `contextPath` | `tomcat.context.path` | `/${project.artifactId}` | Context path for the webapp |
| `warSourceDirectory` | `tomcat.war.directory` | `${project.build.directory}/${project.build.finalName}` | Source directory (exploded WAR) |
| `deploymentOutputName` | `tomcat.deployment.name` | Derived from contextPath | Target name in webapps |

### Auto-publish Configuration

| Parameter | Property | Default | Description |
|-----------|----------|---------|-------------|
| `autopublishEnabled` | `tomcat.autopublish.enabled` | `false` | Enable auto-publish on file changes |
| `autopublishInactivityLimit` | `tomcat.autopublish.inactivity` | `30` | Seconds to wait before publishing |

### Timeout Configuration

| Parameter | Property | Default | Description |
|-----------|----------|---------|-------------|
| `startupTimeout` | `tomcat.timeout.startup` | `120000` | Startup timeout in milliseconds |
| `shutdownTimeout` | `tomcat.timeout.shutdown` | `30000` | Shutdown timeout in milliseconds |

### Debug Configuration

| Parameter | Property | Default | Description |
|-----------|----------|---------|-------------|
| `debugPort` | `tomcat.debug.port` | `5005` | Port for JDWP debugger |
| `debugSuspend` | `tomcat.debug.suspend` | `false` | Suspend until debugger attaches |
| `debugHost` | `tomcat.debug.host` | `*` | Host/interface for debug agent |

### Other

| Parameter | Property | Default | Description |
|-----------|----------|---------|-------------|
| `classpathAdditions` | - | Empty | Additional JARs for Tomcat classpath |
| `skip` | `tomcat.skip` | `false` | Skip plugin execution |

## Command Line Usage

All properties can be set via command line:

```bash
# Custom port
mvn tomcat:run -Dtomcat.http.port=9080

# Custom context path
mvn tomcat:run -Dtomcat.context.path=/api

# Multiple options
mvn tomcat:run -Dtomcat.http.port=9080 -Dtomcat.context.path=/api

# Skip execution
mvn package -Dtomcat.skip=true
```

## Profile-Based Configuration

```xml
<profiles>
    <profile>
        <id>dev</id>
        <build>
            <plugins>
                <plugin>
                    <groupId>io.github.rajendarreddyj</groupId>
                    <artifactId>tomcat-maven-plugin</artifactId>
                    <configuration>
                        <httpPort>8080</httpPort>
                        <autopublishEnabled>true</autopublishEnabled>
                    </configuration>
                </plugin>
            </plugins>
        </build>
    </profile>
    <profile>
        <id>test</id>
        <build>
            <plugins>
                <plugin>
                    <groupId>io.github.rajendarreddyj</groupId>
                    <artifactId>tomcat-maven-plugin</artifactId>
                    <configuration>
                        <httpPort>9080</httpPort>
                        <autopublishEnabled>false</autopublishEnabled>
                    </configuration>
                </plugin>
            </plugins>
        </build>
    </profile>
</profiles>
```

Usage:

```bash
mvn tomcat:run -Pdev
mvn tomcat:run -Ptest
```

## See Also

- [Goals Reference](Goals-Reference)
- [Debugging](Debugging)
- [Hot Deployment](Hot-Deployment)
