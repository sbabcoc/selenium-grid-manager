package com.nordstrom.automation.selenium.plugins;

import java.lang.reflect.Method;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;

import com.nordstrom.automation.selenium.ManagedDriverPlugin;
import com.nordstrom.automation.selenium.SeleniumConfig;
import com.nordstrom.automation.selenium.core.GridUtility;
import com.nordstrom.automation.selenium.plugins.XCUITestPlugin;
import com.nordstrom.common.file.OSInfo;
import com.nordstrom.common.file.OSInfo.OSType;
import com.nordstrom.utility.AbstractGridTest;

public class IOSSafariGridTest extends AbstractGridTest {

    private final ManagedDriverPlugin plugin = new XCUITestPlugin();
    
    @BeforeClass
    public static void beforeClass() {
        Assume.assumeTrue(OSInfo.getDefault().getType() == OSType.MACINTOSH);
    }

    @Override
    public WebDriver provideDriver(Method method) {
        String personality = plugin.getPersonalities().get(plugin.getBrowserName() + ".safari");
        
        SeleniumConfig config = SeleniumConfig.getConfig();
        Capabilities[] capabilities = config.getCapabilitiesForJson(personality);
        return GridUtility.getDriver(config.getHubUrl(), capabilities[0]);
    }

    @Override
    public ManagedDriverPlugin getPlugin() {
        return plugin;
    }
}
