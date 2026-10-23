package citadels;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BishopTest {
    @Test
    void earnsIncomeFromBuiltBlueDistrictsOnly() {
        Player bishop = new Player(1);
        bishop.setGold(3);
        bishop.getBuilt().add(new DistrictCard("Temple", "blue", 1, null));
        bishop.getBuilt().add(new DistrictCard("Church", "BLUE", 2, null));
        bishop.getBuilt().add(new DistrictCard("Castle", "yellow", 4, null));
        bishop.addCard(new DistrictCard("Cathedral", "blue", 5, null));
        new Bishop().usePower(bishop, new Game());
        assertEquals(5, bishop.getGold());
        assertEquals(1, bishop.getHand().size());
        assertEquals(3, bishop.getBuilt().size());
    }

    @Test
    void noBlueDistrictsMeansNoIncome() {
        Player bishop = new Player(1);
        bishop.setGold(3);
        new Bishop().usePower(bishop, new Game());
        assertEquals(3, bishop.getGold());
    }
}
