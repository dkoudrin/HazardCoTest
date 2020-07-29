import PageObjects.LandingPage;
import PageObjects.LoginPage;
import SeleniumHelpers.Driver;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Dmitri Koudrin
 * The following is a possible way that I would automate the Application/Screens given in the
 * HazardCo test instructions.
 * I have used Intellij and created a simple java/maven project (of course it cannot run since this is just
 * a dummy application/screens).
 * I have used a few classes and methods from my previous selenium codebase to give an idea of structure.
 */

@TestDoc(description =
        "Test cases for successful and unsuccessful login attempts.")
@RunWith(Parameterized.class)
public class successfulLoginTest {

    // params
    private String userName;
    private String password;
    private boolean validCredentials;
    private static Driver driver;

    @Parameterized.Parameters(name = "Username({0}), Password({1}), Valid({2})")
    public static Collection<Object[]> setParameters() {
        Collection<Object[]> params = new ArrayList<>();
        params.add(new Object[]{"UserNameOne", "PasswordOne", true});
        params.add(new Object[]{"UserNameTwo", "PasswordTwo", true});
        params.add(new Object[]{"UserNameThree", "PasswordThree", true});
        params.add(new Object[]{"fakeUsername", "fakePassword", false});
        return params;
    }

    public successfulLoginTest(String userName, String password, boolean validCredentials) {
        this.userName = userName;
        this.password = password;
        this.validCredentials = validCredentials;
    }

    @Before
    public static void setUp() {
        // Create a new instance of the driver
        driver = new Driver("testLogger");

        // Go to the test url (we don't actually have one, so it's just made up)
        logger.info("GIVEN: User has landed on the Login page");
        driver.get("https://www.hazardco.test.com");
    }

    public void loginTests() {
        String testName = "loginTests";
        logger.info("\n***** " + testName + " *****");
        logger.info("WHEN: User enters credentials");
        LoginPage loginPage = new LoginPage(driver).waitUntilLoaded();
        LandingPage landingPage = loginPage.loginWithUsernameAndPassword(userName, password);

        // perform assertions based on test parameters
        if (validCredentials) {
            logger.info("THEN: User is logged in (landing page is shown with all expected elements)");
            assertTrue(landingPage.areAllLandingPageElementsVisible());
        } else {
            logger.info("THEN: User is NOT logged in (error message shown instead)");
            assertTrue(loginPage.isLoginErrorMessageShown());
        }
    }


    @AfterClass
    public static void finalWrapUp() {
        // Close the driver
        driver.quit();
    }
}
