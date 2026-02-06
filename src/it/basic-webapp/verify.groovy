import java.io.File
import java.nio.file.Files
import java.nio.file.Path

// Check that the build log contains expected output
File buildLog = new File(basedir, "build.log")
assert buildLog.exists() : "Build log file should exist"

String logContent = buildLog.text

// Verify build succeeded
assert logContent.contains("BUILD SUCCESS") : "Build should succeed"

// Verify plugin was invoked
assert logContent.contains("Deploying webapp") || logContent.contains("Webapp deployed") :
    "Plugin should deploy the webapp"

// Verify port configuration: check that generated CATALINA_BASE has correct port
// The httpPort is configured as 18080 in pom.xml
File tomcatCacheDir = new File(basedir, "target/tomcat-cache")
if (tomcatCacheDir.exists()) {
    // Find the generated CATALINA_BASE directory (base-{version}-{port})
    File[] baseDirs = tomcatCacheDir.listFiles { file ->
        file.isDirectory() && file.name.startsWith("base-") && file.name.contains("18080")
    }

    if (baseDirs != null && baseDirs.length > 0) {
        File catalinaBase = baseDirs[0]
        File serverXml = new File(catalinaBase, "conf/server.xml")

        if (serverXml.exists()) {
            String serverXmlContent = serverXml.text

            // Verify the HTTP connector port is set to 18080
            assert serverXmlContent.contains('port="18080"') :
                "server.xml should contain port=18080 for HTTP connector. Content: ${serverXmlContent}"

            // Verify the default port 8080 is not present for HTTP connector
            // Note: 8080 might appear in redirectPort, so we check specifically for HTTP connector
            boolean hasDefaultHttpPort = serverXmlContent =~ /Connector[^>]*port="8080"[^>]*protocol="HTTP/
            boolean hasDefaultHttpPortAlt = serverXmlContent =~ /Connector[^>]*protocol="HTTP[^>]*port="8080"/
            assert !hasDefaultHttpPort && !hasDefaultHttpPortAlt :
                "server.xml should NOT contain port=8080 for HTTP connector"

            println "SUCCESS: Verified server.xml contains correct HTTP port 18080"
        } else {
            println "WARN: server.xml not found at expected location: ${serverXml}"
        }
    } else {
        println "WARN: Could not find generated CATALINA_BASE with port 18080"
    }
} else {
    println "WARN: Tomcat cache directory not found at: ${tomcatCacheDir}"
}

// Verify the build log shows the correct port
assert logContent.contains("18080") : "Build log should mention configured port 18080"

println "Integration test verification completed successfully"
