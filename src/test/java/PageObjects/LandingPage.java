package PageObjects;

/**
 * Page that you land on after logging in successfully.
 */

import SeleniumHelpers.Driver;
import SeleniumHelpers.Locator;
import SeleniumHelpers.Using;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Basic landing page of the main application.
 */

public class LandingPage extends PageObject {

    public static Locator RADIO_BUTTON1      = new Locator("RADIO_BUTTON1",      "css=input#radbtn_1");
    public static Locator RADIO_BUTTON2      = new Locator("RADIO_BUTTON2",      "css=input#radbtn_2");
    public static Locator RADIO_BUTTON3      = new Locator("RADIO_BUTTON3",      "css=input#radbtn_3");

    public static Locator TEXT_RADIO_BUTTON1 = new Locator("TEXT_RADIO_BUTTON1", "css=button#text_radbtn1");
    public static Locator TEXT_RADIO_BUTTON2 = new Locator("TEXT_RADIO_BUTTON2", "css=button#text_radbtn2");
    public static Locator TEXT_RADIO_BUTTON3 = new Locator("TEXT_RADIO_BUTTON3", "css=button#text_radbtn3");

    public static Locator SUBMIT_BUTTON      = new Locator("SUBMIT_BUTTON",      "css=button#btn_submit");


    public LandingPage(Driver aDriver) {
        super(aDriver);
    }

    public LandingPage waitUntilLoaded() {
        logger.info("Wait for Landing page to load.");
        new WebDriverWait(driver, mediumTimeOut).until(ExpectedConditions.visibilityOfElementLocated(Using.locator(RADIO_BUTTON1)));
        new WebDriverWait(driver, shortTimeOut).until(ExpectedConditions.visibilityOfElementLocated(Using.locator(RADIO_BUTTON2)));
        new WebDriverWait(driver, shortTimeOut).until(ExpectedConditions.visibilityOfElementLocated(Using.locator(RADIO_BUTTON3)));
        return this;
    }

    public boolean areAllLandingPageElementsVisible() {
        if (isVisible(SUBMIT_BUTTON) && isVisible(RADIO_BUTTON1)
                && isVisible(RADIO_BUTTON2) && isVisible(RADIO_BUTTON3)
                && isVisible(TEXT_RADIO_BUTTON1) && isVisible(TEXT_RADIO_BUTTON2)
                && isVisible(TEXT_RADIO_BUTTON3)) {
            return true;
        }
        else return false;
    }
}
