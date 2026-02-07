# Hot Deployment

Enable automatic redeployment when your source files change.

## Overview

The auto-publish feature watches your source directory for changes and automatically redeploys your application to Tomcat after a period of inactivity. This enables a rapid development cycle without manual redeployment.

## Quick Setup

### Command Line

```bash
mvn tomcat:run -Dtomcat.autopublish.enabled=true
```

### POM Configuration

```xml
<plugin>
    <groupId>io.github.rajendarreddyj</groupId>
    <artifactId>tomcat-maven-plugin</artifactId>
    <version>1.0.0</version>
    <configuration>
        <autopublishEnabled>true</autopublishEnabled>
        <autopublishInactivityLimit>30</autopublishInactivityLimit>
    </configuration>
</plugin>
```

## Configuration

| Parameter | Property | Default | Description |
|-----------|----------|---------|-------------|
| `autopublishEnabled` | `tomcat.autopublish.enabled` | `false` | Enable file watching |
| `autopublishInactivityLimit` | `tomcat.autopublish.inactivity` | `30` | Seconds to wait after last change |

## How It Works

```
┌─────────────────────────────────────────────────────────────┐
│                    Development Workflow                      │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. Start Tomcat with autopublish                           │
│     mvn tomcat:run -Dtomcat.autopublish.enabled=true        │
│                                                             │
│  2. Make changes to source files                            │
│     └── Edit Java, JSP, HTML, CSS, JS files                │
│                                                             │
│  3. File watcher detects changes                            │
│     └── Starts inactivity timer                             │
│                                                             │
│  4. After inactivity period (30s default)                   │
│     └── Redeploys application to Tomcat                     │
│                                                             │
│  5. Refresh browser to see changes                          │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Inactivity Timer

The inactivity limit batches rapid changes together. When you save a file:

1. Timer starts (or resets if already running)
2. Additional saves reset the timer
3. When timer expires (no changes for N seconds), redeploy occurs

**Example with 30-second limit:**

```
00:00 - Save File A → Timer starts
00:05 - Save File B → Timer resets
00:10 - Save File C → Timer resets
00:40 - (30 seconds since last save) → Redeploy!
```

### Tuning the Inactivity Limit

**Faster feedback (10 seconds):**

```bash
mvn tomcat:run -Dtomcat.autopublish.enabled=true -Dtomcat.autopublish.inactivity=10
```

**More batching (60 seconds):**

```bash
mvn tomcat:run -Dtomcat.autopublish.enabled=true -Dtomcat.autopublish.inactivity=60
```

## What Gets Redeployed

The watcher monitors your `warSourceDirectory` (default: `target/${project.build.finalName}`):

| File Type | Effect |
|-----------|--------|
| `.class` files | Application reloads, changes take effect |
| `.jsp` files | JSP recompiled on next request |
| `.html`, `.css`, `.js` | Available immediately (static) |
| `web.xml` | Full context reload |
| JAR files in `WEB-INF/lib` | Full context reload |

## Workflow with IDE

### Recommended Setup

1. Configure IDE for automatic compilation on save
2. Start Tomcat with autopublish
3. Make changes in IDE
4. IDE compiles → Plugin detects → Tomcat reloads

### VS Code + Java Extension

1. Enable auto-build (default behavior)
2. Run `mvn tomcat:run -Dtomcat.autopublish.enabled=true`
3. Edit and save files - compilation is automatic

### IntelliJ IDEA

1. Enable "Build project automatically" (Settings → Build → Compiler)
2. Enable "Allow auto-make when app is running" (Registry: `compiler.automake.allow.when.app.running`)
3. Run `mvn tomcat:run -Dtomcat.autopublish.enabled=true`

### Eclipse

1. "Build Automatically" is enabled by default (Project menu)
2. Run `mvn tomcat:run -Dtomcat.autopublish.enabled=true`

## Manual Deploy Alternative

If autopublish doesn't fit your workflow, use manual deployment:

```bash
# Terminal 1: Start Tomcat
mvn tomcat:start

# Terminal 2: Make changes, then redeploy
mvn compile tomcat:deploy
```

## Limitations

- **Java changes** require recompilation before detection
- **Major changes** (new dependencies, web.xml structure) may require restart
- **Memory leaks** can accumulate with many redeploys - restart periodically

## Troubleshooting

### Changes Not Detected

1. Verify `autopublishEnabled=true`
2. Check that files are in the watched directory
3. Ensure IDE is compiling to `target/` directory

### Slow Redeployment

1. Reduce `autopublishInactivityLimit` for faster feedback
2. Ensure no antivirus scanning the project directory
3. Consider SSD if using HDD

### Memory Issues After Many Redeploys

ClassLoader leaks can cause memory growth. Restart Tomcat periodically:

```bash
# Stop and restart
mvn tomcat:stop tomcat:run -Dtomcat.autopublish.enabled=true
```

## See Also

- [Configuration](Configuration) - All parameters
- [Goals Reference](Goals-Reference) - Goal documentation
- [Troubleshooting](Troubleshooting) - Common issues
