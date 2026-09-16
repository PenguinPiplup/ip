package piplupbot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests {@link PiplupBot#respondTo}, the step that both the console and the
 * window take for every line the user types.
 *
 * <p>The console is tested end to end by the text-UI test plan, but the window
 * has no such plan, so these cases are what check that a line typed there is
 * carried out, answered and saved just as the console would do it.</p>
 *
 * <p>The replies are caught by a {@link RecordingUi} of this test's own rather
 * than by the window's {@link GuiUi}, which can only be made while JavaFX is
 * running and which opens the real save file. Every case keeps its tasks in a
 * folder JUnit creates and deletes for it, so no test can read or damage the
 * real save file.</p>
 */
public class PiplupBotTest {

    /**
     * A folder JUnit creates for each test and deletes afterwards. It is left
     * package-private because {@code @TempDir} cannot fill in a private field.
     */
    @TempDir
    Path tempDir;

    /** Catches what the bot says. */
    private final RecordingUi ui = new RecordingUi();

    /**
     * Returns the save file this test should use, inside the temporary folder.
     *
     * @return the path to a save file of this test's own
     */
    private Path saveFile() {
        return tempDir.resolve("piplupbot.txt");
    }

    @Test
    public void respondTo_taskCommand_answersAndSavesTheTask() throws Exception {
        PiplupBot bot = new PiplupBot(saveFile());

        assertFalse(bot.respondTo("todo read book", ui));
        assertEquals(List.of("Piplup! I've tucked this task under my wing:\n"
                        + "  [T][ ] read book\n"
                        + "Now you have 1 task in the list."),
                ui.replies);
        assertEquals("T | 0 | read book\n", Files.readString(saveFile()));
    }

    /** Lines typed one after another act on the same list, as they do in the console. */
    @Test
    public void respondTo_severalLines_actOnTheSameList() {
        PiplupBot bot = new PiplupBot(saveFile());
        bot.respondTo("todo read book", ui);
        bot.respondTo("mark 1", ui);
        ui.replies.clear();

        bot.respondTo("list", ui);

        assertEquals(List.of("Here are the tasks in your list:\n1.[T][X] read book"), ui.replies);
    }

    /**
     * The window has no test plan of its own, so this is what checks that
     * {@code sort} works there too: the same reply the console gives, and a
     * save file holding the tasks in the new order rather than the order they
     * were added in. It is also the only place the sorted order is checked
     * against the file at all -- each text-UI case starts a fresh program, so
     * none of them can see what a sort left behind.
     *
     * <p>The saved line format is unchanged: the same fields, the same
     * separator and the same ISO date as before this command existed. Only
     * which line comes first is different, which is why a file written by an
     * earlier version still loads.</p>
     */
    @Test
    public void respondTo_sortCommand_answersWithTheSortedListAndSavesTheNewOrder()
            throws Exception {
        PiplupBot bot = new PiplupBot(saveFile());
        bot.respondTo("todo read book", ui);
        bot.respondTo("deadline return book /by 2019-10-15 1800", ui);
        ui.replies.clear();

        assertFalse(bot.respondTo("sort date", ui));

        assertEquals(List.of("Here are your tasks, sorted by date:\n"
                        + "1.[D][ ] return book (by: Oct 15 2019 06:00 PM)\n"
                        + "2.[T][ ] read book"),
                ui.replies);
        assertEquals("D | 0 | return book | 2019-10-15T18:00\nT | 0 | read book\n",
                Files.readString(saveFile()));
    }

    /** {@code bye} ends the conversation, and still says goodbye first. */
    @Test
    public void respondTo_bye_saysGoodbyeAndEndsConversation() {
        assertTrue(new PiplupBot(saveFile()).respondTo("bye", ui));
        assertEquals(List.of("Pip-pip! Off for a swim. Hope to see you again soon!"), ui.replies);
    }

    /**
     * A line the bot cannot understand is answered rather than thrown: an
     * exception escaping here would end the console conversation, and leave the
     * window with no reply at all.
     */
    @Test
    public void respondTo_unknownCommand_explainsAndCarriesOn() {
        assertFalse(new PiplupBot(saveFile()).respondTo("blah", ui));
        assertEquals(1, ui.replies.size());
        assertTrue(ui.replies.get(0).startsWith("Pip... I don't know what \"blah\" means."));
    }

    /** A blank line names no command, so it gets no reply, not even an error. */
    @Test
    public void respondTo_blankLine_saysNothingAndCarriesOn() {
        assertFalse(new PiplupBot(saveFile()).respondTo("   ", ui));
        assertEquals(List.of(), ui.replies);
    }

    /**
     * Guards the trim in {@code respondTo}. The console's reader trims every
     * line before the bot sees it, but the window's text box does not, so
     * without it a stray space typed in the window would turn {@code bye} into
     * an unknown command.
     */
    @Test
    public void respondTo_surroundingSpaces_areIgnored() {
        assertTrue(new PiplupBot(saveFile()).respondTo("  bye  ", ui));
    }

    /**
     * A {@link Ui} that keeps each reply instead of showing it, so a test can
     * check exactly what the bot said, and how many times it spoke.
     *
     * <p>It has to write only {@link Ui#show}: the greeting, the goodbye and the
     * rest come with the interface, just as they do for the two real faces.</p>
     */
    private static class RecordingUi implements Ui {
        /** Each reply so far, its lines joined by line breaks. */
        private final List<String> replies = new ArrayList<>();

        @Override
        public void show(String... lines) {
            replies.add(String.join("\n", lines));
        }
    }
}
