package PageObjects;

import SeleniumHelpers.Driver;
import SeleniumHelpers.Locator;
import SeleniumHelpers.Using;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Basic login page used to login to the main application.
 */

public class LoginPage extends PageObject {

    public static Locator USERNAME_FIELD = new Locator("USERNAME_FIELD", "css=input#textfield_username");
    public static Locator PASSWORD_FIELD = new Locator("PASSWORD_FIELD", "css=input#textfield_password");
    public static Locator LOGIN_BUTTON   = new Locator("LOGIN_BUTTON",   "css=button#btn_login");

    public static Locator LOGIN_ERROR_MESSAGE = new Locator("LOGIN_ERROR_MESSAGE",  "css=div#error");


    public LoginPage(Driver aDriver) {
        super(aDriver);
    }

    public LoginPage waitUntilLoaded() {
        logger.info("Wait for Login page to load.");
        new WebDriverWait(driver, mediumTimeOut).until(ExpectedConditions.visibilityOfElementLocated(Using.locator(USERNAME_FIELD)));
        new WebDriverWait(driver, shortTimeOut).until(ExpectedConditions.visibilityOfElementLocated(Using.locator(PASSWORD_FIELD)));
        new WebDriverWait(driver, shortTimeOut).until(ExpectedConditions.visibilityOfElementLocated(Using.locator(LOGIN_BUTTON)));
        return this;
    }

    public LandingPage loginWithUsernameAndPassword(String username, String password) {
        inputUsernameAndPassword(username, password);
        clickLoginButton();
        LandingPage landingPage = new LandingPage(driver).waitUntilLoaded();
        return landingPage;
    }

    public void inputUsernameAndPassword(String username, String password) {
        driver.findElement(Using.locator(USERNAME_FIELD)).clear();
        driver.findElement(Using.locator(USERNAME_FIELD)).sendKeys(username);
        driver.findElement(Using.locator(PASSWORD_FIELD)).clear();
        driver.findElement(Using.locator(PASSWORD_FIELD)).sendKeys(password);
    }

    public void clickLoginButton() {
        driver.findElement(Using.locator(LOGIN_BUTTON)).click();
    }

    public boolean isLoginErrorMessageShown() {
        return isVisible(LOGIN_ERROR_MESSAGE);
    }
}
