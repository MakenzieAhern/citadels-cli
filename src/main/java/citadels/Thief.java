package citadels;

public class Thief extends CharacterCard {
    public Thief() {
        super("Thief", 2);
    }

    @Override
    public void usePower(Player player, Game game) {
        int choice = game.promptCharacterChoice("Who do you want to steal from? (3-8): ", 3, 8);
        if (choice == 1 || game.isCharacterKilled(choice)) {
            System.out.println("Invalid target. Cannot steal from Assassin or a killed character.");
        } else {
            game.setStolenCharacter(choice);
            System.out.println("You chose to steal from character " + choice);
        }
    }
}
