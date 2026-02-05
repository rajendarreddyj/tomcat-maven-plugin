/**
 * Configuration classes for the Tomcat Maven Plugin.
 *
 * <p>
 * This package contains immutable configuration objects and utilities for
 * configuring the Tomcat server and web application deployments.
 * </p>
 *
 * <h2>Configuration Classes</h2>
 * <ul>
 * <li>{@link io.github.rajendarreddyj.tomcat.config.ServerConfiguration} -
 * Immutable configuration for Tomcat server settings including ports,
 * timeouts, JVM options, and environment variables</li>
 * <li>{@link io.github.rajendarreddyj.tomcat.config.DeployableConfiguration} -
 * Immutable configuration for webapp deployment settings including context
 * path, source directory, and auto-publish options</li>
 * <li>{@link io.github.rajendarreddyj.tomcat.config.TomcatVersion} -
 * Enum representing supported Tomcat versions with download URLs and
 * Java version requirements</li>
 * <li>{@link io.github.rajendarreddyj.tomcat.config.CatalinaBaseGenerator} -
 * Utility for generating custom CATALINA_BASE directories with modified
 * port configuration</li>
 * </ul>
 *
 * <h2>Builder Pattern</h2>
 * <p>
 * Configuration classes use the Builder pattern for construction:
 * </p>
 *
 * <pre>{@code
 * ServerConfiguration config = ServerConfiguration.builder()
 *         .catalinaHome(tomcatPath)
 *         .httpPort(8080)
 *         .httpHost("localhost")
 *         .build();
 * }</pre>
 *
 * @author rajendarreddyj
 * @see io.github.rajendarreddyj.tomcat.config.ServerConfiguration
 * @see io.github.rajendarreddyj.tomcat.config.DeployableConfiguration
 * @see io.github.rajendarreddyj.tomcat.config.TomcatVersion
 * @since 1.0.0
 */
package io.github.rajendarreddyj.tomcat.config;
