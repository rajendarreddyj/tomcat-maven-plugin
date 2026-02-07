---
title: DebugMojo and IDE Integration Implementation Plan
date: 2026-02-05
author: AI Agent
status: active
research: .context/domains/maven-tomcat-plugin/research/current/2026-02-05-debug-mojo-ide-integration-research.md
---

# DebugMojo and IDE Integration Implementation Plan

## Overview

Implement a dedicated `DebugMojo` (`tomcat:debug` goal) that provides streamlined debugging support with explicit parameters for port, suspend behavior, and host binding. This enables developers to easily debug web applications on Tomcat from VS Code, IntelliJ IDEA, and Eclipse without manually constructing JDWP arguments.

## Current State Analysis

### What Exists Now
- **vmOptions parameter** in AbstractTomcatMojo at [AbstractTomcatMojo.java#L112-L113](../../../src/main/java/io/github/rajendarreddyj/tomcat/AbstractTomcatMojo.java#L112-L113)
- **TomcatLauncher.configureEnvironment()** sets CATALINA_OPTS at [TomcatLauncher.java#L238-L243](../../../src/main/java/io/github/rajendarreddyj/tomcat/lifecycle/TomcatLauncher.java#L238-L243)
- **RunMojo** pattern to follow at [RunMojo.java](../../../src/main/java/io/github/rajendarreddyj/tomcat/RunMojo.java)
- **VS Code launch.json** with attach config at [.vscode/launch.json](../../../.vscode/launch.json)

### Key Discoveries
- Tomcat supports JPDA via `catalina jpda run` command or `CATALINA_OPTS` with JDWP agent
- JPDA environment variables: `JPDA_ADDRESS`, `JPDA_SUSPEND`, `JPDA_TRANSPORT`
- Default ports: IntelliJ uses 5005, Eclipse/Tomcat use 8000
- Current plugin uses external process model via `TomcatLauncher`

## Desired End State

After implementation:
1. `mvn tomcat:debug` starts Tomcat with JDWP debug agent enabled
2. Console outputs clear instructions for IDE connection
3. Explicit parameters: `debugPort`, `debugSuspend`, `debugHost`
4. README documents debug usage for VS Code, IntelliJ, and Eclipse
5. VS Code launch.json includes pre-configured attach configurations

### Verification
- Run `mvn tomcat:debug` and verify "Listening for transport dt_socket at address: 5005" appears
- Attach debugger from VS Code/IntelliJ and hit breakpoints
- Test with `debugSuspend=true` and verify JVM waits for debugger
- Test `debugPort` parameter changes the listening port

## What We're NOT Doing

- Embedding Tomcat in Maven JVM (different architecture model)
- Automatic IDE configuration file generation (manual documentation instead)
- Hot-swap class reloading without redeploy (separate feature)
- Debug protocol other than JDWP (dt_socket transport only)
- Integration with Maven Debug (`mvnDebug`) - this is for debugging the plugin itself

---

## Implementation Approach

Use **Approach B: Direct VM Options** from research - construct JDWP agent string and prepend to `CATALINA_OPTS`. This is more portable and doesn't rely on Tomcat's JPDA script parsing.

**JDWP Format:**
```
-agentlib:jdwp=transport=dt_socket,server=y,suspend={n|y},address={host}:{port}
```

---

## Phase 1: Add Debug Parameters to AbstractTomcatMojo

### Overview
Add debug-related configuration parameters to the base Mojo class so they can be shared across goals.

### Changes Required:

#### 1. AbstractTomcatMojo.java - Add Debug Parameters

**File:** [src/main/java/io/github/rajendarreddyj/tomcat/AbstractTomcatMojo.java](../../../src/main/java/io/github/rajendarreddyj/tomcat/AbstractTomcatMojo.java)

**Add after line ~118 (after environmentVariables declaration):**

```java
    // ==================== Debug Configuration ====================

    /**
     * Port for the JDWP debug agent to listen on.
     * The debugger will connect to this port.
     * Default: 5005 (IntelliJ IDEA default)
     */
    @Parameter(property = "tomcat.debug.port", defaultValue = "5005")
    protected int debugPort;

    /**
     * Whether to suspend JVM startup until a debugger attaches.
     * Set to true when you need to debug application initialization.
     * Default: false (start immediately)
     */
    @Parameter(property = "tomcat.debug.suspend", defaultValue = "false")
    protected boolean debugSuspend;

    /**
     * Host/interface for the debug agent to bind to.
     * Use "*" to allow connections from any host (remote debugging).
     * Use "localhost" to restrict to local connections only.
     * Default: * (allow remote debugging)
     */
    @Parameter(property = "tomcat.debug.host", defaultValue = "*")
    protected String debugHost;
```

#### 2. AbstractTomcatMojo.java - Add Debug Options Builder Method

**Add new method after buildDeployableConfiguration() (~line 320):**

```java
    /**
     * Builds the JDWP agent string for debug mode.
     *
     * <p>Constructs a JDWP agent argument in the format:
     * {@code -agentlib:jdwp=transport=dt_socket,server=y,suspend={n|y},address={host}:{port}}
     * </p>
     *
     * @return the JDWP agent JVM argument string
     */
    protected String buildJdwpAgentArg() {
        return String.format(
            "-agentlib:jdwp=transport=dt_socket,server=y,suspend=%s,address=%s:%d",
            debugSuspend ? "y" : "n",
            debugHost,
            debugPort
        );
    }

    /**
     * Validates that the debug port is available.
     *
     * @throws MojoExecutionException if the debug port is already in use
     */
    protected void validateDebugPortAvailable() throws MojoExecutionException {
        try (ServerSocket socket = new ServerSocket(debugPort, 1, InetAddress.getByName("0.0.0.0"))) {
            socket.setReuseAddress(true);
            getLog().debug("Debug port " + debugPort + " is available");
        } catch (IOException e) {
            throw new MojoExecutionException(
                    "Debug port " + debugPort + " is already in use. " +
                    "Configure a different port with -Dtomcat.debug.port=XXXX");
        }
    }
```

### Success Criteria:

#### Automated Verification:
- [ ] Build compiles without errors: `mvn clean compile`
- [ ] No new compiler warnings introduced
- [ ] Existing tests pass: `mvn test`

#### Manual Verification:
- [ ] Parameters appear in `mvn help:describe -Dplugin=io.github.rajendarreddyj:tomcat-maven-plugin -Ddetail`

**Implementation Note:** Complete this phase and verify before proceeding.

---

## Phase 2: Create DebugMojo Class

### Overview
Create a new `DebugMojo` class that starts Tomcat in foreground mode with debugging enabled.

### Changes Required:

#### 1. Create DebugMojo.java

**File:** `src/main/java/io/github/rajendarreddyj/tomcat/DebugMojo.java`

```java
package io.github.rajendarreddyj.tomcat;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import io.github.rajendarreddyj.tomcat.config.ServerConfiguration;
import io.github.rajendarreddyj.tomcat.deploy.ExplodedWarDeployer;
import io.github.rajendarreddyj.tomcat.deploy.HotDeployWatcher;
import io.github.rajendarreddyj.tomcat.lifecycle.TomcatLauncher;

/**
 * Runs Apache Tomcat in debug mode with JDWP agent enabled.
 *
 * <p>
 * This goal starts Tomcat with Java debugging enabled, allowing IDEs
 * (VS Code, IntelliJ, Eclipse) to attach and debug the running application.
 * The goal blocks until the process is terminated (Ctrl+C).
 * </p>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code mvn tomcat:debug}</pre>
 *
 * <h2>IDE Connection</h2>
 *
 * <p>Connect your IDE debugger to localhost:{debugPort} (default: 5005)</p>
 *
 * <h3>VS Code</h3>
 * <p>Add to launch.json:</p>
 * <pre>{@code
 * {
 *     "type": "java",
 *     "name": "Attach to Tomcat",
 *     "request": "attach",
 *     "hostName": "localhost",
 *     "port": 5005
 * }
 * }</pre>
 *
 * <h3>IntelliJ IDEA</h3>
 * <p>Run → Edit Configurations → Add → Remote JVM Debug → Port: 5005</p>
 *
 * <h3>Eclipse</h3>
 * <p>Run → Debug Configurations → Remote Java Application → Port: 5005</p>
 *
 * <h2>Configuration Example</h2>
 *
 * <pre>{@code
 * <plugin>
 *     <groupId>io.github.rajendarreddyj</groupId>
 *     <artifactId>tomcat-maven-plugin</artifactId>
 *     <configuration>
 *         <httpPort>8080</httpPort>
 *         <debugPort>5005</debugPort>
 *         <debugSuspend>false</debugSuspend>
 *         <contextPath>/myapp</contextPath>
 *     </configuration>
 * </plugin>
 * }</pre>
 *
 * @author rajendarreddyj
 * @see RunMojo for non-debug foreground execution
 * @see StartMojo for background mode execution
 * @since 1.0.0
 */
@Mojo(name = "debug", defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST,
      requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true)
public class DebugMojo extends AbstractTomcatMojo {

    /**
     * Executes the debug goal.
     *
     * <p>
     * This method performs the following steps:
     * </p>
     * <ol>
     * <li>Validates Java version compatibility with the configured Tomcat version</li>
     * <li>Validates that the HTTP port is available</li>
     * <li>Validates that the debug port is available</li>
     * <li>Builds server configuration with JDWP debug options</li>
     * <li>Deploys the webapp to Tomcat's webapps directory</li>
     * <li>Starts the hot deploy watcher if auto-publish is enabled</li>
     * <li>Prints debug connection instructions</li>
     * <li>Starts Tomcat in foreground mode and blocks until shutdown</li>
     * </ol>
     *
     * @throws MojoExecutionException if an error occurs during execution
     * @throws MojoFailureException   if execution fails due to invalid configuration
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Skipping Tomcat execution (tomcat.skip=true)");
            return;
        }

        validateJavaVersion();
        validatePortAvailable();
        validateDebugPortAvailable();

        try {
            ServerConfiguration serverConfig = buildDebugServerConfiguration();
            var deployConfig = buildDeployableConfiguration(serverConfig);

            // Deploy webapp
            getLog().info("Deploying webapp to: " + deployConfig.getDeployDir());
            ExplodedWarDeployer deployer = new ExplodedWarDeployer(getLog());
            deployer.deploy(deployConfig);

            // Start hot deploy watcher if enabled
            HotDeployWatcher watcher = new HotDeployWatcher(deployConfig, deployer, getLog());

            try {
                watcher.start();

                // Print debug connection instructions
                printDebugInstructions();

                // Start Tomcat
                TomcatLauncher launcher = new TomcatLauncher(serverConfig, getLog());
                getLog().info("Starting Tomcat " + tomcatVersion + " in DEBUG mode on http://" +
                        httpHost + ":" + httpPort + contextPath);

                launcher.run();

            } finally {
                watcher.close();
            }

        } catch (Exception e) {
            throw new MojoExecutionException("Failed to run Tomcat in debug mode: " + e.getMessage(), e);
        }
    }

    /**
     * Builds ServerConfiguration with JDWP debug options included in vmOptions.
     *
     * @return the ServerConfiguration with debug settings
     * @throws MojoExecutionException if configuration fails
     */
    private ServerConfiguration buildDebugServerConfiguration() throws MojoExecutionException {
        // Prepend JDWP agent to vmOptions
        List<String> debugVmOptions = new ArrayList<>();
        debugVmOptions.add(buildJdwpAgentArg());

        if (vmOptions != null) {
            debugVmOptions.addAll(vmOptions);
        }

        // Temporarily set vmOptions with debug agent
        List<String> originalVmOptions = vmOptions;
        vmOptions = debugVmOptions;

        try {
            return buildServerConfiguration();
        } finally {
            // Restore original vmOptions
            vmOptions = originalVmOptions;
        }
    }

    /**
     * Prints debug connection instructions to the console.
     */
    private void printDebugInstructions() {
        getLog().info("");
        getLog().info("╔══════════════════════════════════════════════════════════════════╗");
        getLog().info("║                    DEBUG MODE ENABLED                            ║");
        getLog().info("╠══════════════════════════════════════════════════════════════════╣");
        getLog().info("║ Listening for debugger on port: " + padRight(String.valueOf(debugPort), 31) + "║");
        getLog().info("║ Suspend on startup: " + padRight(String.valueOf(debugSuspend), 43) + "║");
        getLog().info("╠══════════════════════════════════════════════════════════════════╣");
        getLog().info("║ IDE Connection Instructions:                                     ║");
        getLog().info("║                                                                  ║");
        getLog().info("║ VS Code:                                                         ║");
        getLog().info("║   1. Open Run and Debug (Ctrl+Shift+D)                          ║");
        getLog().info("║   2. Add \"Attach\" configuration with port " + padRight(String.valueOf(debugPort), 20) + "║");
        getLog().info("║   3. Start debugging (F5)                                        ║");
        getLog().info("║                                                                  ║");
        getLog().info("║ IntelliJ IDEA:                                                   ║");
        getLog().info("║   Run → Edit Configurations → Remote JVM Debug → Port: " + padRight(String.valueOf(debugPort), 8) + "║");
        getLog().info("║                                                                  ║");
        getLog().info("║ Eclipse:                                                         ║");
        getLog().info("║   Run → Debug Configurations → Remote Java Application          ║");
        getLog().info("║   Connection Type: Standard (Socket Attach), Port: " + padRight(String.valueOf(debugPort), 13) + "║");
        getLog().info("╚══════════════════════════════════════════════════════════════════╝");
        getLog().info("");

        if (debugSuspend) {
            getLog().info(">>> JVM is SUSPENDED - waiting for debugger to attach...");
            getLog().info("");
        }
    }

    /**
     * Right-pads a string with spaces to the specified length.
     *
     * @param str the string to pad
     * @param length the desired total length
     * @return the padded string
     */
    private String padRight(String str, int length) {
        if (str.length() >= length) {
            return str;
        }
        StringBuilder sb = new StringBuilder(str);
        while (sb.length() < length) {
            sb.append(' ');
        }
        return sb.toString();
    }
}
```

### Success Criteria:

#### Automated Verification:
- [ ] Build compiles without errors: `mvn clean compile`
- [ ] Plugin descriptor generates: `mvn plugin:descriptor`
- [ ] Goal appears in help: `mvn tomcat:help`
- [ ] Existing tests pass: `mvn test`

#### Manual Verification:
- [ ] Run `mvn tomcat:debug -DskipTests` in a webapp project
- [ ] Verify "Listening for transport dt_socket at address: 5005" appears
- [ ] Verify debug instructions banner is printed

**Implementation Note:** Complete this phase and all automated verification passes before proceeding.

---

## Phase 3: Create DebugMojo Unit Tests

### Overview
Create comprehensive unit tests for the DebugMojo class.

### Changes Required:

#### 1. Create DebugMojoTest.java

**File:** `src/test/java/io/github/rajendarreddyj/tomcat/DebugMojoTest.java`

```java
package io.github.rajendarreddyj.tomcat;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for {@link DebugMojo}.
 *
 * <p>
 * Tests the debug mode Tomcat execution goal including JDWP configuration,
 * parameter handling, and error scenarios.
 *
 * @author rajendarreddyj
 * @see DebugMojo
 */
class DebugMojoTest {

    @TempDir
    Path tempDir;

    private DebugMojo mojo;
    private Log log;

    @BeforeEach
    void setUp() throws Exception {
        mojo = new DebugMojo();
        log = mock(Log.class);

        // Set up required fields using reflection
        setField(mojo, "log", log);
        setField(mojo, "tomcatVersion", "10.1.52");
        setField(mojo, "httpPort", 8080);
        setField(mojo, "httpHost", "localhost");
        setField(mojo, "debugPort", 5005);
        setField(mojo, "debugSuspend", false);
        setField(mojo, "debugHost", "*");

        // Mock project
        MavenProject project = mock(MavenProject.class);
        when(project.getArtifactId()).thenReturn("test-app");
        setField(mojo, "project", project);

        // Create temp directories
        Path catalinaHome = tempDir.resolve("tomcat");
        Files.createDirectories(catalinaHome.resolve("bin"));
        Files.createDirectories(catalinaHome.resolve("lib"));
        Files.createDirectories(catalinaHome.resolve("webapps"));
        Files.createFile(catalinaHome.resolve("lib/catalina.jar"));

        setField(mojo, "catalinaHome", catalinaHome.toFile());
        setField(mojo, "tomcatCacheDir", tempDir.resolve("cache").toFile());
    }

    /**
     * Verifies that execution is skipped when skip=true.
     *
     * @throws Exception if the test fails
     */
    @Test
    void executeSkipsWhenSkipIsTrue() throws Exception {
        setField(mojo, "skip", true);

        assertDoesNotThrow(() -> mojo.execute());
        verify(log).info("Skipping Tomcat execution (tomcat.skip=true)");
    }

    /**
     * Verifies that buildJdwpAgentArg creates correct JDWP string with defaults.
     *
     * @throws Exception if the test fails
     */
    @Test
    void buildJdwpAgentArgWithDefaults() throws Exception {
        setField(mojo, "debugPort", 5005);
        setField(mojo, "debugSuspend", false);
        setField(mojo, "debugHost", "*");

        String jdwpArg = invokeMethod(mojo, "buildJdwpAgentArg");

        assertEquals("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005", jdwpArg);
    }

    /**
     * Verifies that buildJdwpAgentArg creates correct JDWP string with suspend=true.
     *
     * @throws Exception if the test fails
     */
    @Test
    void buildJdwpAgentArgWithSuspend() throws Exception {
        setField(mojo, "debugPort", 8000);
        setField(mojo, "debugSuspend", true);
        setField(mojo, "debugHost", "localhost");

        String jdwpArg = invokeMethod(mojo, "buildJdwpAgentArg");

        assertEquals("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=localhost:8000", jdwpArg);
    }

    /**
     * Verifies that custom debug port is used.
     *
     * @throws Exception if the test fails
     */
    @Test
    void customDebugPortIsUsed() throws Exception {
        setField(mojo, "debugPort", 9999);
        setField(mojo, "skip", true);

        assertDoesNotThrow(() -> mojo.execute());

        int debugPort = (int) getField(mojo, "debugPort");
        assertEquals(9999, debugPort);
    }

    /**
     * Verifies that execution succeeds with all debug options configured.
     *
     * @throws Exception if the test fails
     */
    @Test
    void executeWithAllDebugOptions() throws Exception {
        setField(mojo, "debugPort", 5005);
        setField(mojo, "debugSuspend", true);
        setField(mojo, "debugHost", "localhost");
        setField(mojo, "skip", true);

        assertDoesNotThrow(() -> mojo.execute());
    }

    /**
     * Verifies that vmOptions are preserved when debug options are added.
     *
     * @throws Exception if the test fails
     */
    @Test
    void vmOptionsArePreservedWithDebugOptions() throws Exception {
        List<String> originalVmOptions = List.of("-Xmx512m", "-XX:+UseG1GC");
        setField(mojo, "vmOptions", originalVmOptions);
        setField(mojo, "skip", true);

        assertDoesNotThrow(() -> mojo.execute());

        // Verify original vmOptions are not modified
        @SuppressWarnings("unchecked")
        List<String> vmOptions = (List<String>) getField(mojo, "vmOptions");
        assertEquals(2, vmOptions.size());
        assertTrue(vmOptions.contains("-Xmx512m"));
    }

    // ==================== Helper Methods ====================

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = findField(target.getClass(), fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private Object getField(Object target, String fieldName) throws Exception {
        Field field = findField(target.getClass(), fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    private Field findField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName + " not found in " + clazz.getName());
    }

    @SuppressWarnings("unchecked")
    private <T> T invokeMethod(Object target, String methodName) throws Exception {
        var method = target.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        return (T) method.invoke(target);
    }
}
```

### Success Criteria:

#### Automated Verification:
- [ ] All tests pass: `mvn test`
- [ ] Test coverage for DebugMojo: `mvn test jacoco:report`
- [ ] No test compilation errors

#### Manual Verification:
- [ ] Review test report at `target/surefire-reports/`

**Implementation Note:** Complete this phase and verify all tests pass before proceeding.

---

## Phase 4: Update Documentation and IDE Configurations

### Overview
Update README.md with debug documentation and add VS Code launch configurations.

### Changes Required:

#### 1. Update README.md

**File:** [README.md](../../../README.md)

**Add new section after "Hot Deployment (Auto-publish)" section (~line 175):**

```markdown
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
```

#### 2. Update Goals Table in README.md

**Update the Goals table (~line 50):**

```markdown
| Goal | Description |
|------|-------------|
| `tomcat:run` | Runs Tomcat in the foreground with your webapp deployed |
| `tomcat:debug` | Runs Tomcat in debug mode with JDWP agent enabled |
| `tomcat:start` | Starts Tomcat in the background |
| `tomcat:stop` | Stops a running Tomcat instance |
| `tomcat:deploy` | Deploys/redeploys the webapp to a running Tomcat |
| `tomcat:help` | Displays help information on the plugin goals |
```

#### 3. Update .vscode/launch.json

**File:** [.vscode/launch.json](../../../.vscode/launch.json)

**Add new configuration after existing "Debug Maven Plugin (Attach)":**

```json
{
    "type": "java",
    "name": "Attach to Tomcat Debug",
    "request": "attach",
    "hostName": "localhost",
    "port": 5005
}
```

### Success Criteria:

#### Automated Verification:
- [ ] Build still compiles: `mvn compile`
- [ ] Plugin help includes debug goal: `mvn tomcat:help`

#### Manual Verification:
- [ ] README renders correctly on GitHub
- [ ] VS Code launch.json is valid JSON
- [ ] New launch configuration appears in VS Code Run and Debug panel

**Implementation Note:** Complete this phase and verify documentation is accurate.

---

## Phase 5: Integration Test

### Overview
Create an integration test to verify the debug goal works end-to-end.

### Changes Required:

#### 1. Create Debug Integration Test

**File:** `src/it/debug-webapp/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>io.github.rajendarreddyj.tomcat.it</groupId>
    <artifactId>debug-webapp</artifactId>
    <version>1.0-SNAPSHOT</version>
    <packaging>war</packaging>

    <name>Debug WebApp Integration Test</name>

    <properties>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
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
                <groupId>io.github.rajendarreddyj</groupId>
                <artifactId>tomcat-maven-plugin</artifactId>
                <version>@project.version@</version>
                <configuration>
                    <httpPort>8090</httpPort>
                    <debugPort>5006</debugPort>
                    <debugSuspend>false</debugSuspend>
                    <contextPath>/debug-test</contextPath>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

#### 2. Create verify.groovy

**File:** `src/it/debug-webapp/verify.groovy`

```groovy
// Verify debug goal configuration
def buildLog = new File(basedir, 'build.log').text

// Check that JDWP agent is configured
assert buildLog.contains('agentlib:jdwp') : 'JDWP agent should be configured'
assert buildLog.contains('5006') : 'Debug port should be 5006'

// Check debug instructions are printed
assert buildLog.contains('DEBUG MODE ENABLED') : 'Debug banner should be printed'
assert buildLog.contains('Listening for debugger') || buildLog.contains('DEBUG mode') : 'Debug mode should be indicated'

println 'Debug integration test passed!'
return true
```

### Success Criteria:

#### Automated Verification:
- [ ] Integration test passes: `mvn verify -Pit`
- [ ] Build log contains JDWP configuration

#### Manual Verification:
- [ ] Test a real debug session by attaching from VS Code

**Implementation Note:** Complete this phase and verify integration test passes.

---

## Testing Strategy

### Unit Tests
- `DebugMojoTest.java` - Test JDWP argument construction
- Test debug port validation
- Test skip functionality
- Test vmOptions preservation

### Integration Tests
- `debug-webapp/` - Verify debug goal starts with JDWP agent
- Verify debug instructions are printed
- Verify custom port configuration

### Manual Testing Steps
1. Create a simple webapp project with a servlet
2. Run `mvn tomcat:debug`
3. Verify "Listening for transport dt_socket" message appears
4. Attach VS Code debugger to port 5005
5. Set a breakpoint in the servlet
6. Access the servlet URL and verify breakpoint is hit
7. Test with `debugSuspend=true` and verify JVM waits

## Performance Considerations

- Debug mode adds minimal overhead to startup time
- JDWP agent has some runtime overhead during debugging sessions
- No impact when running in non-debug mode (tomcat:run)

## Migration Notes

- Existing users of `vmOptions` for debug configuration can migrate to explicit parameters
- Both approaches continue to work (vmOptions takes precedence if JDWP is specified there)

## References

- Research document: [2026-02-05-debug-mojo-ide-integration-research.md](../research/current/2026-02-05-debug-mojo-ide-integration-research.md)
- RunMojo implementation: [RunMojo.java](../../../src/main/java/io/github/rajendarreddyj/tomcat/RunMojo.java)
- TomcatLauncher: [TomcatLauncher.java](../../../src/main/java/io/github/rajendarreddyj/tomcat/lifecycle/TomcatLauncher.java)
- JDWP documentation: https://docs.oracle.com/en/java/javase/21/docs/specs/jpda/jdwp/jdwp-spec.html
