package citadels;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class GameStateStoreTest {
    @TempDir Path directory;

    @Test
    void roundTripPreservesTheEntireCheckpointAndUnicodeFilename() throws Exception {
        GameState original = checkpoint();
        Path file = directory.resolve("My Game 城市.json");
        GameStateStore.save(file, original);
        GameState restored = GameStateStore.load(file);

        assertEquals(original, restored);
        assertTrue(Files.readString(file, StandardCharsets.UTF_8).contains("大学"));
        assertEquals(List.of("Market", "大学"), restored.districtDeck().stream().map(GameState.CardState::name).toList());
        assertEquals(3, restored.currentTurnRank());
        assertTrue(restored.currentTurnStarted());
        assertEquals(1, restored.players().get(0).builtThisTurn());
        assertEquals(2, restored.killedCharacterRank());
        assertEquals(7, restored.stolenCharacterRank());
    }

    @Test
    void dtoCopiesListsAndMaterializesIndependentDomainObjects() {
        Player original = new Player(1);
        original.setGold(5);
        original.setBuildLimit(3);
        original.addCard(new DistrictCard("Market", "green", 2, null));
        original.getBuilt().add(new DistrictCard("University", "purple", 6, "Worth 8 points."));
        GameState.PlayerState detached = GameState.PlayerState.from(original, 1);
        original.getHand().clear();
        original.setGold(0);
        Player restored = detached.toPlayer();
        assertEquals(5, restored.getGold());
        assertEquals(3, restored.getBuildLimit());
        assertEquals(1, restored.getHand().size());
        assertEquals("Worth 8 points.", restored.getBuilt().get(0).getSpecialAbility());
        assertThrows(UnsupportedOperationException.class, () -> detached.hand().clear());
        restored.getHand().clear();
        assertEquals(1, detached.hand().size());

        List<GameState.PlayerState> players = new ArrayList<>(checkpoint().players());
        GameState state = withPlayers(checkpoint(), players);
        players.clear();
        assertEquals(4, state.players().size());
        assertThrows(UnsupportedOperationException.class, () -> state.characterAssignments().clear());
    }

    @Test
    void overwriteReplacesValidSaveAndLeavesNoTemporaryFiles() throws Exception {
        Path file = directory.resolve("game.json");
        Files.writeString(file, "outdated content", StandardCharsets.UTF_8);
        GameStateStore.save(file, checkpoint());
        assertEquals(checkpoint(), GameStateStore.load(file));
        try (var files = Files.list(directory)) { assertEquals(List.of(file), files.toList()); }
    }

    @Test
    void rejectedSnapshotDoesNotOverwriteExistingSave() throws Exception {
        Path file = directory.resolve("game.json");
        GameStateStore.save(file, checkpoint());
        String before = Files.readString(file);
        GameState invalid = withPlayers(checkpoint(), List.of(checkpoint().players().get(0)));
        assertThrows(IOException.class, () -> GameStateStore.save(file, invalid));
        assertEquals(before, Files.readString(file));
    }

    @Test
    void failedDestinationMoveCleansUpTemporaryFile() throws Exception {
        Path destination = Files.createDirectory(directory.resolve("occupied"));
        Files.writeString(destination.resolve("keep.txt"), "keep");
        assertThrows(IOException.class, () -> GameStateStore.save(destination, checkpoint()));
        assertEquals("keep", Files.readString(destination.resolve("keep.txt")));
        try (var files = Files.list(directory)) { assertEquals(List.of(destination), files.toList()); }
    }

    @Test
    void missingDirectoryHasUsefulFailure() {
        IOException failure = assertThrows(IOException.class,
                () -> GameStateStore.save(directory.resolve("missing/game.json"), checkpoint()));
        assertTrue(failure.getMessage().contains("directory"));
    }

    @Test
    void rejectsLegacyAndFutureVersionsRatherThanGuessingMissingTurnState() throws Exception {
        Path legacy = directory.resolve("legacy.json");
        Files.writeString(legacy, "{\"players\":[],\"crownedPlayerIndex\":0}");
        assertThrows(IOException.class, () -> GameStateStore.load(legacy));
        assertInvalid(root -> root.put("version", 99L));
        assertInvalid(root -> root.put("format", "another-game"));
    }

    @Test
    void rejectsTruncatedJsonAndWrongTopLevelType() throws Exception {
        Path file = directory.resolve("broken.json");
        for (String value : List.of("{", "[]", "null", "{\"version\":1,")) {
            Files.writeString(file, value);
            assertThrows(IOException.class, () -> GameStateStore.load(file));
        }
    }

    @Test
    void rejectsMissingOrMistypedFieldsAndOutOfRangeIntegers() throws Exception {
        assertInvalid(root -> root.remove("districtDeck"));
        assertInvalid(root -> root.put("debugMode", "true"));
        assertInvalid(root -> root.put("roundNumber", 1.5));
        assertInvalid(root -> root.put("roundNumber", 2147483648L));
        assertInvalid(root -> root.put("players", "players"));
        assertInvalid(root -> firstPlayer(root).put("hand", null));
    }

    @Test
    void rejectsInvalidPlayerIdentityMoneyAndBuildCounters() throws Exception {
        assertInvalid(root -> firstPlayer(root).put("id", 2L));
        assertInvalid(root -> firstPlayer(root).put("gold", -1L));
        assertInvalid(root -> firstPlayer(root).put("buildLimit", 2L));
        assertInvalid(root -> firstPlayer(root).put("builtThisTurn", 2L));
        assertInvalid(root -> root.put("crownedPlayerIndex", 4L));
        assertInvalid(root -> root.put("humanPlayerId", 9L));
    }

    @Test
    void rejectsBrokenRoleReferencesAndDeckPartitions() throws Exception {
        assertInvalid(root -> ((JSONObject) ((JSONArray) root.get("characterAssignments")).get(0)).put("playerId", 9L));
        assertInvalid(root -> ((JSONArray) root.get("characterAssignments")).add(((JSONArray) root.get("characterAssignments")).get(0)));
        assertInvalid(root -> ((JSONArray) root.get("characterDeckRanks")).set(0, 7L));
        assertInvalid(root -> ((JSONArray) root.get("availableCharacterRanks")).set(0, 3L));
        assertInvalid(root -> ((JSONArray) root.get("discardedCharacterRanks")).remove(0));
        assertInvalid(root -> ((JSONArray) root.get("discardedCharacterRanks")).set(0, 9L));
    }

    @Test
    void rejectsStatesThatWouldReplayOrSkipHumanTurnActions() throws Exception {
        assertInvalid(root -> root.put("currentTurnStarted", false));
        assertInvalid(root -> root.put("currentTurnRank", 4L));
        assertInvalid(root -> root.put("currentTurnRank", 9L));
        assertInvalid(root -> root.put("roundNumber", 0L));
        assertInvalid(root -> root.put("killedCharacterRank", 3L));
        assertInvalid(root -> root.put("killedCharacterRank", 1L));
        assertInvalid(root -> root.put("stolenCharacterRank", 2L));
        assertInvalid(root -> { root.put("killedCharacterRank", 7L); root.put("stolenCharacterRank", 7L); });
    }

    @Test
    void rejectsIncompleteOrInvalidDistrictData() throws Exception {
        assertInvalid(root -> firstCard(root).put("name", " "));
        assertInvalid(root -> firstCard(root).put("color", "orange"));
        assertInvalid(root -> firstCard(root).put("cost", 0L));
        assertInvalid(root -> firstCard(root).put("special", 1L));
        assertInvalid(root -> firstCard(root).remove("special"));
    }

    @Test
    void validatesFirstCityCompleterAndAllowsEmptyDrawPile() throws Exception {
        assertInvalid(root -> root.put("firstCompletedPlayerId", 1L));
        Path file = directory.resolve("complete-city.json");
        JSONObject root = validJson();
        JSONArray built = (JSONArray) firstPlayer(root).get("built");
        for (int i = 1; i < 8; i++) built.add(built.get(0));
        root.put("firstCompletedPlayerId", 1L);
        root.put("districtDeck", new JSONArray());
        Files.writeString(file, root.toJSONString());
        assertEquals(1, GameStateStore.load(file).firstCompletedPlayerId());
        root.put("firstCompletedPlayerId", -1L);
        Files.writeString(file, root.toJSONString());
        assertThrows(IOException.class, () -> GameStateStore.load(file));
    }

    @Test
    void previousPlayersBuildCountSurvivesLaterDestructionOfTheirOnlyDistrict() throws Exception {
        JSONObject root = validJson();
        JSONObject previousPlayer = (JSONObject) ((JSONArray) root.get("players")).get(1);
        previousPlayer.put("builtThisTurn", 1L);
        Path file = directory.resolve("destroyed-city.json");
        Files.writeString(file, root.toJSONString());
        GameState loaded = GameStateStore.load(file);
        assertEquals(1, loaded.players().get(1).builtThisTurn());
        assertTrue(loaded.players().get(1).built().isEmpty());
    }

    private void assertInvalid(Consumer<JSONObject> change) throws Exception {
        JSONObject root = validJson();
        change.accept(root);
        Path file = directory.resolve("invalid.json");
        Files.writeString(file, root.toJSONString(), StandardCharsets.UTF_8);
        assertThrows(IOException.class, () -> GameStateStore.load(file));
    }

    private JSONObject validJson() throws Exception {
        Path file = directory.resolve("source.json");
        GameStateStore.save(file, checkpoint());
        return (JSONObject) new JSONParser().parse(Files.readString(file));
    }

    private static JSONObject firstPlayer(JSONObject root) {
        return (JSONObject) ((JSONArray) root.get("players")).get(0);
    }

    private static JSONObject firstCard(JSONObject root) {
        return (JSONObject) ((JSONArray) root.get("districtDeck")).get(0);
    }

    static GameState checkpoint() {
        GameState.CardState market = new GameState.CardState("Market", "green", 2, null);
        GameState.CardState university = new GameState.CardState("大学", "purple", 6, "Worth 8 points — saved as UTF-8.");
        List<GameState.PlayerState> players = List.of(
                new GameState.PlayerState(1, 8, 1, 1, List.of(university), List.of(market)),
                new GameState.PlayerState(2, 5, 1, 0, List.of(market), List.of()),
                new GameState.PlayerState(3, 3, 1, 0, List.of(university), List.of()),
                new GameState.PlayerState(4, 2, 1, 0, List.of(market), List.of()));
        return new GameState(players, List.of(market, university), 2, 1,
                List.of(8, 7, 6, 5, 4, 3, 2, 1), Map.of(1, 3, 2, 4, 3, 5, 4, 6),
                List.of(8), List.of(1, 2, 7), 2, 7, true, 4, 3, true, -1);
    }

    private static GameState withPlayers(GameState state, List<GameState.PlayerState> players) {
        return new GameState(players, state.districtDeck(), state.crownedPlayerIndex(), state.humanPlayerId(),
                state.characterDeckRanks(), new LinkedHashMap<>(state.characterAssignments()),
                state.availableCharacterRanks(), state.discardedCharacterRanks(), state.killedCharacterRank(),
                state.stolenCharacterRank(), state.debugMode(), state.roundNumber(), state.currentTurnRank(),
                state.currentTurnStarted(), state.firstCompletedPlayerId());
    }
}
