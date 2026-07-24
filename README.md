[![Maven Central](https://img.shields.io/maven-central/v/com.nordstrom.ui-tools/selenium-grid-manager.svg)](https://central.sonatype.com/search?q=com.nordstrom.ui-tools+selenium-grid-manager&core=gav)

# selenium-grid-manager

Built on [Selenium Foundation](https://github.com/sbabcoc/Selenium-Foundation), this project provides standalone local **Selenium Grid** instance management, including launch, augmentation, and shutdown of hub and node servers.

## Implementation Strategy

Unlike other projects with similar objectives, `selenium-grid-manager` simplifies the process of launching **Selenium Grid** collections by leveraging the power of **Gradle** to marshal the dependencies required by the specified grid configuration. Instead of lumping everything together in a massive "uber-JAR", the build configurations defined in this project declare the dependencies of hub and node servers, including a bit of glue to configure and launch these servers.

This approach yields several benefits:
* To install, just download the `selenium-grid-manager` JAR and run it:
  * `java -jar selenium-grid-manager-<version>-s4.jar`
  * **NOTE**: The `maven-central` badge above links to the latest release.
* Because all dependencies are managed individually, remediation of defects and vulnerabilities is easy.
* Your installation gets the dependencies it needs, without getting bulked up with unused extras.

The task of launching the grid servers is performed by the **Gradle** `runGrid` task, which executes the Java command line application implemented in the **Main** class. The Gradle build configuration defines per-browser runtime configurations (one for each supported browser), and it's these configurations that activate the dependency declarations required by their respective grid node servers.

## System Requirements

* As indicated above, `selenium-grid-manager` relies on **Gradle** to manage dependencies and execute the Java command line application that launches the specified grid collection. The Gradle wrapper is included in the installation and no separate Gradle installation is required.
* To run pre-built `selenium-grid-manager` modules, you'll need a Java 17+ runtime environment.
* If you want to explore the code and build it locally, you'll need a `git` client to clone the repository and a Java 17+ development kit to build the project.


## Automatic Installation of Drivers 

Since the release of **Selenium Foundation** [28.0.0](https://github.com/sbabcoc/Selenium-Foundation/releases/tag/v28.0.0), we now use **Selenium Manager** (Selenium 4) and **Web Driver Manager** (Selenium 3) to acquire compatible drivers for the browsers targeted by your tests. If the manager is unable to locate or download a required driver, **DriverExecutableNotFoundException** is thrown.

This feature does not include management of Appium [automation engines](docs/ConfiguringProjectSettings.md#appium-automation-engine-support), which must be installed separately.

**NOTE**: This driver acquisition process is bypassed for test classes that implement the [DriverProvider](docs/ConfiguringProjectSettings.md#testing-with-non-default-browser-sessions) interface.

## Requirements for Appium

Unlike the other drivers supported by `selenium-grid-manager` which are implemented in Java, the "engines" provided by [Appium](https://appium.io) are implemented in NodeJS. To launch a **Selenium Grid** collection that includes Appium nodes, you'll need the following additional tools:
* Platform-specific Node Version Manager: The installation page for `npm` (below) provides links to recommended version managers.
* [NodeJS (node)](https://nodejs.org): Currently, I'm running version 22.7.0
* [Node Package Manager (npm)](https://docs.npmjs.com/downloading-and-installing-node-js-and-npm): Currently, I'm running version 10.8.2
* [Node Process Manager (pm2)](https://pm2.io/): Currently, I'm running version 5.4.2
* [Appium](https://appium.io): Currently, I'm running version 2.11.3

Typically, these tools must be on the system file path. However, you can provide specific paths for each of these via **Selenium Foundation** settings:
* **NPM_BINARY_PATH**: If unspecified, the `PATH` is searched
* **NODE_BINARY_PATH**: If unspecified, the `NODE_BINARY_PATH` environment variable is checked; if this is undefined, the `PATH` is searched
* **PM2_BINARY_PATH**: If unspecified, the `PATH` is searched
* **APPIUM_BINARY_PATH**: If unspecified, the `APPIUM_BINARY_PATH` environment variable is checked; if this is undefined, the `PATH` is searched

## Installation

Download the JAR and run it to extract the Gradle build files:

```bash
java -jar selenium-grid-manager-36.0.2-s4.jar
```

This extracts the following files into the current directory:
* `build.gradle`
* `settings.gradle`
* `selenium4Deps.gradle`
* `selenium3Deps.gradle`
* `gradlew`
* `gradlew.bat`
* `gradle/wrapper/gradle-wrapper.jar`
* `gradle/wrapper/gradle-wrapper.properties`

## Launch a Local Grid

In one step, you can launch a Selenium Grid hub and a single node that supplies **HtmlUnit** browser sessions:

```bash
./gradlew runGrid -Pbrowsers=htmlunit
```

To launch a grid that provides multiple browser types, specify a comma-delimited list of browsers:

```bash
./gradlew runGrid -Pbrowsers=chrome,firefox
```

## Augment an Active Grid

In addition to its ability to launch a Selenium Grid collection, `selenium-grid-manager` enables you to add nodes to an existing active Grid. This can either extend the set of supported browsers or provide additional sessions of browsers that are already supported. For example:

```bash
./gradlew runGrid -Pbrowsers=chrome    # launch a grid providing Chrome sessions
./gradlew runGrid -Pbrowsers=firefox   # attach a node providing Firefox sessions
./gradlew runGrid -Pbrowsers=chrome    # attach a second node providing Chrome
```

## Supported Browsers

| Browser | Plugin |
|:--:|--|
| `chrome` | com.nordstrom.automation.selenium.plugins.ChromePlugin |
| `edge` | com.nordstrom.automation.selenium.plugins.EdgePlugin |
| `espresso` | com.nordstrom.automation.selenium.plugins.EspressoPlugin |
| `firefox` | com.nordstrom.automation.selenium.plugins.FirefoxPlugin |
| `htmlunit` | com.nordstrom.automation.selenium.plugins.HtmlUnitPlugin |
| `mac2` | com.nordstrom.automation.selenium.plugins.Mac2Plugin |
| `safari` | com.nordstrom.automation.selenium.plugins.SafariPlugin |
| `uiautomator2` | com.nordstrom.automation.selenium.plugins.UiAutomator2Plugin |
| `windows` | com.nordstrom.automation.selenium.plugins.WindowsPlugin |
| `xcuitest` | com.nordstrom.automation.selenium.plugins.XCUITestPlugin |

## Shut Down a Local Grid

To shut down an active local grid instance:

```bash
./gradlew runGrid -Pexec.args="-shutdown"
```

**NOTE**: Appium local grid nodes will be shut down by this command, but the `pm2` process manager will remain active. This is due to its meager resource consumption and the possibility that it may be managing other processes.

## Command Line Options

The `runGrid` task accepts command line options via the `exec.args` property. Here's the list of supported options:

* `-port` : specify port for local hub server (default = 4445)
* `-plugins` : path-delimited list of fully-qualified node plugin classes
* `-gridServlets` : comma-delimited list of fully-qualified servlet classes to install
* `-workingDir` : working directory for servers
* `-logsFolder` : server output logs folder (default = "logs")
* `-noRedirect` : disable server output redirection (default = `false`)
* `-shutdown` : shut down active local Grid collection

For example, to add support for the hub status API, specify the corresponding servlets:

```bash
./gradlew runGrid -Pexec.args="-gridServlets org.openqa.grid.web.servlet.HubStatusServlet"
```

## Integration with Selenium Foundation

When `selenium-grid-manager` is on the classpath, it automatically registers its Grid factory with `selenium-foundation` via the `ServiceLoader` mechanism. This enables `selenium-foundation` to:

* Launch a local Grid instance automatically when tests require a driver and no active Grid is found
* Shut down the local Grid instance at the end of the test suite

This integration requires no configuration — simply include `selenium-grid-manager` as a `testImplementation` dependency in your project.

### Maven

```xml
<dependency>
    <groupId>com.nordstrom.ui-tools</groupId>
    <artifactId>selenium-grid-manager</artifactId>
    <version>36.0.2-s4</version>
    <scope>test</scope>
</dependency>
```

### Gradle

```groovy
testImplementation 'com.nordstrom.ui-tools:selenium-grid-manager:36.0.2-s4'
```

## Running the Unit Tests

The unit tests are browser-specific and must be run using the per-browser Gradle tasks:

```bash
./gradlew testChrome -Pprofile=selenium4
./gradlew testFirefox -Pprofile=selenium4
./gradlew testHtmlunit -Pprofile=selenium4
```

To run multiple browser tests:

```bash
./gradlew testChrome testFirefox testHtmlunit -Pprofile=selenium4
```

Available test tasks: `testChrome`, `testEdge`, `testEspresso`, `testFirefox`, `testHtmlunit`, `testMac2`, `testSafari`, `testUiautomator2`, `testAndroidchrome`, `testIossafari`, `testWindows`, `testXcuitest`

> When running the unit tests, be sure that you don't have a conflicting `settings.properties` file in your user "home" folder, as this will conflict with the per-browser settings files provided with each test task.

## Selenium Settings

Because `selenium-grid-manager` is built on **Selenium Foundation**, all of the settings supported by this library are available for your grid configuration. The settings can be specified individually on the command line, or you can specify them collectively in the corresponding settings file (e.g. - `settings.properties`). For details, check out the [Configuring Project Settings](https://github.com/sbabcoc/Selenium-Foundation/blob/master/docs/ConfiguringProjectSettings.md#introduction) page of the **Selenium Foundation** project.

### Settings File Path

The path to the settings file can be customized via the `selenium.settings.path` system property:

```bash
./gradlew runGrid -Dselenium.settings.path=my-settings.properties -Pbrowsers=chrome
```

## Notes

The ports used by the node servers that supply browser sessions are auto-selected via the [PortProber.findFreePort()](https://seleniumhq.github.io/selenium/docs/api/java/org/openqa/selenium/net/PortProber.html#findFreePort--) method of the **`selenium-remote-driver`** library.

If you launch a local grid with no specified browsers, the hub runs as a servlet container.

Unless disabled with the `noRedirect` option, `selenium-grid-manager` redirects the output of the hub and node servers to log files in a `logs` folder under the current working directory. Each log file contains the output from a single launch of its associated server. Log file names are auto-incremented to avoid overwriting or appending to the output of previous launches.

* `grid-hub*.log` for hub server output
* `grid-node*.log` for node server output

The default output folder can be overridden with the `logsFolder` option, specifying either absolute or relative path. If a relative path is specified, or the default ("logs") is accepted, logs are written to a sub-folder of the current working directory, which can be overridden with the `workingDir` option.
