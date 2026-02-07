# Troubleshooting

Solutions to common issues when using the Tomcat Maven Plugin.

## Startup Issues

### Port Already in Use

**Error:**
```
java.net.BindException: Address already in use: bind
```

**Solutions:**

1. **Use a different port:**
   ```bash
   mvn tomcat:run -Dtomcat.http.port=9080
   ```

2. **Find and kill the process using the port:**
   ```bash
   # Linux/macOS
   lsof -i :8080
   kill -9 <PID>

   # Windows
   netstat -ano | findstr :8080
   taskkill /PID <PID> /F
   ```

3. **Stop existing Tomcat:**
   ```bash
   mvn tomcat:stop
   ```

### Java Version Mismatch

**Error:**
```
UnsupportedClassVersionError: class file version XX.0
```

**Solutions:**

1. **Check your Java version:**
   ```bash
   java -version
   ```

2. **Requirements:**
   - Tomcat 10.1.x: Java 11+
   - Tomcat 11.x: Java 17+

3. **Specify JDK path:**
   ```xml
   <configuration>
       <javaHome>/path/to/jdk21</javaHome>
   </configuration>
   ```

### Tomcat Download Failed

**Error:**
```
Failed to download Tomcat: Connection refused
```

**Solutions:**

1. **Check network connectivity:**
   ```bash
   curl https://dlcdn.apache.org/tomcat/
   ```

2. **Check firewall/proxy settings**

3. **Use local Tomcat installation:**
   ```xml
   <configuration>
       <catalinaHome>/path/to/tomcat</catalinaHome>
   </configuration>
   ```

4. **Clear download cache and retry:**
   ```bash
   rm -rf ~/.m2/tomcat-cache
   mvn tomcat:run
   ```

### Startup Timeout

**Error:**
```
Tomcat failed to start within 120000 ms
```

**Solutions:**

1. **Increase timeout:**
   ```xml
   <configuration>
       <startupTimeout>300000</startupTimeout>
   </configuration>
   ```

2. **Check for application errors in logs:**
   ```bash
   tail -f ~/.m2/tomcat-cache/apache-tomcat-*/logs/catalina.out
   ```

3. **Simplify application for initial test**

## Deployment Issues

### Application Not Found (404)

**Causes:**

1. **Wrong context path** - Check your configuration:
   ```xml
   <configuration>
       <contextPath>/myapp</contextPath>
   </configuration>
   ```
   Access: `http://localhost:8080/myapp`

2. **Build not run** - Ensure application is compiled:
   ```bash
   mvn clean package tomcat:run
   ```

3. **Wrong source directory:**
   ```xml
   <configuration>
       <warSourceDirectory>${project.build.directory}/${project.build.finalName}</warSourceDirectory>
   </configuration>
   ```

### ClassNotFoundException

**Error:**
```
java.lang.ClassNotFoundException: com.example.MyServlet
```

**Solutions:**

1. **Rebuild the application:**
   ```bash
   mvn clean compile tomcat:run
   ```

2. **Check dependencies in `pom.xml`** - ensure `provided` scope for servlet API:
   ```xml
   <dependency>
       <groupId>jakarta.servlet</groupId>
       <artifactId>jakarta.servlet-api</artifactId>
       <version>6.0.0</version>
       <scope>provided</scope>
   </dependency>
   ```

### NoClassDefFoundError for jakarta.*

**Error:**
```
NoClassDefFoundError: jakarta/servlet/http/HttpServlet
```

**Cause:** Using `javax.*` imports with Tomcat 10.1.x/11.x

**Solution:** Migrate to Jakarta namespace:

```java
// Change this:
import javax.servlet.http.HttpServlet;

// To this:
import jakarta.servlet.http.HttpServlet;
```

See [Tomcat Versions](Tomcat-Versions#migration-from-javax-to-jakarta) for migration guide.

## Debug Issues

### Cannot Connect Debugger

**Error:**
```
Unable to open debugger port: Connection refused
```

**Solutions:**

1. **Wait for Tomcat to start** - debug port opens after startup

2. **Verify port is correct:**
   ```bash
   mvn tomcat:debug -Dtomcat.debug.port=5005
   # IDE should connect to port 5005
   ```

3. **Check if port is in use:**
   ```bash
   # Linux/macOS
   lsof -i :5005

   # Windows
   netstat -ano | findstr :5005
   ```

### Breakpoints Not Hit

**Solutions:**

1. **Rebuild and redeploy:**
   ```bash
   mvn clean compile tomcat:debug
   ```

2. **Verify source matches bytecode** - check build output directory

3. **Check breakpoint location** - ensure line is executable code

4. **Verify IDE source path** matches project structure

## Hot Deployment Issues

### Changes Not Detected

**Solutions:**

1. **Verify autopublish is enabled:**
   ```bash
   mvn tomcat:run -Dtomcat.autopublish.enabled=true
   ```

2. **Check source directory:**
   - Default: `target/${project.build.finalName}`
   - IDE must compile to this directory

3. **Ensure IDE auto-build is enabled**

### Memory Issues After Redeploys

**Symptoms:** OutOfMemoryError, slow performance after many redeploys

**Solutions:**

1. **Restart Tomcat periodically:**
   ```bash
   mvn tomcat:stop tomcat:run
   ```

2. **Increase heap size:**
   ```xml
   <configuration>
       <vmOptions>
           <vmOption>-Xmx2048m</vmOption>
       </vmOptions>
   </configuration>
   ```

## Platform-Specific Issues

### Windows: File Locking

**Error:**
```
Unable to delete file: The process cannot access the file
```

**Solutions:**

1. **Wait for file release** - Tomcat may still be shutting down

2. **Use shorter file paths** - avoid deep nesting

3. **Disable antivirus scanning** on project directory

4. **Clean and retry:**
   ```bash
   mvn clean
   ```

### macOS: Permission Denied

**Error:**
```
Permission denied: /opt/tomcat
```

**Solutions:**

1. **Use home directory:**
   ```xml
   <configuration>
       <catalinaHome>${user.home}/tomcat</catalinaHome>
   </configuration>
   ```

2. **Fix permissions:**
   ```bash
   chmod -R 755 /opt/tomcat
   ```

### Linux: Too Many Open Files

**Error:**
```
Too many open files
```

**Solution:** Increase ulimit:
```bash
ulimit -n 65536
```

Or permanently in `/etc/security/limits.conf`:
```
* soft nofile 65536
* hard nofile 65536
```

## Getting More Help

### Enable Verbose Logging

```bash
mvn tomcat:run -X
```

### Check Tomcat Logs

```bash
# Default location
tail -f ~/.m2/tomcat-cache/apache-tomcat-*/logs/catalina.out
```

### Report Issues

1. GitHub Issues: https://github.com/rajendarreddyj/tomcat-maven-plugin/issues
2. Include:
   - Plugin version
   - Tomcat version
   - Java version
   - Full error message
   - Minimal reproduction steps

## See Also

- [Configuration](Configuration) - All parameters
- [FAQ](FAQ) - Frequently asked questions
- [Tomcat Versions](Tomcat-Versions) - Version compatibility
