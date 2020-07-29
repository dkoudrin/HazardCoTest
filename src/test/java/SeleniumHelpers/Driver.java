package SeleniumHelpers;


import PageObjects.*;
import SeleniumHelpers.*;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.ios.IOSDriver;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import static org.junit.Assert.fail;

public class Driver extends Thread implements WebDriver {
    private static String SCREEN_SIZE = SessionProperties.getInstance().getProperty("ScreenSize");
    private final LoggingPreferences seleniumLogPreferences = new LoggingPreferences();
    private Logger logger;
    private String uniqueID = Generator.generateUniqueAlphaString(); // this is to identify a specific driver in a pool of drivers
    private WebDriver driver;
    private Boolean isMobile = null;
    private BrowserType chosenBrowserType = null;
    private boolean retryTriggered = false;
    public Eyes eyes = new Eyes();
    private boolean isRemote = false;

    public enum ScreenSize {
        FULLSIZE,
        MOBILESIZE,
        DEFAULT;
    }

    public enum BrowserType {
        ANDROID,
        CHROME,
        CHROME_HEADLESS,
        FIREFOX,
        FIREFOX_HEADLESS,
        HTML_UNIT,
        INTERNET_EXPLORER,
        EDGE,
        IPAD,
        IPHONE,
        OPERA_BLINK,
        SAFARI;
    }

    /**
     * Initialises the Driver object with the default browser.
     * Default browser is either whatever is specified in Webdriver.Browser system setting or (if setting not set) Firefox.
     *
     * Example command line to run from jenkins
     * mvn clean test -DWebdriver.URL="123.100.104.114" -DWebdriver.Host=SAUCELABS -DWebdriver.Browser=INTERNET_EXPLORER -DWebdriver.BrowserVersion=10.0 -DWebdriver.BrowserPlatform=Windows 8 -Dtest=SmokeTest_JBM_AllBrowsers.java
     *
     */
    public Driver(String loggerName) {
        setLogger(loggerName);
        String host = SessionProperties.getInstance().getProperty("BrowserHost"); //System.getProperty(DRIVER_HOST_PROPERTY);

        //get some relevant properties from Maven command line
        String profileBrowserSetting = SessionProperties.getInstance().getProperty("BrowserType");
        String defaultBrowser = System.getProperty("Webdriver.Browser", profileBrowserSetting.startsWith("$")?"FIREFOX":profileBrowserSetting);

        //convert command line argument to internal BrowserType
        if(defaultBrowser.equals("ANDROID")) chosenBrowserType = BrowserType.ANDROID;
        else if(defaultBrowser.equals("CHROME")) chosenBrowserType = BrowserType.CHROME;
        else if (defaultBrowser.equals("FIREFOX")) chosenBrowserType = BrowserType.FIREFOX;
        else if(defaultBrowser.equals("INTERNET_EXPLORER")) chosenBrowserType = BrowserType.INTERNET_EXPLORER;
        else if(defaultBrowser.equals("IPAD")) chosenBrowserType = BrowserType.IPAD;
        else if(defaultBrowser.equals("IPHONE")) chosenBrowserType = BrowserType.IPHONE;
        else if(defaultBrowser.equals("OPERA")) chosenBrowserType = BrowserType.OPERA_BLINK;
        else if(defaultBrowser.equals("SAFARI")) chosenBrowserType = BrowserType.SAFARI;
        else if(defaultBrowser.equals("HTML_UNIT")) chosenBrowserType = BrowserType.HTML_UNIT;

        if(chosenBrowserType == null)
            throw new TestError("Unknown browser type requested: " + defaultBrowser);
        if(host!= null && host.equals("SAUCELABS")) {
            driver = initRemote(chosenBrowserType);
        }
        else {
            if(chosenBrowserType == BrowserType.ANDROID || chosenBrowserType == BrowserType.IPAD || chosenBrowserType == BrowserType.IPHONE) {
                throw new TestError(defaultBrowser + " tests can only be run through SauceLabs.");
            }
            driver = initLocal(chosenBrowserType);
        }

        seleniumLogPreferences.enable(LogType.DRIVER, Level.ALL);
    }

    public boolean isRemote() {
        return isRemote;
    }

    /**
     * set the apache logger logger for this instance using the name provided
     * @param loggerName
     */
    public void setLogger(String loggerName) {
        logger = new K1Logger().setUpLogger(loggerName);
    }

    /**
     * Returns the apache logger for this driver instance
     */
    public Logger getLogger() {
        if(logger==null){
            throw new TestError("Found a null logger from the driver");
        }
        return logger;
    }

    public String getID() {
        return uniqueID;
    }

    /**
     * Creates a Webdriver instance with the specified browser.
     * @param browser - can be FIREFOX, CHROME, INTERNET_EXPLORER
     * @return initialised instance of WebDriver
     */
    private WebDriver initLocal(BrowserType browser) {
        getLogger().info("Initialise a new local WebDriver instance");

        // Local drivers
        System.setProperty("webdriver.chrome.driver", new File("src/main/resources/drivers/chromedriver.exe").getAbsolutePath());
        System.setProperty("webdriver.ie.driver", new File("src/main/resources/drivers/IEDriverServer.exe").getAbsolutePath());
        String geckoPath = "src/main/resources/drivers/geckodriver";
        String OS = System.getProperty("os.name").toLowerCase();
        if (OS.indexOf("win") >= 0){ // for windows use the exe
            geckoPath = geckoPath + ".exe";
        }
        System.setProperty("webdriver.gecko.driver", new File(geckoPath).getAbsolutePath());
        String browserBinaryLocation = System.getProperty("Webdriver.Browser.Binary", "");


        DateTime initRequestTime = new DateTime();
        if(driver == null) {
            switch(browser) {
                case FIREFOX:
                    FirefoxProfile ffp = new FirefoxProfile();
                    ffp.setPreference("reader.parse-on-load.enabled", false);
                    FirefoxOptions firefoxOptions = new FirefoxOptions();
                    if(browserBinaryLocation != null && !browserBinaryLocation.isEmpty()){
                        firefoxOptions.setBinary(browserBinaryLocation);
                    }
                    String allowPlugins = SessionProperties.getInstance().getProperty("AllowPlugins");
                    if(allowPlugins.equals("TRUE")) {
                        //start with Firebug enabled
                        try {
                            ffp.addExtension(new File("plugins/firebug-2.0.15-fx.xpi"));
                            ffp.addExtension(new File("plugins/netExport-0.9b7.xpi"));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        String networkCapture = SessionProperties.getInstance().getProperty("NetworkTrafficCapture");
                        if(networkCapture.equals("TRUE")) {
                            //netExport preferences
                            ffp.setPreference("extensions.firebug.netexport.alwaysEnableAutoExport", true);
                            ffp.setPreference("extensions.firebug.netexport.autoExportToFile", true);
                            ffp.setPreference("extensions.firebug.netexport.Automation", true);
                            ffp.setPreference("extensions.firebug.netexport.showPreview", false);
                            ffp.setPreference("extensions.firebug.net.defaultPersist", true);
                            ffp.setPreference("extensions.firebug.console.defaultPersist", true);
                            //ffp.setPreference("extensions.firebug.netexport.secretToken", "hard24get");
                            ffp.setPreference("extensions.firebug.netexport.defaultLogDir", System.getProperty("user.dir") + File.separator + "target"+File.separator +"NetworkTraffic");
                            getLogger().info(System.getProperty("user.dir") + File.separator + "target"+File.separator +"NetworkTraffic");
                        }
                    }

                    firefoxOptions.setCapability("marionette", true);
                    firefoxOptions.setCapability(FirefoxDriver.PROFILE, ffp);
                    firefoxOptions.setCapability(CapabilityType.LOGGING_PREFS, seleniumLogPreferences);
                    firefoxOptions.addPreference("devtools.jsonview.enabled", false); // due to KIT-17452
                    if(SessionProperties.getInstance().getProperty("HeadLessGecko").equalsIgnoreCase("true")) {
                        firefoxOptions.addArguments("--headless");
                        if(SCREEN_SIZE.equals(ScreenSize.FULLSIZE.toString()) || SCREEN_SIZE.startsWith("$")) {
                            firefoxOptions.addArguments("--window-size").addArguments("1920,1080");
                        }
                    }
                    System.setProperty(FirefoxDriver.SystemProperty.BROWSER_LOGFILE,"target"+File.separator+"BrowserLog.txt");
                    driver =  new FirefoxDriver(firefoxOptions);
                    break;
                case CHROME:
                    ChromeOptions chromeOptions = new ChromeOptions();
                    chromeOptions.addArguments("test-type");
                    chromeOptions.setCapability(CapabilityType.LOGGING_PREFS, seleniumLogPreferences);

                    driver =  new ChromeDriver(chromeOptions);
                    break;
                case INTERNET_EXPLORER: driver =  new InternetExplorerDriver();
                    break;
            }
            if(driver == null)
                throw new TestError("Unsupported browser requested or init failed.");
        }
        else { // if driver has already been initialised in the past
            try {
                driver.getWindowHandle(); // try re-using the same browser window
            }
            catch(Exception e) { // if window can no longer be obtained, then clean up and try starting a new instance
                if(retryTriggered)                //want to kill it if we unsuccessfully tried initialising twice, on the off-chance that we start endlessly looping the init
                    throw new TestError("Unable to create new or re-use existing browser window.");
                driver.quit();
                driver = null;
                retryTriggered = true;
                driver = initLocal(browser);
            }
        }
        retryTriggered = false;
        getLogger().info("Local WebDriver instance initialised. Took " + new Duration(initRequestTime, new DateTime()).getStandardSeconds() + " seconds.\n");
        return driver;
    }

    private WebDriver initRemote(BrowserType browser) {
        getLogger().info("Initialise a new remote WebDriver instance");
        DateTime initRequestTime = new DateTime();
        DesiredCapabilities capabilities = null;
        URL sauceLabUrl = null;
        try {
            sauceLabUrl = new URL("http://test_kitomba:a7c037de-5eaa-4611-8e2b-b32e2c28c5e6@ondemand.saucelabs.com:80/wd/hub");
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error building the saucelab url: " + e.toString());
        }


        String deviceName = SessionProperties.getInstance().getProperty("DeviceName");
        if(deviceName!= null && !deviceName.startsWith("${")){
            // We need to configure sauce labs for mobile device type
            capabilities = new DesiredCapabilities();
            capabilities.setCapability("deviceName", deviceName);
            capabilities.setCapability("deviceOrientation", getAndCheckProperty("DeviceOrientation"));
            capabilities.setCapability("platformVersion", getAndCheckProperty("PlatformVersion"));
            capabilities.setCapability("platformName", getAndCheckProperty("PlatformName"));
            capabilities.setCapability("browserName", getAndCheckProperty("BrowserName"));

        } else {
            // We need to configure sauce labs for desktop/laptop type
            capabilities = selectCapabilityType(browser);
            if (capabilities == null)  throw new TestError("Unsupported browser requested.");
            capabilities.setCapability("version", getAndCheckProperty("BrowserVersion"));
            capabilities.setCapability("platform", getAndCheckProperty("BrowserPlatform"));
            capabilities.setCapability("device-type", getAndCheckProperty("DeviceType"));
            capabilities.setCapability("device-orientation", getAndCheckProperty("DeviceOrientation"));
            capabilities.setCapability("screenResolution", getAndCheckProperty("DeviceScreenRes"));

        }

        capabilities.setCapability("commandTimeout", "600");
        capabilities.setCapability("idleTimeout", "1000");

        // Here we decide on what kind of remote driver we want.
        // For IOS devices we want IOSDriver so we can get the extra functionality need to test for IOS keyboard pop up
        // For desktop hosting we can set the VM time zone
        String platformName = getProperty("PlatformName");
        if(platformName == null){
            // Desktop profile
            // remote browsers need to set timezone to NZ although this only works for desktop remote browsers
            // not appium driven devices or ios driven
            // https://github.com/appium/appium/issues/12567
            capabilities.setCapability("timeZone", "Auckland");
            getLogger().info("Starting web remote driver with " + capabilities.toString());
            driver = new RemoteWebDriver(sauceLabUrl, capabilities);

        } else {
            // Device profile
            if (platformName.equals("iOS")) {
                capabilities.setCapability("timeZone", "Auckland");
                getLogger().info("Starting IOS remote driver with " + capabilities.toString());
                driver = new IOSDriver(sauceLabUrl, capabilities);
            } else {
                getLogger().info("Starting appium remote driver with " + capabilities.toString());
                driver = new AppiumDriver(sauceLabUrl, capabilities);
            }
        }

        if (driver==null) {
            throw new TestError("Failed to initialise remote web driver.");
        }
        isRemote = true;
        getLogger().info("Remote WebDriver instance initialised. Took " + new Duration(initRequestTime, new DateTime()).getStandardSeconds() + "seconds.\n");
        return driver;
    }

    private DesiredCapabilities selectCapabilityType(BrowserType browser) {
        switch (browser) {
            case ANDROID:
                return DesiredCapabilities.android();
            case CHROME:
                return DesiredCapabilities.chrome();
            case FIREFOX:
                return DesiredCapabilities.firefox();
            case INTERNET_EXPLORER:
                return DesiredCapabilities.internetExplorer();
            case EDGE:
                return DesiredCapabilities.edge();
            case IPAD:
                return DesiredCapabilities.ipad();
            case IPHONE:
                return DesiredCapabilities.iphone();
            case OPERA_BLINK:
                return DesiredCapabilities.operaBlink();
            case SAFARI:
                return DesiredCapabilities.safari();
        }
        return null;
    }

    /**
     * Gets the property name dealing with the case it doesnt exist or is still set to the paramter template and returns the value or NULL
     * @param propertyName
     * @return The value if it existed or NULL
     */
    private String getProperty(String propertyName) {
        String propertyValue = SessionProperties.getInstance().getProperty(propertyName);
        if(propertyValue== null || propertyValue.startsWith("${")){
            return null;
        }
        return propertyValue;
    }

    /**
     * Gets the property name using getProperty() and returns value, if NULL throws error
     * @param propertyName
     * @return
     */
    private String getAndCheckProperty(String propertyName) {
        String propertyValue = getProperty(propertyName);
        if(propertyValue == null){
            throw new TestError(propertyName+" must be set");
        }
        return propertyValue;
    }



    public ScreenSize getSizeFromProfile(){
        ScreenSize size;
        try {
            size = ScreenSize.valueOf(SCREEN_SIZE);
        } catch (IllegalArgumentException e){
            //     getLogger().info("Parsing screen size from getProperty('ScreenSize') failed, will use FullSize. \n Error was: "+e.toString());
            size = ScreenSize.FULLSIZE;
        }
        return size;
    }

    public BrowserType getBrowserType() {
        return chosenBrowserType;
    }

    public WebDriver getWebDriver() {
        return driver;
    }


    // ################## Navigation and driver find methods below #################################################

    /**
     * Takes browser to a specified URL, auto resizes window to profile dimensions
     * @param aUrl - URL to be called up in the browser
     */
    public void get(String aUrl) {
        get(aUrl, getSizeFromProfile());
    }

    /**
     * Takes browser to a specified URL.
     * Typlically this method version is used for Langely or OB. If K1 use the base version that auto resizes window to profile dimensions
     * @param aUrl - URL to go to.
     * @param fullScreen - if true, set to max size, else leave at default
     */
    public void get(String aUrl, boolean fullScreen) {
        if(fullScreen) {
            get(aUrl, ScreenSize.FULLSIZE);
        }else {
            get(aUrl, ScreenSize.DEFAULT);
        }
    }

    /**
     * Takes browser to a specified URL.
     * @param aUrl - URL to go to.
     * @param size - switch on screensize
     */
    public void get(String aUrl, ScreenSize size) {
        getLogger().info("Open URL: " + aUrl);
        if(!isMobile()) { // Don't alter screen size if this is an actual mobile device
            try {
                switch (size) {
                    case FULLSIZE:
                        driver.manage().window().maximize();
                        break;
                    case MOBILESIZE:
                        driver.manage().window().setSize(new Dimension(640, 820));
                        break;
                    case DEFAULT:        // no change
                        break;
                }
            } catch (Exception e) {
                //maximize isn't implemented for some SauceLabs platforms. If it fails, can continue, not a massive issue};
            }
        }
        driver.get(aUrl);
    }

    public void close() {
        getLogger().info("Close browser window");
        driver.close();
    }

    public WebElement findElement(By by) {
        getLogger().info("Find element " + by.toString());
        WebElement elt;
        elt = driver.findElement(by);
        return elt;
    }

    public List<WebElement> findElements(By by) {
        getLogger().info("Find all elements " + by.toString());
        List<WebElement> lst = driver.findElements(by);
        return lst;
    }

    public void assertElementWithTextNotPresent(WebDriver driver, Locator locator, String locatorText) {
        try {
            driver.findElement(Using.locator(locator, locatorText));
            fail("Element is present");
        }
        catch (NoSuchElementException ex) {
            /* do nothing, element is not present, assert is passed */
        }
    }

    /**
     *
     * @return the current URL from the browser, or NULL if there was a dead connection to the browser
     */
    public String getCurrentUrl() {
        try {
            return driver.getCurrentUrl();
        } catch (NoSuchSessionException e){
            return null;
        }
    }

    public String getPageSource() {
        return driver.getPageSource();
    }

    public String getTitle() {
        return driver.getTitle();
    }

    public String getWindowHandle() {
        return driver.getWindowHandle();
    }

    public Set<String> getWindowHandles() {
        getLogger().info("Get window handles");
        return driver.getWindowHandles();
    }

    public WebDriver.Options manage() {
        return driver.manage();
    }

    public WebDriver.Navigation navigate() {
        return driver.navigate();
    }

    public void quit() {
        String autoQuit = SessionProperties.getInstance().getProperty("AutoQuit");
        if (!autoQuit.equals("FALSE")) {
            getLogger().info("Quit browser.");
            driver.quit();
        }
    }

    public WebDriver.TargetLocator switchTo() {
        getLogger().info("SwitchTo");
        return driver.switchTo();
    }



    public void takeScreenshot(String fileName) {
        getLogger().info("Take screenshot named: " + fileName);
        try {
            File scrFile = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
            FileUtils.copyFile(scrFile, new File("target//" + fileName + ".png"));
        }
        catch (ClassCastException cce) {
            getLogger().info("CCError taking screenshot: "  + cce.toString());
        }
        catch (IOException ioe) {
            getLogger().info("IOError saving screenshot: "  + ioe.toString());
        }
        catch (WebDriverException wde){
            getLogger().info("WDError saving screenshot: "  + wde.toString());
        }
    }

    /**
     * Take screenshot with specified filename and the current date, trims any spaces to make it safe
     * for jenkins.
     * @param fileName - The desired identifying filename (e.g. name of test or descriptive keyword).
     */
    public void takeScreenshotWithCurrentDate(String fileName) {
        DateTimeFormatter dateFormatter = DateTimeFormat.forPattern("yyyy-MM-dd_HH_mm_ss");
        String dateString = new DateTime().toString(dateFormatter);
        takeScreenshot((fileName+dateString).trim());
    }

    /**
     * Attempts to switch to a window by Title.
     * @param windowTitle  string to look for
     */
    public void switchToWindowWithTitle(String windowTitle) {
        switchToWindowWithTitle(windowTitle, false, true);
    }

    /**
     * Attempts to switch to a window by Title (or its part).
     * @param windowTitle  string to look for
     * @param partialName true if we are looking for a partial match on string (i.e. we are looking for substring),
     *                      false if it has to be an exact match
     * @param caseSensitive true if string case should match exactly, false if case is irrelevant
     */
    public void switchToWindowWithTitle(String windowTitle, boolean partialName, boolean caseSensitive) {
        getLogger().info("Switch to window/tab with title" + (partialName?" containing ":" ")
                + "'" + windowTitle + "'.");
        switchToWindowWithString(windowTitle, true, partialName, caseSensitive, -1);
    }

    /**
     * Attempts to switch to a window by URL (or its part).
     * @param windowURL  string to look for
     * @param partialName true if we are looking for a partial match on string (i.e. we are looking for substring),
     *                      false if it has to be an exact match
     * @param caseSensitive true if string case should match exactly, false if case is irrelevant
     */
    public void switchToWindowWithURL(String windowURL, boolean partialName, boolean caseSensitive) {
        getLogger().info("Switch to window/tab with URL" + (partialName?" containing ":" ")
                + "'" + windowURL + "'.");
        switchToWindowWithString(windowURL, false, partialName, caseSensitive, -1);
    }

    /**
     * Attempts to switch to a window, which has a (sub-)string in it's title or URL.
     * Leaving access public, since it allows for greater flexibility, but it's better to use higher level methods.
     * @param windowString  string to look for
     * @param isTitle       true if string refers to title, false if URL
     * @param partialString true if we are looking for a partial match on string (i.e. we are looking for substring),
     *                      false if it has to be an exact match
     * @param caseSensitive true if string case should match exactly, false if case is irrelevant
     * @param timeoutSeconds the number of seconds to try before timing out with an error
     */
    public void switchToWindowWithString(String windowString, boolean isTitle,
                                         boolean partialString, boolean caseSensitive, int timeoutSeconds) {
        StringBuffer encounteredWindows;
        DateTime switchTimeout = new DateTime().plusSeconds(timeoutSeconds<0?PageObject.mediumTimeOut:timeoutSeconds);
        do {
            encounteredWindows = new StringBuffer();
            for (String winHandle : driver.getWindowHandles()) {
                driver.switchTo().window(winHandle);
                String text =isTitle?driver.getTitle():driver.getCurrentUrl();
                encounteredWindows.append(text).append("\n");
                if(caseSensitive) {
                    if (partialString && text.contains(windowString)
                            || !partialString && text.equals(windowString)) {
                        return;
                    }
                } else {
                    if (partialString && text.toLowerCase().contains(windowString.toLowerCase())
                            || !partialString && text.equalsIgnoreCase(windowString)) {
                        return;
                    }
                }
            }
        } while (new DateTime().isBefore(switchTimeout));
        // if haven't found the window to switch to
        throw new TestError("Error: unable to switch to window with " + (isTitle?"title":"URL")
                + (partialString?" containing ":" ") +"'" + windowString
                + "'. Found only the following windows:\n" + encounteredWindows);
    }

    public void closeWindow(String windowTitle) {
        String currentWindowHandle = getWindowHandle();
        switchToWindowWithTitle(windowTitle);
        close();
        switchTo().window(currentWindowHandle);
    }

    /**
     * Explicitly scrolls to element (may come in handy in case it's off-screen).
     * @param element element to scroll to
     */
    public void scrollToElement(WebElement element) {
        getLogger().info("Scroll to element " + element);
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
        try {
            Thread.sleep(500);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public String toString(){
        return "ID: " + getID() + "\n" + ((RemoteWebDriver)driver).getCapabilities();
    }

    public String getBrowserDetails(){
        Capabilities cap = ((RemoteWebDriver) driver).getCapabilities();
        String browser_name = cap.getBrowserName();
        String browser_version = cap.getVersion();
        return "Browser:"+browser_name + " Version:" + browser_version;
    }

    /**
     * Use this to update the Driver wrapper with a new WebDriver to wrap (adding to support Applitools Eyes)
     * @param newWebDriver Webdriver to update the current Driver object
     */
    public void setWebDriver(WebDriver newWebDriver){
        driver = newWebDriver;
    }

    /**
     * Using selenium driver to return useragent from browser we match on magic strings to tell us if this is
     * a mobile device or not.
     * The vairable holding the boolean is set only once during testing
     * @return true if mobile device
     */
    public boolean isMobile(){
        if(isMobile!=null) { // Variable already been set
            return isMobile;
        } else { // Set the variable now
            String agent = getUserAgent();
            getLogger().info("User Agent details are " + agent);
            if (agent.toLowerCase().contains("android") ||
                    agent.toLowerCase().contains("mobile") ||
                    agent.toLowerCase().contains("ipad") ||
                    agent.toLowerCase().contains("Mac OS X".toLowerCase())
            ) {
                getLogger().info("Yes we consider this a mobile device");
                isMobile = true;
                return true;
            }
            getLogger().info("No we don't consider this a mobile device");
            isMobile = false;
            return false;
        }
    }

    public String getUserAgent() {
        return (String) ((JavascriptExecutor) driver).executeScript("return navigator.userAgent;");

    }


    /**
     * Wrapper of webdriver wait presenceOfElementLocated
     * @param seconds
     * @param locator
     * @param variable
     * @return the web element if found
     */
    public WebElement waitForPresence(int seconds, Locator locator, String... variable) {
        String printableString="";
        for (String var:variable) {
            printableString =printableString.concat(var+" ");
        }
        getLogger().info("Wait "+ seconds+" seconds for the presence of element located by "+locator.toString()+ " with variable "+printableString);
        return new WebDriverWait(driver,seconds).until(ExpectedConditions.presenceOfElementLocated(Using.locator(locator, variable)));
    }

    /**
     * Wrapper of webdriver wait presenceOfElementLocated
     * @param seconds
     * @param locator
     * @return the web element if found
     */
    public WebElement waitForPresence(int seconds, Locator locator) {
        getLogger().info("Wait "+ seconds+" seconds for the presence of element located by "+locator.toString());
        return new WebDriverWait(driver,seconds).until(ExpectedConditions.presenceOfElementLocated(Using.locator(locator)));
    }

    /**
     * Wrapper of webdriver wait elementToBeClickable
     * elementToBeClickable also checks for visibilityOfElementLocated first!
     * @param seconds
     * @param locator
     * @return the web element if found
     */
    public WebElement waitForClickable(int seconds, Locator locator) {
        getLogger().info("Wait "+ seconds+" seconds for the element located by "+locator.toString()+" to be clickable");
        return new WebDriverWait(driver,seconds).until(ExpectedConditions.elementToBeClickable(Using.locator(locator)));
    }

    /**
     * Wrapper of webdriver wait elementToBeClickable
     * elementToBeClickable also checks for visibilityOfElementLocated first!
     * @param seconds
     * @param locator
     * @param variable variables to be substituted into the locator
     * @return
     */
    public WebElement waitForClickable(int seconds, Locator locator, String... variable) {
        String printableString="";
        for (String var:variable) {
            printableString =printableString.concat(var+" ");
        }
        getLogger().info("Wait "+ seconds+" seconds for the element located by "+locator.toString()+ " with variable "+printableString+" to be clickable");
        return new WebDriverWait(driver,seconds).until(ExpectedConditions.visibilityOfElementLocated(Using.locator(locator, variable)));
    }

    /**
     * Wrapper of webdriver wait visibilityOfElementLocated
     * @param seconds
     * @param locator
     * @return the web element if found
     */
    public WebElement waitForVisibility(int seconds, Locator locator) {
        getLogger().info("Wait "+ seconds+" seconds for the element located by "+locator.toString() +" to be visible");
        return new WebDriverWait(driver,seconds).until(ExpectedConditions.visibilityOfElementLocated(Using.locator(locator)));
    }

    public WebElement waitForVisibility(int seconds, Locator locator, String... variable) {
        String printableString="";
        for (String var:variable) {
            printableString =printableString.concat(var+" ");
        }
        getLogger().info("Wait "+ seconds+" seconds for the element located by "+locator.toString()+ " with variable "+printableString+" to be visible");
        return new WebDriverWait(driver,seconds).until(ExpectedConditions.visibilityOfElementLocated(Using.locator(locator, variable)));
    }

    /**
     * Wrapper of webdriver wait invisibilityOfElementLocated
     * @param seconds
     * @param locator
     */
    public void waitForInvisibility(int seconds, Locator locator) {
        getLogger().info("Wait "+ seconds+" seconds for the element located by "+locator.toString() +" to be invisible");
        new WebDriverWait(driver,seconds).until(ExpectedConditions.invisibilityOfElementLocated(Using.locator(locator)));
    }

    public void waitForInvisibility(int seconds, Locator locator, String variable) {
        getLogger().info("Wait "+ seconds+" seconds for the element located by "+locator.toString()+ " with variable "+variable+" to be visible");
        new WebDriverWait(driver,seconds).until(ExpectedConditions.invisibilityOfElementLocated(Using.locator(locator, variable)));
    }

    /**
     * Wrapper of webdriver wait visibilityOfAllElementsLocatedBy
     * @param seconds
     * @param locator
     * @return all the web elements found
     */
    public List<WebElement> waitForAllVisibility(int seconds, Locator locator) {
        getLogger().info("Wait "+ seconds+" seconds for elements located by "+locator.toString() +" to be visible");
        return new WebDriverWait(driver,seconds).until(ExpectedConditions.visibilityOfAllElementsLocatedBy(Using.locator(locator)));
    }
}
