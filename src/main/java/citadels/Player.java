package citadels;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Player {
    private final int id;
    private int gold = 0;
    private final List<DistrictCard> hand = new ArrayList<>();
    private final List<DistrictCard> built = new ArrayList<>();
    private int buildLimit = 1;

    public Player(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public int getGold() {
        return gold;
    }

    public void setGold(int gold) {
        this.gold = gold;
    }

    public void addGold(int amount) {
        this.gold += amount;
    }

    public List<DistrictCard> getHand() {
        return hand;
    }

    public void addCard(DistrictCard card) {
        hand.add(card);
    }

    public List<DistrictCard> getBuilt() {
        return built;
    }

    public void buildDistrict(DistrictCard card) {
        built.add(card);
        hand.remove(card);
    }

    public int getBuildLimit() {
        return buildLimit;
    }

    public void setBuildLimit(int buildLimit) {
        this.buildLimit = buildLimit;
    }

    public void resetBuildLimit() {
        this.buildLimit = 1;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Player player = (Player) obj;
        return id == player.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

}
