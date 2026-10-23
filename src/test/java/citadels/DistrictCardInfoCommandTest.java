package citadels;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class DistrictCardInfoCommandTest {
    private final List<DistrictCard> cards = List.of(
            new DistrictCard("Library", "purple", 6, "Keep both drawn cards."),
            new DistrictCard("Temple", "blue", 1, null));

    @Test
    void looksUpTrimmedCaseInsensitiveNameInTurkishLocale() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            String output = capture("  LIBRARY  ");
            assertTrue(output.contains("Card: Library"));
            assertTrue(output.contains("Cost: 6"));
            assertTrue(output.contains("Special Ability: Keep both drawn cards."));
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test
    void excludesNormalDistrictsAndReportsUnknownNames() {
        assertTrue(capture("Temple").contains("No purple district card named 'Temple' found."));
        assertTrue(capture("Unknown").contains("No purple district card named 'Unknown' found."));
    }

    @Test
    void emptyOrMissingNameShowsUsage() {
        assertTrue(capture("  ").contains("Usage: info"));
        assertTrue(capture(null).contains("Usage: info"));
    }

    private String capture(String name) {
        PrintStream original = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (PrintStream stream = new PrintStream(output, true, StandardCharsets.UTF_8)) {
            System.setOut(stream);
            DistrictCardInfoCommand.execute(name, cards);
        } finally {
            System.setOut(original);
        }
        return output.toString(StandardCharsets.UTF_8);
    }
}
