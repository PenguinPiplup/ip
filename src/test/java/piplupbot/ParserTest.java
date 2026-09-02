package piplupbot;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import piplupbot.command.AddCommand;
import piplupbot.command.Command;
import piplupbot.command.DeleteCommand;
import piplupbot.command.ExitCommand;
import piplupbot.command.ListCommand;
import piplupbot.command.MarkCommand;
import piplupbot.task.TaskList;

/**
 * Tests {@link Parser#parse}, which turns a typed line into the command it asks
 * for.
 *
 * <p>Most of the cases below are refusals, and that is deliberate: a command
 * built from a half-understood line is worse than no command at all, because the
 * mistake is then stored rather than reported. {@code parse} therefore refuses
 * before anything happens, and each refusal is a rule worth pinning down.</p>
 *
 * <p>The rules that are easiest to break by accident are the ones about where a
 * separator is looked for. {@code /by}, {@code /from} and {@code /to} are
 * matched surrounded by spaces, so a description may contain those very
 * characters; and {@code /to} is looked for only after {@code /from}, so the two
 * cannot be read in the wrong order. Both are invisible in ordinary use and
 * would be lost by "simplifying" the search.</p>
 *
 * <p>A command deliberately keeps to itself what it was built from, so a
 * successful parse is checked by carrying the command out and looking at the
 * list it produced -- which is also the only thing the user could observe. The
 * commands are given a save file inside a temporary folder, so a test can never
 * touch the real one.</p>
 */
public class ParserTest {

    /**
     * A folder JUnit creates for each test and deletes afterwards. It is left
     * package-private because {@code @TempDir} cannot fill in a private field.
     */
    @TempDir
    Path tempDir;

    /**
     * Parses each line and carries it out, returning the list it left behind.
     *
     * @param inputs the lines to type, in order
     * @return the task list after all of them have run
     * @throws PiplupBotException if any line is refused
     */
    private TaskList listAfter(String... inputs) throws PiplupBotException {
        TaskList tasks = new TaskList();
        Ui ui = new Ui();
        Storage storage = new Storage(tempDir.resolve("piplupbot.txt"));

        for (String input : inputs) {
            Parser.parse(input).execute(tasks, ui, storage);
        }
        return tasks;
    }

    // ---------- Which command a line names ----------

    @Test
    public void parse_taskCommands_returnAddCommand() throws PiplupBotException {
        assertInstanceOf(AddCommand.class, Parser.parse("todo read book"));
        assertInstanceOf(AddCommand.class, Parser.parse("deadline return book /by 2019-10-15 1800"));
        assertInstanceOf(AddCommand.class,
                Parser.parse("event meeting /from 2019-10-02 1400 /to 2019-10-02 1600"));
    }

    @Test
    public void parse_otherCommands_returnTheirOwnCommandTypes() throws PiplupBotException {
        assertInstanceOf(ListCommand.class, Parser.parse("list"));
        assertInstanceOf(MarkCommand.class, Parser.parse("mark 1"));
        assertInstanceOf(MarkCommand.class, Parser.parse("unmark 1"));
        assertInstanceOf(DeleteCommand.class, Parser.parse("delete 1"));
        assertInstanceOf(ExitCommand.class, Parser.parse("bye"));
    }

    /**
     * Only {@code bye} ends the conversation. The loop asks the command rather
     * than recognising the word, so this is the property it actually relies on.
     */
    @Test
    public void parse_byeCommand_isTheOnlyExitCommand() throws PiplupBotException {
        assertTrue(Parser.parse("bye").isExit());

        for (String input : new String[] {"list", "todo read book", "mark 1", "delete 1"}) {
            Command command = Parser.parse(input);
            assertFalse(command.isExit(), input + " should not end the conversation");
        }
    }

    // ---------- Reading the details out of a line ----------

    @Test
    public void parse_todoCommand_storesTheDescription() throws PiplupBotException {
        assertArrayEquals(new String[] {"1.[T][ ] read book"},
                listAfter("todo read book").toNumberedLines());
    }

    @Test
    public void parse_deadlineCommand_storesDescriptionAndDate() throws PiplupBotException {
        assertArrayEquals(new String[] {"1.[D][ ] return book (by: Oct 15 2019 06:00 PM)"},
                listAfter("deadline return book /by 2019-10-15 1800").toNumberedLines());
    }

    @Test
    public void parse_eventCommand_storesDescriptionStartAndEnd() throws PiplupBotException {
        assertArrayEquals(
                new String[] {
                    "1.[E][ ] project meeting (from: Oct 2 2019 02:00 PM to: Oct 2 2019 04:00 PM)",
                },
                listAfter("event project meeting /from 2019-10-02 1400 /to 2019-10-02 1600")
                        .toNumberedLines());
    }

    /** Extra spaces around the parts are removed, so they never reach the list. */
    @Test
    public void parse_extraSpacesAroundParts_storesThemTrimmed() throws PiplupBotException {
        assertArrayEquals(new String[] {"1.[D][ ] return book (by: Oct 15 2019 06:00 PM)"},
                listAfter("deadline    return book    /by    2019-10-15 1800   ").toNumberedLines());
    }

    @Test
    public void parse_markAndUnmarkCommands_changeTheNamedTask() throws PiplupBotException {
        assertArrayEquals(new String[] {"1.[T][X] read book", "2.[T][ ] write notes"},
                listAfter("todo read book", "todo write notes", "mark 1").toNumberedLines());

        assertArrayEquals(new String[] {"1.[T][ ] read book"},
                listAfter("todo read book", "mark 1", "unmark 1").toNumberedLines());
    }

    @Test
    public void parse_deleteCommand_removesTheNamedTask() throws PiplupBotException {
        assertArrayEquals(new String[] {"1.[T][ ] write notes"},
                listAfter("todo read book", "todo write notes", "delete 1").toNumberedLines());
    }

    // ---------- Where a separator is looked for ----------

    /**
     * The separator is {@code /by} surrounded by spaces, so a description may
     * contain those characters itself. Searching for a bare {@code /by} would
     * cut this description in half.
     */
    @Test
    public void parse_descriptionContainingBySeparator_keepsItInTheDescription()
            throws PiplupBotException {
        assertArrayEquals(new String[] {"1.[D][ ] submit A/by B (by: Oct 15 2019 06:00 PM)"},
                listAfter("deadline submit A/by B /by 2019-10-15 1800").toNumberedLines());
    }

    /**
     * {@code /to} is looked for only after {@code /from}, so an earlier
     * {@code /to} belongs to the description.
     */
    @Test
    public void parse_descriptionContainingToSeparator_keepsItInTheDescription()
            throws PiplupBotException {
        assertArrayEquals(
                new String[] {
                    "1.[E][ ] lunch /to dinner (from: Oct 2 2019 01:00 PM to: Oct 2 2019 02:00 PM)",
                },
                listAfter("event lunch /to dinner /from 2019-10-02 1300 /to 2019-10-02 1400")
                        .toNumberedLines());
    }

    /**
     * The same rule read from the other side: an event written with its end
     * before its start is refused rather than being pieced together in whatever
     * order the separators happen to appear.
     */
    @Test
    public void parse_eventWithToBeforeFrom_exceptionThrown() {
        assertThrows(PiplupBotException.class,
                () -> Parser.parse("event meeting /to 2019-10-02 1600 /from 2019-10-02 1400"));
    }

    // ---------- Lines that are missing something ----------

    @Test
    public void parse_todoWithoutDescription_exceptionThrown() {
        assertThrows(PiplupBotException.class, () -> Parser.parse("todo"));
    }

    @Test
    public void parse_todoWithoutDescription_messageShowsAnExample() {
        PiplupBotException exception =
                assertThrows(PiplupBotException.class, () -> Parser.parse("todo"));
        assertArrayEquals(
                new String[] {"A todo needs a description, e.g. todo borrow book."},
                exception.getMessageLines());
    }

    @Test
    public void parse_deadlineWithoutByPart_exceptionThrown() {
        assertThrows(PiplupBotException.class, () -> Parser.parse("deadline return book"));
        assertThrows(PiplupBotException.class, () -> Parser.parse("deadline"));
    }

    /** Both sides of {@code /by} must have something in them. */
    @Test
    public void parse_deadlineMissingOneSideOfSeparator_exceptionThrown() {
        assertThrows(PiplupBotException.class, () -> Parser.parse("deadline /by 2019-10-15 1800"));
        assertThrows(PiplupBotException.class, () -> Parser.parse("deadline return book /by "));
    }

    @Test
    public void parse_eventMissingASeparator_exceptionThrown() {
        assertThrows(PiplupBotException.class,
                () -> Parser.parse("event meeting /from 2019-10-02 1400"));
        assertThrows(PiplupBotException.class,
                () -> Parser.parse("event meeting /to 2019-10-02 1600"));
        assertThrows(PiplupBotException.class, () -> Parser.parse("event meeting"));
    }

    @Test
    public void parse_eventMissingOnePart_exceptionThrown() {
        assertThrows(PiplupBotException.class,
                () -> Parser.parse("event /from 2019-10-02 1400 /to 2019-10-02 1600"));
        assertThrows(PiplupBotException.class,
                () -> Parser.parse("event meeting /from  /to 2019-10-02 1600"));
    }

    /**
     * A date the bot cannot read reaches the user the same way a missing part
     * does, even though it is noticed further in, by the {@code Deadline}
     * constructor rather than by the parser.
     */
    @Test
    public void parse_taskWithUnreadableDate_exceptionThrown() {
        assertThrows(PiplupBotException.class,
                () -> Parser.parse("deadline return book /by next Friday"));
        assertThrows(PiplupBotException.class,
                () -> Parser.parse("event meeting /from sometime /to 2019-10-02 1600"));
    }

    // ---------- Lines that should name a task number ----------

    @Test
    public void parse_taskNumberMissing_exceptionThrown() {
        assertThrows(PiplupBotException.class, () -> Parser.parse("mark"));
        assertThrows(PiplupBotException.class, () -> Parser.parse("unmark"));
        assertThrows(PiplupBotException.class, () -> Parser.parse("delete"));
    }

    @Test
    public void parse_taskNumberNotANumber_exceptionThrown() {
        assertThrows(PiplupBotException.class, () -> Parser.parse("mark two"));
        assertThrows(PiplupBotException.class, () -> Parser.parse("delete 1 2"));
    }

    /**
     * The hint names the command the user actually typed, so the example it
     * gives is one they can copy. A hard-coded "mark" would be wrong for two of
     * the three commands that share this method.
     */
    @Test
    public void parse_taskNumberNotANumber_messageNamesTheTypedCommand() {
        PiplupBotException exception =
                assertThrows(PiplupBotException.class, () -> Parser.parse("delete three"));
        assertArrayEquals(new String[] {"Please give me a task number, e.g. delete 2."},
                exception.getMessageLines());
    }

    /**
     * Whether a task with that number exists is not the parser's question, so a
     * number naming no task is accepted here and refused later, when the command
     * runs against the list.
     */
    @Test
    public void parse_taskNumberNamingNoTask_returnsCommandThatFailsWhenRun()
            throws PiplupBotException {
        assertInstanceOf(MarkCommand.class, Parser.parse("mark 99"));
        assertThrows(PiplupBotException.class, () -> listAfter("mark 99"));
    }

    // ---------- Lines that name no command at all ----------

    @Test
    public void parse_unknownCommand_exceptionThrown() {
        assertThrows(PiplupBotException.class, () -> Parser.parse("blah"));
        assertThrows(PiplupBotException.class, () -> Parser.parse("list now"));
    }
}
