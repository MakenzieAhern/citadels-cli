package citadels;

import java.util.List;

/** Prints the reference text of a purple district, independent of the user's locale. */
public final class DistrictCardInfoCommand {
    private DistrictCardInfoCommand() {
    }

    public static void execute(String inputName, List<DistrictCard> deck) {
        String name = inputName == null ? "" : inputName.trim();
        if (name.isEmpty()) {
            System.out.println("Usage: info <purple district name>");
            return;
        }
        for (DistrictCard card : deck) {
            if (card.getName().equalsIgnoreCase(name) && card.isPurple()) {
                System.out.println("Card: " + card.getName());
                System.out.println("Color: " + card.getColor());
                System.out.println("Cost: " + card.getCost());
                System.out.println("Special Ability: " + card.getSpecialAbility());
                return;
            }
        }
        System.out.println("No purple district card named '" + name + "' found.");
    }
}
