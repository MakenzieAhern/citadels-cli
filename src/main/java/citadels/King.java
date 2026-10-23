package citadels;

public class King extends CharacterCard {
    public King() {
        super("King", 4);
    }

    @Override
    public void usePower(Player player, Game game) {
        int bonus = game.countDistrictsByColor(player, "yellow");
        player.addGold(bonus);
        game.setCrownedPlayer(player);
        System.out.println("You gained " + bonus + " gold from yellow districts and claimed the crown.");
    }
}
