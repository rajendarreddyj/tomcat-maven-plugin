package io.github.rajendarreddyj.tomcat.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Generates a custom CATALINA_BASE directory with modified server.xml for port
 * configuration.
 * This allows running Tomcat with custom ports without modifying the original
 * installation.
 *
 * @author rajendarreddyj
 * @since 1.0.0
 */
public final class CatalinaBaseGenerator {

    /**
     * Pattern to match HTTP connector port attribute.
     * Captures the port="8080" portion of HTTP connectors for replacement.
     */
    private static final Pattern CONNECTOR_PORT_PATTERN = Pattern.compile(
            "(<Connector[^>]*port=\")8080(\"[^>]*protocol=\"HTTP)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    /**
     * Pattern to match HTTP connector for adding address attribute.
     * Captures the connector element to allow insertion of address attribute.
     */
    private static final Pattern CONNECTOR_ADDRESS_PATTERN = Pattern.compile(
            "(<Connector[^>]*protocol=\"HTTP[^\"]*\"[^>]*)(/?>)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    /**
     * Pattern to match Server element shutdown port.
     * Captures the port="8005" for disabling shutdown port.
     */
    private static final Pattern SERVER_SHUTDOWN_PORT_PATTERN = Pattern.compile(
            "(<Server[^>]*port=\")8005(\")",
            Pattern.CASE_INSENSITIVE);

    /**
     * Pattern to match AJP connector port.
     * Used to identify AJP connectors for commenting out.
     */
    private static final Pattern AJP_PORT_PATTERN = Pattern.compile(
            "(<Connector[^>]*port=\")8009(\"[^>]*protocol=\"AJP)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private CatalinaBaseGenerator() {
        // Utility class
    }

    /**
     * Generates a CATALINA_BASE directory with customized configuration.
     *
     * @param catalinaHome the CATALINA_HOME directory (original Tomcat
     *                     installation)
     * @param catalinaBase the target CATALINA_BASE directory to create
     * @param httpPort     the HTTP port to configure
     * @param httpHost     the HTTP host/address to bind to
     * @throws IOException if an I/O error occurs
     */
    public static void generate(Path catalinaHome, Path catalinaBase, int httpPort, String httpHost)
            throws IOException {
        // Create the base directory structure
        Files.createDirectories(catalinaBase);

        // Create required subdirectories
        Files.createDirectories(catalinaBase.resolve("conf"));
        Files.createDirectories(catalinaBase.resolve("logs"));
        Files.createDirectories(catalinaBase.resolve("temp"));
        Files.createDirectories(catalinaBase.resolve("webapps"));
        Files.createDirectories(catalinaBase.resolve("work"));

        // Copy conf directory contents
        Path sourceConf = catalinaHome.resolve("conf");
        Path targetConf = catalinaBase.resolve("conf");

        if (Files.exists(sourceConf)) {
            try (Stream<Path> paths = Files.walk(sourceConf)) {
                paths.forEach(source -> {
                    try {
                        Path target = targetConf.resolve(sourceConf.relativize(source));
                        if (Files.isDirectory(source)) {
                            Files.createDirectories(target);
                        } else {
                            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to copy config file: " + source, e);
                    }
                });
            }
        }

        // Modify server.xml with custom port settings
        Path serverXml = targetConf.resolve("server.xml");
        if (Files.exists(serverXml)) {
            modifyServerXml(serverXml, httpPort, httpHost);
        }
    }

    /**
     * Modifies server.xml to use custom port and host settings.
     *
     * @param serverXml the path to server.xml
     * @param httpPort  the HTTP port
     * @param httpHost  the HTTP host/address
     * @throws IOException if an I/O error occurs
     */
    private static void modifyServerXml(Path serverXml, int httpPort, String httpHost) throws IOException {
        String content = Files.readString(serverXml);

        // Replace HTTP connector port (8080 -> custom port)
        content = CONNECTOR_PORT_PATTERN.matcher(content)
                .replaceAll("$1" + httpPort + "$2");

        // Add address attribute if host is specified and not localhost/0.0.0.0
        if (httpHost != null && !httpHost.isBlank()
                && !"localhost".equalsIgnoreCase(httpHost)
                && !"0.0.0.0".equals(httpHost)) {
            Matcher addressMatcher = CONNECTOR_ADDRESS_PATTERN.matcher(content);
            if (addressMatcher.find()) {
                String connectorContent = addressMatcher.group(1);
                // Only add address if not already present
                if (!connectorContent.contains("address=")) {
                    content = addressMatcher.replaceFirst("$1 address=\"" + httpHost + "\"$2");
                }
            }
        }

        // Disable shutdown port for security (use -1)
        content = SERVER_SHUTDOWN_PORT_PATTERN.matcher(content)
                .replaceAll("$1-1$2");

        // Comment out AJP connector (typically not needed for development)
        content = content.replaceAll(
                "(<Connector[^>]*protocol=\"AJP[^\"]*\"[^>]*/?>)",
                "<!-- Disabled for plugin use: $1 -->");

        Files.writeString(serverXml, content);
    }

    /**
     * Checks if a CATALINA_BASE already exists and appears valid.
     *
     * @param catalinaBase the CATALINA_BASE path to check
     * @return true if the directory exists and has required structure
     */
    public static boolean isValidCatalinaBase(Path catalinaBase) {
        if (!Files.isDirectory(catalinaBase)) {
            return false;
        }
        // Check for essential directories and files
        return Files.isDirectory(catalinaBase.resolve("conf"))
                && Files.exists(catalinaBase.resolve("conf").resolve("server.xml"));
    }
}
