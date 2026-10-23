
package citadels;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AssassinTest {

    @Test
    public void testAssassinPowerKillsCharacter() {
        Game game = new Game();
        game.markCharacterAsKilled(3);
        assertTrue(game.isCharacterKilled(3));
    }
}
