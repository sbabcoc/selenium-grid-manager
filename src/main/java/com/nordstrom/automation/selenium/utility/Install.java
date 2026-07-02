package com.nordstrom.automation.selenium.utility;

import java.io.*;
import java.nio.file.*;

/**
 * This class implements the installer for {@code selenium-grid-manager}.
 * To run the installer:
 * <ul>
 *     <li>Download the {@code selenium-grid-manager} JAR</li>
 *     <li>Run the JAR:<br>
 *     {@code $ java -jar selenium-grid-manager-<version>.jar}</li>
 * </ul>
 * This extracts the Gradle build files and wrapper needed to launch,
 * augment, and shut down local Selenium Grid collections.
 */
public class Install {

    private static final String BASE_PATH = "META-INF/gradle/com.nordstrom.ui-tools/selenium-grid-manager/";

    private static final String[] RESOURCES = {
        "build.gradle",
        "settings.gradle",
        "selenium4Deps.gradle",
        "selenium3Deps.gradle",
        "gradlew",
        "gradlew.bat",
        "gradle/wrapper/gradle-wrapper.jar",
        "gradle/wrapper/gradle-wrapper.properties"
    };

    /**
     * This is the main entry point for the {@code selenium-grid-manager} installer.
     *
     * @param args (unused) command line arguments
     * @throws IOException An error occurred trying to extract a build file.
     */
    public static void main(String... args) throws IOException {
        for (String resource : RESOURCES) {
            File target = Paths.get(resource).toFile();
            if (target.getParentFile() != null) {
                target.getParentFile().mkdirs();
            }
            if (target.createNewFile()) {
                try (InputStream is = Install.class.getClassLoader()
                        .getResourceAsStream(BASE_PATH + resource);
                     OutputStream os = new FileOutputStream(target)) {
                    if (is == null) {
                        System.err.println("Resource not found: " + resource);
                        continue;
                    }
                    copy(is, os);
                }
                // make gradlew executable on Unix
                if (resource.equals("gradlew")) {
                    target.setExecutable(true);
                }
                System.out.println("Extracted: " + target.getAbsolutePath());
            } else {
                System.out.println("Already exists: " + target.getAbsolutePath());
            }
        }
    }

    private static void copy(InputStream source, OutputStream target) throws IOException {
        byte[] buf = new byte[8192];
        int length;
        while ((length = source.read(buf)) > 0) {
            target.write(buf, 0, length);
        }
    }
}
