
package citadels;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class KingTest {

    @Test
    public void testKingGoldBonus() {
        Player player = new Player(1);
        Game game = new Game();
        player.buildDistrict(new DistrictCard("Castle", "yellow", 4, null));
        player.setGold(0);
        King king = new King();
        king.usePower(player, game);
        assertEquals(1, player.getGold());
    }
}
