/**
 * Main package for the Tomcat Maven Plugin.
 *
 * <p>
 * This package contains the Maven Mojo implementations that provide the
 * plugin's
 * goals for running, starting, stopping, and deploying web applications to
 * Apache Tomcat.
 * </p>
 *
 * <h2>Available Goals</h2>
 * <table border="1">
 * <caption>Plugin Goals</caption>
 * <tr>
 * <th>Goal</th>
 * <th>Description</th>
 * </tr>
 * <tr>
 * <td>{@code tomcat:run}</td>
 * <td>Runs Tomcat in foreground mode</td>
 * </tr>
 * <tr>
 * <td>{@code tomcat:start}</td>
 * <td>Starts Tomcat in background mode</td>
 * </tr>
 * <tr>
 * <td>{@code tomcat:stop}</td>
 * <td>Stops a running Tomcat instance</td>
 * </tr>
 * <tr>
 * <td>{@code tomcat:deploy}</td>
 * <td>Deploys the webapp to a running Tomcat</td>
 * </tr>
 * </table>
 *
 * <h2>Supported Tomcat Versions</h2>
 * <ul>
 * <li>Tomcat 10.1.x (Jakarta EE 10, requires Java 11+)</li>
 * <li>Tomcat 11.x (Jakarta EE 11, requires Java 17+)</li>
 * </ul>
 *
 * <h2>Key Features</h2>
 * <ul>
 * <li>Hot code deployment via Maven</li>
 * <li>Automatic Tomcat download if not installed locally</li>
 * <li>Environment variable and JVM options configuration</li>
 * <li>Context path and port configuration</li>
 * <li>Auto-publish with file change detection</li>
 * </ul>
 *
 * @author rajendarreddyj
 * @see io.github.rajendarreddyj.tomcat.RunMojo
 * @see io.github.rajendarreddyj.tomcat.StartMojo
 * @see io.github.rajendarreddyj.tomcat.StopMojo
 * @see io.github.rajendarreddyj.tomcat.DeployMojo
 * @since 1.0.0
 */
package io.github.rajendarreddyj.tomcat;
