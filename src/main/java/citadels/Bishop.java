package citadels;

public class Bishop extends CharacterCard {
    public Bishop() {
        super("Bishop", 5);
    }

    @Override
    public void usePower(Player player, Game game) {
        int bonus = game.countDistrictsByColor(player, "blue");
        player.addGold(bonus);
        System.out.println("You gained " + bonus + " gold from blue districts.");
    }
}
