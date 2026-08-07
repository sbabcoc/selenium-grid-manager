package com.nordstrom.automation.selenium.sidecar.servlet;

/**
 * This interface defines the URL paths of the servlets that make up the sidecar servlet container.
 */
public interface SidecarPathName {

    /** path: sidecar control root — parent of all sidecar servlet paths */
    String CONTROL_ROOT = "/grid/control";

    /** path: grid server registration */
    String REGISTER_PATH = "/grid/control/register";

    /** path: grid server deregistration */
    String DEREGISTER_PATH = "/grid/control/deregister";

    /** path: grid collection shutdown */
    String SHUTDOWN_PATH = "/grid/control/shutdown";

    /** path: sidecar stop */
    String STOP_PATH = "/grid/control/stop";

    /** path: sidecar status */
    String STATUS_PATH = "/grid/control/status";

    /** path: sidecar management console */
    String CONSOLE_PATH = "/grid/control/console";
    
    /** path: monitored grid registration */
    String MONITOR_PATH = "/grid/control/monitor";

    /** path: monitored grid removal */
    String UNMONITOR_PATH = "/grid/control/unmonitor";

    /** path: device provisioning (Appium/UiAutomator2) */
    String PROVISION_PATH = "/grid/control/provision";
}
