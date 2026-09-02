package piplupbot.task;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import piplupbot.PiplupBotException;

/**
 * Tests {@link TaskList}, which owns the tasks and the one rule that goes with
 * them: the user names a task by the number {@code list} showed, counting from
 * 1, while the {@code ArrayList} underneath counts from 0.
 *
 * <p>That conversion is the reason this class is worth testing carefully. An
 * off-by-one here does not crash: it quietly marks or deletes the task next to
 * the one the user meant, which is the kind of bug a person notices only after
 * losing something. The cases below therefore lean on the boundaries -- the
 * first task, the last task, and the numbers just outside both ends -- because
 * that is exactly where a {@code -1} in the wrong place still looks right for
 * every number in the middle.</p>
 *
 * <p>The other group of cases covers the copies this class makes of the list it
 * is given and the list it hands out. Those copies are invisible in normal use
 * and cost nothing to remove, so without a test that notices, they are easy to
 * "tidy away" -- taking with them the guarantee that the only ways to change
 * the list are {@link TaskList#add} and {@link TaskList#remove}.</p>
 */
public class TaskListTest {

    /**
     * Builds a list holding the given descriptions as todos, so each test can
     * say what it is about rather than how to set itself up.
     *
     * @param descriptions what each task is, in the order they are added
     * @return a list holding one todo per description
     */
    private static TaskList listOfTodos(String... descriptions) {
        TaskList tasks = new TaskList();
        for (String description : descriptions) {
            tasks.add(new Todo(description));
        }
        return tasks;
    }

    // ---------- Counting and adding ----------

    @Test
    public void size_newList_isZero() {
        assertEquals(0, new TaskList().size());
    }

    @Test
    public void add_severalTasks_sizeCountsThemAll() {
        assertEquals(3, listOfTodos("a", "b", "c").size());
    }

    // ---------- Reading a task by the number the user sees ----------

    /** Task 1 is the first one added, not the second. */
    @Test
    public void get_firstTaskNumber_returnsFirstTaskAdded() throws PiplupBotException {
        TaskList tasks = listOfTodos("first", "second", "third");
        assertEquals("[T][ ] first", tasks.get(1).toString());
    }

    /** The last valid number is the size itself, not one less. */
    @Test
    public void get_lastTaskNumber_returnsLastTaskAdded() throws PiplupBotException {
        TaskList tasks = listOfTodos("first", "second", "third");
        assertEquals("[T][ ] third", tasks.get(3).toString());
    }

    /**
     * The task handed back is the stored object rather than a copy of it, which
     * is what lets {@code mark} change the list by changing what it was given.
     */
    @Test
    public void get_taskNumber_returnsStoredTaskItself() throws PiplupBotException {
        Todo stored = new Todo("read book");
        TaskList tasks = new TaskList();
        tasks.add(stored);
        assertSame(stored, tasks.get(1));
    }

    /** 0 is the index the list uses internally, and is never a task number. */
    @Test
    public void get_zero_exceptionThrown() {
        TaskList tasks = listOfTodos("only task");
        assertThrows(PiplupBotException.class, () -> tasks.get(0));
    }

    @Test
    public void get_numberJustPastTheEnd_exceptionThrown() {
        TaskList tasks = listOfTodos("first", "second");
        assertThrows(PiplupBotException.class, () -> tasks.get(3));
    }

    @Test
    public void get_negativeNumber_exceptionThrown() {
        TaskList tasks = listOfTodos("only task");
        assertThrows(PiplupBotException.class, () -> tasks.get(-1));
    }

    /** Every number is out of range when there are no tasks at all. */
    @Test
    public void get_emptyList_exceptionThrown() {
        TaskList tasks = new TaskList();
        assertThrows(PiplupBotException.class, () -> tasks.get(1));
    }

    /**
     * The refusal names the number the user typed, so they can see which one the
     * bot rejected rather than guessing.
     */
    @Test
    public void get_numberNamingNoTask_messageNamesThatNumber() {
        TaskList tasks = listOfTodos("only task");
        PiplupBotException exception = assertThrows(PiplupBotException.class, () -> tasks.get(5));
        assertArrayEquals(new String[] {"There is no task numbered 5."},
                exception.getMessageLines());
    }

    // ---------- Removing a task ----------

    @Test
    public void remove_taskNumber_returnsRemovedTask() throws PiplupBotException {
        TaskList tasks = listOfTodos("first", "second", "third");
        assertEquals("[T][ ] second", tasks.remove(2).toString());
    }

    /**
     * The tasks after the removed one move up, so the numbering has no gap in
     * it. This is what makes a second {@code delete 2} remove what used to be
     * task 3, which is what the user sees on screen.
     */
    @Test
    public void remove_middleTask_renumbersTheTasksAfterIt() throws PiplupBotException {
        TaskList tasks = listOfTodos("first", "second", "third");
        tasks.remove(2);

        assertEquals(2, tasks.size());
        assertArrayEquals(new String[] {"1.[T][ ] first", "2.[T][ ] third"},
                tasks.toNumberedLines());
    }

    @Test
    public void remove_numberNamingNoTask_exceptionThrown() {
        TaskList tasks = listOfTodos("first", "second");
        assertThrows(PiplupBotException.class, () -> tasks.remove(0));
        assertThrows(PiplupBotException.class, () -> tasks.remove(3));
    }

    /**
     * A refused removal must leave the list exactly as it was. Checking the
     * whole list, not just its size, is what would catch a version that removed
     * the wrong task before noticing the number was out of range.
     */
    @Test
    public void remove_numberNamingNoTask_leavesListUnchanged() {
        TaskList tasks = listOfTodos("first", "second");
        assertThrows(PiplupBotException.class, () -> tasks.remove(3));

        assertArrayEquals(new String[] {"1.[T][ ] first", "2.[T][ ] second"},
                tasks.toNumberedLines());
    }

    // ---------- The numbered lines the user is shown ----------

    /**
     * An empty list produces no lines at all rather than a line saying so:
     * what to print when there is nothing is the caller's decision, not this
     * class's.
     */
    @Test
    public void toNumberedLines_emptyList_returnsNoLines() {
        assertArrayEquals(new String[0], new TaskList().toNumberedLines());
    }

    /** Numbering starts at 1 and matches the numbers {@code get} accepts. */
    @Test
    public void toNumberedLines_severalTasks_numbersFromOne() {
        assertArrayEquals(
                new String[] {"1.[T][ ] first", "2.[T][ ] second", "3.[T][ ] third"},
                listOfTodos("first", "second", "third").toNumberedLines());
    }

    /** Each task prints itself, so the three kinds appear in their own shapes. */
    @Test
    public void toNumberedLines_differentKindsOfTask_showsEachInItsOwnForm()
            throws PiplupBotException {
        TaskList tasks = new TaskList();
        tasks.add(new Todo("read book"));
        tasks.add(new Deadline("return book", "2019-10-15 1800"));
        tasks.add(new Event("project meeting", "2019-10-02 1400", "2019-10-02 1600"));

        assertArrayEquals(new String[] {
            "1.[T][ ] read book",
            "2.[D][ ] return book (by: Oct 15 2019 06:00 PM)",
            "3.[E][ ] project meeting (from: Oct 2 2019 02:00 PM to: Oct 2 2019 04:00 PM)",
        }, tasks.toNumberedLines());
    }

    // ---------- Searching for a keyword ----------

    /**
     * Only the matching tasks come back, and they are numbered from 1 among
     * themselves: "return book" was task 3 and is shown as 2. That renumbering
     * is the part worth pinning down, because it is what makes a number read off
     * a search result mean something different from the same number typed at
     * {@code delete}.
     */
    @Test
    public void find_keywordInSomeDescriptions_returnsOnlyThoseNumberedFromOne() {
        TaskList tasks = listOfTodos("read book", "write notes", "return book");
        assertArrayEquals(new String[] {"1.[T][ ] read book", "2.[T][ ] return book"},
                tasks.find("book").toNumberedLines());
    }

    @Test
    public void find_keywordMatchingNothing_returnsEmptyList() {
        assertEquals(0, listOfTodos("read book", "write notes").find("homework").size());
    }

    /** The matches keep the order they were added in. */
    @Test
    public void find_keywordMatchingEveryTask_returnsThemInTheStoredOrder() {
        TaskList tasks = listOfTodos("book one", "book two", "book three");
        assertArrayEquals(
                new String[] {"1.[T][ ] book one", "2.[T][ ] book two", "3.[T][ ] book three"},
                tasks.find("book").toNumberedLines());
    }

    /**
     * The keyword reaches each task as typed, so a search through the list is as
     * forgiving about capitals as {@link Task#descriptionContains} is on its
     * own. Lower-casing the keyword here as well would be harmless; lower-casing
     * it instead of there would not, which is why this is checked from both
     * ends.
     */
    @Test
    public void find_keywordInADifferentCase_stillMatches() {
        assertArrayEquals(new String[] {"1.[T][ ] Read Book"},
                listOfTodos("Read Book", "write notes").find("bOOk").toNumberedLines());
    }

    /** Searching reads the list; it must not change what is stored. */
    @Test
    public void find_anyKeyword_leavesTheStoredTasksUnchanged() {
        TaskList tasks = listOfTodos("read book", "write notes");
        tasks.find("book");
        tasks.find("nothing matches this");

        assertArrayEquals(new String[] {"1.[T][ ] read book", "2.[T][ ] write notes"},
                tasks.toNumberedLines());
    }

    /**
     * A match is the stored task itself rather than a copy of it, so a search
     * shows the task's real done status rather than a snapshot of it.
     */
    @Test
    public void find_matchingTask_isTheStoredTaskItself() throws PiplupBotException {
        Todo stored = new Todo("read book");
        TaskList tasks = new TaskList();
        tasks.add(stored);

        assertSame(stored, tasks.find("book").get(1));
    }

    /**
     * The result is a list of its own, so changing it changes nothing stored.
     * Without that, handing out a search result would be a second way into the
     * list, past {@link TaskList#add} and {@link TaskList#remove}.
     */
    @Test
    public void find_returnedListChanged_storedTasksUnchanged() {
        TaskList tasks = listOfTodos("read book");

        TaskList matches = tasks.find("book");
        matches.add(new Todo("added to the search result"));

        assertEquals(1, tasks.size());
        assertArrayEquals(new String[] {"1.[T][ ] read book"}, tasks.toNumberedLines());
    }

    // ---------- The copies that keep the list this class's own ----------

    /**
     * Adding to the list {@link TaskList#asList} handed back must not add to the
     * stored list. Were the stored list returned directly, any caller could add
     * or remove tasks without going through the numbering rule above.
     */
    @Test
    public void asList_returnedListChanged_storedTasksUnchanged() {
        TaskList tasks = listOfTodos("first");

        ArrayList<Task> copy = tasks.asList();
        copy.add(new Todo("added behind the list's back"));
        copy.remove(0);

        assertEquals(1, tasks.size());
        assertArrayEquals(new String[] {"1.[T][ ] first"}, tasks.toNumberedLines());
    }

    /**
     * The same in the other direction: the list handed to the constructor is
     * copied, so whoever still holds it -- {@link piplupbot.Storage} after a
     * load, for instance -- cannot change the task list by changing theirs.
     */
    @Test
    public void constructor_givenListChangedAfterwards_storedTasksUnchanged() {
        ArrayList<Task> initialTasks = new ArrayList<>();
        initialTasks.add(new Todo("first"));

        TaskList tasks = new TaskList(initialTasks);
        initialTasks.add(new Todo("added after construction"));

        assertEquals(1, tasks.size());
        assertArrayEquals(new String[] {"1.[T][ ] first"}, tasks.toNumberedLines());
    }
}
