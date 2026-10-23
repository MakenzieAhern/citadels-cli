package citadels;

public class Architect extends CharacterCard {
    public Architect() {
        super("Architect", 7);
    }

    @Override
    public void usePower(Player player, Game game) {
        game.drawCards(player, 2);
        player.setBuildLimit(3);  // Allow building up to 3 districts this turn
        System.out.println("You drew 2 extra cards. You can build up to 3 districts this turn.");
    }
}
