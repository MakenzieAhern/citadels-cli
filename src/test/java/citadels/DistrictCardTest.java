
package citadels;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DistrictCardTest {

    @Test
    public void testToString() {
        DistrictCard card = new DistrictCard("Castle", "yellow", 4, null);
        assertEquals("Castle [yellow4]", card.toString());
    }

    @Test
    public void testGetters() {
        DistrictCard card = new DistrictCard("Temple", "blue", 1, "Special");
        assertEquals("Temple", card.getName());
        assertEquals("blue", card.getColor());
        assertEquals(1, card.getCost());
        assertEquals("Special", card.getSpecialAbility());
    }
}
