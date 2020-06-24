import io.restassured.RestAssured;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.is;

/**
 * @author Dmitri Koudrin
 * The following has been split into three test cases as per the instruction sheet.
 * I have used Intellij with Maven to run the tests as junit.
 * The pom file contains all the required dependencies that I needed to get this running.
 */

public class trademeAPITests {

    private static JSONArray usedCarBrands;

    @BeforeClass
    public static void initialSetup() throws ParseException {

        // The base URI to be used
        RestAssured.baseURI = "https://api.tmsandbox.co.nz/v1/";

        // Get the UsedCars endpoint in json format and extract it as a JSONObject
        String jsonResponse = RestAssured.get("Categories/UsedCars.json?with_counts=true")
                .then().extract().asString();
        JSONParser parser = new JSONParser();
        JSONObject jsonCars = (JSONObject) parser.parse(jsonResponse);

        // Get all the subcategories (car brands) as a JSONArray
        usedCarBrands = (JSONArray) jsonCars.get("Subcategories");
    }

    @Test
    /*
      Print out the number of unique named brands of used cars that are
      available in the TradeMe Used Cars category.
     */
    public void getAllNamedCarBrands() {
        assertThat("The number of Used car brands returned should be positive.",
                usedCarBrands.isEmpty(), is(false));
        System.out.print("The number of named brands of used cars is "
                + usedCarBrands.size());
    }

    @Test
    /*
      Check that the brand 'Kia' exists in the used cars category.
      Print out the current number of Kia cars listed.
     */
    public void doesKiaBrandExist() {

        boolean doesKiaExist=false;
        int kiaCount = 0;

        // Go through the Subcategories Jsonarray and check that it contains "Kia" brand.
        for (Object usedCarBrand : usedCarBrands) {
            JSONObject carCategory = (JSONObject) usedCarBrand;
            String brandName = carCategory.get("Name").toString();
            if (brandName.equals("Kia")) {
                doesKiaExist = true;
                kiaCount = Integer.parseInt(String.valueOf(carCategory.get("Count")));
            }
        }
        assertTrue("Kia brand should have existed in the list of used cars.",
                doesKiaExist);
        System.out.print("The number of used Kia cars currently listed is "
                + kiaCount);
    }

    @Test
    /*
      Check that the brand 'Hispano Suiza' does not exist in the used cars category.
     */
    public void doesHispanoSuizaBrandExist() {

        boolean doesHispanoSuizaExist=false;

        // Go through the Subcategories Jsonarray and check that it contains "Kia" brand.
        for (Object usedCarBrand : usedCarBrands) {
            JSONObject carCategory = (JSONObject) usedCarBrand;
            String brandName = carCategory.get("Name").toString();
            if (brandName.equals("Hispano Suiza")) {
                doesHispanoSuizaExist = true;
            }
        }
        assertFalse("Hispano Suiza brand should NOT have existed in" +
                    " the list of used cars.", doesHispanoSuizaExist);
    }
}
