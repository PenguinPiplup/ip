package piplupbot.command;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import piplupbot.PiplupBotException;
import piplupbot.Storage;
import piplupbot.Ui;
import piplupbot.task.Task;
import piplupbot.task.TaskList;
import piplupbot.task.Todo;

/**
 * Tests {@link MarkCommand}, which sets a task's done status for both
 * {@code mark} and {@code unmark}.
 *
 * <p>The case worth guarding is a task that already has the status asked for.
 * It is refused rather than confirmed, and nothing is saved. That branch is easy
 * to lose, because every ordinary {@code mark} and {@code unmark} still works
 * without it; the cases below fail if it is removed.</p>
 *
 * <p>Every case keeps its tasks in a folder JUnit creates and deletes for it, so
 * no test can read or damage the real save file.</p>
 */
public class MarkCommandTest {

    /**
     * A folder JUnit creates for each test and deletes afterwards. It is left
     * package-private because {@code @TempDir} cannot fill in a private field.
     */
    @TempDir
    Path tempDir;

    /** Catches what the command says. */
    private final RecordingUi ui = new RecordingUi();

    /**
     * Returns the save file this test should use, inside the temporary folder.
     *
     * @return the path to a save file of this test's own
     */
    private Path saveFile() {
        return tempDir.resolve("piplupbot.txt");
    }

    /**
     * Builds a list holding one todo, done or not.
     *
     * @param isDone whether the todo starts out done
     * @return a list holding just that todo, as task 1
     */
    private static TaskList listWithOneTodo(boolean isDone) {
        Todo todo = new Todo("read book");
        if (isDone) {
            todo.markAsDone();
        }
        ArrayList<Task> tasks = new ArrayList<>();
        tasks.add(todo);
        return new TaskList(tasks);
    }

    // ---------- Changing the status ----------

    @Test
    public void execute_markTaskNotDone_marksConfirmsAndSaves() throws Exception {
        TaskList tasks = listWithOneTodo(false);

        new MarkCommand(1, true).execute(tasks, ui, new Storage(saveFile()));

        assertTrue(tasks.get(1).isDone());
        assertEquals(List.of("Piplup! One more fish in the bucket. I've marked this task as done:\n"
                + "  [T][X] read book"), ui.replies);
        assertEquals("T | 1 | read book\n", Files.readString(saveFile()));
    }

    @Test
    public void execute_unmarkDoneTask_unmarksConfirmsAndSaves() throws Exception {
        TaskList tasks = listWithOneTodo(true);

        new MarkCommand(1, false).execute(tasks, ui, new Storage(saveFile()));

        assertFalse(tasks.get(1).isDone());
        assertEquals(List.of("Pip-pip, no rush! I've marked this task as not done yet:\n"
                + "  [T][ ] read book"), ui.replies);
        assertEquals("T | 0 | read book\n", Files.readString(saveFile()));
    }

    // ---------- A task that already has that status ----------

    /**
     * Marking a done task is refused, and the message shows the task so a
     * mistyped number is easy to spot. No confirmation is shown, and the save
     * file is never written -- it does not even exist afterwards.
     */
    @Test
    public void execute_markDoneTask_exceptionThrownAndNothingSaved() {
        TaskList tasks = listWithOneTodo(true);

        PiplupBotException exception = assertThrows(PiplupBotException.class, () ->
                new MarkCommand(1, true).execute(tasks, ui, new Storage(saveFile())));

        assertArrayEquals(new String[] {
            "Pip... This task is already marked as done:",
            "  [T][X] read book",
        }, exception.getMessageLines());
        assertTrue(ui.replies.isEmpty(), "Nothing should be confirmed, but was: " + ui.replies);
        assertFalse(Files.exists(saveFile()), "Nothing should be saved");
    }

    /**
     * The same for {@code unmark} on a task that was never done. Its wording is
     * separate, so it has a case of its own.
     */
    @Test
    public void execute_unmarkTaskNotDone_exceptionThrownAndNothingSaved() {
        TaskList tasks = listWithOneTodo(false);

        PiplupBotException exception = assertThrows(PiplupBotException.class, () ->
                new MarkCommand(1, false).execute(tasks, ui, new Storage(saveFile())));

        assertArrayEquals(new String[] {
            "Pip... This task is not marked as done yet:",
            "  [T][ ] read book",
        }, exception.getMessageLines());
        assertTrue(ui.replies.isEmpty(), "Nothing should be confirmed, but was: " + ui.replies);
        assertFalse(Files.exists(saveFile()), "Nothing should be saved");
    }

    /**
     * A {@link Ui} that keeps each reply instead of showing it, so a test can
     * check exactly what the command said -- including that it said nothing.
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
