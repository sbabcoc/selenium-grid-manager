package com.nordstrom.automation.selenium.plugins;

import static org.junit.Assert.assertEquals;

import java.util.UUID;

import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.nordstrom.automation.selenium.ManagedDriverPlugin;
import com.nordstrom.automation.selenium.annotations.InitialPage;
import com.nordstrom.automation.selenium.core.SeleniumGrid;
import com.nordstrom.automation.selenium.examples.ExamplePage;
import com.nordstrom.automation.selenium.examples.IOSApplicationEchoScreenView;
import com.nordstrom.automation.selenium.examples.IOSApplicationMainView;
import com.nordstrom.automation.selenium.junit.JUnitBase;
import com.nordstrom.automation.selenium.utility.GridLauncher;
import com.nordstrom.common.file.OSInfo;
import com.nordstrom.common.file.OSInfo.OSType;

@InitialPage(IOSApplicationMainView.class)
public class XCUITestGridTest extends JUnitBase {

    private static SeleniumGrid seleniumGrid = null;
    private final ManagedDriverPlugin plugin = new XCUITestPlugin();

    @BeforeClass
    public static void beforeClass() {
        Assume.assumeTrue(OSInfo.getDefault().getType() == OSType.MACINTOSH);
    }

    @Before
    public void beforeTest() {
        createSeleniumGrid();
        ExamplePage.setHubAsTarget();
    }

    @Test
    public void testEditing() {
        IOSApplicationMainView mainView = getInitialPage();
        IOSApplicationEchoScreenView echoScreen = mainView.openEchoScreen();
        UUID uuid = UUID.randomUUID();
        echoScreen.setSavedMessage(uuid.toString());
        assertEquals(echoScreen.getSavedMessage(), uuid.toString());
    }

    private void createSeleniumGrid() {
        if (seleniumGrid == null) {
            seleniumGrid = createGrid();
        }
    }

    private SeleniumGrid createGrid() {
        return GridLauncher.create(getPlugin());
    }

    private ManagedDriverPlugin getPlugin() {
        return plugin;
    }

}
