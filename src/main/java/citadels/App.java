package citadels;

import java.util.Random;
import java.util.Scanner;

public final class App {
    private App() { }

    public static void main(String[] args) {
        Long seed = null;
        String savedGame = null;
        try {
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--help", "-h" -> {
                        System.out.println("Usage: citadels-cli [--seed <number>] [--load <save.json>]");
                        System.out.println("Play a simplified Citadels-inspired game against computer opponents. Type quit at any prompt.");
                        return;
                    }
                    case "--seed" -> { if (++i >= args.length) throw new IllegalArgumentException("--seed needs an integer."); seed = Long.parseLong(args[i]); }
                    case "--load" -> { if (++i >= args.length) throw new IllegalArgumentException("--load needs a filename."); savedGame = args[i]; }
                    default -> throw new IllegalArgumentException("Unknown option: " + args[i]);
                }
            }
            Game game = new Game(new Scanner(System.in), seed == null ? new Random() : new Random(seed));
            if (savedGame == null) game.start();
            else if (game.loadGame(savedGame)) game.continueGame();
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid arguments: " + e.getMessage());
            System.err.println("Use --help for usage.");
        }
    }
}
