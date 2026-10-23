package citadels;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AppTest {
    @Test void eofAndQuitExitWithoutCrash() {
        InputStream original=System.in;
        try {
            System.setIn(new ByteArrayInputStream(new byte[0]));
            assertDoesNotThrow(() -> App.main(new String[]{"--seed","42"}));
            System.setIn(new ByteArrayInputStream("quit\n".getBytes(StandardCharsets.UTF_8)));
            assertDoesNotThrow(() -> App.main(new String[]{}));
        } finally { System.setIn(original); }
    }

    @Test void helpDoesNotReadInput() {
        PrintStream original=System.out;
        ByteArrayOutputStream output=new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(output));
            App.main(new String[]{"--help"});
            assertTrue(output.toString(StandardCharsets.UTF_8).contains("--seed"));
        } finally { System.setOut(original); }
    }

    @Test void invalidOptionsAreReportedWithoutStartingGame() {
        PrintStream original=System.err;
        ByteArrayOutputStream output=new ByteArrayOutputStream();
        try {
            System.setErr(new PrintStream(output));
            for(String[] args:new String[][]{{"--seed"},{"--seed","no"},{"--load"},{"--unknown"}}) App.main(args);
            assertEquals(4,output.toString(StandardCharsets.UTF_8).split("Invalid arguments:",-1).length-1);
        } finally { System.setErr(original); }
    }

    @Test void missingSaveReportsErrorInsteadOfStartingANewGame() {
        PrintStream original=System.out;
        ByteArrayOutputStream output=new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(output));
            App.main(new String[]{"--load","/definitely-missing/citadels-save.json"});
            assertTrue(output.toString(StandardCharsets.UTF_8).contains("Failed to load"));
            assertFalse(output.toString(StandardCharsets.UTF_8).contains("Enter how many"));
        } finally { System.setOut(original); }
    }
}
