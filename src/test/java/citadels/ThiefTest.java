package citadels;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ThiefTest {
    @Test
    void recordsChosenCharacterUsingTheAllowedTargetRange() {
        TargetGame game = new TargetGame(6);
        new Thief().usePower(new Player(1), game);
        assertEquals(3, game.requestedMin);
        assertEquals(8, game.requestedMax);
        assertEquals(6, game.getStolenCharacter());
    }

    @Test
    void doesNotStealFromAnAssassinatedCharacter() {
        TargetGame game = new TargetGame(6);
        game.markCharacterAsKilled(6);
        new Thief().usePower(new Player(1), game);
        assertEquals(-1, game.getStolenCharacter());
    }

    @Test
    void rejectsAssassinEvenIfInputProviderReturnsIt() {
        TargetGame game = new TargetGame(1);
        new Thief().usePower(new Player(1), game);
        assertEquals(-1, game.getStolenCharacter());
    }

    private static final class TargetGame extends Game {
        private final int target;
        private int requestedMin;
        private int requestedMax;
        TargetGame(int target) { this.target = target; }
        @Override public int promptCharacterChoice(String message, int min, int max) {
            requestedMin = min;
            requestedMax = max;
            return target;
        }
    }
}
