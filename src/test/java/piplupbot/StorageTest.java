package piplupbot;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import piplupbot.task.Deadline;
import piplupbot.task.Event;
import piplupbot.task.Task;
import piplupbot.task.TaskList;
import piplupbot.task.Todo;

/**
 * Tests {@link Storage}, which keeps the task list on the hard disk.
 *
 * <p>This is the class whose mistakes cost the most. Everything else in the bot
 * fails within one run and can be undone by typing the command again, but a
 * fault here loses tasks the user cannot get back -- and does so quietly, since
 * the file is only read again the next time the bot starts.</p>
 *
 * <p>Two groups of cases follow from that. The first is the round trip: whatever
 * {@link Storage#save} writes, {@link Storage#load} must read back as the same
 * tasks, including descriptions containing the very characters the file format
 * uses. The second is what happens when the file is not what the bot wrote --
 * hand-edited, truncated, or left by an older version. None of those may stop
 * the bot starting, and none may be overwritten without a copy being kept
 * first. A third, smaller group checks that a save which fails leaves the
 * previous file as it was.</p>
 *
 * <p>Every case works inside a folder JUnit creates and deletes for it, so no
 * test can read or damage the real save file.</p>
 */
public class StorageTest {

    /**
     * A folder JUnit creates for each test and deletes afterwards. It is left
     * package-private because {@code @TempDir} cannot fill in a private field.
     */
    @TempDir
    Path tempDir;

    /**
     * Returns the save file this test should use, inside the temporary folder.
     *
     * @return the path to a save file of this test's own
     */
    private Path saveFile() {
        return tempDir.resolve("piplupbot.txt");
    }

    /**
     * Puts the given text in the save file, as though an earlier run -- or a
     * person with a text editor -- had left it there.
     *
     * @param text exactly what the file should hold
     * @throws IOException if the temporary file could not be written
     */
    private void writeSaveFile(String text) throws IOException {
        Files.writeString(saveFile(), text);
    }

    /**
     * Collects tasks into the list {@link Storage#save} expects.
     *
     * @param tasks the tasks to save, in order
     * @return the tasks as a list
     */
    private static ArrayList<Task> listOf(Task... tasks) {
        ArrayList<Task> list = new ArrayList<>();
        for (Task task : tasks) {
            list.add(task);
        }
        return list;
    }

    // ---------- Saving and loading are the reverse of each other ----------

    /**
     * The promise the whole class exists for: the three kinds of task, done and
     * not done, come back exactly as they went in. Comparing the displayed lines
     * checks the descriptions, the kinds, the dates and the done flags at once.
     */
    @Test
    public void saveThenLoad_everyKindOfTask_returnsTheSameTasks() throws Exception {
        Todo todo = new Todo("read book");
        todo.markAsDone();
        ArrayList<Task> original = listOf(
                todo,
                new Deadline("return book", "2019-10-15 1800"),
                new Event("project meeting", "2019-10-02 1400", "2019-10-02 1600"));

        Storage storage = new Storage(saveFile());
        storage.save(original);
        Storage.LoadResult loaded = storage.load();

        assertFalse(loaded.hasWarning());
        assertArrayEquals(new String[] {
            "1.[T][X] read book",
            "2.[D][ ] return book (by: Oct 15 2019 06:00 PM)",
            "3.[E][ ] project meeting (from: Oct 2 2019 02:00 PM to: Oct 2 2019 04:00 PM)",
        }, new TaskList(loaded.tasks()).toNumberedLines());
    }

    /**
     * A description holding the character the file uses to separate fields must
     * survive the trip. Without the escaping it would be read back as an extra
     * field, and the task dropped as damaged -- so this is a case where a
     * missing feature loses a task rather than showing an error.
     */
    @Test
    public void saveThenLoad_descriptionContainingSeparator_returnsTheSameText() throws Exception {
        ArrayList<Task> original = listOf(
                new Todo("buy milk | eggs"),
                new Todo("back \\slash"),
                new Todo("| leading bar"),
                new Todo("trailing backslash \\"));

        Storage storage = new Storage(saveFile());
        storage.save(original);
        Storage.LoadResult loaded = storage.load();

        assertFalse(loaded.hasWarning());
        assertArrayEquals(new String[] {
            "1.[T][ ] buy milk | eggs",
            "2.[T][ ] back \\slash",
            "3.[T][ ] | leading bar",
            "4.[T][ ] trailing backslash \\",
        }, new TaskList(loaded.tasks()).toNumberedLines());
    }

    // ---------- What the file actually holds ----------

    /**
     * The recorded file format, checked as text rather than through a round
     * trip: a round trip would still pass if both halves changed together, and
     * a file written by this version has to stay readable by the next one.
     */
    @Test
    public void save_tasks_writesTheRecordedFormat() throws Exception {
        Todo todo = new Todo("read book");
        todo.markAsDone();

        new Storage(saveFile()).save(listOf(
                todo,
                new Deadline("return book", "2019-10-15 1800"),
                new Event("project meeting", "2019-10-02 1400", "2019-10-02 1600")));

        assertEquals("T | 1 | read book\n"
                        + "D | 0 | return book | 2019-10-15T18:00\n"
                        + "E | 0 | project meeting | 2019-10-02T14:00 | 2019-10-02T16:00\n",
                Files.readString(saveFile()));
    }

    /**
     * A bar inside a description is written after a backslash, and a backslash
     * after another backslash, so the three characters {@code " | "} can only
     * ever mean "next field".
     */
    @Test
    public void save_descriptionContainingSeparator_escapesIt() throws Exception {
        new Storage(saveFile()).save(listOf(new Todo("buy milk | eggs"), new Todo("back \\slash")));

        assertEquals("T | 0 | buy milk \\| eggs\n"
                        + "T | 0 | back \\\\slash\n",
                Files.readString(saveFile()));
    }

    /**
     * Lines end with a line feed on every operating system rather than with
     * whatever the machine prefers, so a file written on Windows and one written
     * on macOS are byte for byte the same.
     */
    @Test
    public void save_tasks_writesLineFeedsOnly() throws Exception {
        new Storage(saveFile()).save(listOf(new Todo("read book"), new Todo("write notes")));

        assertFalse(Files.readString(saveFile()).contains("\r"),
                "The save file should not hold carriage returns on any machine");
    }

    /** An empty list writes an empty file, not a file holding one blank line. */
    @Test
    public void save_emptyList_writesAnEmptyFile() throws Exception {
        new Storage(saveFile()).save(new ArrayList<>());

        assertEquals("", Files.readString(saveFile()));
    }

    /** The list is replaced rather than added to, so a delete really deletes. */
    @Test
    public void save_calledTwice_replacesTheEarlierContents() throws Exception {
        Storage storage = new Storage(saveFile());
        storage.save(listOf(new Todo("first"), new Todo("second")));
        storage.save(listOf(new Todo("second")));

        assertEquals("T | 0 | second\n", Files.readString(saveFile()));
    }

    /**
     * The folder the save file lives in is created if it is not there, so a
     * fresh clone of the project needs no setting up by hand.
     */
    @Test
    public void save_missingParentDirectory_createsIt() throws Exception {
        Path nested = tempDir.resolve("data").resolve("piplupbot.txt");

        new Storage(nested).save(listOf(new Todo("read book")));

        assertTrue(Files.exists(nested));
    }

    // ---------- A save that goes wrong ----------

    /**
     * A save is written to a temporary file and then moved into place, so after
     * one that works, the save file is all that is left.
     */
    @Test
    public void save_successfulSave_leavesNoTemporaryFile() throws Exception {
        new Storage(saveFile()).save(listOf(new Todo("read book")));

        assertFalse(Files.exists(tempDir.resolve("piplupbot.txt.tmp")));
    }

    /**
     * A write that fails must not cost the tasks already saved. A full disk
     * cannot be arranged in a test, so a folder is put where the temporary file
     * goes, which makes writing it fail. Writing straight over the save file, as
     * this class used to, would never touch that folder -- the save would
     * succeed, and this case would fail.
     */
    @Test
    public void save_writeFails_leavesTheOldFileIntact() throws Exception {
        writeSaveFile("T | 0 | read book\n");
        Files.createDirectory(tempDir.resolve("piplupbot.txt.tmp"));

        assertThrows(PiplupBotException.class, () ->
                new Storage(saveFile()).save(listOf(new Todo("write notes"))));
        assertEquals("T | 0 | read book\n", Files.readString(saveFile()));
    }

    /**
     * When the move into place is what fails -- here because a folder stands
     * where the save file should be -- the finished temporary file is removed
     * rather than left beside the save file, and the folder is left alone.
     */
    @Test
    public void save_moveFails_exceptionThrownAndTemporaryFileRemoved() throws Exception {
        Files.createDirectory(saveFile());

        assertThrows(PiplupBotException.class, () ->
                new Storage(saveFile()).save(listOf(new Todo("read book"))));
        assertFalse(Files.exists(tempDir.resolve("piplupbot.txt.tmp")));
        assertTrue(Files.isDirectory(saveFile()));
    }

    // ---------- Reading a file that is missing or empty ----------

    /** A missing file is what the very first run sees, and is not a problem. */
    @Test
    public void load_missingFile_returnsEmptyListWithoutWarning() {
        Storage.LoadResult loaded = new Storage(saveFile()).load();

        assertTrue(loaded.tasks().isEmpty());
        assertFalse(loaded.hasWarning());
    }

    /** Nor is a file that exists but holds nothing, which is what a save of an empty list leaves. */
    @Test
    public void load_emptyFile_returnsEmptyListWithoutWarning() throws Exception {
        writeSaveFile("");

        Storage.LoadResult loaded = new Storage(saveFile()).load();

        assertTrue(loaded.tasks().isEmpty());
        assertFalse(loaded.hasWarning());
    }

    // ---------- What a text editor may add ----------

    /**
     * Blank lines hold no task, so they are passed over without a warning. A
     * warning would send the user looking for a loss that never happened, and
     * leave a pointless rescue copy behind.
     */
    @Test
    public void load_blankLines_skippedWithoutWarning() throws Exception {
        writeSaveFile("\nT | 0 | read book\n   \n\nT | 0 | write notes\n\n");

        Storage.LoadResult loaded = new Storage(saveFile()).load();

        assertArrayEquals(new String[] {"1.[T][ ] read book", "2.[T][ ] write notes"},
                new TaskList(loaded.tasks()).toNumberedLines());
        assertFalse(loaded.hasWarning());
        assertFalse(Files.exists(tempDir.resolve("piplupbot.txt.damaged")));
    }

    /**
     * Some editors start a UTF-8 file with an invisible byte order mark. Left
     * in, it would stick to the first type code, and the first task would be
     * skipped as damaged.
     */
    @Test
    public void load_fileStartingWithByteOrderMark_readsTheFirstTask() throws Exception {
        writeSaveFile("\uFEFFT | 1 | read book\nT | 0 | write notes\n");

        Storage.LoadResult loaded = new Storage(saveFile()).load();

        assertArrayEquals(new String[] {"1.[T][X] read book", "2.[T][ ] write notes"},
                new TaskList(loaded.tasks()).toNumberedLines());
        assertFalse(loaded.hasWarning());
    }

    /**
     * A file saved by a Windows editor ends each line with a carriage return and
     * a line feed. The pair must be read as one line break; splitting on the
     * line feed alone would leave the carriage return at the end of each task's
     * last field.
     */
    @Test
    public void load_windowsLineEndings_readsEveryTask() throws Exception {
        writeSaveFile("T | 0 | read book\r\nD | 0 | return book | 2019-10-15T18:00\r\n");

        Storage.LoadResult loaded = new Storage(saveFile()).load();

        assertArrayEquals(new String[] {
            "1.[T][ ] read book",
            "2.[D][ ] return book (by: Oct 15 2019 06:00 PM)",
        }, new TaskList(loaded.tasks()).toNumberedLines());
        assertFalse(loaded.hasWarning());
    }

    // ---------- Reading a file the bot could not have written ----------

    /**
     * A line that cannot be read is skipped rather than stopping the bot, and
     * the tasks around it still load. Refusing to start would leave the user
     * with no way in at all.
     */
    @Test
    public void load_damagedLine_skipsItAndKeepsTheRest() throws Exception {
        writeSaveFile("T | 0 | read book\n"
                + "this line is nonsense\n"
                + "T | 0 | write notes\n");

        Storage.LoadResult loaded = new Storage(saveFile()).load();

        assertArrayEquals(new String[] {"1.[T][ ] read book", "2.[T][ ] write notes"},
                new TaskList(loaded.tasks()).toNumberedLines());
        assertTrue(loaded.hasWarning());
    }

    /**
     * The warning has to be honest about what was lost and what happens next,
     * because the user's very next command overwrites the file.
     */
    @Test
    public void load_damagedLine_warnsInSingularForOneLine() throws Exception {
        writeSaveFile("T | 0 | read book\nnonsense\n");

        String[] warning = new Storage(saveFile()).load().warningLines();

        assertEquals(3, warning.length);
        assertTrue(warning[0].startsWith("I could not understand 1 line in "),
                "Expected a singular warning, but was: " + warning[0]);
        assertTrue(warning[0].endsWith("so I skipped it."),
                "Expected a singular warning, but was: " + warning[0]);
    }

    @Test
    public void load_severalDamagedLines_warnsInPluralAndCountsThem() throws Exception {
        writeSaveFile("nonsense\nT | 0 | read book\nmore nonsense\nstill more\n");

        String[] warning = new Storage(saveFile()).load().warningLines();

        assertTrue(warning[0].startsWith("I could not understand 3 lines in "),
                "Expected a plural warning counting 3 lines, but was: " + warning[0]);
        assertTrue(warning[0].endsWith("so I skipped them."),
                "Expected a plural warning, but was: " + warning[0]);
    }

    /**
     * The rescue copy is the whole reason a damaged line may be skipped at all:
     * without it, the next command the user typed would destroy the only record
     * of the tasks that could not be read.
     */
    @Test
    public void load_damagedLine_keepsACopyOfTheOriginalFile() throws Exception {
        String originalContents = "T | 0 | read book\nnonsense\n";
        writeSaveFile(originalContents);

        String[] warning = new Storage(saveFile()).load().warningLines();

        Path damagedCopy = tempDir.resolve("piplupbot.txt.damaged");
        assertTrue(Files.exists(damagedCopy), "The damaged file should have been copied aside");
        assertEquals(originalContents, Files.readString(damagedCopy),
                "The copy should hold the file exactly as it was found");
        assertTrue(warning[2].contains("piplupbot.txt.damaged"),
                "The user should be told where the copy is, but was told: " + warning[2]);
    }

    /** A file that reads cleanly leaves no rescue copy behind to confuse anyone. */
    @Test
    public void load_undamagedFile_leavesNoCopyBehind() throws Exception {
        writeSaveFile("T | 0 | read book\n");

        new Storage(saveFile()).load();

        assertFalse(Files.exists(tempDir.resolve("piplupbot.txt.damaged")));
    }

    /**
     * A second damaged start must not replace the copy the first one took,
     * which may be the only record of tasks the user has not yet typed back in.
     * The new copy takes the next free name, and the user is told which.
     */
    @Test
    public void load_differentDamageLater_keepsTheEarlierCopy() throws Exception {
        writeSaveFile("T | 0 | read book\nfirst damage\n");
        new Storage(saveFile()).load();
        writeSaveFile("T | 0 | read book\nsecond damage\n");

        String[] warning = new Storage(saveFile()).load().warningLines();

        assertEquals("T | 0 | read book\nfirst damage\n",
                Files.readString(tempDir.resolve("piplupbot.txt.damaged")));
        assertEquals("T | 0 | read book\nsecond damage\n",
                Files.readString(tempDir.resolve("piplupbot.txt.damaged-2")));
        assertTrue(warning[2].endsWith("piplupbot.txt.damaged-2."),
                "The user should be told where the new copy is, but was told: " + warning[2]);
    }

    /**
     * Starting twice on the same damaged file, with no command in between to
     * overwrite it, points at the copy already taken instead of making another
     * identical one.
     */
    @Test
    public void load_sameDamageTwice_reusesTheCopy() throws Exception {
        writeSaveFile("T | 0 | read book\nnonsense\n");
        new Storage(saveFile()).load();

        String[] warning = new Storage(saveFile()).load().warningLines();

        assertFalse(Files.exists(tempDir.resolve("piplupbot.txt.damaged-2")));
        assertTrue(warning[2].endsWith("piplupbot.txt.damaged."),
                "The user should be pointed at the existing copy, but was told: " + warning[2]);
    }

    /**
     * A folder that happens to have the rescue copy's name is stepped over.
     * Comparing its contents with the save file is not possible, and without
     * the check that rules folders out first, the attempt would fail and no
     * copy would be kept at all.
     */
    @Test
    public void load_folderWithTheCopysName_copiesToTheNextName() throws Exception {
        Files.createDirectory(tempDir.resolve("piplupbot.txt.damaged"));
        writeSaveFile("nonsense\n");

        new Storage(saveFile()).load();

        assertEquals("nonsense\n", Files.readString(tempDir.resolve("piplupbot.txt.damaged-2")));
    }

    // ---------- The particular ways a line can be wrong ----------

    /**
     * Each of these is a line the bot could not itself have written, and each is
     * refused for its own reason. They are checked together because the point is
     * that <em>all</em> of them are caught: a line that slipped through would
     * build a task no command could ever have created, such as a deadline with
     * no date.
     */
    @Test
    public void load_linesTheBotCouldNotHaveWritten_areAllSkipped() throws Exception {
        writeSaveFile("T | 0\n" // too few fields
                + "X | 0 | unknown type\n" // no such kind of task
                + "T | 2 | not a done flag\n" // done flag is 0 or 1
                + "T | 0 | \n" // empty description
                + "D | 0 | no date field\n" // a deadline needs its date
                + "D | 0 | empty date | \n" // and it may not be blank
                + "E | 0 | only one time | 2019-10-02T14:00\n" // an event needs two
                + "T | 0 | extra | field\n" // a todo has exactly three
                + "D | 0 | unreadable date | last Tuesday\n" // the date must be a date
                + "E | 0 | ends first | 2019-10-02T16:00 | 2019-10-02T14:00\n" // end before start
                + "T | 0 | stray escape \\x\n" // no such escape is ever written
                + "T | 0 | read book\n"); // the only good line

        Storage.LoadResult loaded = new Storage(saveFile()).load();

        assertArrayEquals(new String[] {"1.[T][ ] read book"},
                new TaskList(loaded.tasks()).toNumberedLines());
        assertTrue(loaded.warningLines()[0].startsWith("I could not understand 11 lines in "),
                "Expected all eleven bad lines to be skipped, but was: " + loaded.warningLines()[0]);
    }

    /**
     * A file written by an earlier version, which stored the date as the user
     * typed it, no longer parses. Those lines are skipped like any other damaged
     * line -- and, as ever, the file is copied aside first.
     */
    @Test
    public void load_fileFromAnEarlierVersion_skipsItsLinesAndKeepsACopy() throws Exception {
        writeSaveFile("D | 0 | return book | Sunday\n"
                + "E | 0 | project meeting | Mon 2pm | 4pm\n");

        Storage.LoadResult loaded = new Storage(saveFile()).load();

        assertTrue(loaded.tasks().isEmpty());
        assertTrue(loaded.hasWarning());
        assertTrue(Files.exists(tempDir.resolve("piplupbot.txt.damaged")));
    }

    // ---------- A file that cannot be read at all ----------

    /**
     * If something other than a file is in the save file's place, the bot still
     * starts, and it does not promise a rescue copy it cannot make -- copying a
     * directory would quietly produce an empty one, which is exactly the false
     * reassurance the rescue copy exists to avoid.
     */
    @Test
    public void load_directoryInPlaceOfSaveFile_startsEmptyAndPromisesNoCopy() throws Exception {
        Files.createDirectory(saveFile());

        Storage.LoadResult loaded = new Storage(saveFile()).load();

        assertTrue(loaded.tasks().isEmpty());
        assertTrue(loaded.hasWarning());

        String[] warning = loaded.warningLines();
        assertTrue(warning[0].startsWith("I could not read "),
                "Expected a warning about reading the file, but was: " + warning[0]);
        assertTrue(warning[2].endsWith("is not a file."),
                "Expected the bot to admit it has no copy, but was: " + warning[2]);
    }
}
