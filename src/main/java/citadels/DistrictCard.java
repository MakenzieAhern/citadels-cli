package citadels;

public class DistrictCard {
    private final String name;
    private final String color;
    private final int cost;
    private final String specialAbility; // nullable, used for purple cards

    public DistrictCard(String name, String color, int cost, String specialAbility) {
        this.name = name;
        this.color = color;
        this.cost = cost;
        this.specialAbility = specialAbility;
    }

    public String getName() {
        return name;
    }

    public String getColor() {
        return color;
    }

    public int getCost() {
        return cost;
    }

    public String getSpecialAbility() {
        return specialAbility;
    }

    public boolean isPurple() {
        return "purple".equalsIgnoreCase(color);
    }

    @Override
    public String toString() {
        return name + " [" + color + cost + "]";
    }
}
