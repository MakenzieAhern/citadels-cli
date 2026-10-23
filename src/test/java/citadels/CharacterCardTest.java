
package citadels;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CharacterCardTest {

    @Test
    public void testToString() {
        CharacterCard card = new King();
        assertEquals("King (Rank 4)", card.toString());
    }

    @Test
    public void testGetters() {
        CharacterCard card = new Bishop();
        assertEquals("Bishop", card.getName());
        assertEquals(5, card.getRank());
    }
}
