# Goals Reference

Detailed documentation for all plugin goals.

## Available Goals

| Goal | Description |
|------|-------------|
| [`tomcat:run`](#tomcatrun) | Run Tomcat in foreground with webapp deployed |
| [`tomcat:debug`](#tomcatdebug) | Run Tomcat with JDWP debug agent enabled |
| [`tomcat:start`](#tomcatstart) | Start Tomcat in background |
| [`tomcat:stop`](#tomcatstop) | Stop running Tomcat instance |
| [`tomcat:deploy`](#tomcatdeploy) | Deploy/redeploy webapp to running Tomcat |
| [`tomcat:help`](#tomcathelp) | Display help information |

---

## tomcat:run

Runs Tomcat in the foreground with your web application deployed. Press `Ctrl+C` to stop.

### Usage

```bash
mvn tomcat:run
```

### What It Does

1. Downloads Tomcat (if needed)
2. Generates CATALINA_BASE (if needed for custom port)
3. Deploys your exploded WAR to webapps
4. Starts Tomcat and streams logs to console
5. Optionally watches for file changes (if autopublish enabled)

### Configuration

All [configuration parameters](Configuration) apply.

### Examples

```bash
# Basic run
mvn tomcat:run

# Custom port
mvn tomcat:run -Dtomcat.http.port=9080

# With auto-publish
mvn tomcat:run -Dtomcat.autopublish.enabled=true

# Deploy as ROOT
mvn tomcat:run -Dtomcat.context.path=/
```

---

## tomcat:debug

Runs Tomcat with the Java Debug Wire Protocol (JDWP) agent enabled for remote debugging.

### Usage

```bash
mvn tomcat:debug
```

### What It Does

1. Configures JDWP agent in CATALINA_OPTS
2. Starts Tomcat with debug port open
3. Displays connection instructions within console output

### Debug-Specific Parameters

| Parameter | Property | Default | Description |
|-----------|----------|---------|-------------|
| `debugPort` | `tomcat.debug.port` | `5005` | Debug port |
| `debugSuspend` | `tomcat.debug.suspend` | `false` | Wait for debugger |
| `debugHost` | `tomcat.debug.host` | `*` | Bind interface |

### Examples

```bash
# Default debug (port 5005)
mvn tomcat:debug

# Custom port
mvn tomcat:debug -Dtomcat.debug.port=8000

# Suspend until debugger attaches
mvn tomcat:debug -Dtomcat.debug.suspend=true

# Local connections only
mvn tomcat:debug -Dtomcat.debug.host=localhost
```

### IDE Setup

See [Debugging](Debugging) for IDE configuration guides.

---

## tomcat:start

Starts Tomcat in the background. Use `tomcat:stop` to shut down.

### Usage

```bash
mvn tomcat:start
```

### What It Does

1. Downloads and configures Tomcat (same as `run`)
2. Deploys your application
3. Starts Tomcat as a background process
4. Returns control to the terminal

### Examples

```bash
# Start in background
mvn tomcat:start

# Start on different port
mvn tomcat:start -Dtomcat.http.port=9080
```

### Process Management

The plugin creates a PID file to track the background process. Use `tomcat:stop` to cleanly shut down.

---

## tomcat:stop

Stops a running Tomcat instance started with `tomcat:start`.

### Usage

```bash
mvn tomcat:stop
```

### What It Does

1. Locates the running Tomcat process
2. Sends shutdown signal
3. Waits for graceful shutdown (up to `shutdownTimeout`)
4. Force kills if timeout exceeded

### Parameters

| Parameter | Property | Default | Description |
|-----------|----------|---------|-------------|
| `shutdownTimeout` | `tomcat.timeout.shutdown` | `30000` | Shutdown timeout (ms) |

### Examples

```bash
# Stop server
mvn tomcat:stop

# Match port from start
mvn tomcat:stop -Dtomcat.http.port=9080
```

---

## tomcat:deploy

Deploys or redeploys the web application to a running Tomcat instance.

### Usage

```bash
mvn tomcat:deploy
```

### What It Does

1. Copies the exploded WAR to Tomcat's webapps directory
2. Tomcat auto-detects the change and reloads the context

### When to Use

- After `tomcat:start` to redeploy changes
- When not using auto-publish
- For manual deployment control

### Examples

```bash
# Start server, make changes, redeploy
mvn tomcat:start
# ... make code changes and recompile ...
mvn compile tomcat:deploy
```

---

## tomcat:help

Displays help information about the plugin and its goals.

### Usage

```bash
mvn tomcat:help
```

### Detailed Help

```bash
# Help for specific goal
mvn tomcat:help -Ddetail=true -Dgoal=run

# All configuration parameters
mvn help:describe -Dplugin=io.github.rajendarreddyj:tomcat-maven-plugin -Ddetail
```

---

## Goal Lifecycle

```
┌─────────────────┐
│   tomcat:run    │  (foreground, Ctrl+C to stop)
└─────────────────┘

┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  tomcat:start   │ ──▶ │  tomcat:deploy  │ ──▶ │   tomcat:stop   │
└─────────────────┘     └─────────────────┘     └─────────────────┘
   (background)           (optional, repeatable)     (shutdown)
```

## See Also

- [Configuration](Configuration) - All parameters
- [Debugging](Debugging) - IDE setup for debug goal
- [Hot Deployment](Hot-Deployment) - Auto-publish setup
