package PageObjects;


import PageObjects.*;
import SeleniumHelpers.*;
import io.appium.java_client.ios.IOSDriver;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.math.BigDecimal;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class PageObject {

    public static Locator BROWSER_OUT_OF_DATE                               = new Locator ("BROWSER_OUT_OF_DATE",                              "css=div#outdated");
    public static Locator ERROR_BLOCK                                       = new Locator ("ERROR_BLOCK",                                      "xpath=//*[name()!='script'  and @type!='text/javascript' and contains(text(), 'Error')]");
    public static Locator BLOCKUI_OVERLAY                                   = new Locator ("BLOCKUI_OVERLAY",                                  "css=div.blockUI.blockOverlay");
    public static Locator GRAYOUT_OVERLAY                                   = new Locator ("GRAYOUT_OVERLAY",                                  "css=div.fancybox-overlay");
    public static Locator VERSION_BANNER                                    = new Locator ("VERSION_BANNER",                                   "css=div#versionBanner");
    public static Locator NO_ACCESS_MESSAGE                                 = new Locator("NO_ACCESS_MESSAGE",                                 "css=div.noaccessWrap");
    public static Locator NO_PERMISSION_MESSAGE                             = new Locator("NO_PERMISSION_MESSAGE",                             "xpath=//p[text()=\"If you need access to the features on this page to get your job done, ask your administrator to change your permissions.\"]");


    public static int ludicrousTimeOut = 600;
    public static int extremeTimeOut = 300;
    public static int veryLongTimeOut = 120;
    public static int longTimeOut = 60;
    public static int mediumTimeOut = 30;
    public static int mediumShortTimeOut = 10;
    public static int shortTimeOut = 6;
    public static int veryShortTimeOut = 1;
    public static int reportTimeOut = 600;
    protected Logger logger;
    public Driver driver = null;
    private static final int SMALL_SCREEN_EDGE = 1080;
    private static String env = SessionProperties.getInstance().getProperty("PROFILE.NAME");
    private static boolean environmentTimeOutsSet = false;


    public PageObject(Driver driver) {
        this.driver = driver;
        logger = this.driver.getLogger();
        if ( env!=null && !environmentTimeOutsSet && env.equals("develop")) {
            logger.info("Increase some selenium timeouts because the test host is '"+env+"'");
            longTimeOut = 120;
            mediumTimeOut = 60;
            mediumShortTimeOut = 30;
            shortTimeOut = 7;
            environmentTimeOutsSet = true;
        }
    }

    /**
     * Allow children classes to get the logger
     * @return
     */
    public Logger getLogger() {
        if(logger==null){
            throw new TestError("Found a null logger from the super class page object");
        }
        return logger;
    }

    public PageObject onPageLoaded(){
        checkForForbiddenTerms();
        return this;
    }

    /**
     * Tests the browser window size and returns true if the width is smaller than the specific size where responsive css magic affects layout
     * @return True when small, else false
     */
    public boolean isSmallScreen(){
        Dimension size = driver.manage().window().getSize();
        int width = size.getWidth();
        logger.info("Screen Width:"+width);
        return width < SMALL_SCREEN_EDGE;
    }

    public PageObject waitForVisibilityOf(Locator screenElementLocator, int seconds) {
        logger.info("Wait for element " + screenElementLocator.name + " to be visible (no more than " + seconds + " seconds).");
        new WebDriverWait(driver, seconds).until(ExpectedConditions.visibilityOfElementLocated(Using.locator(screenElementLocator)));
        return this;
    }

    public PageObject waitForPresenceOf(Locator screenElementLocator, int seconds) {
        logger.info("Wait for element " + screenElementLocator.name + " to be present (no more than " + seconds + " seconds).");
        new WebDriverWait(driver, seconds).until(ExpectedConditions.presenceOfElementLocated(Using.locator(screenElementLocator)));
        return this;
    }

    /**
     * Checks if the element passed as a parameter has focus.
     * @return true if element has focus
     */
    public boolean isFocused(Locator locator) {
        logger.info("See if " + locator.name + " has focus:");
        boolean isFocused = false;
        WebElement shouldBeFocused = driver.findElement(Using.locator(locator));
        WebElement elementFocused = driver.switchTo().activeElement();
        if(shouldBeFocused.equals(elementFocused)) {
            logger.info("Yes");
            isFocused = true;
        }
        else {
            logger.info("No. The following element is focused instead: " + elementFocused.getTagName() + " class=" + elementFocused.getAttribute("class") + " id=" + elementFocused.getAttribute("id"));
        }
        return isFocused;
    }

    /**
     * Checks that there are no (visible) errors on the screen.
     */
    public boolean isErrorDisplayed() {
        logger.info("Check if page contains any errors.");
        boolean errorsDisplayed;
        List<WebElement> errorElements = driver.findElements(Using.locator(ERROR_BLOCK));
        if(errorElements.size() > 0) {
            errorsDisplayed = true;
            logger.info("Errors encountered:");
            for (WebElement element : errorElements) {
                logger.info(element.getTagName() + ":" + element.getText() + " " + element.getAttribute("value"));
            }
        }
        else {
            errorsDisplayed = false;
            logger.info("No errors encountered.");
        }
        return errorsDisplayed;
    }

    /**
     * Ticks or unticks the checkbox.
     * @param locator checkbox to tick/untick.
     * @param value true = ticked, false = unticked.
     * @return self-reference
     */
    protected PageObject setCheckboxValue(Locator locator, boolean value) {
        logger.info("Set " + locator.name + " to " + value + ".");
        WebElement checkbox = driver.findElement(Using.locator(locator));
        if(checkbox.isSelected() != value) // only need to click the checkbox if it's not set to the desired setting already
            checkbox.click();
        return this;
    }

    /**
     * Ticks or unticks the checkbox using the matching label
     * @param checkboxLocator check box that will get enabled or disabled
     * @param labelLocator label to click on
     * @param value
     * @return self-reference
     */
    protected PageObject setCheckboxValue(Locator checkboxLocator, Locator labelLocator, boolean value) {
        logger.info("Set " + checkboxLocator.name + " to " + value + ".");
        WebElement checkbox = driver.findElement(Using.locator(checkboxLocator));
        if(checkbox.isSelected() != value) { // only need to click the checkbox if it's not set to the desired setting already
            WebElement label = driver.findElement(Using.locator(labelLocator));
            label.click();
        }
        return this;
    }

    /**
     * Ticks or unticks the checkbox.
     * @param locator checkbox to tick/untick.
     * @param variable parameter for locator (makes sense only locators that are parameterised).
     * @param value true = ticked, false = unticked.
     * @return self-reference
     */
    protected void setCheckboxValue(Locator locator, String variable, boolean value) {
        logger.info("Set " + locator.name + "(" + variable + ")  to '" + value + "'.");
        WebElement checkbox = driver.findElement(Using.locator(locator, variable));
        if(checkbox.isSelected() != value) {// only need to click the checkbox if it's not set to the desired setting already
            checkbox.click();
        }
    }

    /**
     * Reads from the screen whether a checkbox is ticked or not.
     * @param locator checkbox to read the value from.
     * @return true if checkbox is ticked, otherwise - false.
     */
    public boolean getCheckboxValue(Locator locator) {
        logger.info("Read state of " + locator.name + " from screen:");
        WebElement checkbox = driver.findElement(Using.locator(locator));
        boolean isTicked = checkbox.isSelected();
        logger.info("Ticked: "+ isTicked);
        return isTicked;
    }

    public boolean getCheckboxValue(Locator locator, String variable) {
        logger.info("Read state of " + locator.name + " from screen:");
        WebElement checkbox = driver.findElement(Using.locator(locator, variable));
        boolean isTicked = checkbox.isSelected();
        logger.info("Ticked: "+ isTicked);
        return isTicked;
    }

    protected String getTextFieldValue(Locator field) {
        logger.info("Read " + field.name + " from screen:");
        String value = driver.findElement(Using.locator(field)).getAttribute("value");
        logger.info(value);
        return value;
    }

    protected String getTextFieldValue(Locator field, String variable) {
        logger.info("Read " + field.name + "(" + variable + ") from screen:");
        String value = driver.findElement(Using.locator(field, variable)).getAttribute("value");
        logger.info(value);
        return value;
    }

    protected String getLabelText(Locator label) {
        logger.info("Read " + label.name + " from screen:");
        String value = driver.findElement(Using.locator(label)).getText();
        logger.info(value);
        return value;
    }

    protected PageObject setTextFieldValue(Locator locator, String value) {
        logger.info("Set " + locator.name + " to '" + value + "'.");
        WebElement field = driver.findElement(Using.locator(locator));
        field.clear();
        field.sendKeys(value);
        return this;
    }

    protected void setTextFieldValue(Locator field, String variable, String value) {
        logger.info("Set " + field.name + "(" + variable + ")  to '" + value + "'.");
        WebElement element = driver.findElement(Using.locator(field, variable));
        element.clear();
        element.sendKeys(value);
    }

    protected BigDecimal getMoneyLabelValue(Locator locator, String... args) {
        logger.info("Read money value in locator " + locator.name + ".");
        return new BigDecimal(driver.findElement(Using.locator(locator, args)).getText());
    }

    /**
     * Clears and populates a text field at a lower level by using Keys.
     * This is a workaround for Selenium bug (https://github.com/seleniumhq/selenium-google-code-issue-archive/issues/214),
     * which fires off onchange event after clearing the field, which for some fields results in re-populating with
     * default value (e.g. Kmail templates, Packages).
     *
     * @param field field to set
     * @param args this can accept up to two arguments
     *             If only one argument is passed - it's the value to set the field to
     *             If two arguments are passed - first one is variable for the locator, second one is the value
     * @return the element that text was entered into
     */
    protected WebElement clearAndSetTextFieldValueUsingKeys(Locator field, String... args) {
        WebElement element;
        String variable;
        String value;
        switch(args.length){
            case 1: value = args[0];
                logger.info("Clear field" + field.name + " and set it to '" + value + "'.");
                element = driver.findElement(Using.locator(field));
                break;
            case 2: variable = args[0];
                value = args[1];
                logger.info("Clear field " + field.name + "(" + variable + ") and set to '" + value + "'.");
                element = driver.findElement(Using.locator(field, variable));
                break;
            default: throw new TestError("Wrong number of arguments passed to method, at this point only 1 or 2 are supported");
        }
        StringBuffer backspaces = new StringBuffer();
        String placeholderText = element.getAttribute("value");
        for (int i = 0; i < placeholderText.length(); i++) {
            backspaces.append(Keys.BACK_SPACE);
        }
        if (value.contains(":")) {
            value = value.replace(":", Keys.chord(Keys.SHIFT, ";"));
        }
        element.sendKeys(backspaces + value);

        return element;
    }


    protected void setDropdownValue(Locator field, String value) {
        logger.info("Set " + field.name + " to '" + value + "'.");
        Select dropdown = new Select(driver.findElement(Using.locator(field)));
        dropdown.selectByVisibleText(value);
    }

    protected void setDropdownValue(Locator field, String variable, String value) {
        logger.info("Set " + field.name + "(" + variable + ")  to '" + value + "'.");
        Select dropdown = new Select(driver.findElement(Using.locator(field, variable)));
        dropdown.selectByVisibleText(value);
    }

    protected String getDropdownValue(Locator field) {
        logger.info("Read " + field.name + " from screen:");
        Select dropdown = new Select(driver.findElement(Using.locator(field)));
        String value = dropdown.getFirstSelectedOption().getText();
        logger.info(value);
        return value;
    }

    protected String getDropdownValue(Locator field, String variable) {
        logger.info("Read " + field.name + "(" + variable + ") from screen:");
        Select dropdown = new Select(driver.findElement(Using.locator(field, variable)));
        String value = dropdown.getFirstSelectedOption().getText();
        logger.info(value);
        return value;
    }

    public void sendKeysAndTab(Locator locator, String keys) {
        logger.info("Type the following into element " + locator.name + ": " + keys);
        driver.findElement(Using.locator(locator)).sendKeys(keys);
        driver.findElement(Using.locator(locator)).sendKeys(Keys.TAB);
    }

    /**
     * Checks if element is either not displayed or not present in the page at all.
     * @param element locator of element to check for.
     * @return true if element is either not displayed or not even present in the DOM, otherwise false.
     */
    public boolean isElementAbsent(Locator element) {
        List<WebElement> elements = driver.findElements(Using.locator(element));
        if(elements.size() > 0) {
            return !elements.get(0).isDisplayed();
        }
        return true;
    }

    /**
     * Looks for a displayed locator, handles NoSuchElementException.
     * @return true if found and displayed, else false.
     */
    public boolean isVisible(Locator locator) {
        logger.info("Check if " + locator + " is visible.");
        try {
            boolean isDisplayed = driver.findElement(Using.locator(locator)).isDisplayed();
            logger.info(isDisplayed?"Yes":"No, present in DOM, but not displayed");
            return isDisplayed;
        }
        catch (NoSuchElementException e) {
            logger.info("No such element");
            return false;
        }
        catch (StaleElementReferenceException ee) {
            logger.info("Stale reference, try again");
            return isVisible(locator);
        }
    }

    public boolean isK1StyledElementVisible(Locator locator){
        try {
            WebElement nativeElement = driver.findElement(Using.locator(locator));
            String id = nativeElement.getAttribute("id");
            WebElement labelElement = nativeElement.findElement(By.xpath("..//label[@for='" + id + "']"));
            return labelElement.isDisplayed();
        }catch (NoSuchElementException e) {
            return false;
        }
    }

    /**
     * Looks for a displayed locator, within a parent element (e.g. element of the table, button in a modal), handles NoSuchElementException.
     * @param element - parent element to search in
     * @param locator - dynamic locator
     * @return true if found and displayed, else false.
     */
    public boolean isVisible(WebElement element, Locator locator) {
        logger.info("Check if " + locator + " is visible.");
        try {
            boolean isDisplayed = element.findElement(Using.locator(locator)).isDisplayed();
            return isDisplayed;
        }
        catch (NoSuchElementException e) {
            return false;
        }
    }

    /**
     * Looks for a displayed locator, handles NoSuchElementException.
     * @param locator - dynamic locator
     * @param params - parameters for dynamic locator
     * @return true if found and displayed, else false.
     */
    public boolean isVisible(Locator locator, String... params) {
        logger.info("Check if " + locator + " is visible for params: " + params);
        try {
            boolean isDisplayed = driver.findElement(Using.locator(locator, params)).isDisplayed();
            return isDisplayed;
        }
        catch (NoSuchElementException e) {
            logger.info("No such element");
            return false;
        }
        catch (StaleElementReferenceException ee) {
            logger.info("Stale reference, try again");
            return isVisible(locator, params);
        }
    }

    /**
     * Looks for a displayed locator, within a parent element (e.g. element of the table, button in a modal), handles NoSuchElementException.
     * @param element - parent element to search in
     * @param locator - dynamic locator
     * @param params - parameters for dynamic locator
     * @return true if found and displayed, else false.
     */
    public boolean isVisible(WebElement element, Locator locator, String... params) {
        logger.info("Check if " + locator + " is visible for params: " + params);
        try {
            boolean isDisplayed = element.findElement(Using.locator(locator, params)).isDisplayed();
            return isDisplayed;
        }
        catch (NoSuchElementException e) {
            return false;
        }

    }


    /**
     * Sometimes DOM will contain a number of elements, which will be impossible to tell apart via locators,
     * but some of which will be visible (usable). This method helps find such *visible* elements.
     * @param locator
     * @return the first visible element found by the locator
     */
    public WebElement findVisibleElement(Locator locator, String... params) {
        logger.info("Find only the visible element matching locator " + locator + " with params: "+ params);
        List<WebElement> elements = driver.findElements(Using.locator(locator, params));
        for (WebElement elt:elements) {
            if(elt.isDisplayed()) {
                return elt;
            }
        }
        throw new NoSuchElementException("Unable to find a single visible element matching locator: " + locator);
    }

    /**
     * Sometimes DOM will contain a number of elements, which will be impossible to tell apart via locators,
     * but some of which will be visible (usable). This method helps wait for at least one of these elements to show.
     * @param timeout timeout value in seconds (use standard timeout constants)
     * @param locator
     * @return the first visible element found by the locator
     */
    public WebElement waitForVisibilityOfAtLeastOneElement(int timeout, Locator locator, String... params) {
        logger.info("Wait for visibility of at least one element matching locator " + locator + " with params: "+ params);
        DateTime timeoutThreshold = DateTime.now().plusSeconds(timeout);
        do {
            try{
                return findVisibleElement(locator, params);
            } catch(NoSuchElementException e){/*as long as the timeout hasn't been reached we are ok to ignore any exceptions*/}
        } while (DateTime.now().isBefore(timeoutThreshold));
        throw new NoSuchElementException("Wait failed: Tried for " + timeout + " seconds, but unable to find a single visible element matching locator: " + locator);
    }


    /**
     * Uses Javascript to get the text value element even if it is invisible.
     * This overcomes Selenium quirk where getText() would return a blank value if the element is fully or partially invisible.
     * Note: when/if we wrap Selenium WebElement, this method should be moved there.
     * @param element page element from which to read text.
     * @return text stored in the element.
     */
    protected String getEvenInvisibleText(WebElement element) {
        String text	= (String) ((JavascriptExecutor) driver.getWebDriver()).executeScript("return $(arguments[0]).text()", element);
        return text;
    }

    /**
     * Updating 14/8/2109 to just do a click once found as clickable
     * Click with confidence
     * @param locator
     */
    public void clickWithConfidence(Locator locator) {
        new WebDriverWait(driver, shortTimeOut).until(
                ExpectedConditions.elementToBeClickable(Using.locator(locator))).click();
    }

    /**
     * Updating 14/8/2109 to just do a click once found as clickable
     * Click with confidence
     * @param locator
     */
    public void clickWithConfidence(Locator locator, String... variables ) {
        new WebDriverWait(driver, shortTimeOut).until(
                ExpectedConditions.elementToBeClickable(Using.locator(locator, variables))).click();
    }
    /**
     * Checks page source for terms (defined with regex) that must not be there. Main use case for it is Baxus, which shouldn't have any references to Kitomba.
     * Idea is to call this from every waitUntilLoaded method, so that all the pages that Selenium goes through will be checked.
     */
    public void checkForForbiddenTerms(){
        checkForForbiddenTerms(SessionProperties.getInstance().getProperty("ForbiddenTerms"));
    }
    /**
     * Checks page source for terms (defined with regex) that must not be there. Main use case for it is Baxus, which shouldn't have any references to Kitomba.
     * Idea is to call this from every waitUntilLoaded method, so that all the pages that Selenium goes through will be checked.
     */
    public void checkForForbiddenTerms(String forbidden){
        if(forbidden!= null && !forbidden.isEmpty()){
            forbidden = forbidden.replace("&quot;", "\"")
                    .replace("&apos;", "'")
                    .replace("&lt;","<")
                    .replace("&gt;",">")
                    .replace("&amp;","&");
            logger.info("Check for forbidden terms appearing in the page: " + forbidden);
            String pageSource = driver.getPageSource();
            Pattern pattern = Pattern.compile(forbidden);
            Matcher matcher = pattern.matcher(pageSource);

            if(matcher.find()) {
                int snippetStart = Math.max(0, matcher.start() - 75);
                int snippetEnd = Math.min(matcher.end()+75, pageSource.length());
                throw new TestError("Forbidden term '" + forbidden + "' found in page source. Snippet:\n" +
                        pageSource.substring(snippetStart, snippetEnd));
            }
        }
    }

    public boolean isOutOfDateBrowserDisplayed() {
        return isVisible(BROWSER_OUT_OF_DATE);
    }

    /**
     * Thread sleep for millis, catches InterruptedException to keep code clean
     * @param millis
     */
    public void sleep(int millis){
        logger.info("Thread.sleep for " + millis + " milli seconds");
        try {
            Thread.sleep(millis);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void switchToDefaultFrame(){
        driver.switchTo().defaultContent();
    }

    protected void firefoxBlurWorkaround(WebElement field) {
        // Send TAB if we are in a firefox browser to allow the onchange event listener to detect the inserted text
        // FF can queue up the changes but then fail to fire the onchange when field doesn't have focus
        //http://stackoverflow.com/questions/4689969/onchange-event-does-not-get-fired-on-selenium-type-command
        //Don't use the direct call to fire the change event ourselves, that could lead us to miss a broken form binding
        if (driver.getBrowserType().equals(Driver.BrowserType.FIREFOX)) {
            field.sendKeys(Keys.TAB);
        }
    }

    /**
     * Clicks on an locator, for example a text field, and then returns true if the IOS keyboard has popped up
     * Only works for a remote driver created by Driver.initRemote
     * @param locator
     * @return true if keyboard is shown
     */
    public boolean checkForIOSKeyboard(Locator locator) {
        driver.waitForClickable(shortTimeOut, locator).click();
        sleep(500); // no way to wait for the IOS keyboard, so need a sleep
        driver.takeScreenshot("keyboard_"+locator.name);

        IOSDriver iosDriver = (IOSDriver) driver.getWebDriver();
        return iosDriver.isKeyboardShown();

    }

    /**
     * Checks if passed in locator is enabled
     * @param locator being checked in the modal
     * @return true if it is
     */
    public boolean isElementEnabled(Locator locator) {
        logger.info("Checks if element in passed in locator is enabled");
        try {
            return driver.findElement(Using.locator(locator)).isEnabled();
        }
        catch (NoSuchElementException e) {
            return false;
        }
    }
}
