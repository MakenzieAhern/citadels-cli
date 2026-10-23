
package citadels;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PlayerTest {

    @Test
    public void testAddAndSpendGold() {
        Player player = new Player(1);
        player.addGold(5);
        assertEquals(5, player.getGold());
        player.addGold(-2);
        assertEquals(3, player.getGold());
    }

    @Test
    public void testBuildDistrict() {
        Player player = new Player(1);
        DistrictCard card = new DistrictCard("Temple", "blue", 1, null);
        player.addCard(card);
        player.buildDistrict(card);
        assertTrue(player.getBuilt().contains(card));
        assertFalse(player.getHand().contains(card));
    }

    @Test
    public void testEqualsAndHashCode() {
        Player p1 = new Player(1);
        Player p2 = new Player(1);
        Player p3 = new Player(2);
        assertEquals(p1, p2);
        assertNotEquals(p1, p3);
        assertEquals(p1.hashCode(), p2.hashCode());
    }
}
