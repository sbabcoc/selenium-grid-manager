package com.nordstrom.automation.selenium.utility;

import static com.google.common.base.Preconditions.checkState;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import com.nordstrom.automation.selenium.AbstractSeleniumConfig.SeleniumSettings;
import com.nordstrom.automation.selenium.exceptions.GridServerLaunchFailedException;
import com.nordstrom.common.file.PathUtils;

/**
 * Utility for locating Node.js-ecosystem executables (npm, node, pm2, appium)
 * on the system PATH or at a configured location.
 *
 * @since [next-major]
 */
public class NodeBinaryFinder {

    /**
     * Private constructor to prevent instantiation.
     */
    private NodeBinaryFinder() {
        throw new AssertionError("NodeBinaryFinder is a static utility class that cannot be instantiated");
    }

    /**
     * Find the specified binary.
     * 
     * @param exeName file name of binary to find
     * @param setting associated configuration setting
     * @param what human-readable description of binary
     * @return path to specified binary as a {@link File} object
     * @throws GridServerLaunchFailedException if specified binary isn't found
     */
    public static File findBinary(String exeName, SeleniumSettings setting, String what)
            throws GridServerLaunchFailedException {
        try {
            return findBinary(exeName, setting.key());
        } catch (IllegalStateException eaten) {
            IOException cause = fileNotFound(what, setting);
            throw new GridServerLaunchFailedException("node", cause);
        }
    }

    /**
    * Find the specified executable file.
    * 
    * @param exeName Name of the executable file to look for in PATH
    * @param exeProperty Name of a system property that specifies the path to the executable file
    * @return The specified executable as a {@link File} object
    * @throws IllegalStateException if the executable is not found or cannot be executed
    */
    public static File findBinary(String exeName, String exeProperty) {
        String defaultPath = PathUtils.findExecutableOnSystemPath(exeName);
        String exePath = System.getProperty(exeProperty, defaultPath);
        checkState(exePath != null,
                "The path to the driver executable must be set by the %s system property",
                exeProperty);

        File exe = new File(exePath);
        checkExecutable(exe);
        return exe;
    }
    
    /**
     * Assemble a 'file not found' exception for the indicated binary.
     * 
     * @param what human-readable description of binary
     * @param setting associated configuration setting
     * @return {@link FileNotFoundException} object
     */
    public static IOException fileNotFound(String what, SeleniumSettings setting) {
        String template = "%s not found; configure the %s setting (key: %s)";
        return new FileNotFoundException(String.format(template, what, setting.name(), setting.key()));
    }

    /**
     * Ensure that the specified object exists as an executable file.
     * 
     * @param exe executable to check as a {@link File} object
     * @throws IllegalStateException if the executable is not found or cannot be executed
     */
    protected static void checkExecutable(File exe) {
        checkState(exe.exists(), "Specified file does not exist: %s", exe.getAbsolutePath());
        checkState(!exe.isDirectory(), "Specified file is a directory: %s", exe.getAbsolutePath());
        checkState(exe.canExecute(), "Specified file is not executable: %s", exe.getAbsolutePath());
    }
}
