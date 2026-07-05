package com.nordstrom.automation.selenium.plugins;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Stream;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;

import com.nordstrom.automation.selenium.ManagedDriverPlugin;
import com.nordstrom.automation.selenium.SeleniumConfig;
import com.nordstrom.automation.selenium.core.IGridServer;
import com.nordstrom.automation.selenium.core.LocalSeleniumGrid;
import com.nordstrom.automation.selenium.core.registration.RegistrationStrategy;

import net.bytebuddy.implementation.Implementation;

/**
 * This class provides the base plug-in implementation for drivers that extend {@code RemoteWebDriver}.
 */
public abstract class RemoteWebDriverPlugin implements ManagedDriverPlugin {
    
    private final String browserName;
    
    /**
     * Base constructor for <b>RemoteWebDriver</b> plug-in objects.
     * 
     * @param browserName browser name
     */
    protected RemoteWebDriverPlugin(String browserName) {
        this.browserName = browserName;
    }
    
    /**
     * Start local Selenium Grid node for this driver.
     * 
     * @param config {@link SeleniumConfig} object
     * @param launcherClassName fully-qualified class name for Grid launcher
     * @param dependencyContexts common dependency contexts for all Grid nodes
     * @param hubUrl Grid hub {@link URL} with which node should register
     * @param workingPath {@link Path} of working directory for server process; {@code null} for default
     * @param outputPath {@link Path} to output log file; {@code null} to decline log-to-file
     * @return {@link IGridServer} object for specified node
     * @throws IOException if an I/O error occurs
     */
    @Override
    public IGridServer create(SeleniumConfig config, int hubPort, String launcherClassName,
            String[] dependencyContexts, URL hubUrl, Path workingPath,
            Path outputPath) throws IOException {
        String[] combinedContexts = combineDependencyContexts(dependencyContexts, this);
        String capabilities = getCapabilities(config);
        Path nodeConfigPath = config.createNodeConfig(capabilities, hubUrl);
        String[] propertyNames = getPropertyNames(capabilities);
        return LocalSeleniumGrid.create(config, launcherClassName, combinedContexts,
                false, -1, hubPort, getRegistrationStrategy(config),
                nodeConfigPath, workingPath, outputPath, propertyNames);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends RemoteWebDriver> Constructor<T> getRemoteWebDriverCtor(Capabilities desiredCapabilities) {
        return null;
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
    public String getBrowserName() {
        return browserName;
    }
    
    /**
     * Combine driver dependency contexts with the specified core Selenium Grid contexts.
     *
     * @param dependencyContexts core Selenium Grid dependency contexts
     * @param driverPlugin driver plug-in from which to acquire dependencies
     * @return combined contexts for Selenium Grid dependencies
     */
    public static String[] combineDependencyContexts(String[] dependencyContexts, ManagedDriverPlugin driverPlugin) {
        return Stream.concat(Arrays.stream(dependencyContexts), Arrays.stream(driverPlugin.getDependencyContexts()))
                .toArray(size -> (String[]) Array.newInstance(String.class, size));
    }
    
    /**
     * Get the {@link RegistrationStrategy} appropriate for the current Selenium API version.
     * <p>
     * For Selenium 3, returns a {@link LifecycleRegistrationStrategy} that uses the
     * {@code LifecycleServlet} to register the node with the sidecar.
     * For Selenium 4, returns a {@link PidNodeRegistrationStrategy} that uses the
     * node process PID to register with the sidecar.
     *
     * @param config {@link SeleniumConfig} object
     * @return {@link RegistrationStrategy} for registering this node server with the sidecar
     */
    private RegistrationStrategy getRegistrationStrategy(final SeleniumConfig config) {
        String className = (config.getVersion() == 3) ?
            "com.nordstrom.automation.selenium.core.registration.LifecycleRegistrationStrategy" :
            "com.nordstrom.automation.selenium.core.registration.PidNodeRegistrationStrategy";
        try {
            return (RegistrationStrategy) Class.forName(className).getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed instantiating RegistrationStrategy: " + className, e);
        }
    }
}
