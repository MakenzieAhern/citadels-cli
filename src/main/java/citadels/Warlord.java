package citadels;

public class Warlord extends CharacterCard {
    public Warlord() {
        super("Warlord", 8);
    }

    @Override
    public void usePower(Player player, Game game) {
        int redBonus = game.countDistrictsByColor(player, "red");
        player.addGold(redBonus);
        System.out.println("You gained " + redBonus + " gold from red districts.");
        game.executeWarlordAbility(player);
    }
}
