package citadels;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.junit.jupiter.api.io.TempDir;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GameTest {
    @TempDir Path temporary;
    private static GameState.CardState card(String name, String color, int cost) {
        return new GameState.CardState(name, color, cost, null);
    }

    private static GameState.PlayerState player(int id, int gold, int limit, int used,
                                                List<GameState.CardState> hand, List<GameState.CardState> built) {
        return new GameState.PlayerState(id, gold, limit, used, hand, built);
    }

    private static GameState checkpoint(List<GameState.PlayerState> players, List<GameState.CardState> deck,
                                         Map<Integer, Integer> roles, int killed, int stolen, int first) {
        List<Integer> unused = new ArrayList<>(List.of(1, 2, 3, 4, 5, 6, 7, 8));
        unused.removeAll(roles.values());
        return new GameState(players, deck, 0, 1, List.of(1,2,3,4,5,6,7,8), roles,
                List.of(unused.remove(0)), unused, killed, stolen, false, 1, roles.get(1), true, first);
    }

    private static List<GameState.PlayerState> players(GameState.PlayerState human) {
        return new ArrayList<>(List.of(human, player(2, 2, 1, 0, List.of(), List.of()),
                player(3, 2, 1, 0, List.of(), List.of()), player(4, 2, 1, 0, List.of(), List.of())));
    }

    private static Game game(String input, GameState state) {
        Game game = new Game(new Scanner(input), new Random(42));
        game.restoreState(state);
        return game;
    }

    private static String capture(Runnable run) {
        PrintStream original = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
            run.run();
        } finally { System.setOut(original); }
        return output.toString(StandardCharsets.UTF_8);
    }

    @Test void emptyInputAndQuitExitCleanlyBeforeSetup() {
        for (String input : List.of("", "quit\n", "invalid\n99\nexit\n")) {
            assertTrue(capture(() -> new Game(new Scanner(input), new Random(1)).start()).contains("Goodbye."));
        }
    }

    @Test void seededSelectionsAndDealsAreReproducible() {
        String first = capture(() -> new Game(new Scanner("4\nquit\n"), new Random(7)).start());
        String second = capture(() -> new Game(new Scanner("4\nquit\n"), new Random(7)).start());
        assertEquals(first, second);
        assertTrue(first.contains("ROUND 1"));
    }

    @Test void buildTransactionRejectsBadIndicesInsufficientGoldAndDuplicates() {
        var hand = List.of(card("Market", "green", 2), card("Castle", "yellow", 4), card("TAVERN", "green", 1));
        Game game = game("", checkpoint(players(player(1,3,1,0,hand,List.of(card("Tavern","green",1)))),
                List.of(), Map.of(1,4,2,5,3,6,4,8),-1,-1,-1));
        Player human = game.getPlayerByCharacterRank(4);
        assertEquals("Invalid card number.", game.buildDistrict(human, -1));
        assertEquals("Not enough gold.", game.buildDistrict(human, 1));
        assertEquals("You already have a district with that name.", game.buildDistrict(human, 2));
        assertEquals(3, human.getGold());
        assertEquals(3, human.getHand().size());
        assertTrue(game.buildDistrict(human,0).startsWith("Built Market"));
        assertEquals(1, human.getGold());
        assertEquals(2, human.getBuilt().size());
        assertTrue(game.buildDistrict(human,0).contains("build limit"));
    }

    @Test void architectCanBuildThreeButNotFourAndOrdinaryLimitResetsNextRound() {
        var hand = List.of(card("A","red",1), card("B","blue",1), card("C","green",1), card("D","yellow",1));
        Game game = game("end\nquit\n",checkpoint(players(player(1,8,3,0,hand,List.of())),List.of(),
                Map.of(1,7,2,4,3,5,4,6),-1,-1,-1));
        Player human = game.getPlayerByCharacterRank(7);
        for (int i=0;i<3;i++) assertTrue(game.buildDistrict(human,0).startsWith("Built"));
        assertTrue(game.buildDistrict(human,0).contains("build limit"));
        String output = capture(game::continueGame);
        assertTrue(output.contains("ROUND 2"));
        assertEquals(5, human.getGold()); // Resume did not repeat resources or Architect's power.
    }

    @Test void inspectionsAndInvalidCommandsDoNotEndOrReplayTheTurn() {
        Game game = game("info King\ncity nonsense\ncity 99\nbuild x\nbuild -1\naction\nt\nquit\n",
                checkpoint(players(player(1,7,1,0,List.of(),List.of())),List.of(),Map.of(1,8,2,4,3,5,4,6),-1,-1,-1));
        Player human = game.getPlayerByCharacterRank(8);
        String output = capture(game::continueGame);
        assertTrue(output.contains("King: take the crown"));
        assertTrue(output.contains("No such player"));
        assertTrue(output.contains("Usage: build"));
        assertTrue(output.contains("already resolved once"));
        assertTrue(output.contains("ROUND 2"));
        assertEquals(7, human.getGold());
        assertFalse(output.contains("StackOverflow"));
    }

    @Test void computerTurnsCollectIncomeAndBuildWithoutReadingHumanInput() {
        var ps = players(player(1,4,1,0,List.of(),List.of()));
        ps.set(1, player(2,0,1,0,List.of(card("Market","green",2)),List.of()));
        ps.set(2, player(3,0,1,0,List.of(card("Temple","blue",1)),List.of()));
        ps.set(3, player(4,0,1,0,List.of(card("Watchtower","red",1)),List.of()));
        Game game = game("end\n",checkpoint(ps,List.of(),Map.of(1,4,2,5,3,6,4,7),-1,-1,-1));
        capture(game::runTurnPhase);
        assertEquals(1,game.getPlayerByCharacterRank(5).getBuilt().size());
        assertEquals(1,game.getPlayerByCharacterRank(6).getBuilt().size());
        assertEquals(1,game.getPlayerByCharacterRank(7).getBuilt().size());
        assertEquals(0,game.getPlayerByCharacterRank(5).getGold());
        assertEquals(2,game.getPlayerByCharacterRank(6).getGold());
    }

    @Test void computerAssassinThiefMagicianAndWarlordNeverConsumeInput() {
        var ps = players(player(1,5,1,0,List.of(),List.of()));
        ps.set(1,player(2,2,1,0,List.of(card("A","red",1)),List.of()));
        ps.set(2,player(3,2,1,0,List.of(card("B","blue",1)),List.of()));
        ps.set(3,player(4,2,1,0,List.of(card("C","green",1)),List.of()));
        Game game = game("end\n",checkpoint(ps,List.of(),Map.of(1,1,2,2,3,3,4,8),-1,-1,-1));
        assertDoesNotThrow(() -> capture(game::runTurnPhase));
        assertTrue(game.getStolenCharacter() >= 3);
    }

    @Test void assassinatedPlayerGetsNoIncomeAndAssassinatedKingStillGetsCrown() {
        Game game = game("end\n",checkpoint(players(player(1,4,1,0,List.of(),List.of())),List.of(),
                Map.of(1,2,2,4,3,5,4,6),4,-1,-1));
        capture(game::runTurnPhase);
        assertEquals(2,game.getPlayerByCharacterRank(4).getGold());
        // Saving is correctly unavailable after the turn phase.
        assertThrows(IllegalStateException.class,game::snapshotState);
    }

    @Test void thiefStealsBeforeVictimIncomeAndOnlyOnce() {
        Game game = game("end\n",checkpoint(players(player(1,4,1,0,List.of(),List.of())),List.of(),
                Map.of(1,2,2,4,3,5,4,6),-1,4,-1));
        capture(game::runTurnPhase);
        assertEquals(6,game.getPlayerByCharacterRank(2).getGold());
        assertEquals(2,game.getPlayerByCharacterRank(4).getGold());
        capture(game::runTurnPhase);
        assertEquals(6,game.getPlayerByCharacterRank(2).getGold());
    }

    @Test void magicianDeduplicatesRedrawAndRejectsPartialInvalidSelections() {
        var initial=checkpoint(players(player(1,4,1,0,List.of(card("A","red",1),card("B","blue",2)),List.of())),
                List.of(card("C","green",3),card("D","yellow",4)),Map.of(1,3,2,4,3,5,4,6),-1,-1,-1);
        Game valid=game("2\n1,1\n",initial);
        Player human=valid.getPlayerByCharacterRank(3);
        capture(() -> valid.executeMagicianAbility(human));
        assertEquals(2,human.getHand().size());
        assertEquals("B",human.getHand().get(0).getName());
        assertEquals("C",human.getHand().get(1).getName());
        Game invalid=game("2\n1,99\n",initial);
        capture(() -> invalid.executeMagicianAbility(invalid.getPlayerByCharacterRank(3)));
        assertEquals(initial.players().get(0).hand(),invalid.snapshotState().players().get(0).hand());
    }

    @Test void warlordProtectsLivingBishopAndCompleteCities() {
        var ps=players(player(1,10,1,0,List.of(),List.of()));
        ps.set(1,player(2,2,1,0,List.of(),List.of(card("Temple","blue",1))));
        List<GameState.CardState> full=new ArrayList<>();
        for(int i=0;i<8;i++) full.add(card("District"+i,"green",1));
        ps.set(2,player(3,2,1,0,List.of(),full));
        Game game=game("",checkpoint(ps,List.of(),Map.of(1,8,2,5,3,4,4,6),-1,-1,3));
        String output=capture(() -> game.executeWarlordAbility(game.getPlayerByCharacterRank(8)));
        assertTrue(output.contains("No eligible cities"));
        assertEquals(1,game.getPlayerByCharacterRank(5).getBuilt().size());
        assertEquals(8,game.getPlayerByCharacterRank(4).getBuilt().size());
    }

    @Test void warlordChargesOnceAndRecyclesDestroyedCard() {
        var ps=players(player(1,10,1,0,List.of(),List.of()));
        ps.set(1,player(2,2,1,0,List.of(),List.of(card("Castle","yellow",4))));
        Game game=game("2\n1\n",checkpoint(ps,List.of(),Map.of(1,8,2,4,3,5,4,6),-1,-1,-1));
        capture(() -> game.executeWarlordAbility(game.getPlayerByCharacterRank(8)));
        assertEquals(7,game.getPlayerByCharacterRank(8).getGold());
        assertTrue(game.getPlayerByCharacterRank(4).getBuilt().isEmpty());
        assertEquals("Castle",game.snapshotState().districtDeck().get(0).name());
    }

    @Test void eighthDistrictFinishesOnlyAfterRemainingCharactersAndScoresBonuses() {
        List<GameState.CardState> built=new ArrayList<>();
        String[] colors={"red","blue","green","yellow","purple","red","blue"};
        for(int i=0;i<7;i++) built.add(card("D"+i,colors[i],1));
        var ps=players(player(1,2,1,0,List.of(card("Last","green",1)),built));
        ps.set(1,player(2,0,1,0,List.of(card("Market","green",1)),List.of()));
        Game game=game("build 1\nend\n",checkpoint(ps,List.of(),Map.of(1,4,2,6,3,7,4,8),-1,-1,-1));
        String output=capture(game::continueGame);
        assertTrue(output.contains("FINAL SCORES"));
        assertFalse(output.contains("ROUND 2"));
        assertEquals(15,game.calculateScore(game.getPlayerByCharacterRank(4))); // 8 costs + 3 colours + 4 completion
        assertTrue(output.contains("Player 2: Built Market"));
        assertTrue(output.contains("Player 1: 15 points — winner"));
    }

    @Test void checkpointResumeKeepsResourcesAndBuildUsageWithoutReplay() {
        GameState state=checkpoint(players(player(1,9,3,2,List.of(card("A","red",1),card("B","blue",1)),List.of())),
                List.of(card("C","green",1)),Map.of(1,7,2,4,3,5,4,6),-1,-1,-1);
        Game game=game("build 1\nbuild 1\nquit\n",state);
        String output=capture(game::continueGame);
        assertEquals(8,game.getPlayerByCharacterRank(7).getGold());
        assertEquals(1,game.getPlayerByCharacterRank(7).getHand().size());
        assertTrue(output.contains("build limit"));
        assertFalse(output.contains("extra cards"));
    }

    @Test void checkpointRoundTripPreservesAllGameFields() {
        GameState state=checkpoint(players(player(1,9,3,2,List.of(card("A","red",1)),List.of())),
                List.of(card("C","green",1)),Map.of(1,7,2,4,3,5,4,6),2,8,-1);
        assertEquals(state,game("",state).snapshotState());
    }

    @Test void colorCountsAndEmptyDeckDrawAreSafe() {
        Game game=new Game(new Scanner(""),new Random(1));
        Player player=new Player(1);
        player.buildDistrict(new DistrictCard("Watchtower","red",1,null));
        player.buildDistrict(new DistrictCard("Church","blue",2,null));
        assertEquals(1,game.countDistrictsByColor(player,"RED"));
        assertDoesNotThrow(() -> game.drawCards(player,2));
        assertTrue(player.getHand().isEmpty());
    }
    @Test void loadingInsideCommandLoopResumesRestoredRankAndReferences() throws Exception {
        GameState saved=checkpoint(players(player(1,8,1,1,List.of(card("A","red",1)),List.of(card("Existing","blue",1)))),
                List.of(),Map.of(1,4,2,5,3,6,4,7),-1,-1,-1);
        Path file=temporary.resolve("Mixed Case Save.json");
        GameStateStore.save(file,saved);
        GameState initial=checkpoint(players(player(1,20,1,0,List.of(),List.of())),List.of(),
                Map.of(1,8,2,4,3,5,4,6),-1,-1,-1);
        Game game=game("load "+file+"\nbuild 1\nend\n",initial);
        String output=capture(game::runTurnPhase);
        assertTrue(output.contains("Game loaded"));
        assertTrue(output.contains("build limit"));
        assertEquals(8,game.getPlayerByCharacterRank(4).getGold());
        assertEquals(4,game.getPlayerByCharacterRank(5).getGold());
        assertEquals(5,game.getPlayerByCharacterRank(6).getGold());
        assertEquals(4,game.getPlayerByCharacterRank(7).getGold());
        assertFalse(output.contains("Player 1 — King"));
    }

    @Test void seededGameRunsMultipleRoundsToFinalScoring() {
        String commands="4\n"+("1\n2\n3\n4\n5\n6\n7\n8\ngold\n0\n1\n2\n3\n4\n5\n6\n7\n8\nend\n").repeat(300);
        String output=capture(() -> new Game(new Scanner(commands),new Random(42)).start());
        assertTrue(output.contains("ROUND 2"));
        assertTrue(output.contains("FINAL SCORES"), "AI opponents must be able to finish a complete game.");
        assertTrue(output.contains("winner"));
    }

}
