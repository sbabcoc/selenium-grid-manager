package com.nordstrom.automation.selenium.plugins;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import org.apache.commons.lang3.SystemUtils;
import org.openqa.selenium.net.PortProber;
import com.nordstrom.automation.selenium.AbstractSeleniumConfig.SeleniumSettings;
import com.nordstrom.automation.selenium.SeleniumConfig;
import com.nordstrom.automation.selenium.core.AppiumGridServer;
import com.nordstrom.automation.selenium.core.LocalGridServer;
import com.nordstrom.automation.selenium.core.registration.PM2RegistrationStrategy;
import com.nordstrom.automation.selenium.utility.NodeBinaryFinder;
import com.nordstrom.automation.selenium.utility.HostUtils;
import com.nordstrom.common.file.PathUtils;

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
 *  &lt;version&gt;7.6.0&lt;/version&gt;
 *  &lt;exclusions&gt;
 *    &lt;exclusion&gt;
 *      &lt;groupId&gt;org.seleniumhq.selenium&lt;/groupId&gt;
 *      &lt;artifactId&gt;selenium-java&lt;/artifactId&gt;
 *    &lt;/exclusion&gt;
 *    &lt;exclusion&gt;
 *      &lt;groupId&gt;org.seleniumhq.selenium&lt;/groupId&gt;
 *      &lt;artifactId&gt;selenium-support&lt;/artifactId&gt;
 *    &lt;/exclusion&gt;
 *  &lt;/exclusions&gt;
 *&lt;/dependency&gt;</pre>
 */
public abstract class AbstractAppiumPlugin extends AppiumPluginBase {

    /**
     * Base constructor for <b>Appium</b> plug-in objects.
     * 
     * @param browserName browser name
     */
    protected AbstractAppiumPlugin(String browserName) {
        super(browserName);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public LocalGridServer create(SeleniumConfig config, int hubPort, String launcherClassName,
            String[] dependencyContexts, URL hubUrl, Path workingPath,
            Path outputPath) throws IOException {
        
        String address;
        Integer portNum;
        List<String> argsList = new ArrayList<>();
        
        // create node configuration for this plug-in
        Path nodeConfigPath = config.createNodeConfig(getCapabilities(config), hubUrl);
        
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
        
        // get 'localhost' and free port
        address = HostUtils.getLocalHost();
        portNum = PortProber.findFreePort();
        
        // add 'base-path' argument
        argsList.add("--base-path");
        argsList.add("/wd/hub");
        
        // add 'nodeconfig' path
        argsList.add("--nodeconfig");
        argsList.add(nodeConfigPath.toString());
        
        // specify server port
        argsList.add(0, portNum.toString());
        argsList.add(0, "--port");
        
        // specify server host
        argsList.add(0, address);
        argsList.add(0, "--address");
        
        // UiAutomator2: enable ChromeDriver auto-download
        if (getBrowserName().equalsIgnoreCase("uiautomator2")) {
            argsList.add(0, "*:chromedriver_autodownload,*:adb_shell");
            argsList.add(0, "--allow-insecure");
        }
        
        String appiumBinaryPath = findMainScript().getAbsolutePath();
        
        File pm2Binary = NodeBinaryFinder.findPM2Binary().getAbsoluteFile();
        String winQuote = (SystemUtils.IS_OS_WINDOWS) ? "\"" : "";
        
        argsList.add(0, "--");
        
        // if capturing output
        if (outputPath != null) {
            // specify 'pm2' log output path
            argsList.add(0, winQuote + outputPath + winQuote);
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

        ProcessBuilder builder = new ProcessBuilder(argsList);
        builder.environment().put("PATH", PathUtils.getSystemPath());
        builder.environment().put("nodeConfigPath", nodeConfigPath.toString());
        // set APPIUM_HOME to work around auto-detection issue
        builder.environment().put(APPIUM_HOME, Optional.ofNullable(System.getenv(APPIUM_HOME))
                .orElse(System.getProperty("user.home") + File.separator + ".appium"));
        return new AppiumGridServer(address, portNum, false, hubPort,
                builder, workingPath, outputPath, new PM2RegistrationStrategy(3));
    }
}
