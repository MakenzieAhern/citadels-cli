package citadels;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class DistrictCardLoaderTest {
    private static final String HEADER = "Name\tQty\tcolor\tcost\ttext\n";

    @Test
    void bundledResourceHasCompleteDeckAndIndependentCopies() {
        List<DistrictCard> cards = DistrictCardLoader.loadDistrictCards();
        assertEquals(79, cards.size());
        assertEquals(41, cards.stream().map(DistrictCard::getName).distinct().count());
        Map<String, Long> colors = cards.stream().map(DistrictCard::getColor)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        assertEquals(Map.of("red", 11L, "yellow", 12L, "green", 20L, "blue", 11L, "purple", 25L), colors);
        assertTrue(cards.stream().filter(DistrictCard::isPurple)
                .allMatch(card -> card.getSpecialAbility() != null && !card.getSpecialAbility().isEmpty()));
        assertEquals("Watchtower", cards.get(0).getName());
        assertEquals("Watchtower", cards.get(1).getName());
        assertNotSame(cards.get(0), cards.get(1));
    }

    @Test
    void utf8NamesAndAbilitiesRoundTrip() {
        List<DistrictCard> cards = parse(HEADER + "Bibliothèque\t1\tpurple\t6\t保留两张牌 — café\n");
        assertEquals("Bibliothèque", cards.get(0).getName());
        assertEquals("保留两张牌 — café", cards.get(0).getSpecialAbility());
    }

    @Test
    void acceptsBomBlankLinesCaseAndOptionalEmptyAbilityField() {
        List<DistrictCard> cards = parse("\uFEFFName \tQty\tcolor\tcost\ttext\n\n"
                + " Temple \t 2 \t BLUE \t 1 \t\nTavern\t1\tgreen\t1\n");
        assertEquals(3, cards.size());
        assertEquals("Temple", cards.get(0).getName());
        assertEquals("blue", cards.get(0).getColor());
        assertNull(cards.get(0).getSpecialAbility());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Temple\t3\tblue", "Temple\t3\tblue\t1\textra\tfield",
            "\t3\tblue\t1", "Temple\tzero\tblue\t1", "Temple\t0\tblue\t1",
            "Temple\t-2\tblue\t1", "Temple\t2147483648\tblue\t1",
            "Temple\t3\torange\t1", "Temple\t3\tblue\tzero", "Temple\t3\tblue\t0",
            "Temple\t3\tblue\t-1", "Library\t1\tpurple\t6", "Library\t1\tpurple\t6\t "
    })
    void rejectsInvalidRowInsteadOfReturningAlreadyParsedCards(String row) {
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> parse(HEADER + "Tavern\t1\tgreen\t1\n" + row + "\n"));
        assertTrue(failure.getMessage().contains("line 3"), failure.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "Name\tQty\tcolor\tcost\n", "Name\tAmount\tcolor\tcost\ttext\n"})
    void rejectsMissingOrIncorrectHeader(String table) {
        assertThrows(IllegalArgumentException.class, () -> parse(table));
    }

    @Test
    void rejectsEmptyDeck() {
        assertThrows(IllegalArgumentException.class, () -> parse(HEADER + "\n"));
    }

    @Test
    void rejectsDuplicateDistrictDefinitionsIgnoringCase() {
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> parse(HEADER + "Temple\t1\tblue\t1\ntemple\t1\tblue\t1\n"));
        assertTrue(failure.getMessage().contains("duplicate district name"));
    }

    @Test
    void missingClasspathResourceFailsExplicitly() {
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> DistrictCardLoader.loadDistrictCards(null));
        assertTrue(failure.getMessage().contains("/citadels/cards.tsv"));
    }

    @Test
    void rejectsMalformedUtf8InsteadOfReplacingCharacters() {
        byte[] bytes = (HEADER + "Temple\t1\tblue\t1\n").getBytes(StandardCharsets.UTF_8);
        bytes[bytes.length - 1] = (byte) 0xFF;
        assertThrows(IllegalStateException.class,
                () -> DistrictCardLoader.loadDistrictCards(new ByteArrayInputStream(bytes)));
    }

    @Test
    void ioFailureIsReportedAndClosesSource() {
        class BrokenSource extends InputStream {
            boolean closed;
            @Override public int read() throws IOException { throw new IOException("read failed"); }
            @Override public void close() { closed = true; }
        }
        BrokenSource source = new BrokenSource();
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> DistrictCardLoader.loadDistrictCards(source));
        assertInstanceOf(IOException.class, failure.getCause());
        assertTrue(source.closed);
    }

    private List<DistrictCard> parse(String table) {
        return DistrictCardLoader.loadDistrictCards(
                new ByteArrayInputStream(table.getBytes(StandardCharsets.UTF_8)));
    }
}
