package com.nordstrom.automation.selenium.plugins;

import java.lang.reflect.Method;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;

import com.nordstrom.automation.selenium.ManagedDriverPlugin;
import com.nordstrom.automation.selenium.SeleniumConfig;
import com.nordstrom.automation.selenium.core.GridUtility;
import com.nordstrom.automation.selenium.plugins.UiAutomator2Plugin;
import com.nordstrom.automation.selenium.utility.AbstractGridTest;

public class AndroidChromeGridTest extends AbstractGridTest {

    private final ManagedDriverPlugin plugin = new UiAutomator2Plugin();
    
    @Override
    public WebDriver provideDriver(Method method) {
        String personality = plugin.getPersonalities().get(plugin.getBrowserName() + ".chrome");
        
        SeleniumConfig config = SeleniumConfig.getConfig();
        Capabilities[] capabilities = config.getCapabilitiesForJson(personality);
        return GridUtility.getDriver(config.getHubUrl(), capabilities[0]);
    }

    @Override
    public ManagedDriverPlugin getPlugin() {
        return plugin;
    }
}
