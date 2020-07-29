package SeleniumHelpers;


public class Locator {

    public String name;
    public String locatorText;

    public Locator(String name, String locatorText) {
        this.name = name;
        this.locatorText = locatorText;
    }

    public String toString() {
        return name + ": " + locatorText;
    }
}
