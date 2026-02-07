# Debugging

Complete guide for debugging your web application with the Tomcat Maven Plugin.

## Quick Start

```bash
mvn tomcat:debug
```

Then attach your IDE debugger to `localhost:5005`.

## Debug Configuration

### Command Line Options

```bash
# Default (port 5005, no suspend)
mvn tomcat:debug

# Custom debug port
mvn tomcat:debug -Dtomcat.debug.port=8000

# Suspend until debugger attaches (for startup debugging)
mvn tomcat:debug -Dtomcat.debug.suspend=true

# Restrict to local connections only
mvn tomcat:debug -Dtomcat.debug.host=localhost

# Combined options
mvn tomcat:debug -Dtomcat.debug.port=8000 -Dtomcat.debug.suspend=true
```

### POM Configuration

```xml
<plugin>
    <groupId>io.github.rajendarreddyj</groupId>
    <artifactId>tomcat-maven-plugin</artifactId>
    <version>1.0.0</version>
    <configuration>
        <httpPort>8080</httpPort>
        <debugPort>5005</debugPort>
        <debugSuspend>false</debugSuspend>
        <debugHost>*</debugHost>
        <contextPath>/myapp</contextPath>
    </configuration>
</plugin>
```

## IDE Setup

### Visual Studio Code

1. Install the "Debugger for Java" extension

2. Create `.vscode/launch.json`:

```json
{
    "version": "0.2.0",
    "configurations": [
        {
            "type": "java",
            "name": "Attach to Tomcat",
            "request": "attach",
            "hostName": "localhost",
            "port": 5005
        }
    ]
}
```

3. Run `mvn tomcat:debug` in terminal
4. Set breakpoints in your code
5. Press `F5` or select "Attach to Tomcat" in Run and Debug panel

### IntelliJ IDEA

1. Go to **Run → Edit Configurations**
2. Click **+** → **Remote JVM Debug**
3. Configure:
   - **Name:** `Attach to Tomcat`
   - **Debugger mode:** `Attach to remote JVM`
   - **Host:** `localhost`
   - **Port:** `5005`
   - **Use module classpath:** Select your project module
4. Click **OK**
5. Run `mvn tomcat:debug` in terminal
6. Set breakpoints and click the **Debug** button

### Eclipse

1. Go to **Run → Debug Configurations**
2. Right-click **Remote Java Application** → **New Configuration**
3. Configure:
   - **Name:** `Attach to Tomcat`
   - **Project:** Select your project
   - **Connection Type:** `Standard (Socket Attach)`
   - **Host:** `localhost`
   - **Port:** `5005`
4. Click **Apply**
5. Run `mvn tomcat:debug` in terminal
6. Set breakpoints and click **Debug**

### NetBeans

1. Go to **Debug → Attach Debugger**
2. Configure:
   - **Debugger:** `Java Debugger (JPDA)`
   - **Connector:** `SocketAttach`
   - **Host:** `localhost`
   - **Port:** `5005`
3. Click **OK**
4. Set breakpoints in your code

## Debugging Tips

### Debug Startup Issues

Use `debugSuspend=true` to pause Tomcat until the debugger attaches:

```bash
mvn tomcat:debug -Dtomcat.debug.suspend=true
```

This is useful for debugging:
- Servlet initialization (`init()` methods)
- Spring context startup
- Application listeners
- Filter initialization

### Debug JSP Pages

JSPs are compiled to servlets. Set breakpoints in the generated Java code or:

1. Enable JSP development mode in your `web.xml`:

```xml
<servlet>
    <servlet-name>jsp</servlet-name>
    <servlet-class>org.apache.jasper.servlet.JspServlet</servlet-class>
    <init-param>
        <param-name>development</param-name>
        <param-value>true</param-value>
    </init-param>
</servlet>
```

2. Modify JSP and refresh - changes are picked up automatically

### Remote Debugging

To debug from a remote machine:

```bash
# On server (allow all hosts)
mvn tomcat:debug -Dtomcat.debug.host=0.0.0.0

# In IDE, connect to server's IP address instead of localhost
```

**Security Warning:** Only use `0.0.0.0` in development environments. Never expose debug ports in production.

### Conditional Breakpoints

In your IDE, set conditional breakpoints to pause only when specific conditions are met:

```java
// Break only when userId equals 123
user.getId() == 123

// Break only when list is empty
items.isEmpty()
```

### Evaluate Expressions

While paused at a breakpoint, use your IDE's expression evaluator to:
- Inspect variable values
- Call methods
- Modify variable values
- Test logic

## Troubleshooting

### Connection Refused

```
Unable to open debugger port: Connection refused
```

**Causes:**
1. Tomcat not started - wait for startup to complete
2. Wrong port - verify debug port matches
3. Firewall blocking - check firewall settings

### Address Already in Use

```
Address already in use: 5005
```

**Solutions:**
1. Use a different port: `mvn tomcat:debug -Dtomcat.debug.port=5006`
2. Kill the process using the port
3. Wait for previous debug session to fully terminate

### Breakpoints Not Hit

1. Ensure source code matches deployed classes
2. Rebuild: `mvn clean compile tomcat:debug`
3. Verify breakpoint is in reachable code
4. Check IDE's breakpoint settings

## See Also

- [Goals Reference](Goals-Reference#tomcatdebug) - Debug goal details
- [Configuration](Configuration) - All parameters
- [Troubleshooting](Troubleshooting) - Common issues
