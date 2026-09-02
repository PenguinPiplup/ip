package piplupbot.task;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Tests {@link Todo}, and with it the behaviour every task inherits from
 * {@link Task}.
 *
 * <p>{@code Task} is abstract, so its methods can only be reached through one of
 * the three kinds of task. {@code Todo} is used for that here because it adds
 * nothing of its own beyond its letter: whatever these cases show is therefore
 * the shared behaviour rather than anything peculiar to todos, and
 * {@link DeadlineTest} and {@link EventTest} are left to cover only the parts
 * that differ.</p>
 *
 * <p>Both of the renderings a task has are checked, because they are two
 * separate promises to two different readers: {@code toString} is what the user
 * sees, and {@code toFileFields} is what the save file must hold for the task to
 * survive being closed and reopened.</p>
 */
public class TodoTest {

    // ---------- What the user sees ----------

    @Test
    public void toString_newTodo_showsTypeLabelAndEmptyStatusBox() {
        assertEquals("[T][ ] read book", new Todo("read book").toString());
    }

    @Test
    public void toString_doneTodo_showsCrossInStatusBox() {
        Todo todo = new Todo("read book");
        todo.markAsDone();
        assertEquals("[T][X] read book", todo.toString());
    }

    /** The description is shown exactly as typed, punctuation and all. */
    @Test
    public void toString_descriptionWithPunctuation_showsItUnchanged() {
        assertEquals("[T][ ] buy milk | eggs", new Todo("buy milk | eggs").toString());
    }

    // ---------- The done status ----------

    @Test
    public void getStatusIcon_newTask_returnsSpace() {
        assertEquals(" ", new Todo("read book").getStatusIcon());
    }

    @Test
    public void getStatusIcon_afterMarkAsDone_returnsCross() {
        Todo todo = new Todo("read book");
        todo.markAsDone();
        assertEquals("X", todo.getStatusIcon());
    }

    /** {@code unmark} has to be able to undo {@code mark}, not just refuse it. */
    @Test
    public void getStatusIcon_afterMarkAsNotDone_returnsSpaceAgain() {
        Todo todo = new Todo("read book");
        todo.markAsDone();
        todo.markAsNotDone();
        assertEquals(" ", todo.getStatusIcon());
    }

    /** Marking a task that is already done leaves it done rather than toggling. */
    @Test
    public void getStatusIcon_markedDoneTwice_staysDone() {
        Todo todo = new Todo("read book");
        todo.markAsDone();
        todo.markAsDone();
        assertEquals("X", todo.getStatusIcon());
    }

    // ---------- What the save file holds ----------

    /**
     * The fields are handed over separately, in the order the file writes them,
     * and the done status as {@code 0} or {@code 1} rather than
     * {@code true}/{@code false}.
     */
    @Test
    public void toFileFields_newTodo_returnsTypeCodeDoneFlagAndDescription() {
        assertArrayEquals(new String[] {"T", "0", "read book"},
                new Todo("read book").toFileFields());
    }

    @Test
    public void toFileFields_doneTodo_returnsDoneFlagOfOne() {
        Todo todo = new Todo("read book");
        todo.markAsDone();
        assertArrayEquals(new String[] {"T", "1", "read book"}, todo.toFileFields());
    }

    /**
     * The description is handed over exactly as the user typed it, including the
     * character the file uses to separate fields. Protecting it is
     * {@link piplupbot.Storage}'s job, not the task's -- so a task that escaped
     * it here would have it escaped twice.
     */
    @Test
    public void toFileFields_descriptionContainingSeparator_returnsItUnescaped() {
        assertArrayEquals(new String[] {"T", "0", "buy milk | eggs"},
                new Todo("buy milk | eggs").toFileFields());
    }
}
