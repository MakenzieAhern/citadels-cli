package citadels;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Loads the bundled district definitions without returning an incomplete deck. */
public final class DistrictCardLoader {
    private static final Set<String> COLORS = new HashSet<>(
            Arrays.asList("red", "yellow", "green", "blue", "purple"));
    private static final String[] HEADER = {"name", "qty", "color", "cost", "text"};

    private DistrictCardLoader() {
    }

    public static List<DistrictCard> loadDistrictCards() {
        return loadDistrictCards(DistrictCardLoader.class.getResourceAsStream("/citadels/cards.tsv"));
    }

    /** Package-visible entry point for testing the same parser used by the bundled resource. */
    static List<DistrictCard> loadDistrictCards(InputStream source) {
        if (source == null) {
            throw new IllegalStateException("Missing district card resource: /citadels/cards.tsv");
        }
        List<DistrictCard> deck = new ArrayList<>();
        Set<String> names = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(source,
                StandardCharsets.UTF_8.newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT)))) {
            String header = reader.readLine();
            if (header == null) {
                throw invalid(1, "missing header");
            }
            // A UTF-8 byte-order mark is optional in exported TSV files.
            if (header.startsWith("\uFEFF")) {
                header = header.substring(1);
            }
            String[] columns = header.split("\t", -1);
            if (columns.length != HEADER.length) {
                throw invalid(1, "expected Name, Qty, color, cost and text columns");
            }
            for (int i = 0; i < HEADER.length; i++) {
                if (!HEADER[i].equals(columns[i].trim().toLowerCase(Locale.ROOT))) {
                    throw invalid(1, "unexpected header column " + (i + 1));
                }
            }

            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) {
                    continue;
                }
                String[] parts = line.split("\t", -1);
                // Normal districts may omit the empty final text column.
                if (parts.length < 4 || parts.length > 5) {
                    throw invalid(lineNumber, "expected four or five tab-separated fields");
                }
                String name = parts[0].trim();
                if (name.isEmpty()) {
                    throw invalid(lineNumber, "district name is empty");
                }
                if (!names.add(name.toLowerCase(Locale.ROOT))) {
                    throw invalid(lineNumber, "duplicate district name: " + name);
                }
                int quantity = positiveInteger(parts[1], lineNumber, "quantity");
                String color = parts[2].trim().toLowerCase(Locale.ROOT);
                if (!COLORS.contains(color)) {
                    throw invalid(lineNumber, "unknown district color: " + color);
                }
                int cost = positiveInteger(parts[3], lineNumber, "cost");
                String special = parts.length == 5 ? parts[4].trim() : "";
                if ("purple".equals(color) && special.isEmpty()) {
                    throw invalid(lineNumber, "purple districts require ability text");
                }
                for (int i = 0; i < quantity; i++) {
                    deck.add(new DistrictCard(name, color, cost, special.isEmpty() ? null : special));
                }
            }
            if (deck.isEmpty()) {
                throw invalid(lineNumber, "the table contains no district cards");
            }
            return deck;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read UTF-8 district card table", e);
        }
    }

    private static int positiveInteger(String value, int line, String field) {
        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed > 0) {
                return parsed;
            }
        } catch (NumberFormatException ignored) {
            // Include the source line and field in the validation error below.
        }
        throw invalid(line, field + " must be a positive integer");
    }

    private static IllegalArgumentException invalid(int line, String detail) {
        return new IllegalArgumentException("Invalid district card table at line " + line + ": " + detail);
    }
}
