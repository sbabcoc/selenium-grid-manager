package com.nordstrom.automation.selenium.plugins;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.SystemUtils;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import com.nordstrom.automation.selenium.AbstractSeleniumConfig.SeleniumSettings;
import com.nordstrom.automation.selenium.ManagedDriverPlugin;
import com.nordstrom.automation.selenium.exceptions.GridServerLaunchFailedException;
import com.nordstrom.automation.selenium.utility.NodeBinaryFinder;
import com.nordstrom.common.file.PathUtils;

import net.bytebuddy.implementation.Implementation;

/**
 * This abstract class provides the version-agnostic base implementation for
 * drivers provided by {@code appium}.
 *
 * @since 36.0.0
 */
public abstract class AppiumPluginBase implements ManagedDriverPlugin {

    static final String[] DEPENDENCY_CONTEXTS = {};
    static final String[] APPIUM_PATH_TAIL = { "appium", "build", "lib", "main.js" };
    static final String[] PROPERTY_NAMES = { SeleniumSettings.APPIUM_CLI_ARGS.key(),
            SeleniumSettings.NPM_BINARY_PATH.key(), SeleniumSettings.NODE_BINARY_PATH.key(),
            SeleniumSettings.PM2_BINARY_PATH.key(), SeleniumSettings.APPIUM_BINARY_PATH.key() };

    static final Class<?>[] ARG_TYPES = { URL.class, Capabilities.class };

    static final Pattern OPTION_PATTERN = Pattern.compile("\\s*(-[a-zA-Z0-9]+|--[a-zA-Z0-9]+(?:-[a-zA-Z0-9]+)*)");
    static final String APPIUM_HOME = "APPIUM_HOME";

    private final String browserName;

    /**
     * Base constructor for Appium plug-in objects.
     *
     * @param browserName browser name
     */
    protected AppiumPluginBase(String browserName) {
        this.browserName = browserName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getDependencyContexts() {
        return DEPENDENCY_CONTEXTS;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getBrowserName() {
        return browserName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getPropertyNames(String capabilities) {
        return PROPERTY_NAMES;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Implementation getWebElementCtor(WebDriver driver, Class<? extends WebElement> refClass) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T extends RemoteWebDriver> Constructor<T> getRemoteWebDriverCtor(Capabilities desiredCapabilities) {
        String automationName = (String) desiredCapabilities.getCapability("appium:automationName");
        if (automationName == null) {
            automationName = (String) desiredCapabilities.getCapability("automationName");
        }
        if (getBrowserName().equalsIgnoreCase(automationName)) {
            try {
                return (Constructor<T>) Class.forName(getDriverClassName()).getConstructor(ARG_TYPES);
            } catch (SecurityException | ClassNotFoundException | NoSuchMethodException | ClassCastException eaten) {
                // nothing to do here
            }
        }
        return null;
    }

    /**
     * Get the name of the {@link WebDriver} implementation for this plug-in.
     *
     * @return driver-specific {@link WebDriver} class name
     */
    public abstract String getDriverClassName();

    /**
     * Parse the specified CLI argument specifications into a list of arguments.
     *
     * @param cliArgs array of CLI argument specifications; may be {@code null}
     * @return list of parsed CLI arguments
     */
    protected static List<String> parseCliArgs(String[] cliArgs) {
        List<String> argsList = new ArrayList<>();
        if (cliArgs != null) {
            int head = 0;
            int tail = 0;
            int next = 0;
            int index = 0;
            boolean doLoop;

            for (String thisArg : cliArgs) {
                doLoop = true;
                Matcher matcher = OPTION_PATTERN.matcher(thisArg);

                while (doLoop) {
                    index = argsList.size();

                    if (matcher.find()) {
                        argsList.add(matcher.group(1));
                        tail = matcher.start();
                        next = matcher.end() + 1;
                    } else {
                        tail = thisArg.length();
                        doLoop = false;
                    }

                    if (head < tail) {
                        String value = thisArg.substring(head, tail).trim();
                        if (!value.isEmpty()) {
                            argsList.add(index, value);
                        }
                    }

                    head = next;
                }
            }
        }
        return argsList;
    }

    /**
     * Find the 'appium' main script in the global 'node' modules repository.
     *
     * @return path to the 'appium' main script as a {@link File} object
     * @throws GridServerLaunchFailedException if the 'appium' main script isn't found
     */
    protected static File findMainScript() throws GridServerLaunchFailedException {
        // check configuration for path to 'appium' main script
        try {
            return NodeBinaryFinder.findBinary("main.js", SeleniumSettings.APPIUM_BINARY_PATH, "'appium' main script");
        } catch (GridServerLaunchFailedException eaten) {
            // path not specified - check modules repository below
        }

        // check for 'appium' main script in global 'node' modules repository
        String nodeModulesRoot;
        File npm = NodeBinaryFinder.findNPMBinary().getAbsoluteFile();

        String executable;
        List<String> argsList = new ArrayList<>();

        if (SystemUtils.IS_OS_WINDOWS) {
            executable = "cmd.exe";
            argsList.add("/c");
            argsList.add("\"" + npm.getAbsolutePath() + "\"");
        } else {
            executable = npm.getAbsolutePath();
        }

        argsList.add("root");
        argsList.add("-g");

        argsList.add(0, executable);
        ProcessBuilder builder = new ProcessBuilder(argsList);
        builder.environment().put("PATH", PathUtils.getSystemPath());

        try {
            Process process = builder.start();
            process.waitFor();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                nodeModulesRoot = reader.lines().parallel().collect(Collectors.joining("\n"));
            }

            int index = nodeModulesRoot.lastIndexOf('\n');
            if (index > 0) nodeModulesRoot = nodeModulesRoot.substring(index).trim();
            File appiumMain = Paths.get(nodeModulesRoot, APPIUM_PATH_TAIL).toFile();
            if (appiumMain.exists()) return appiumMain;
            throw NodeBinaryFinder.fileNotFound("'appium' main script", SeleniumSettings.APPIUM_BINARY_PATH);
        } catch (IOException cause) {
            throw new GridServerLaunchFailedException("node", cause);
        } catch (InterruptedException cause) {
            Thread.currentThread().interrupt();
            throw new GridServerLaunchFailedException("node", cause);
        }
    }
}
