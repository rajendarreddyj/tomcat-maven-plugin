---
title: Apache Tomcat Maven Plugin (Legacy) Architecture Documentation
date: 2026-02-05
author: AI Research Assistant
sources:
  - https://github.com/apache/tomcat-maven-plugin
  - https://raw.githubusercontent.com/apache/tomcat-maven-plugin/trunk/tomcat7-maven-plugin/
  - https://raw.githubusercontent.com/apache/tomcat-maven-plugin/trunk/common-tomcat-maven-plugin/
---

# Apache Tomcat Maven Plugin (Legacy) Architecture Documentation

## 1. Project Structure Overview

The repository is organized as a multi-module Maven project:

```
tomcat-maven-plugin/
├── common-tomcat-maven-plugin/     # Shared code for all Tomcat versions
│   └── src/main/java/org/apache/tomcat/maven/common/
│       ├── config/                 # Configuration classes
│       ├── deployer/               # TomcatManager for remote deployment
│       ├── messages/               # Internationalization support
│       └── run/                    # Embedded Tomcat support classes
├── tomcat7-maven-plugin/           # Tomcat 7.x specific plugin
├── tomcat8-maven-plugin/           # Tomcat 8.x specific plugin
├── tomcat7-war-runner/             # Executable WAR runner for Tomcat 7
├── tomcat8-war-runner/             # Executable WAR runner for Tomcat 8
├── tomcat-maven-archetype/         # Project archetype
├── tomcat-maven-plugin-it/         # Integration tests
├── tomcat-main-maven-plugin/       # Current API plugin (new)
└── tomcat-main-war-runner/         # Current API war runner (new)
```

## 2. Available Goals

### 2.1 Run Goals (Embedded Tomcat)

| Goal | Class | Description |
|------|-------|-------------|
| `run` | `RunMojo` | Runs the current project as a dynamic web application using embedded Tomcat. Executes `PROCESS_CLASSES` phase first. |
| `run-war` | `RunWarMojo` | Runs a packaged WAR file using embedded Tomcat. |
| `run-war-only` | `RunWarOnlyMojo` | Same as `run-war` but doesn't trigger packaging. |
| `shutdown` | `ShutdownMojo` | Shuts down all embedded Tomcat instances started by the plugin. |

### 2.2 Deploy Goals (Remote Tomcat Manager)

| Goal | Class | Description |
|------|-------|-------------|
| `deploy` | `DeployMojo` | Deploys a WAR to remote Tomcat via Manager API. Executes `PACKAGE` phase first. |
| `deploy-only` | `DeployOnlyMojo` | Same as `deploy` but doesn't trigger packaging. |
| `redeploy` | `RedeployMojo` | Redeploys a WAR (undeploy + deploy with `update=true`). |
| `redeploy-only` | `RedeployOnlyMojo` | Same as `redeploy` but doesn't trigger packaging. |
| `undeploy` | `UndeployMojo` | Undeploys a web application from remote Tomcat. |

### 2.3 Executable WAR Goals

| Goal | Class | Description |
|------|-------|-------------|
| `exec-war` | `ExecWarMojo` | Creates a self-executable JAR containing the WAR and embedded Tomcat. |
| `exec-war-only` | `ExecWarOnlyMojo` | Same as `exec-war` but doesn't trigger packaging. |
| `standalone-war` | `StandaloneWarMojo` | Creates a standalone WAR that can run with `java -jar`. |
| `standalone-war-only` | `StandaloneWarOnlyMojo` | Same as `standalone-war` but doesn't trigger packaging. |

## 3. Class Hierarchy and Key Classes

### 3.1 Mojo Class Hierarchy

```
AbstractMojo (Maven API)
└── AbstractTomcat7Mojo (base for all tomcat7 goals)
    ├── AbstractCatalinaMojo (base for Tomcat Manager operations)
    │   ├── AbstractWarCatalinaMojo (WAR-specific manager operations)
    │   │   ├── AbstractDeployMojo (deployment base)
    │   │   │   ├── AbstractDeployWarMojo
    │   │   │   │   ├── DeployMojo (@Mojo name="deploy")
    │   │   │   │   ├── DeployOnlyMojo (@Mojo name="deploy-only")
    │   │   │   │   ├── RedeployMojo (@Mojo name="redeploy")
    │   │   │   │   └── RedeployOnlyMojo (@Mojo name="redeploy-only")
    │   │   └── UndeployMojo (@Mojo name="undeploy")
    ├── AbstractRunMojo (base for embedded Tomcat)
    │   ├── RunMojo (@Mojo name="run")
    │   ├── RunWarMojo (@Mojo name="run-war")
    │   ├── AbstractRunWarMojo
    │   │   └── RunWarOnlyMojo (@Mojo name="run-war-only")
    │   └── ShutdownMojo (@Mojo name="shutdown")
    └── AbstractExecWarMojo (base for executable WAR)
        ├── ExecWarMojo (@Mojo name="exec-war")
        ├── ExecWarOnlyMojo (@Mojo name="exec-war-only")
        └── AbstractStandaloneWarMojo
            ├── StandaloneWarMojo (@Mojo name="standalone-war")
            └── StandaloneWarOnlyMojo (@Mojo name="standalone-war-only")
```

### 3.2 Key Classes in Common Module

| Class | Package | Responsibility |
|-------|---------|----------------|
| `TomcatManager` | `o.a.t.m.common.deployer` | HTTP client wrapper for Tomcat Manager webapp API |
| `TomcatManagerResponse` | `o.a.t.m.common.deployer` | Response wrapper for manager operations |
| `TomcatManagerException` | `o.a.t.m.common.deployer` | Exception for manager operation failures |
| `EmbeddedRegistry` | `o.a.t.m.common.run` | Singleton registry for embedded Tomcat instances with shutdown hook |
| `ClassLoaderEntriesCalculator` | `o.a.t.m.common.run` | Calculates classpath entries for embedded execution |
| `MessagesProvider` | `o.a.t.m.common.messages` | Internationalization message provider |
| `AbstractWebapp` | `o.a.t.m.common.config` | Base class for webapp configuration |

## 4. Configuration Parameters

### 4.1 Base Configuration (AbstractTomcat7Mojo)

| Parameter | Property | Default | Description |
|-----------|----------|---------|-------------|
| `path` | `maven.tomcat.path` | `/${project.artifactId}` | Webapp context path |
| `settings` | - | `${settings}` | Maven settings (readonly) |

### 4.2 Tomcat Manager Configuration (AbstractCatalinaMojo)

| Parameter | Property | Default | Description |
|-----------|----------|---------|-------------|
| `url` | `maven.tomcat.url` | `http://localhost:8080/manager/text` | Full URL of Tomcat manager |
| `server` | `maven.tomcat.server` | - | Server ID in settings.xml for auth |
| `username` | `tomcat.username` | `admin` | Manager username |
| `password` | `tomcat.password` | (empty) | Manager password |
| `charset` | `maven.tomcat.charset` | `ISO-8859-1` | URL encoding charset |
| `skip` | `maven.tomcat.skip` | `false` | Skip goal execution |

### 4.3 Deployment Configuration (AbstractDeployMojo)

| Parameter | Property | Default | Description |
|-----------|----------|---------|-------------|
| `mode` | `maven.tomcat.mode` | `war` | Deployment mode: `war`, `context`, or `both` |
| `contextFile` | - | `${project.build.directory}/${project.build.finalName}/META-INF/context.xml` | Context XML path |
| `update` | `maven.tomcat.update` | `false` | Auto-undeploy existing apps |
| `tag` | `maven.tomcat.tag` | - | Tag name for deployment |

### 4.4 Embedded Run Configuration (AbstractRunMojo)

| Parameter | Property | Default | Description |
|-----------|----------|---------|-------------|
| `port` | `maven.tomcat.port` | `8080` | HTTP port (exposed as `tomcat.maven.http.port`) |
| `httpsPort` | `maven.tomcat.httpsPort` | `0` | HTTPS port (0 = disabled) |
| `ajpPort` | `maven.tomcat.ajp.port` | `0` | AJP port (0 = disabled) |
| `ajpProtocol` | `maven.tomcat.ajp.protocol` | `org.apache.coyote.ajp.AjpProtocol` | AJP connector protocol |
| `address` | `maven.tomcat.address` | - | Binding IP address |
| `configurationDir` | - | `${project.build.directory}/tomcat` | Tomcat config directory |
| `serverXml` | `maven.tomcat.serverXml` | - | Custom server.xml path |
| `tomcatWebXml` | `maven.tomcat.webXml` | - | Custom web.xml for Tomcat |
| `contextFile` | `maven.tomcat.contextFile` | - | Context XML file path |
| `fork` | `maven.tomcat.fork` | `false` | Continue Maven after starting Tomcat |
| `protocol` | `maven.tomcat.protocol` | `HTTP/1.1` | HTTP connector protocol |
| `uriEncoding` | `maven.tomcat.uriEncoding` | `ISO-8859-1` | URI encoding |
| `maxPostSize` | `maven.tomcat.maxPostSize` | `2097152` | Max POST size in bytes |
| `useNaming` | `maven.tomcat.useNaming` | `true` | Enable JNDI naming |
| `contextReloadable` | `maven.tomcat.contextReloadable` | `false` | Enable hot reload |
| `backgroundProcessorDelay` | `maven.tomcat.backgroundProcessorDelay` | `-1` | Class scanning delay in seconds |
| `useSeparateTomcatClassLoader` | `tomcat.useSeparateTomcatClassLoader` | `false` | Isolate Tomcat from Maven |
| `systemProperties` | - | - | Map of system properties |
| `additionalConfigFilesDir` | `maven.tomcat.additionalConfigFilesDir` | `${basedir}/src/main/tomcatconf` | Additional config files |
| `tomcatUsers` | `maven.tomcat.tomcatUsers.file` | - | Custom tomcat-users.xml |
| `tomcatLoggingFile` | `maven.tomcat.tomcatLogging.file` | - | Custom logging config |
| `hostName` | `maven.tomcat.hostName` | `localhost` | Virtual host name |
| `aliases` | - | - | Host name aliases |
| `webapps` | - | - | Additional webapps to deploy |
| `staticContextPath` | `maven.tomcat.staticContextPath` | `/` | Static content context path |
| `staticContextDocbase` | `maven.tomcat.staticContextDocbase` | - | Static content docbase |
| `propertiesPortFilePath` | `maven.tomcat.propertiesPortFilePath` | - | File to write port info |

### 4.5 SSL/TLS Configuration (AbstractRunMojo)

| Parameter | Property | Default | Description |
|-----------|----------|---------|-------------|
| `keystoreFile` | - | - | Keystore file path |
| `keystorePass` | - | - | Keystore password |
| `keystoreType` | - | `JKS` | Keystore type |
| `truststoreFile` | - | - | Truststore file path |
| `truststorePass` | - | - | Truststore password |
| `truststoreType` | - | - | Truststore type |
| `truststoreAlgorithm` | - | - | Truststore algorithm |
| `truststoreProvider` | - | - | Truststore provider |
| `clientAuth` | `maven.tomcat.https.clientAuth` | `false` | Client certificate auth |
| `trustManagerClassName` | - | - | Custom trust manager class |
| `trustMaxCertLength` | - | - | Max certificate path length |

### 4.6 Run Mojo Specific (RunMojo)

| Parameter | Property | Default | Description |
|-----------|----------|---------|-------------|
| `warSourceDirectory` | `tomcat.warSourceDirectory` | `${basedir}/src/main/webapp` | Webapp source directory |
| `delegate` | `tomcat.delegate` | `true` | Webapp classloader delegation model |
| `addWarDependenciesInClassloader` | `maven.tomcat.addWarDependenciesInClassloader` | `true` | Add WAR deps to classloader |
| `useTestClasspath` | `maven.tomcat.useTestClasspath` | `false` | Include test scope dependencies |
| `additionalClasspathDirs` | - | - | Additional classpath directories |

### 4.7 Executable WAR Configuration (AbstractExecWarMojo)

| Parameter | Property | Default | Description |
|-----------|----------|---------|-------------|
| `finalName` | `tomcat.jar.finalName` | `${project.artifactId}-${project.version}-war-exec.jar` | Output JAR name |
| `mainClass` | `maven.tomcat.exec.war.mainClass` | `o.a.t.m.runner.Tomcat7RunnerCli` | Main class |
| `attachArtifact` | `maven.tomcat.exec.war.attachArtifact` | `true` | Attach to Maven build |
| `attachArtifactClassifier` | `maven.tomcat.exec.war.attachArtifactClassifier` | `exec-war` | Artifact classifier |
| `attachArtifactClassifierType` | `maven.tomcat.exec.war.attachArtifactType` | `jar` | Artifact type |
| `enableNaming` | `maven.tomcat.exec.war.enableNaming` | `false` | Enable JNDI |
| `enableRemoteIpValve` | `maven.tomcat.exec.war.enableRemoteIpValve` | `true` | Enable RemoteIpValve |
| `accessLogValveFormat` | `maven.tomcat.exec.war.accessLogValveFormat` | `%h %l %u %t %r %s %b %I %D` | Access log format |
| `connectorHttpProtocol` | `maven.tomcat.exec.war.connectorHttpProtocol` | `HTTP/1.1` | HTTP protocol |
| `httpPort` | `maven.tomcat.exec.war.httpPort` | - | Default HTTP port |
| `extraDependencies` | - | - | Extra runtime dependencies |
| `extraResources` | - | - | Extra resources to bundle |
| `warRunDependencies` | - | - | Additional WARs to bundle |
| `serverXml` | `maven.tomcat.exec.war.serverXml` | `src/main/tomcatconf/server.xml` | Custom server.xml |
| `tomcatConfigurationFilesDirectory` | `maven.tomcat.exec.war.tomcatConf` | `src/main/tomcatconf` | Tomcat config directory |

## 5. Process Management

### 5.1 Embedded Tomcat Lifecycle

The plugin uses **embedded Tomcat** (not a separate process):

1. **Initialization** (`initConfiguration()`):
   - Creates configuration directory (`${project.build.directory}/tomcat`)
   - Copies `logging.properties`, `tomcat-users.xml`, `web.xml` from plugin resources or user-provided files
   - Copies additional config files from `additionalConfigFilesDir`

2. **Container Start** (`startContainer()`):
   - Sets `catalina.base` system property
   - Two modes:
     - **With server.xml**: Uses `Catalina` class to parse and start
     - **Without server.xml**: Programmatically configures `Tomcat` instance
   - Configures connectors (HTTP, HTTPS, AJP)
   - Registers with `EmbeddedRegistry` for shutdown tracking

3. **Context Creation** (`createContext()`):
   - Parses context.xml if provided
   - Configures `WebappLoader` with calculated classpath
   - Sets up `JarScanner` for servlet/fragment scanning
   - Optionally configures multiple webapps from dependencies or configuration

4. **Wait/Fork Behavior**:
   - **fork=false** (default): Calls `waitIndefinitely()` - blocks on `Object.wait()`
   - **fork=true**: Returns immediately, Tomcat runs in background

5. **Shutdown**:
   - **ShutdownMojo**: Calls `EmbeddedRegistry.getInstance().shutdownAll()`
   - **Shutdown Hook**: Automatically registered to clean up on JVM exit
   - Uses reflection to call `stop()` and `destroy()` on registered containers

### 5.2 EmbeddedRegistry Pattern

```java
public final class EmbeddedRegistry {
    private static EmbeddedRegistry instance;
    private Set<Object> containers = new HashSet<>(1);
    
    // Lazy initialization with shutdown hook
    public static EmbeddedRegistry getInstance() {
        if (instance == null) {
            instance = new EmbeddedRegistry();
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    getInstance().shutdownAll(null);
                }
            });
        }
        return instance;
    }
    
    public synchronized boolean register(Object container) {
        return containers.add(container);
    }
    
    public synchronized void shutdownAll(Log log) throws Exception {
        for (Object embedded : containers) {
            Method stop = embedded.getClass().getMethod("stop");
            stop.invoke(embedded);
            Method destroy = embedded.getClass().getMethod("destroy");
            destroy.invoke(embedded);
        }
    }
}
```

### 5.3 TomcatManager (Remote Deployment)

The `TomcatManager` class communicates with Tomcat's Manager webapp via HTTP:

**Supported Operations:**
- `deploy(path, war, update, tag)` - Deploy WAR via PUT request
- `deployContext(path, config, war, update, tag)` - Deploy with context.xml
- `undeploy(path)` - Undeploy application
- `reload(path)` - Reload application
- `start(path)` - Start stopped application
- `stop(path)` - Stop running application
- `list()` - List deployed applications
- `getServerInfo()` - Get server information
- `getSessions(path)` - Get session information

**Implementation Details:**
- Uses Apache HttpClient with connection pooling
- Supports Basic Authentication
- Supports HTTP proxy configuration from Maven settings
- Handles HTTP redirects (301, 302, 303)
- Manager API URLs: `/deploy?path=...`, `/undeploy?path=...`, etc.

## 6. Hot Deployment / Context Reloading

### 6.1 Enabling Hot Reload

Hot deployment is achieved through Tomcat's built-in context reloading:

1. **Configuration Option 1**: Set `contextReloadable=true` in plugin config
2. **Configuration Option 2**: Add `reloadable="true"` in `context.xml`
3. **Configuration Option 3**: Set `backgroundProcessorDelay > 0`

### 6.2 Implementation

```java
protected boolean isContextReloadable() throws MojoExecutionException {
    if (contextReloadable || backgroundProcessorDelay > 0) {
        return true;
    }
    // Also check context.xml for reloadable="true" attribute
    if (contextFile != null && contextFile.exists()) {
        // Parse XML and check reloadable attribute
    }
    return false;
}

protected WebappLoader createWebappLoader() {
    if (isContextReloadable()) {
        return new ExternalRepositoriesReloadableWebappLoader(...);
    }
    return new WebappLoader(...);
}
```

### 6.3 ExternalRepositoriesReloadableWebappLoader

Custom `WebappLoader` that:
- Extends standard `WebappLoader`
- Watches external class directories (e.g., `target/classes`)
- Triggers context reload when classes change

## 7. Executable WAR Feature

### 7.1 Architecture

The exec-war creates a self-contained JAR with:

```
myapp-war-exec.jar
├── myapp.war                       # The application WAR
├── conf/
│   ├── web.xml                     # Default Tomcat web.xml
│   └── server.xml                  # Optional custom server.xml
├── war-exec.properties             # Runtime configuration
├── META-INF/MANIFEST.MF            # Main-Class entry
└── org/apache/tomcat/...           # Tomcat classes (extracted from JARs)
    org/apache/catalina/...
    org/eclipse/jdt/...             # JDT compiler for JSP
    commons-cli/...                 # CLI parsing
    tomcat7-war-runner/...          # Runner classes
```

### 7.2 war-exec.properties Contents

```properties
archiveGenerationTimestamp=1234567890
enableNaming=false
enableRemoteIpValve=true
useServerXml=false
httpProtocol=HTTP/1.1
accessLogValveFormat=%h %l %u %t %r %s %b %I %D
httpPort=8080
wars=myapp.war|/myapp
```

### 7.3 Runner Classes

- **Tomcat7RunnerCli**: Main class with CLI argument parsing
- **Tomcat7Runner**: Core runner that extracts WARs and configures embedded Tomcat

### 7.4 Usage

```bash
# Run with defaults
java -jar myapp-war-exec.jar

# Run with custom port
java -jar myapp-war-exec.jar -httpPort 9090

# Run with HTTPS
java -jar myapp-war-exec.jar -httpsPort 8443 -keystoreFile keystore.jks
```

## 8. Classpath Management

### 8.1 ClassLoaderEntriesCalculator

Calculates classpath entries for embedded execution:

```java
public interface ClassLoaderEntriesCalculator {
    ClassLoaderEntriesCalculatorResult calculateClassPathEntries(
        ClassLoaderEntriesCalculatorRequest request
    ) throws TomcatRunException;
}
```

### 8.2 Classpath Sources

1. **Project Dependencies** - Filtered by scope (compile/runtime/test)
2. **WAR Dependencies** - Extracted `/WEB-INF/lib/*.jar` and `/WEB-INF/classes`
3. **Additional Directories** - User-configured `additionalClasspathDirs`
4. **Build Output** - `target/classes` for change detection

### 8.3 Classloader Isolation

When `useSeparateTomcatClassLoader=true`:
- Creates a new `ClassRealm` (Plexus classloader)
- Adds all plugin artifacts to the realm
- Sets as parent classloader for webapp context
- Prevents conflicts between Maven and Tomcat dependencies

## 9. Multi-Webapp Support

### 9.1 Configuration

```xml
<webapps>
    <webapp>
        <groupId>com.example</groupId>
        <artifactId>other-webapp</artifactId>
        <version>1.0</version>
        <type>war</type>
        <contextPath>/other</contextPath>
        <contextFile>src/main/tomcatconf/other-context.xml</contextFile>
        <asWebapp>true</asWebapp>
    </webapp>
</webapps>
```

### 9.2 Legacy Support

Setting `addContextWarDependencies=true` auto-deploys WAR dependencies with scope `tomcat`:

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>other-webapp</artifactId>
    <version>1.0</version>
    <type>war</type>
    <scope>tomcat</scope>
</dependency>
```

## 10. Port Management

### 10.1 Port Exposure

Actual ports are exposed via:
- System properties: `tomcat.maven.http.port`, `tomcat.maven.https.port`, `tomcat.maven.ajp.port`
- Session execution properties (for downstream plugins)
- Optional properties file (`propertiesPortFilePath`)

### 10.2 Dynamic Port Allocation

Set port to `0` for dynamic allocation, then read actual port from exposed properties.

## 11. Error Handling Pattern

```java
public void execute() throws MojoExecutionException {
    if (this.skip) {
        getLog().info("skip execution");
        return;
    }
    try {
        invokeManager();
    } catch (TomcatManagerException exception) {
        throw new MojoExecutionException(
            messagesProvider.getMessage("AbstractCatalinaMojo.managerError", 
                exception.getMessage()));
    } catch (IOException exception) {
        throw new MojoExecutionException(
            messagesProvider.getMessage("AbstractCatalinaMojo.managerIOError"), 
            exception);
    }
}
```

## 12. Key Design Patterns Used

1. **Template Method Pattern**: Abstract base classes define execution flow, subclasses implement specifics
2. **Singleton Pattern**: `EmbeddedRegistry` for container tracking
3. **Strategy Pattern**: Different deployment modes (war/context/both)
4. **Factory Pattern**: Connector and context creation based on configuration
5. **Facade Pattern**: `TomcatManager` simplifies Tomcat Manager API interactions
6. **Registry Pattern**: `EmbeddedRegistry` tracks all started containers

## 13. Dependencies

### 13.1 Core Dependencies

- `maven-plugin-api` - Maven plugin framework
- `maven-plugin-annotations` - Annotation processing
- `org.apache.tomcat:tomcat-catalina` - Tomcat core
- `org.apache.tomcat.embed:tomcat-embed-*` - Embedded Tomcat
- `org.eclipse.jdt.core.compiler:ecj` - JSP compilation

### 13.2 Utility Dependencies

- `org.apache.httpcomponents:httpclient` - HTTP client for Manager API
- `commons-io` - File utilities
- `commons-lang` - String utilities
- `commons-compress` - Archive handling for exec-war
- `commons-cli` - Command line parsing (exec-war runner)
- `plexus-archiver` - Archive creation
- `plexus-classworlds` - Classloader management

## 14. Thread Safety

All public Mojos are marked with `threadSafe = true`:

```java
@Mojo(name = "run", requiresDependencyResolution = ResolutionScope.TEST, threadSafe = true)
```

This enables parallel builds with Maven 3+.
