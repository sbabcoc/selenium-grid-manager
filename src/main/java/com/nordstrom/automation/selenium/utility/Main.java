package com.nordstrom.automation.selenium.utility;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.apache.commons.configuration2.ex.ConfigurationException;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.nordstrom.automation.selenium.ManagedDriverPlugin;
import com.nordstrom.automation.selenium.SeleniumConfig;
import com.nordstrom.automation.selenium.AbstractSeleniumConfig.SeleniumSettings;
import com.nordstrom.automation.selenium.core.GridServer;
import com.nordstrom.automation.selenium.core.LocalSeleniumGrid;
import com.nordstrom.automation.selenium.core.SeleniumGrid;
import com.nordstrom.automation.selenium.sidecar.SidecarClient;
import com.nordstrom.automation.selenium.sidecar.SidecarManager;
import com.nordstrom.common.file.PathUtils;
import com.nordstrom.common.uri.UriUtils;

/**
 * This class implements the command line interface for {@code selenium-grid-manager}.
 */
public class Main {

    /** system property marking a re-invocation of this class as the detached grid/sidecar worker */
    private static final String DETACHED_PROPERTY = "selenium.grid.detached";

    /**
     * This is the main entry point for the {@code selenium-grid-manager} command line interface. From here, you're
     * able to launch, augment, and shut down local Selenium Grid collections.
     *
     * @param args command line arguments
     * @throws ConfigurationException A failure was encountered while initializing the configuration object.
     * @throws IOException Thrown to indicate one of the following: <ul>
     *     <li>A failure was encountered while reading from a configuration input stream;</li>
     *     <li>A malformed host URL was synthesized;</li>
     *     <li>An error occurred during Selenium Grid creation;</li>
     *     <li>An error occurred during driver plug-in creation;</li>
     *     <li>An error occurred during Grid server startup;</li>
     * </ul>
     * @throws InterruptedException Interrupted during Grid creation, startup, or activation.
     * @throws TimeoutException Timed out during Grid creation, startup, or activation.
     * @throws InstantiationException Browser plug-in instantiation failed.
     * @throws IllegalAccessException Access to browser plug-in constructor was denied.
     * @throws InvocationTargetException An exception was thrown during browser plug-in instantiation.
     * @throws NoSuchMethodException Browser plug-in no-argument constructor was not found.
     * @throws ClassNotFoundException Specified browser plug-in class was not found.
     */
    public static void main(String... args) throws ConfigurationException, IOException, InterruptedException,
            TimeoutException, InstantiationException, IllegalAccessException, InvocationTargetException,
            NoSuchMethodException, ClassNotFoundException {

        LocalGridOptions opts = new LocalGridOptions();
        JCommander parser = new JCommander(opts);
        try {
            parser.parse(args);
        } catch (ParameterException e) {
            parser.getConsole().println(e.getMessage());
            opts.setHelp();
        }
        if (opts.isHelp()) {
            parser.setProgramName("selenium-grid-manager");
            parser.usage();
            return;
        }
        opts.injectSettings();
        SeleniumConfig config = new SeleniumConfig();
        URL hubUrl = config.getHubUrl();
        if (hubUrl == null) {
            int hubPort = config.getInt(SeleniumSettings.HUB_PORT.key());
            String[] pathAndParams = config.isW3C() ? new String[] {} : new String[] {"/wd/hub"};
            hubUrl = UriUtils.makeBasicURI("http", HostUtils.getLocalHost(), hubPort, pathAndParams).toURL();
        }
        boolean isActive = GridServer.isHubActive(hubUrl);

        if (opts.doShutdown()) {
            if (isActive) {
                parser.getConsole().println("Shutting down active grid at: " + hubUrl.toString());
                SidecarClient.shutdown(hubUrl.getPort());
            }
            return;
        }

        if (isActive) {
            parser.getConsole().println("Adding local nodes to grid at: " + hubUrl.toString());
            SeleniumGrid grid = SeleniumGrid.create(config, hubUrl);
            GridServer hubServer = grid.getHubServer();
            List<GridServer> nodeServers = new ArrayList<>();

            for (String pluginName : opts.getPlugins()) {
                Object plugin = Class.forName(pluginName).getConstructor().newInstance();
                nodeServers.add(ManagedDriverPlugin.class.cast(plugin).create(config, hubUrl));
            }

            for (GridServer nodeServer : nodeServers) {
                nodeServer.start();
            }

            LocalSeleniumGrid.awaitGridReady(hubServer, nodeServers);
            SeleniumGrid.create(config, hubUrl);
            parser.getConsole().println(hubUrl.toString());
            return;
        }

        if (Boolean.getBoolean(DETACHED_PROPERTY)) {
            // this JVM is the detached worker — actually create and activate the grid,
            // then block here (keeping the in-process sidecar alive) until explicitly stopped
            parser.getConsole().println("Creating new local grid at: " + hubUrl.toString());
            SeleniumGrid grid = LocalSeleniumGrid.create(config, hubUrl);
            hubUrl = grid.getHubServer().getUrl();
            grid.activate();
            parser.getConsole().println(hubUrl.toString());
            SidecarManager.awaitTermination();
            return;
        }

        // launcher process — fork a detached worker to host the grid + sidecar, then return
        hubUrl = launchDetached(args, hubUrl);
        parser.getConsole().println(hubUrl.toString());
    }

    /**
     * Fork a detached JVM that re-invokes {@link #main(String...)} as the grid/sidecar worker,
     * then wait for the resulting grid hub to become active before returning.
     *
     * @param args original command line arguments
     * @param hubUrl resolved hub {@link URL} the detached worker is expected to bind
     * @return the same {@code hubUrl}, once confirmed active
     * @throws IOException if the detached process fails to launch
     * @throws InterruptedException if interrupted while waiting for the hub to become active
     * @throws TimeoutException if the hub does not become active within the configured timeout
     */
    private static URL launchDetached(String[] args, URL hubUrl)
            throws IOException, InterruptedException, TimeoutException {
        List<String> command = new ArrayList<>();
        command.add(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
        command.add("-D" + DETACHED_PROPERTY + "=true");
        command.add("-D" + SeleniumSettings.HUB_PORT.key() + "=" + hubUrl.getPort());
        for (String name : System.getProperties().stringPropertyNames()) {
            if (!name.equals(DETACHED_PROPERTY) && !name.equals(SeleniumSettings.HUB_PORT.key())) {
                command.add("-D" + name + "=" + System.getProperty(name));
            }
        }
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add(Main.class.getName());
        command.addAll(Arrays.asList(args));

        File logFile = File.createTempFile("selenium-grid-manager-", ".log");
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectOutput(ProcessBuilder.Redirect.to(logFile));
        pb.redirectErrorStream(true);
        pb.environment().put("PATH", PathUtils.getSystemPath());
        pb.start();
        // intentionally not calling waitFor() — this process is meant to outlive the launcher

        SeleniumConfig config = SeleniumConfig.getConfig();
        long maxWait = config.getLong(SeleniumSettings.HOST_TIMEOUT.key()) * 1000;
        long maxTime = System.currentTimeMillis() + maxWait;
        while (!GridServer.isHubActive(hubUrl)) {
            if (System.currentTimeMillis() > maxTime) {
                throw new TimeoutException("Timed out waiting for detached grid process to become active; see "
                        + logFile.getAbsolutePath());
            }
            Thread.sleep(100);
        }
        return hubUrl;
    }
}
