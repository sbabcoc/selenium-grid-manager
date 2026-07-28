package com.nordstrom.automation.selenium.utility;

import java.io.*;
import java.nio.file.*;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

/**
 * This class implements the installer for {@code selenium-grid-manager}.
 * To run the installer:
 * <ul>
 *     <li>Download the {@code selenium-grid-manager} JAR</li>
 *     <li>Run the JAR:<br>
 *     {@code $ java -jar selenium-grid-manager-<version>.jar}</li>
 * </ul>
 * This extracts the Gradle build files and wrapper needed to launch,
 * augment, and shut down local Selenium Grid collections, and records
 * the installed artifact's version and Selenium API profile in
 * {@code gradle.properties} so the extracted build resolves dependencies
 * correctly without needing a git repository or manual profile selection.
 */
public class Install {

    private static final String BASE_PATH = "META-INF/gradle/com.nordstrom.ui-tools/selenium-grid-manager/";

    private static final String[] RESOURCES = {
        "build.gradle",
        "grid.gradle",
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
        writeInstallProperties();
    }

    /**
     * Record this JAR's own artifact version and Selenium API profile in
     * {@code gradle.properties}, so the extracted build resolves matching
     * dependency versions and the correct API profile without needing a
     * git repository or manual {@code -Pprofile} selection.
     *
     * @throws IOException if an error occurs writing the properties file
     */
    private static void writeInstallProperties() throws IOException {
        String version = Install.class.getPackage().getImplementationVersion();
        String profile = readManifestAttribute("Selenium-Profile");

        if (version == null && profile == null) {
            System.err.println("Unable to determine artifact version or profile — "
                    + "expected attributes not found in JAR manifest");
            return;
        }

        File propsFile = Paths.get("gradle.properties").toFile();
        StringBuilder toAppend = new StringBuilder();
        String existing = propsFile.exists()
                ? new String(Files.readAllBytes(propsFile.toPath())) : "";

        if (version != null && !existing.contains("artifactVersion=")) {
            toAppend.append("artifactVersion=").append(version).append(System.lineSeparator());
        }
        if (profile != null && !existing.contains("profile=")) {
            toAppend.append("profile=").append(profile).append(System.lineSeparator());
        }

        if (toAppend.length() == 0) {
            System.out.println("gradle.properties already specifies artifactVersion/profile — leaving unchanged");
            return;
        }

        if (propsFile.exists()) {
            Files.write(propsFile.toPath(), toAppend.toString().getBytes(),
                    java.nio.file.StandardOpenOption.APPEND);
        } else {
            Files.write(propsFile.toPath(), toAppend.toString().getBytes());
        }
        System.out.println("Recorded install properties in " + propsFile.getAbsolutePath());
    }

    /**
     * Read the specified attribute from this JAR's own manifest.
     *
     * @param name manifest attribute name
     * @return attribute value; {@code null} if not found or the JAR location cannot be determined
     */
    private static String readManifestAttribute(String name) {
        try {
            String jarPath = Install.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            try (JarFile jarFile = new JarFile(jarPath)) {
                Attributes attrs = jarFile.getManifest().getMainAttributes();
                return attrs.getValue(name);
            }
        } catch (Exception e) {
            return null;
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
