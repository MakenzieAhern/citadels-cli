package citadels;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/** A deliberately simplified, single-human Citadels-inspired game. */
public class Game {
    private final List<Player> players = new ArrayList<>();
    private final List<DistrictCard> districtDeck = new ArrayList<>();
    private final Scanner scanner;
    private final Random random;
    private int crownedPlayerIndex;
    private Player humanPlayer;
    private Player activePlayer;
    private final List<CharacterCard> characterDeck = new ArrayList<>();
    private final List<CharacterCard> availableCharacters = new ArrayList<>();
    private final List<CharacterCard> discardedCharacters = new ArrayList<>();
    private final Map<Player, CharacterCard> playerCharacterMap = new LinkedHashMap<>();
    private final Map<Integer, Integer> builtThisTurn = new HashMap<>();
    private int killedCharacterRank = -1;
    private int stolenCharacterRank = -1;
    private boolean debugMode;
    private int roundNumber;
    private int currentTurnRank = 1;
    private boolean currentTurnStarted;
    private int firstCompletedPlayerId = -1;
    private boolean quit;
    private boolean gameOver;

    public Game() { this(new Scanner(System.in), new Random()); }

    /** Injectable input and randomness make complete command sessions reproducible. */
    public Game(Scanner scanner, Random random) {
        this.scanner = Objects.requireNonNull(scanner);
        this.random = Objects.requireNonNull(random);
    }

    public void start() {
        try {
            System.out.println("Citadels CLI — a simplified strategy game. Type quit at any prompt.");
            int count = promptCharacterChoice("Enter how many players [4-7]: ", 4, 7);
            initialize(count);
            continueGame();
        } catch (QuitRequested ignored) {
            System.out.println("Goodbye.");
        }
    }

    private void initialize(int playerCount) {
        players.clear();
        districtDeck.clear();
        districtDeck.addAll(DistrictCardLoader.loadDistrictCards());
        if (districtDeck.size() < playerCount * 4) {
            throw new IllegalStateException("The district deck does not contain enough starting cards.");
        }
        Collections.shuffle(districtDeck, random);
        for (int id = 1; id <= playerCount; id++) {
            Player player = new Player(id);
            player.addGold(2);
            drawCards(player, 4);
            players.add(player);
        }
        humanPlayer = players.get(0);
        crownedPlayerIndex = random.nextInt(playerCount);
        roundNumber = 0;
        firstCompletedPlayerId = -1;
        gameOver = false;
        quit = false;
        playerCharacterMap.clear();
        System.out.println("You are Player 1. Player " + players.get(crownedPlayerIndex).getId() + " holds the crown.");
    }

    /** Continue either a newly dealt game or a validated human-turn checkpoint. */
    public void continueGame() {
        if (players.isEmpty()) throw new IllegalStateException("Start or load a game first.");
        try {
            while (!quit && !gameOver) {
                if (playerCharacterMap.isEmpty() || currentTurnRank > 8) {
                    runCharacterSelectionPhase();
                }
                runTurnPhase();
                if (!quit && firstCompletedPlayerId != -1) {
                    gameOver = true;
                    printFinalScores();
                }
            }
        } catch (QuitRequested ignored) {
            System.out.println("Goodbye.");
        }
    }

    private String readInput() {
        if (!scanner.hasNextLine()) { quit = true; throw new QuitRequested(); }
        String input = scanner.nextLine().trim();
        if (input.equalsIgnoreCase("quit") || input.equalsIgnoreCase("exit")) {
            quit = true;
            throw new QuitRequested();
        }
        return input;
    }

    private static final class QuitRequested extends RuntimeException { }

    private void setupCharacters() {
        characterDeck.clear();
        for (int rank = 1; rank <= 8; rank++) characterDeck.add(characterForRank(rank));
    }

    private static CharacterCard characterForRank(int rank) {
        return switch (rank) {
            case 1 -> new Assassin(); case 2 -> new Thief(); case 3 -> new Magician();
            case 4 -> new King(); case 5 -> new Bishop(); case 6 -> new Merchant();
            case 7 -> new Architect(); case 8 -> new Warlord();
            default -> throw new IllegalArgumentException("Invalid character rank: " + rank);
        };
    }

    public void runCharacterSelectionPhase() {
        roundNumber++;
        System.out.println("\n=== ROUND " + roundNumber + " — CHARACTER SELECTION ===");
        setupCharacters();
        availableCharacters.clear();
        availableCharacters.addAll(characterDeck);
        Collections.shuffle(availableCharacters, random);
        discardedCharacters.clear();
        playerCharacterMap.clear();
        builtThisTurn.clear();
        killedCharacterRank = -1;
        stolenCharacterRank = -1;
        currentTurnRank = 1;
        currentTurnStarted = false;
        activePlayer = null;
        discardedCharacters.add(availableCharacters.remove(0));
        System.out.println("One character was discarded face down.");
        int visibleDiscards = players.size() == 4 ? 2 : players.size() == 5 ? 1 : 0;
        for (int i = 0; i < visibleDiscards; i++) {
            CharacterCard discard = availableCharacters.stream().filter(c -> c.getRank() != 4).findFirst().orElseThrow();
            availableCharacters.remove(discard);
            discardedCharacters.add(discard);
            System.out.println(discard.getName() + " was discarded face up.");
        }
        for (int offset = 0; offset < players.size(); offset++) {
            Player player = players.get((crownedPlayerIndex + offset) % players.size());
            if (player.equals(humanPlayer)) humanPickCharacter(player);
            else {
                CharacterCard chosen = availableCharacters.remove(random.nextInt(availableCharacters.size()));
                playerCharacterMap.put(player, chosen);
                System.out.println("Player " + player.getId() + " chose a hidden character.");
            }
        }
    }

    private void humanPickCharacter(Player player) {
        System.out.println("Choose a character by name or rank:");
        availableCharacters.stream().sorted(Comparator.comparingInt(CharacterCard::getRank))
                .forEach(c -> System.out.println(c.getRank() + ". " + c.getName()));
        while (true) {
            System.out.print("> ");
            String input = readInput();
            Optional<CharacterCard> chosen = availableCharacters.stream().filter(c ->
                    c.getName().equalsIgnoreCase(input) || Integer.toString(c.getRank()).equals(input)).findFirst();
            if (chosen.isPresent()) {
                playerCharacterMap.put(player, chosen.get());
                availableCharacters.remove(chosen.get());
                System.out.println("You chose " + chosen.get().getName() + ".");
                return;
            }
            System.out.println("Choose one of the available characters.");
        }
    }

    public void runTurnPhase() {
        while (currentTurnRank <= 8 && !quit) {
            CharacterCard character = getCharacterByRank(currentTurnRank);
            Player player = getPlayerByCharacterRank(currentTurnRank);
            if (player == null) { currentTurnRank++; continue; }
            activePlayer = player;
            if (isCharacterKilled(currentTurnRank)) {
                if (currentTurnRank == 4) setCrownedPlayer(player);
                System.out.println("Player " + player.getId() + " (" + character.getName() + ") was assassinated and skips this turn.");
                currentTurnRank++;
                continue;
            }
            if (!currentTurnStarted) {
                System.out.println("\nPlayer " + player.getId() + " — " + character.getName());
                player.resetBuildLimit();
                builtThisTurn.put(player.getId(), 0);
                applyTheft(player);
                collectResources(player);
                character.usePower(player, this);
                currentTurnStarted = true;
            }
            if (player.equals(humanPlayer)) runCommandLoop();
            else buildForComputer(player);
            currentTurnStarted = false;
            currentTurnRank++;
        }
        activePlayer = null;
    }

    private void applyTheft(Player player) {
        if (stolenCharacterRank != currentTurnRank) return;
        Player thief = getPlayerByCharacterRank(2);
        if (thief != null && !isCharacterKilled(2)) {
            int gold = player.getGold();
            player.setGold(0);
            thief.addGold(gold);
            System.out.println("The Thief steals " + gold + " gold from Player " + player.getId() + ".");
        }
    }

    private void collectResources(Player player) {
        boolean takeCards;
        if (player.equals(humanPlayer)) {
            System.out.println("Choose turn resources: gold (+2) or cards (draw 2, keep 1).");
            while (true) {
                System.out.print("resources> ");
                String choice = readInput();
                if (choice.equalsIgnoreCase("gold") || choice.equals("1")) { takeCards = false; break; }
                if (choice.equalsIgnoreCase("cards") || choice.equals("2")) { takeCards = true; break; }
                System.out.println("Enter gold or cards.");
            }
        } else {
            takeCards = player.getHand().size() < 2 || player.getGold() >= 8;
        }
        if (!takeCards || districtDeck.isEmpty()) {
            player.addGold(2);
            System.out.println("Player " + player.getId() + " collected 2 gold.");
            return;
        }
        List<DistrictCard> drawn = new ArrayList<>();
        for (int i = 0; i < 2 && !districtDeck.isEmpty(); i++) drawn.add(districtDeck.remove(0));
        int keep;
        if (player.equals(humanPlayer)) {
            for (int i = 0; i < drawn.size(); i++) System.out.println((i + 1) + ". " + drawn.get(i));
            keep = promptCharacterChoice("Keep which card? ", 1, drawn.size()) - 1;
        } else {
            keep = 0;
            for (int i = 1; i < drawn.size(); i++) if (cardPreference(player, drawn.get(i)) < cardPreference(player, drawn.get(keep))) keep = i;
        }
        player.addCard(drawn.remove(keep));
        districtDeck.addAll(drawn);
        System.out.println("Player " + player.getId() + " kept one district card.");
    }

    private int cardPreference(Player player, DistrictCard card) {
        return card.getCost() + (hasDistrict(player, card.getName()) ? 100 : 0);
    }

    private void runCommandLoop() {
        System.out.println("Your turn: hand, build <n>, all, help, or end.");
        while (!quit) {
            System.out.print("> ");
            String input = readInput();
            String[] parts = input.split("\\s+", 2);
            String command = parts[0].toLowerCase(Locale.ROOT);
            String argument = parts.length == 2 ? parts[1].trim() : "";
            switch (command) {
                case "end", "t" -> { return; }
                case "hand" -> printHand(humanPlayer);
                case "gold" -> System.out.println("You have " + humanPlayer.getGold() + " gold.");
                case "build" -> {
                    try { System.out.println(buildDistrict(humanPlayer, Integer.parseInt(argument) - 1)); }
                    catch (NumberFormatException e) { System.out.println("Usage: build <card number>"); }
                }
                case "save" -> { if (argument.isEmpty()) System.out.println("Usage: save <file>"); else saveGame(argument); }
                case "load" -> { if (argument.isEmpty()) System.out.println("Usage: load <file>"); else loadGame(argument); }
                case "all" -> printAllPlayers();
                case "city", "citadel", "list" -> {
                    try { printPlayerCity(argument.isEmpty() ? humanPlayer.getId() : Integer.parseInt(argument)); }
                    catch (NumberFormatException e) { System.out.println("Usage: city [player number]"); }
                }
                case "action" -> System.out.println("Your character's ability was already resolved once at the start of this turn.");
                case "info" -> printInfo(argument);
                case "debug" -> { debugMode = !debugMode; System.out.println("Debug mode " + (debugMode ? "enabled" : "disabled") + "."); }
                case "help" -> printHelp();
                default -> System.out.println("Unknown command. Type help for commands.");
            }
        }
    }

    /** Build through one guarded transaction; rejected builds never spend gold or cards. */
    String buildDistrict(Player player, int index) {
        if (index < 0 || index >= player.getHand().size()) return "Invalid card number.";
        if (player.getBuilt().size() >= 8) return "Your city already has eight districts.";
        if (builtThisTurn.getOrDefault(player.getId(), 0) >= player.getBuildLimit()) return "Your build limit for this turn has been reached.";
        DistrictCard card = player.getHand().get(index);
        if (hasDistrict(player, card.getName())) return "You already have a district with that name.";
        if (player.getGold() < card.getCost()) return "Not enough gold.";
        player.addGold(-card.getCost());
        player.buildDistrict(card);
        builtThisTurn.merge(player.getId(), 1, Integer::sum);
        if (player.getBuilt().size() == 8 && firstCompletedPlayerId == -1) {
            firstCompletedPlayerId = player.getId();
            System.out.println("Player " + player.getId() + " completed a city! This is the final round.");
        }
        return "Built " + card.getName() + " for " + card.getCost() + " gold.";
    }

    private boolean hasDistrict(Player player, String name) {
        return player.getBuilt().stream().anyMatch(card -> card.getName().equalsIgnoreCase(name));
    }

    private void buildForComputer(Player player) {
        while (builtThisTurn.getOrDefault(player.getId(), 0) < player.getBuildLimit() && player.getBuilt().size() < 8) {
            DistrictCard chosen = player.getHand().stream().filter(c -> c.getCost() <= player.getGold() && !hasDistrict(player, c.getName()))
                    .min(Comparator.comparingInt(DistrictCard::getCost)).orElse(null);
            if (chosen == null) break;
            System.out.println("Player " + player.getId() + ": " + buildDistrict(player, player.getHand().indexOf(chosen)));
        }
    }

    private void printHand(Player player) {
        if (player.getHand().isEmpty()) System.out.println("No cards in hand.");
        for (int i = 0; i < player.getHand().size(); i++) System.out.println((i + 1) + ". " + player.getHand().get(i));
    }

    private void printPlayerCity(int id) {
        Player player = players.stream().filter(p -> p.getId() == id).findFirst().orElse(null);
        if (player == null) { System.out.println("No such player."); return; }
        System.out.println("Player " + id + " city:");
        player.getBuilt().forEach(System.out::println);
    }

    private void printAllPlayers() {
        for (Player player : players) {
            System.out.println("Player " + player.getId() + (player.equals(humanPlayer) ? " (you)" : "")
                    + ": gold=" + player.getGold() + ", cards=" + player.getHand().size() + ", districts=" + player.getBuilt().size());
            if (debugMode && !player.equals(humanPlayer)) printHand(player);
        }
    }

    private void printInfo(String query) {
        CharacterCard character = characterDeck.stream().filter(c -> c.getName().equalsIgnoreCase(query)).findFirst().orElse(null);
        if (character != null) {
            System.out.println(switch (character.getRank()) {
                case 1 -> "Assassin: skip the turn of one character ranked 2–8.";
                case 2 -> "Thief: steal a living character's gold before their turn (ranks 3–8).";
                case 3 -> "Magician: swap a hand or discard selected cards and draw replacements.";
                case 4 -> "King: take the crown and gain one gold per yellow district.";
                case 5 -> "Bishop: gain one gold per blue district; a living Bishop's city is protected.";
                case 6 -> "Merchant: gain one gold plus one per green district.";
                case 7 -> "Architect: draw two extra cards and build up to three districts.";
                default -> "Warlord: gain red-district income and optionally destroy one eligible district.";
            });
            return;
        }
        DistrictCard card = humanPlayer.getHand().stream().filter(c -> c.getName().equalsIgnoreCase(query)).findFirst().orElse(null);
        if (card == null) {
            try { int i = Integer.parseInt(query) - 1; if (i >= 0 && i < humanPlayer.getHand().size()) card = humanPlayer.getHand().get(i); }
            catch (NumberFormatException ignored) { }
        }
        if (card == null) System.out.println("Usage: info <character name | card name | hand number>");
        else {
            System.out.println(card);
            if (card.getSpecialAbility() != null && !card.getSpecialAbility().isBlank()) {
                DistrictCardInfoCommand.execute(card.getName(), humanPlayer.getHand());
                System.out.println("Purple district powers are not applied in this simplified variant.");
            }
        }
    }

    private void printHelp() {
        System.out.println("""
                hand / gold       Show your hand or gold
                build <n>         Build a hand card (one per turn; Architect: three)
                city [player]     Show a city (aliases: citadel, list)
                all               Show public player summaries
                info <name|n>     Explain a character or show a hand card
                action            Show when the character ability is resolved
                save <file>       Save this human-turn checkpoint
                load <file>       Resume a saved checkpoint
                debug             Toggle computer hand visibility
                end / t           End this turn and advance normally
                quit / exit       Leave the game (also accepted at prompts)
                Purple district powers are not implemented; printed text is reference only.
                """);
    }

    public CharacterCard getCharacterByRank(int rank) {
        return characterDeck.stream().filter(c -> c.getRank() == rank).findFirst().orElse(null);
    }
    public Player getPlayerByCharacter(CharacterCard character) {
        return playerCharacterMap.entrySet().stream().filter(e -> e.getValue().equals(character)).map(Map.Entry::getKey).findFirst().orElse(null);
    }
    public Player getPlayerByCharacterRank(int rank) {
        return playerCharacterMap.entrySet().stream().filter(e -> e.getValue().getRank() == rank).map(Map.Entry::getKey).findFirst().orElse(null);
    }

    public int promptCharacterChoice(String message, int min, int max) {
        if (activePlayer != null && !activePlayer.equals(humanPlayer)) {
            List<Integer> options = new ArrayList<>();
            for (int value = min; value <= max; value++) {
                if (currentTurnRank != 2 || !isCharacterKilled(value)) options.add(value);
            }
            return options.get(random.nextInt(options.size()));
        }
        while (true) {
            System.out.print(message);
            try {
                int choice = Integer.parseInt(readInput());
                if (choice >= min && choice <= max && !(currentTurnRank == 2 && min == 3 && isCharacterKilled(choice))) return choice;
            } catch (NumberFormatException ignored) { }
            System.out.println("Enter a valid number between " + min + " and " + max + ".");
        }
    }
    public void markCharacterAsKilled(int rank) { killedCharacterRank = rank; }
    public boolean isCharacterKilled(int rank) { return killedCharacterRank == rank; }
    public void setStolenCharacter(int rank) { stolenCharacterRank = rank; }
    public int getStolenCharacter() { return stolenCharacterRank; }
    public int countDistrictsByColor(Player player, String color) {
        return (int) player.getBuilt().stream().filter(c -> c.getColor().equalsIgnoreCase(color)).count();
    }
    public void setCrownedPlayer(Player player) {
        int index = players.indexOf(player);
        if (index >= 0) crownedPlayerIndex = index;
    }
    public void drawCards(Player player, int count) {
        for (int i = 0; i < count && !districtDeck.isEmpty(); i++) player.addCard(districtDeck.remove(0));
    }

    public void executeMagicianAbility(Player player) {
        if (!player.equals(humanPlayer)) {
            Player target = players.stream().filter(p -> !p.equals(player)).max(Comparator.comparingInt(p -> p.getHand().size())).orElse(null);
            if (target != null && target.getHand().size() > player.getHand().size()) swapHands(player, target);
            return;
        }
        int choice = promptCharacterChoice("Magician: 0 skip, 1 swap hands, 2 discard and redraw: ", 0, 2);
        if (choice == 0) return;
        if (choice == 1) {
            int id = promptCharacterChoice("Swap with player (0 cancels): ", 0, players.size());
            if (id == 0) return;
            Player target = players.get(id - 1);
            if (target.equals(player)) { System.out.println("Cannot swap with yourself."); return; }
            swapHands(player, target);
        } else {
            printHand(player);
            System.out.print("Discard card numbers separated by commas (empty cancels): ");
            String input = readInput();
            if (input.isEmpty()) return;
            Set<Integer> indices = new TreeSet<>(Comparator.reverseOrder());
            try {
                for (String token : input.split(",", -1)) {
                    int index = Integer.parseInt(token.trim()) - 1;
                    if (index < 0 || index >= player.getHand().size()) throw new NumberFormatException();
                    indices.add(index);
                }
            } catch (NumberFormatException e) { System.out.println("Invalid card selection; no cards discarded."); return; }
            List<DistrictCard> discarded = new ArrayList<>();
            for (int index : indices) discarded.add(player.getHand().remove(index));
            districtDeck.addAll(discarded);
            drawCards(player, discarded.size());
            System.out.println("Replaced " + discarded.size() + " card(s).");
        }
    }

    private void swapHands(Player player, Player target) {
        List<DistrictCard> temp = new ArrayList<>(player.getHand());
        player.getHand().clear();
        player.getHand().addAll(target.getHand());
        target.getHand().clear();
        target.getHand().addAll(temp);
        System.out.println("Player " + player.getId() + " swapped hands with Player " + target.getId() + ".");
    }

    public void executeWarlordAbility(Player warlord) {
        List<Player> targets = players.stream().filter(p -> canAttack(warlord, p)).toList();
        if (targets.isEmpty()) { System.out.println("No eligible cities to attack."); return; }
        Player target;
        DistrictCard card;
        if (!warlord.equals(humanPlayer)) {
            target = targets.stream().filter(p -> p.getBuilt().stream().anyMatch(c -> c.getCost() - 1 <= warlord.getGold())).findFirst().orElse(null);
            if (target == null) return;
            card = target.getBuilt().stream().filter(c -> c.getCost() - 1 <= warlord.getGold()).min(Comparator.comparingInt(DistrictCard::getCost)).orElseThrow();
        } else {
            targets.forEach(p -> System.out.println("Player " + p.getId() + ": " + p.getBuilt().size() + " districts"));
            int id = promptCharacterChoice("Warlord target player (0 skips): ", 0, players.size());
            if (id == 0) return;
            target = players.get(id - 1);
            if (!targets.contains(target)) { System.out.println("That city cannot be attacked."); return; }
            for (int i = 0; i < target.getBuilt().size(); i++) System.out.println((i + 1) + ". " + target.getBuilt().get(i));
            int selection = promptCharacterChoice("Destroy which district (0 skips): ", 0, target.getBuilt().size());
            if (selection == 0) return;
            card = target.getBuilt().get(selection - 1);
        }
        int cost = Math.max(0, card.getCost() - 1);
        if (warlord.getGold() < cost) { System.out.println("Not enough gold to destroy that district."); return; }
        warlord.addGold(-cost);
        target.getBuilt().remove(card);
        districtDeck.add(card);
        System.out.println("Player " + warlord.getId() + " destroyed " + card.getName() + " in Player " + target.getId() + "'s city for " + cost + " gold.");
    }

    private boolean canAttack(Player warlord, Player target) {
        return !target.equals(warlord) && !target.getBuilt().isEmpty() && target.getBuilt().size() < 8
                && !(playerCharacterMap.get(target) instanceof Bishop && !isCharacterKilled(5));
    }

    public int calculateScore(Player player) {
        int score = player.getBuilt().stream().mapToInt(DistrictCard::getCost).sum();
        if (player.getBuilt().stream().map(c -> c.getColor().toLowerCase(Locale.ROOT)).distinct().count() == 5) score += 3;
        if (player.getBuilt().size() >= 8) score += player.getId() == firstCompletedPlayerId ? 4 : 2;
        return score;
    }

    private void printFinalScores() {
        System.out.println("\n=== FINAL SCORES ===");
        int best = players.stream().mapToInt(this::calculateScore).max().orElse(0);
        players.stream().sorted(Comparator.comparingInt(this::calculateScore).reversed()).forEach(p ->
                System.out.println("Player " + p.getId() + ": " + calculateScore(p) + " points" + (calculateScore(p) == best ? " — winner" : "")));
        System.out.println("Tied highest scores share the win.");
    }

    public GameState snapshotState() {
        if (!currentTurnStarted || activePlayer == null || !activePlayer.equals(humanPlayer)) {
            throw new IllegalStateException("Saving is available during your action commands after resources and role power.");
        }
        Map<Integer, Integer> assignments = new LinkedHashMap<>();
        playerCharacterMap.forEach((player, character) -> assignments.put(player.getId(), character.getRank()));
        return new GameState(players.stream().map(p -> GameState.PlayerState.from(p, builtThisTurn.getOrDefault(p.getId(), 0))).toList(),
                districtDeck.stream().map(GameState.CardState::from).toList(), crownedPlayerIndex, humanPlayer.getId(),
                characterDeck.stream().map(CharacterCard::getRank).toList(), assignments,
                availableCharacters.stream().map(CharacterCard::getRank).toList(), discardedCharacters.stream().map(CharacterCard::getRank).toList(),
                killedCharacterRank, stolenCharacterRank, debugMode, roundNumber, currentTurnRank, currentTurnStarted, firstCompletedPlayerId);
    }

    public void restoreState(GameState state) {
        try { GameStateStore.validate(state); }
        catch (IOException e) { throw new IllegalArgumentException(e.getMessage(), e); }
        players.clear();
        districtDeck.clear();
        playerCharacterMap.clear();
        builtThisTurn.clear();
        for (GameState.PlayerState playerState : state.players()) {
            players.add(playerState.toPlayer());
            builtThisTurn.put(playerState.id(), playerState.builtThisTurn());
        }
        districtDeck.addAll(state.districtDeck().stream().map(GameState.CardState::toCard).toList());
        crownedPlayerIndex = state.crownedPlayerIndex();
        humanPlayer = players.stream().filter(p -> p.getId() == state.humanPlayerId()).findFirst().orElseThrow();
        characterDeck.clear();
        characterDeck.addAll(state.characterDeckRanks().stream().map(Game::characterForRank).toList());
        availableCharacters.clear();
        availableCharacters.addAll(state.availableCharacterRanks().stream().map(Game::characterForRank).toList());
        discardedCharacters.clear();
        discardedCharacters.addAll(state.discardedCharacterRanks().stream().map(Game::characterForRank).toList());
        state.characterAssignments().forEach((id, rank) -> playerCharacterMap.put(players.stream().filter(p -> p.getId() == id).findFirst().orElseThrow(), getCharacterByRank(rank)));
        killedCharacterRank = state.killedCharacterRank();
        stolenCharacterRank = state.stolenCharacterRank();
        debugMode = state.debugMode();
        roundNumber = state.roundNumber();
        currentTurnRank = state.currentTurnRank();
        currentTurnStarted = state.currentTurnStarted();
        firstCompletedPlayerId = state.firstCompletedPlayerId();
        activePlayer = humanPlayer;
        quit = false;
        gameOver = false;
    }

    public void saveGame(String filename) {
        try {
            GameStateStore.save(Path.of(filename), snapshotState());
            System.out.println("Game saved to " + filename);
        } catch (IOException | IllegalArgumentException | IllegalStateException e) {
            System.out.println("Failed to save game: " + e.getMessage());
        }
    }

    public boolean loadGame(String filename) {
        try {
            GameState loaded = GameStateStore.load(Path.of(filename));
            restoreState(loaded);
            System.out.println("Game loaded from " + filename);
            return true;
        } catch (IOException | IllegalArgumentException e) {
            System.out.println("Failed to load game: " + e.getMessage());
            return false;
        }
    }
}
