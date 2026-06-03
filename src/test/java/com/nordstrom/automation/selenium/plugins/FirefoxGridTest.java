package com.nordstrom.automation.selenium.plugins;

import com.nordstrom.automation.selenium.ManagedDriverPlugin;
import com.nordstrom.automation.selenium.plugins.FirefoxPlugin;
import com.nordstrom.utility.AbstractGridTest;

public class FirefoxGridTest extends AbstractGridTest {
    
    private final ManagedDriverPlugin plugin = new FirefoxPlugin();

    @Override
    public ManagedDriverPlugin getPlugin() {
        return plugin;
    }
    
}
