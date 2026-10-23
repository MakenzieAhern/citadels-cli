package citadels;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MerchantTest {
    @Test
    void earnsGreenDistrictIncomeAndOneBonusGold() {
        Player merchant = new Player(1);
        merchant.setGold(2);
        merchant.getBuilt().add(new DistrictCard("Tavern", "green", 1, null));
        merchant.getBuilt().add(new DistrictCard("Market", "green", 2, null));
        merchant.getBuilt().add(new DistrictCard("Temple", "blue", 1, null));
        merchant.addCard(new DistrictCard("Docks", "green", 3, null));
        new Merchant().usePower(merchant, new Game());
        assertEquals(5, merchant.getGold());
        assertEquals(1, merchant.getHand().size());
    }

    @Test
    void earnsBonusEvenWithoutGreenDistricts() {
        Player merchant = new Player(1);
        new Merchant().usePower(merchant, new Game());
        assertEquals(1, merchant.getGold());
    }
}
