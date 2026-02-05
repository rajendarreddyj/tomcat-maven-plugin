/**
 * Tomcat process lifecycle management for the Tomcat Maven Plugin.
 *
 * <p>
 * This package contains classes responsible for managing the Tomcat process
 * lifecycle, including starting, stopping, and monitoring the server process.
 * </p>
 *
 * <h2>Lifecycle Operations</h2>
 * <ul>
 * <li><strong>Start</strong> - Launch Tomcat as a foreground or background
 * process</li>
 * <li><strong>Stop</strong> - Gracefully shutdown Tomcat with configurable
 * timeout</li>
 * <li><strong>Monitor</strong> - Wait for server readiness and handle shutdown
 * signals</li>
 * </ul>
 *
 * <h2>Classes</h2>
 * <ul>
 * <li>{@link io.github.rajendarreddyj.tomcat.lifecycle.TomcatLauncher} -
 * Manages Tomcat process lifecycle, environment configuration, and
 * startup/shutdown</li>
 * </ul>
 *
 * <h2>Process Management</h2>
 * <p>
 * The launcher uses the catalina.sh (Unix) or catalina.bat (Windows) scripts
 * to start and stop Tomcat. Environment variables like CATALINA_HOME,
 * CATALINA_BASE,
 * JAVA_HOME, and CATALINA_OPTS are configured before process launch.
 * </p>
 *
 * <h2>Shutdown Handling</h2>
 * <p>
 * When running in foreground mode, a JVM shutdown hook is registered to ensure
 * graceful Tomcat termination when the user presses Ctrl+C or the process is
 * terminated.
 * </p>
 *
 * @author rajendarreddyj
 * @see io.github.rajendarreddyj.tomcat.lifecycle.TomcatLauncher
 * @since 1.0.0
 */
package io.github.rajendarreddyj.tomcat.lifecycle;
