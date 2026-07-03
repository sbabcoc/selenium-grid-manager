package com.nordstrom.automation.selenium.plugins;

import static org.openqa.selenium.json.Json.MAP_TYPE;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.SystemUtils;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.grid.config.ConfigException;
import org.openqa.selenium.json.Json;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nordstrom.automation.selenium.AbstractSeleniumConfig.SeleniumSettings;
import com.nordstrom.automation.selenium.ManagedDriverPlugin;
import com.nordstrom.automation.selenium.SeleniumConfig;
import com.nordstrom.automation.selenium.core.GridUtility;
import com.nordstrom.automation.selenium.core.LocalGridServer;
import com.nordstrom.automation.selenium.core.registration.PM2RegistrationStrategy;
import com.nordstrom.automation.selenium.core.registration.RegistrationStrategy;
import com.nordstrom.automation.selenium.exceptions.GridServerLaunchFailedException;
import com.nordstrom.automation.selenium.utility.NodeBinaryFinder;
import com.nordstrom.common.file.PathUtils;

import net.bytebuddy.implementation.Implementation;

/**
 * This class provides the base plug-in implementation for drivers provided by {@code appium}.
 * <p>
 * All of the Java driver classes associated with this plug-in are contained in a single dependency:
 * 
 * <ul>
 *     <li><b>io.appium.java_client.android.AndroidDriver</b></li>
 *     <li><b>io.appium.java_client.android.IOSDriver</b></li>
 *     <li><b>io.appium.java_client.android.Mac2Driver</b></li>
 *     <li><b>io.appium.java_client.android.WindowsDriver</b></li>
 * </ul>
 * 
 * <pre>&lt;dependency&gt;
 *  &lt;groupId&gt;io.appium&lt;/groupId&gt;
 *  &lt;artifactId&gt;java-client&lt;/artifactId&gt;
 *  &lt;version&gt;10.0.0&lt;/version&gt;
 *  &lt;exclusions&gt;
 *    &lt;exclusion&gt;
 *      &lt;groupId&gt;org.seleniumhq.selenium&lt;/groupId&gt;
 *      &lt;artifactId&gt;selenium-java&lt;/artifactId&gt;
 *    &lt;/exclusion&gt;
 *    &lt;exclusion&gt;
 *      &lt;groupId&gt;org.seleniumhq.selenium&lt;/groupId&gt;
 *      &lt;artifactId&gt;selenium-support&lt;/artifactId&gt;
 *    &lt;/exclusion&gt;
 *    &lt;exclusion&gt;
 *      &lt;groupId&gt;org.slf4j&lt;/groupId&gt;
 *      &lt;artifactId&gt;slf4j-api&lt;/artifactId&gt;
 *    &lt;/exclusion&gt;
 *  &lt;/exclusions&gt;
 *&lt;/dependency&gt;</pre>
 */
public abstract class AbstractAppiumPlugin implements ManagedDriverPlugin {

    private static final String[] DEPENDENCY_CONTEXTS = {};
    private static final String[] APPIUM_PATH_TAIL = { "appium", "build", "lib", "main.js" };
	private static final String[] PROPERTY_NAMES = { SeleniumSettings.APPIUM_CLI_ARGS.key(),
			SeleniumSettings.NPM_BINARY_PATH.key(), SeleniumSettings.NODE_BINARY_PATH.key(),
			SeleniumSettings.PM2_BINARY_PATH.key(), SeleniumSettings.APPIUM_BINARY_PATH.key() };
    
    private static final Class<?>[] ARG_TYPES = {URL.class, Capabilities.class};
    
    private static final Pattern OPTION_PATTERN = Pattern.compile("\\s*(-[a-zA-Z0-9]+|--[a-zA-Z0-9]+(?:-[a-zA-Z0-9]+)*)");
    private static final String APPIUM_HOME = "APPIUM_HOME";
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractAppiumPlugin.class);
    
    private final String browserName;
    
    /**
     * Base constructor for <b>Appium</b> plug-in objects.
     * 
     * @param browserName browser name
     */
    protected AbstractAppiumPlugin(String browserName) {
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
    @SuppressWarnings("unchecked")
    public LocalGridServer create(SeleniumConfig config, int hubPort, String launcherClassName,
            String[] dependencyContexts, URL hubUrl, Path workingPath, Path outputPath) throws IOException {
        
        String address;
        Integer portNum;
        List<String> argsList = new ArrayList<>();
        
        // create node configuration for this plug-in
        Path nodeConfigPath = config.createNodeConfig(getCapabilities(config), hubUrl);
        
        // get path to 'appium' configuration file
        Path appiumConfigPath = config.getAppiumConfigPath();
        // if file path is specified
        if (appiumConfigPath != null) {
            argsList.add("--config-file");
            argsList.add(appiumConfigPath.toString());
        }
        
        // allow specification of multiple command line arguments
        String[] cliArgs = config.getStringArray(SeleniumSettings.APPIUM_CLI_ARGS.key());
        // if args specified
        if (cliArgs != null) {
            int head = 0;
            int tail = 0;
            int next = 0;
            int index = 0;
            boolean doLoop;
            
            // iterate over specifications
            for (String thisArg : cliArgs) {
                doLoop = true;
                Matcher matcher = OPTION_PATTERN.matcher(thisArg);
                
                // until done
                while (doLoop) {
                    // save list end index
                    index = argsList.size();
                    
                    // if option found
                    if (matcher.find()) {
                        // add option to args list
                        argsList.add(matcher.group(1));
                        // set last value tail 
                        tail = matcher.start();
                        // save next value head
                        next = matcher.end() + 1;
                    // otherwise
                    } else {
                        // set final value tail
                        tail = thisArg.length();
                        // set 'done'
                        doLoop = false;
                    }
                    
                    // if maybe value
                    if (head < tail) {
                        // extract potential value, trimming ends
                        String value = thisArg.substring(head, tail).trim();
                        
                        // if value is defined
                        if ( ! value.isEmpty()) {
                            // insert at saved index
                            argsList.add(index, value);
                        }
                    }
                    
                    // advance
                    head = next;
                }
            }
        }
        
        // extract address and port from relay configuration
        try (Reader reader = Files.newBufferedReader(nodeConfigPath)) {
            Map<String, Object> nodeConfig = new Json().toType(reader, MAP_TYPE);
            Map<String, Object> relayOptions = (Map<String, Object>) nodeConfig.get("relay");
            address = (String) relayOptions.get("host");
            portNum = ((Long) relayOptions.get("port")).intValue();
        } catch (IOException e) {
            throw new ConfigException("Failed reading node configuration.", e);
        }
        
        // add driver specification
        argsList.add("--use-drivers");
        argsList.add(getBrowserName().toLowerCase());
        
        // UiAutomator2: enable ChromeDriver auto-download
        if (getBrowserName().equalsIgnoreCase("uiautomator2")) {
            argsList.add(0, "*:chromedriver_autodownload,*:adb_shell");
            argsList.add(0, "--allow-insecure");
        }
        
        // specify server port
        argsList.add(0, portNum.toString());
        argsList.add(0, "--port");
        
        // specify server host
        argsList.add(0, address);
        argsList.add(0, "--address");
        
        ProcessBuilder builder;
        String appiumBinaryPath = findMainScript().getAbsolutePath();
        
        File pm2Binary = findPM2Binary().getAbsoluteFile();
        String winQuote = (SystemUtils.IS_OS_WINDOWS) ? "\"" : "";

        argsList.add(0, "--");
        
        // if capturing output
        if (outputPath != null) {
            // specify 'pm2' log output path
            argsList.add(0, winQuote + outputPath.toString() + winQuote);
            argsList.add(0, "--log");
        }
        
        // specify 'pm2' process name
        argsList.add(0, "appium-" + portNum);
        argsList.add(0, "--name");
        
        // specify path to 'appium' main script 
        argsList.add(0, winQuote + appiumBinaryPath + winQuote);
        argsList.add(0, "start");
        
        String executable;
        if (SystemUtils.IS_OS_WINDOWS) {
            argsList.add(0, "\"" + pm2Binary.getAbsolutePath() + "\"");
            String command = String.join(" ", argsList);
            argsList.clear();
            
            executable = "cmd.exe";
            argsList.add("/c");
            argsList.add("\"" + command + "\"");
        } else {
            executable = pm2Binary.getAbsolutePath();
        }

        argsList.add(0, executable);
        
        builder = new ProcessBuilder(argsList);
        builder.environment().put("PATH", PathUtils.getSystemPath());
        
        // store path to relay configuration in Appium process environment
        builder.environment().put("nodeConfigPath", nodeConfigPath.toString());
        // set APPIUM_HOME to work around auto-detection issue
        builder.environment().put(APPIUM_HOME, Optional.ofNullable(System.getenv(APPIUM_HOME))
                .orElse(System.getProperty("user.home") + File.separator + ".appium"));
        return new AppiumGridServer(address, portNum, false, hubPort, builder, workingPath, null,
                new PM2RegistrationStrategy(4));
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
     * Find the 'npm' (Node Package Manager) binary.
     * 
     * @return path to the 'npm' binary as a {@link File} object
     * @throws GridServerLaunchFailedException if 'npm' isn't found
     */
    private static File findNPMBinary() throws GridServerLaunchFailedException {
        return NodeBinaryFinder.findBinary("npm", SeleniumSettings.NPM_BINARY_PATH, "'npm' package manager");
    }
    
    /**
     * Find the 'pm2' binary.
     * 
     * @return path to the 'pm2' binary as a {@link File} object
     * @throws GridServerLaunchFailedException if 'node' isn't found
     */
    private static File findPM2Binary() throws GridServerLaunchFailedException {
        return NodeBinaryFinder.findBinary("pm2", SeleniumSettings.PM2_BINARY_PATH, "'pm2' process manager");
    }
    
    /**
     * Find the 'appium' main script in the global 'node' modules repository.
     * 
     * @return path path to the 'appium' main script as a {@link File} object
     * @throws GridServerLaunchFailedException if the 'appium' main script isn't found
     */
    private static File findMainScript() throws GridServerLaunchFailedException {
        // check configuration for path to 'appium' main script
        try {
            return NodeBinaryFinder.findBinary("main.js", SeleniumSettings.APPIUM_BINARY_PATH, "'appium' main script");
        } catch (GridServerLaunchFailedException eaten) {
            // path not specified - check modules repository below
        }
        
        // check for 'appium' main script in global 'node' modules repository
        
        String nodeModulesRoot;
        File npm = findNPMBinary().getAbsoluteFile();
        
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
    
    /**
     * This class represents a single Appium node server belonging to a local Grid collection.
     */
    public static class AppiumGridServer extends LocalGridServer {

        /**
         * Constructor for local Grid Appium node server object.
         *
         * @param host IP address of local Grid server
         * @param port port of local Grid server
         * @param isHub role of Grid server being started ({@code true} = hub; {@code false} = node)
         * @param hubPort port of the hub for the Grid collection this server belongs to
         * @param builder {@link ProcessBuilder} for local Grid server process
         * @param workingPath {@link Path} of working directory for server process; {@code null} for default
         * @param outputPath {@link Path} to output log file; {@code null} to decline log-to-file
         * @param registrationStrategy {@link RegistrationStrategy} for registering this server with the sidecar
         */
        public AppiumGridServer(String host, Integer port, boolean isHub, int hubPort, ProcessBuilder builder,
                Path workingPath, Path outputPath, RegistrationStrategy registrationStrategy) {
            super(host, port, isHub, hubPort, builder, workingPath, outputPath, registrationStrategy);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean shutdown() throws InterruptedException {
            if (!isActive()) return true;
            if (shutdownAppiumWithPM2(getUrl())) return true;
            return super.shutdown();
        }
        
        /**
         * Get stored relay node configuration path. <br>
         * <b>NOTE</b>: A relay node is needed to connect the Appium server to a Selenium 4+ Grid hub.
         * 
         * @return path to relay node configuration; may be {@code null}
         */
        public Path getNodeConfigPath() {
            String nodeConfigPath = getEnvironment().get("nodeConfigPath");
            return (nodeConfigPath != null) ? Paths.get(nodeConfigPath) : null;
        }
        
        /**
         * If the specified URL is a local 'appium' node running with 'pm2', delete the process.
         * 
         * @param nodeUrl {@link URL} object for target node server
         * @return {@code true} if node was shut down; otherwise {@code false}
         */
        public static boolean shutdownAppiumWithPM2(URL nodeUrl) {
            if ( ! GridUtility.isLocalHost(nodeUrl)) return false;
            
            int exitCode = 0;
            String executable;
            ProcessBuilder builder;
            List<String> argsList = new ArrayList<>();
            File pm2Binary = findPM2Binary().getAbsoluteFile();

            argsList.add("delete");
            argsList.add("appium-" + nodeUrl.getPort());
            
            if (SystemUtils.IS_OS_WINDOWS) {
                executable = "cmd.exe";
                argsList.add(0, "\"" + pm2Binary.getAbsolutePath() + "\"");
                argsList.add(0, "/c");
            } else {
                executable = pm2Binary.getAbsolutePath();
            }
            
            argsList.add(0, executable);
            builder = new ProcessBuilder(argsList);
            builder.environment().put("PATH", PathUtils.getSystemPath());
            
            try {
                exitCode = builder.start().waitFor();
                LOGGER.debug("Deleted PM2 process: appium-{}", nodeUrl.getPort());
            } catch (IOException e) {
                LOGGER.debug("I/O exception while shutting down PM2-managed Appium node", e);
                exitCode = -1;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.debug("Interrupted while shutting down PM2-managed Appium node", e);
                exitCode = -1;
            }
            
            return exitCode == 0;
        }
    }
}
