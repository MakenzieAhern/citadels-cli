package citadels;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArchitectTest {
    @Test
    void drawsTwoExtraCardsAndRaisesBuildLimitForCurrentPlayer() {
        Player architect = new Player(1);
        Player otherPlayer = new Player(2);
        class RecordingGame extends Game {
            Player recipient;
            int requestedCards;
            @Override public void drawCards(Player player, int count) {
                recipient = player;
                requestedCards += count;
            }
        }
        RecordingGame game = new RecordingGame();
        new Architect().usePower(architect, game);
        assertSame(architect, game.recipient);
        assertEquals(2, game.requestedCards);
        assertEquals(3, architect.getBuildLimit());
        assertEquals(1, otherPlayer.getBuildLimit());
        architect.resetBuildLimit();
        assertEquals(1, architect.getBuildLimit());
    }
}
