package citadels;

public abstract class CharacterCard {
    private final String name;
    private final int rank;

    public CharacterCard(String name, int rank) {
        this.name = name;
        this.rank = rank;
    }

    public String getName() {
        return name;
    }

    public int getRank() {
        return rank;
    }

    // This method must be implemented in each character class
    public abstract void usePower(Player currentPlayer, Game game);

    @Override
    public String toString() {
        return name + " (Rank " + rank + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        CharacterCard that = (CharacterCard) obj;
        return rank == that.rank && name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode() * 31 + rank;
    }
}
