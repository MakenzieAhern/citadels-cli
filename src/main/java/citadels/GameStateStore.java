package citadels;

import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Versioned UTF-8 save files. Loading never mutates a live game. */
public final class GameStateStore {
    public static final int FORMAT_VERSION = 1;
    private static final long MAX_FILE_BYTES = 10 * 1024 * 1024;
    private static final Set<String> COLORS = Set.of("red", "yellow", "green", "blue", "purple");

    private GameStateStore() {}

    /** Validate before touching the destination, then replace it with one atomic filesystem move. */
    public static void save(Path path, GameState state) throws IOException {
        validate(state);
        String json = JSONValue.toJSONString(toJson(state)) + System.lineSeparator();
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_FILE_BYTES) throw invalid("Save exceeds the 10 MiB size limit");
        Path destination = path.toAbsolutePath().normalize();
        Path parent = destination.getParent();
        if (parent == null || !Files.isDirectory(parent)) {
            throw new IOException("Save directory does not exist: " + parent);
        }
        Path temporary = Files.createTempFile(parent, ".citadels-save-", ".tmp");
        try {
            Files.write(temporary, bytes);
            // Do not silently fall back to a non-atomic overwrite on unsupported filesystems.
            Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    /** Return a completely parsed and validated snapshot, or throw without changing application state. */
    public static GameState load(Path path) throws IOException {
        if (Files.size(path) > MAX_FILE_BYTES) throw invalid("Save exceeds the 10 MiB size limit");
        try {
            Map<?, ?> root = object(new JSONParser().parse(Files.readString(path, StandardCharsets.UTF_8)), "save");
            if (integer(root, "version") != FORMAT_VERSION) {
                throw invalid("Unsupported save version; expected " + FORMAT_VERSION);
            }
            if (!"citadels-cli".equals(string(root, "format"))) throw invalid("Unrecognized save format");
            List<GameState.PlayerState> players = new ArrayList<>();
            for (Object value : array(root, "players")) {
                Map<?, ?> player = object(value, "player");
                players.add(new GameState.PlayerState(integer(player, "id"), integer(player, "gold"),
                        integer(player, "buildLimit"), integer(player, "builtThisTurn"),
                        cards(player, "hand"), cards(player, "built")));
            }
            Map<Integer, Integer> assignments = new LinkedHashMap<>();
            for (Object value : array(root, "characterAssignments")) {
                Map<?, ?> assignment = object(value, "character assignment");
                int id = integer(assignment, "playerId");
                if (assignments.put(id, integer(assignment, "rank")) != null) {
                    throw invalid("Duplicate character assignment for player " + id);
                }
            }
            GameState state = new GameState(players, cards(root, "districtDeck"),
                    integer(root, "crownedPlayerIndex"), integer(root, "humanPlayerId"),
                    ranks(root, "characterDeckRanks"), assignments, ranks(root, "availableCharacterRanks"),
                    ranks(root, "discardedCharacterRanks"), integer(root, "killedCharacterRank"),
                    integer(root, "stolenCharacterRank"), bool(root, "debugMode"),
                    integer(root, "roundNumber"), integer(root, "currentTurnRank"),
                    bool(root, "currentTurnStarted"), integer(root, "firstCompletedPlayerId"));
            validate(state);
            return state;
        } catch (ParseException | IllegalArgumentException | NullPointerException e) {
            throw new IOException("Invalid save data: " + e.getMessage(), e);
        }
    }

    public static void validate(GameState state) throws IOException {
        if (state == null) throw invalid("Snapshot is missing");
        int count = state.players().size();
        require(count >= 4 && count <= 7, "A saved game must have 4 to 7 players");
        Set<Integer> playerIds = new HashSet<>();
        int completedCities = 0;
        for (int index = 0; index < count; index++) {
            GameState.PlayerState player = state.players().get(index);
            require(player.id() == index + 1 && playerIds.add(player.id()), "Player IDs must be ordered from 1");
            require(player.gold() >= 0, "Player gold cannot be negative");
            require(player.buildLimit() == 1 || player.buildLimit() == 3, "Invalid player build limit");
            require(player.builtThisTurn() >= 0 && player.builtThisTurn() <= player.buildLimit(),
                    "Build count exceeds the turn limit");
            validateCards(player.hand());
            validateCards(player.built());
            if (player.built().size() >= 8) completedCities++;
        }
        validateCards(state.districtDeck());
        require(state.crownedPlayerIndex() >= 0 && state.crownedPlayerIndex() < count, "Invalid crown index");
        require(playerIds.contains(state.humanPlayerId()), "Unknown human player");
        require(state.roundNumber() >= 1, "Round number must be positive");
        require(state.currentTurnStarted(), "Saves must be taken at the human command prompt");
        require(state.currentTurnRank() >= 1 && state.currentTurnRank() <= 8, "Invalid current character rank");
        require(state.characterAssignments().keySet().equals(playerIds), "Every player must have one character");
        require(state.characterAssignments().get(state.humanPlayerId()) == state.currentTurnRank(),
                "Current character must belong to the human player");

        Set<Integer> deckRanks = new HashSet<>();
        for (int rank : state.characterDeckRanks()) addRank(deckRanks, rank);
        require(deckRanks.size() == 8, "Character deck must contain all eight ranks");
        Set<Integer> partition = new HashSet<>();
        for (int rank : state.characterAssignments().values()) addRank(partition, rank);
        for (int rank : state.availableCharacterRanks()) addRank(partition, rank);
        for (int rank : state.discardedCharacterRanks()) addRank(partition, rank);
        require(partition.equals(deckRanks), "Assigned, available and discarded characters must partition the deck");
        int expectedDiscards = count == 4 ? 3 : count == 5 ? 2 : 1;
        require(state.discardedCharacterRanks().size() == expectedDiscards, "Incorrect number of discarded characters");
        require(state.killedCharacterRank() == -1 || (state.killedCharacterRank() >= 2 && state.killedCharacterRank() <= 8),
                "Invalid assassination target");
        require(state.stolenCharacterRank() == -1 || (state.stolenCharacterRank() >= 3 && state.stolenCharacterRank() <= 8),
                "Invalid theft target");
        require(state.killedCharacterRank() != state.currentTurnRank(), "An assassinated human cannot have an active turn");
        require(state.stolenCharacterRank() == -1 || state.stolenCharacterRank() != state.killedCharacterRank(),
                "The Thief cannot target an assassinated character");
        if (state.firstCompletedPlayerId() == -1) {
            require(completedCities == 0, "Completed city is missing its first completer");
        } else {
            require(playerIds.contains(state.firstCompletedPlayerId()), "Unknown first completer");
            require(state.players().get(state.firstCompletedPlayerId() - 1).built().size() >= 8,
                    "First completer must have at least eight districts");
        }
    }

    private static void validateCards(List<GameState.CardState> cards) throws IOException {
        for (GameState.CardState card : cards) {
            require(card.name() != null && !card.name().isBlank(), "District name is missing");
            require(card.color() != null && COLORS.contains(card.color().toLowerCase(Locale.ROOT)), "Unknown district color");
            require(card.cost() >= 1, "District cost must be positive");
        }
    }

    private static void addRank(Set<Integer> seen, int rank) throws IOException {
        require(rank >= 1 && rank <= 8, "Character rank is outside 1 to 8");
        require(seen.add(rank), "Duplicate character rank " + rank);
    }

    private static Map<String, Object> toJson(GameState state) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("format", "citadels-cli");
        root.put("version", FORMAT_VERSION);
        root.put("players", state.players().stream().map(player -> {
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("id", player.id()); value.put("gold", player.gold());
            value.put("buildLimit", player.buildLimit()); value.put("builtThisTurn", player.builtThisTurn());
            value.put("hand", toJsonCards(player.hand())); value.put("built", toJsonCards(player.built()));
            return value;
        }).toList());
        root.put("districtDeck", toJsonCards(state.districtDeck()));
        root.put("crownedPlayerIndex", state.crownedPlayerIndex());
        root.put("humanPlayerId", state.humanPlayerId());
        root.put("characterDeckRanks", state.characterDeckRanks());
        root.put("characterAssignments", state.characterAssignments().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> Map.of("playerId", entry.getKey(), "rank", entry.getValue())).toList());
        root.put("availableCharacterRanks", state.availableCharacterRanks());
        root.put("discardedCharacterRanks", state.discardedCharacterRanks());
        root.put("killedCharacterRank", state.killedCharacterRank());
        root.put("stolenCharacterRank", state.stolenCharacterRank());
        root.put("debugMode", state.debugMode());
        root.put("roundNumber", state.roundNumber());
        root.put("currentTurnRank", state.currentTurnRank());
        root.put("currentTurnStarted", state.currentTurnStarted());
        root.put("firstCompletedPlayerId", state.firstCompletedPlayerId());
        return root;
    }

    private static List<Map<String, Object>> toJsonCards(List<GameState.CardState> cards) {
        return cards.stream().map(card -> {
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("name", card.name()); value.put("color", card.color());
            value.put("cost", card.cost()); value.put("special", card.special());
            return value;
        }).toList();
    }

    private static List<GameState.CardState> cards(Map<?, ?> source, String field) throws IOException {
        List<GameState.CardState> result = new ArrayList<>();
        for (Object value : array(source, field)) {
            Map<?, ?> card = object(value, field + " card");
            Object special = required(card, "special");
            if (special != null && !(special instanceof String)) throw invalid("Card special must be text or null");
            result.add(new GameState.CardState(string(card, "name"), string(card, "color"),
                    integer(card, "cost"), (String) special));
        }
        return result;
    }

    private static List<Integer> ranks(Map<?, ?> source, String field) throws IOException {
        List<Integer> result = new ArrayList<>();
        for (Object value : array(source, field)) result.add(integer(value, field));
        return result;
    }

    private static Object required(Map<?, ?> source, String field) throws IOException {
        if (!source.containsKey(field)) throw invalid("Missing field: " + field);
        return source.get(field);
    }

    private static Map<?, ?> object(Object value, String field) throws IOException {
        if (!(value instanceof Map<?, ?> object)) throw invalid(field + " must be an object");
        return object;
    }

    private static List<?> array(Map<?, ?> source, String field) throws IOException {
        if (!(required(source, field) instanceof List<?> array)) throw invalid(field + " must be an array");
        return array;
    }

    private static String string(Map<?, ?> source, String field) throws IOException {
        if (!(required(source, field) instanceof String string)) throw invalid(field + " must be text");
        return string;
    }

    private static boolean bool(Map<?, ?> source, String field) throws IOException {
        if (!(required(source, field) instanceof Boolean value)) throw invalid(field + " must be true or false");
        return value;
    }

    private static int integer(Map<?, ?> source, String field) throws IOException {
        return integer(required(source, field), field);
    }

    private static int integer(Object value, String field) throws IOException {
        if (!(value instanceof Long number) || number < Integer.MIN_VALUE || number > Integer.MAX_VALUE) {
            throw invalid(field + " must be a 32-bit integer");
        }
        return number.intValue();
    }

    private static void require(boolean condition, String message) throws IOException {
        if (!condition) throw invalid(message);
    }

    private static IOException invalid(String message) { return new IOException("Invalid save data: " + message); }
}
