package citadels;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MagicianTest {
    @Test
    void delegatesAbilityForTheCurrentPlayerExactlyOnce() {
        Player magician = new Player(1);
        class RecordingGame extends Game {
            Player recipient;
            int calls;
            @Override public void executeMagicianAbility(Player player) {
                recipient = player;
                calls++;
            }
        }
        RecordingGame game = new RecordingGame();
        new Magician().usePower(magician, game);
        assertSame(magician, game.recipient);
        assertEquals(1, game.calls);
    }
}
