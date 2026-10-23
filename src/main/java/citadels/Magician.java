package citadels;

public class Magician extends CharacterCard {
    public Magician() {
        super("Magician", 3);
    }

    @Override
    public void usePower(Player player, Game game) {
        game.executeMagicianAbility(player);
    }
}
