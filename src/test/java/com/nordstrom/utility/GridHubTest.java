package com.nordstrom.utility;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.json.Json;

import com.nordstrom.automation.selenium.SeleniumConfig;
import com.nordstrom.automation.selenium.core.GridUtility;
import com.nordstrom.automation.selenium.core.SeleniumGrid;

public class GridHubTest {

    private static final String HUB_STATUS = "/status";
    
    @BeforeClass
    public static void beforeClass() {
        try {
        	getSeleniumGrid().activate();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted activating local grid instance", e);
        } catch (IOException | TimeoutException e) {
            throw new IllegalStateException("Failed activating local grid instance", e);
		}
    }

	@Test
    @SuppressWarnings("unchecked")
    public void testBasicPage() throws IOException {
        URL hubUrl = getSeleniumGrid().getHubServer().getUrl();
        String json = queryHub(hubUrl);
        Map<String, Object> response = new Json().toType(json, Map.class);
        assertTrue(response.containsKey("value"));
        Map<String, Object> value = (Map<String, Object>) response.get("value");
        assertTrue(value.containsKey("ready"));
        assertTrue((boolean) value.get("ready"));
    }
	
	private static SeleniumGrid getSeleniumGrid() {
		return SeleniumConfig.getConfig().getSeleniumGrid();
	}
    
    private static String queryHub(URL hubUrl) throws IOException {
        String json;
        String url = hubUrl.getProtocol() + "://" + hubUrl.getAuthority() + HUB_STATUS;
        try (InputStream is = new URL(url).openStream()) {
            json = GridUtility.readAvailable(is);
        }
        return json;
    }
    
    @AfterClass
    public static void afterClass() throws InterruptedException {
        getSeleniumGrid().shutdown();
    }
    
}
