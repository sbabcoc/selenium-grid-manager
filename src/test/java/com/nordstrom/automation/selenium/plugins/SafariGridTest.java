package com.nordstrom.automation.selenium.plugins;

import org.junit.Assume;
import com.nordstrom.automation.selenium.ManagedDriverPlugin;
import com.nordstrom.automation.selenium.utility.AbstractGridTest;
import com.nordstrom.common.file.OSInfo;
import com.nordstrom.common.file.OSInfo.OSType;

public class SafariGridTest extends AbstractGridTest {

    private final ManagedDriverPlugin plugin = new SafariPlugin();

    @Override
    public void beforeTest() {
        Assume.assumeTrue(OSInfo.getDefault().getType() == OSType.MACINTOSH);
        super.beforeTest();
    }
    
    @Override
    public ManagedDriverPlugin getPlugin() {
        return plugin;
    }
}
