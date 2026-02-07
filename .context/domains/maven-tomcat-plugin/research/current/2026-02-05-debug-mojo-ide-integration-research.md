---
title: DebugMojo and IDE Integration Research
date: 2026-02-05
author: AI Agent (Research Codebase)
repository: tomcat-maven-plugin
topic: "Adding DebugMojo and ability to debug application from VS Code and other IDEs"
tags: [research, codebase, debug, jpda, ide-integration, vscode, intellij, eclipse]
status: complete
---

# Research: DebugMojo and IDE Integration

## Research Question

How to add a DebugMojo to enable debugging applications from VS Code and other IDEs.

## Summary

This research documents how the tomcat-maven-plugin currently handles debugging and the patterns used by Tomcat and other Maven plugins to enable IDE debugging. The plugin uses an external process model with `TomcatLauncher` to start Tomcat via catalina scripts. Debug functionality can be achieved through JPDA (Java Platform Debugger Architecture) using Tomcat's built-in `jpda` command or by passing JVM debug options via existing `vmOptions` parameter. A dedicated `DebugMojo` would provide a cleaner developer experience with explicit debug parameters.

## Detailed Findings

### 1. Current Plugin Architecture

#### 1.1 Existing Mojos

| Mojo | File | Goal | Description |
|------|------|------|-------------|
| AbstractTomcatMojo | [AbstractTomcatMojo.java](../../../src/main/java/io/github/rajendarreddyj/tomcat/AbstractTomcatMojo.java) | (base) | Common configuration parameters |
| RunMojo | [RunMojo.java](../../../src/main/java/io/github/rajendarreddyj/tomcat/RunMojo.java) | `run` | Foreground execution, blocks until Ctrl+C |
| StartMojo | [StartMojo.java](../../../src/main/java/io/github/rajendarreddyj/tomcat/StartMojo.java) | `start` | Background execution, stores PID |
| StopMojo | [StopMojo.java](../../../src/main/java/io/github/rajendarreddyj/tomcat/StopMojo.java) | `stop` | Stops running instance |
| DeployMojo | [DeployMojo.java](../../../src/main/java/io/github/rajendarreddyj/tomcat/DeployMojo.java) | `deploy` | Deploy/redeploy webapp |

#### 1.2 How Tomcat is Launched

The `TomcatLauncher` class at [TomcatLauncher.java](../../../src/main/java/io/github/rajendarreddyj/tomcat/lifecycle/TomcatLauncher.java) starts Tomcat as an external process:

1. Resolves catalina script (`catalina.bat` or `catalina.sh`)
2. Builds command array (e.g., `cmd.exe /c catalina.bat run`)
3. Configures environment variables via `configureEnvironment()`
4. Launches process with `ProcessBuilder`

**Key method:** `startProcess(String command)` at line 108-140

#### 1.3 Current VM Options Handling

**Parameter declaration** in AbstractTomcatMojo (line 98-99):
```java
@Parameter(property = "tomcat.vm.options")
protected List<String> vmOptions;
```

**Applied in TomcatLauncher.configureEnvironment()** (line 175-179):
```java
if (!config.getVmOptions().isEmpty()) {
    String existingOpts = env.getOrDefault("CATALINA_OPTS", "");
    String newOpts = String.join(" ", config.getVmOptions());
    env.put("CATALINA_OPTS", (existingOpts + " " + newOpts).trim());
}
```

### 2. Tomcat JPDA Debugging Mechanism

#### 2.1 Tomcat's Built-in JPDA Support

Tomcat scripts support a `jpda` command variant:
```bash
# Unix/Linux/macOS
catalina.sh jpda run
catalina.sh jpda start

# Windows
catalina.bat jpda run
catalina.bat jpda start
```

When `jpda` is the first argument, the script prepends JPDA options to `CATALINA_OPTS`.

#### 2.2 JPDA Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `JPDA_TRANSPORT` | Debug transport protocol | `dt_socket` |
| `JPDA_ADDRESS` | Debug address (host:port) | `localhost:8000` |
| `JPDA_SUSPEND` | Suspend JVM until debugger attaches | `n` |
| `JPDA_OPTS` | Complete JPDA string (overrides above) | Auto-generated |

**Default JPDA_OPTS construction:**
```
-agentlib:jdwp=transport=$JPDA_TRANSPORT,address=$JPDA_ADDRESS,server=y,suspend=$JPDA_SUSPEND
```

#### 2.3 Alternative: Direct JDWP Agent

Without using `jpda` command, add debug agent directly to `CATALINA_OPTS`:
```
-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005
```

**JDWP Parameters:**
| Parameter | Value | Description |
|-----------|-------|-------------|
| `transport` | `dt_socket` | TCP/IP socket transport |
| `server` | `y` | JVM acts as debug server |
| `suspend` | `n` or `y` | `y` = wait for debugger before starting |
| `address` | `*:5005` | Listen on all interfaces, port 5005 |

### 3. Current Debug Capability

The plugin currently supports debugging through `vmOptions`:

```xml
<plugin>
    <groupId>io.github.rajendarreddyj</groupId>
    <artifactId>tomcat-maven-plugin</artifactId>
    <configuration>
        <vmOptions>
            <vmOption>-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005</vmOption>
        </vmOptions>
    </configuration>
</plugin>
```

Or via `environmentVariables`:
```xml
<configuration>
    <environmentVariables>
        <JPDA_ADDRESS>localhost:5005</JPDA_ADDRESS>
        <JPDA_SUSPEND>n</JPDA_SUSPEND>
    </environmentVariables>
</configuration>
```

### 4. IDE Integration Patterns

#### 4.1 VS Code Configuration

**Existing launch.json** at [.vscode/launch.json](../../../.vscode/launch.json) includes:
```json
{
    "type": "java",
    "name": "Debug Maven Plugin (Attach)",
    "request": "attach",
    "hostName": "localhost",
    "port": 8000,
    "projectName": "tomcat-maven-plugin"
}
```

**Required VS Code extensions** from [.vscode/extensions.json](../../../.vscode/extensions.json):
- `vscjava.vscode-java-debug` - Java debugging support
- `vscjava.vscode-java-pack` - Comprehensive Java support

**Attach Configuration Pattern:**
```json
{
    "type": "java",
    "name": "Debug Tomcat Application",
    "request": "attach",
    "hostName": "localhost",
    "port": 5005
}
```

#### 4.2 IntelliJ IDEA Configuration

1. **Run → Edit Configurations → Add → Remote JVM Debug**
2. Configure:
   - Name: `Debug Tomcat`
   - Host: `localhost`
   - Port: `5005`
   - Command line arguments for JVM: auto-generated

IntelliJ historically defaults to port `5005`.

#### 4.3 Eclipse Configuration

1. **Run → Debug Configurations → Remote Java Application**
2. Configure:
   - Connection Type: Standard (Socket Attach)
   - Host: `localhost`
   - Port: `8000`

Eclipse historically defaults to port `8000`.

### 5. Maven Plugin Debug Goal Patterns

#### 5.1 Common Debug Parameters

Based on patterns from maven-jetty-plugin, cargo-maven-plugin:

| Parameter | Property | Default | Purpose |
|-----------|----------|---------|---------|
| `debugPort` | `plugin.debug.port` | `5005` | JDWP agent port |
| `debugSuspend` | `plugin.debug.suspend` | `false` | Wait for debugger |
| `debugHost` | `plugin.debug.host` | `*` | Host/interface to bind |
| `debugForked` | `plugin.debug.forked` | `true` | Run in separate JVM |

#### 5.2 Legacy Apache Tomcat Maven Plugin

The legacy tomcat7-maven-plugin used embedded Tomcat (same JVM as Maven). Debugging was done by running Maven with debug arguments:
```bash
mvnDebug tomcat7:run
```

The current plugin uses external process model, requiring JPDA configuration.

### 6. Proposed DebugMojo Design

#### 6.1 Parameters

```java
@Parameter(property = "tomcat.debug.port", defaultValue = "5005")
protected int debugPort;

@Parameter(property = "tomcat.debug.suspend", defaultValue = "false")
protected boolean debugSuspend;

@Parameter(property = "tomcat.debug.host", defaultValue = "*")
protected String debugHost;
```

#### 6.2 JPDA Options Construction

```java
String jpdaOpts = String.format(
    "-agentlib:jdwp=transport=dt_socket,server=y,suspend=%s,address=%s:%d",
    debugSuspend ? "y" : "n",
    debugHost,
    debugPort
);
```

#### 6.3 Implementation Approaches

**Approach A: Use JPDA Environment Variables**
- Set `JPDA_TRANSPORT`, `JPDA_ADDRESS`, `JPDA_SUSPEND`
- Change launch command from `catalina.sh run` to `catalina.sh jpda run`
- Leverages Tomcat's built-in JPDA handling

**Approach B: Direct VM Options**
- Construct JDWP agent string
- Prepend to `CATALINA_OPTS`
- Use standard `catalina.sh run` command
- More portable, doesn't rely on JPDA script support

## Code References

| File | Lines | Description |
|------|-------|-------------|
| [AbstractTomcatMojo.java](../../../src/main/java/io/github/rajendarreddyj/tomcat/AbstractTomcatMojo.java) | 98-105 | vmOptions and environmentVariables parameters |
| [TomcatLauncher.java](../../../src/main/java/io/github/rajendarreddyj/tomcat/lifecycle/TomcatLauncher.java) | 108-140 | startProcess() method |
| [TomcatLauncher.java](../../../src/main/java/io/github/rajendarreddyj/tomcat/lifecycle/TomcatLauncher.java) | 167-196 | configureEnvironment() method |
| [RunMojo.java](../../../src/main/java/io/github/rajendarreddyj/tomcat/RunMojo.java) | 66-95 | execute() method pattern |
| [ServerConfiguration.java](../../../src/main/java/io/github/rajendarreddyj/tomcat/config/ServerConfiguration.java) | 60-65 | vmOptions storage |
| [launch.json](../../../.vscode/launch.json) | 31-37 | Existing attach debug configuration |

## Architecture Documentation

### Process Model

```
┌─────────────────┐      ┌─────────────────┐      ┌─────────────────┐
│   Maven JVM     │      │  DebugMojo      │      │  Tomcat JVM     │
│                 │ ──── │  configures     │ ──── │  with JDWP      │
│                 │      │  JPDA options   │      │  agent on       │
│                 │      │                 │      │  port 5005      │
└─────────────────┘      └─────────────────┘      └─────────────────┘
                                                          │
                                                          │ TCP Socket
                                                          │
                                                  ┌───────▼───────┐
                                                  │  IDE Debugger │
                                                  │  (VS Code/    │
                                                  │   IntelliJ/   │
                                                  │   Eclipse)    │
                                                  └───────────────┘
```

### Command Flow

```
mvn tomcat:debug
    │
    ▼
DebugMojo.execute()
    │
    ├── Validate Java version
    ├── Validate port available (http + debug)
    ├── Build ServerConfiguration with debug vmOptions
    ├── Deploy webapp
    │
    ▼
TomcatLauncher.run()
    │
    ├── configureEnvironment()
    │       └── Add JDWP to CATALINA_OPTS
    │
    ├── startProcess("run")
    │       └── catalina.bat/sh run
    │
    ▼
Console output:
"Listening for transport dt_socket at address: 5005"
"Tomcat started. Attach debugger to localhost:5005"
```

## Related Files

- [README.md](../../../README.md) - Plugin documentation
- [ARCHITECTURE.md](../../../docs/ARCHITECTURE.md) - Plugin architecture
- [2026-02-05-tomcat-auto-download-implementation.md](../../tomcat/research/current/2026-02-05-tomcat-auto-download-implementation.md) - JPDA environment variables documentation

## Open Questions

1. **Default debug port**: Use `5005` (IntelliJ default) or `8000` (Tomcat/Eclipse default)?
2. **Should `tomcat:run` and `tomcat:start` accept debug parameters?** Or keep debug as separate goal?
3. **IDE-specific launch configurations**: Should plugin generate VS Code launch.json or IntelliJ run config?
4. **Debug output enhancement**: Print banner with attach instructions for different IDEs?
5. **Source path configuration**: How to handle source lookup for multi-module projects?
