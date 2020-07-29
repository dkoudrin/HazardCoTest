package SeleniumHelpers;


import Logger;
import org.apache.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.internal.FindsByCssSelector;
import org.openqa.selenium.internal.FindsByXPath;

import java.io.Serializable;
import java.util.List;

public abstract class Using extends By {

    public static By locator(final Locator locator) {
        if (locator == null)
            throw new IllegalArgumentException(
                    "Cannot find elements with a null locator.");
        return new ByLocator(locator);
    }

    public static By locator(final Object object, String... variables) {
        if ((Locator)object == null)
            throw new IllegalArgumentException(
                    "Cannot find elements with a null locator.");
        return new ByLocator(processVariableLocator((Locator)object, variables));
    }

    public static By locator(final Locator locator, int variable) {
        if (locator == null)
            throw new IllegalArgumentException(
                    "Cannot find elements with a null locator.");
        return new ByLocator(processVariableLocator(locator, "" + variable));
    }

    public static Locator processVariableLocator(Locator locator, int variable) {
        return processVariableLocator(locator, "" + variable);
    }

    public static Locator processVariableLocator(Locator locator, String... variables) {
        Locator processedLocator = new Locator(locator.name, locator.locatorText);
        int totalVars = variables.length;
        if(totalVars > 0) {
            if(totalVars == 1) {
                processedLocator.locatorText = processedLocator.locatorText.replace("{}","{1}");
            }
            for (int i = 0; i < totalVars; i++) {
                if (i == 0) {
                    processedLocator.name += "(" + variables[i];
                }
                else if (i > 0 && i < totalVars) {
                    if (totalVars > 1) {
                        {
                            processedLocator.name += ", " + variables[i];
                        }
                    }
                    else {
                        {
                            processedLocator.name += variables[i];
                        }
                    }
                }
                if (i == totalVars - 1) {
                    processedLocator.name += ")";
                }
                if (processedLocator.locatorText.indexOf("{" + (i + 1) + "}") > -1) {
                    processedLocator.locatorText = processedLocator.locatorText.replace("{" + (i + 1) + "}", variables[i]);
                }
                else {
                    Logger logger = new K1Logger().setUpLogger("Using"); // Not ideal that we have another log file, but this is a static method
                    logger.info("WARNING: Some of the supplied variables are NOT used by the locator: " + locator.toString());
                }
            }
        }
        if(processedLocator.locatorText.split("\\{([0-9]*)\\}").length > 1) { // we still have a {N} in the locator
            throw new TestError("TEST/LOCATOR ERROR: Insufficient parameters supplied for dynamic locator");
        }
        return processedLocator;
    }

    public static class ByLocator extends By implements Serializable {

        private static final long serialVersionUID = -540918614223537731L;
        private final Locator locator;
        public ByLocator(Locator locator) {
            this.locator = locator;
        }

        @Override
        public List<WebElement> findElements(SearchContext context) {
            if(locator.locatorText.toLowerCase().startsWith("xpath=")) {
                return ((FindsByXPath) context).findElementsByXPath(locator.locatorText.substring(6));
            }
            else if(locator.locatorText.toLowerCase().startsWith("css=")) {
                return ((FindsByCssSelector) context).findElementsByCssSelector(locator.locatorText.substring(4));
            }
            else {
                throw new WebDriverException("Unknown locator type used for locator: " + locator.toString());
            }
        }

        @Override
        public WebElement findElement(SearchContext context) {
            if(locator.locatorText.toLowerCase().startsWith("xpath=")) {
                return ((FindsByXPath) context).findElementByXPath(locator.locatorText.substring(6));
            }
            else if(locator.locatorText.toLowerCase().startsWith("css=")) {
                return ((FindsByCssSelector) context).findElementByCssSelector(locator.locatorText.substring(4));
            }
            else {
                throw new WebDriverException("Unknown locator type used for locator: " + locator.toString());
            }
        }

        @Override
        public String toString() {
            return locator.name + ": " + locator.locatorText;
        }
    }

    public static By locator(Object declaredField) {
        return locator((Locator)declaredField);
    }
}
