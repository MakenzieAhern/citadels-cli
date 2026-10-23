package citadels;

public class Assassin extends CharacterCard {
    public Assassin() {
        super("Assassin", 1);
    }

    @Override
    public void usePower(Player player, Game game) {
        int choice = game.promptCharacterChoice("Who do you want to assassinate? (2-8): ", 2, 8);
        game.markCharacterAsKilled(choice);
        System.out.println("You assassinated character " + choice);
    }
}
