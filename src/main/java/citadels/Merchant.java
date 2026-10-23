package citadels;

public class Merchant extends CharacterCard {
    public Merchant() {
        super("Merchant", 6);
    }

    @Override
    public void usePower(Player player, Game game) {
        int greenGold = game.countDistrictsByColor(player, "green");
        player.addGold(greenGold + 1); // +1 bonus gold
        System.out.println("You gained " + greenGold + " gold from green districts and 1 bonus gold.");
    }
}
