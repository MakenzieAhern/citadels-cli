
package citadels;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class WarlordTest {

    @Test
    public void testRedDistrictBonus() {
        Player warlord = new Player(1);
        warlord.buildDistrict(new DistrictCard("Watchtower", "red", 1, null));
        Game game = new Game();
        int redCount = game.countDistrictsByColor(warlord, "red");
        assertEquals(1, redCount);
    }
}
