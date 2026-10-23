package citadels;

import java.util.List;
import java.util.Map;

/** A detached snapshot taken at the human player's command prompt. */
public record GameState(
        List<PlayerState> players,
        List<CardState> districtDeck,
        int crownedPlayerIndex,
        int humanPlayerId,
        List<Integer> characterDeckRanks,
        Map<Integer, Integer> characterAssignments,
        List<Integer> availableCharacterRanks,
        List<Integer> discardedCharacterRanks,
        int killedCharacterRank,
        int stolenCharacterRank,
        boolean debugMode,
        int roundNumber,
        int currentTurnRank,
        boolean currentTurnStarted,
        int firstCompletedPlayerId) {

    public GameState {
        players = List.copyOf(players);
        districtDeck = List.copyOf(districtDeck);
        characterDeckRanks = List.copyOf(characterDeckRanks);
        characterAssignments = Map.copyOf(characterAssignments);
        availableCharacterRanks = List.copyOf(availableCharacterRanks);
        discardedCharacterRanks = List.copyOf(discardedCharacterRanks);
    }

    public record PlayerState(int id, int gold, int buildLimit, int builtThisTurn,
                              List<CardState> hand, List<CardState> built) {
        public PlayerState {
            hand = List.copyOf(hand);
            built = List.copyOf(built);
        }

        public static PlayerState from(Player player, int builtThisTurn) {
            return new PlayerState(player.getId(), player.getGold(), player.getBuildLimit(), builtThisTurn,
                    player.getHand().stream().map(CardState::from).toList(),
                    player.getBuilt().stream().map(CardState::from).toList());
        }

        public Player toPlayer() {
            Player player = new Player(id);
            player.setGold(gold);
            player.setBuildLimit(buildLimit);
            hand.forEach(card -> player.addCard(card.toCard()));
            built.forEach(card -> player.getBuilt().add(card.toCard()));
            return player;
        }
    }

    public record CardState(String name, String color, int cost, String special) {
        public static CardState from(DistrictCard card) {
            return new CardState(card.getName(), card.getColor(), card.getCost(), card.getSpecialAbility());
        }

        public DistrictCard toCard() {
            return new DistrictCard(name, color, cost, special);
        }
    }
}
