import java.util.ArrayList;

/**
 * ACKNOWLEDGEMENTS: This Java file was written with the help of Claude.
 */

/**
 * The tasks the user has stored, in the order they were added.
 *
 * <p>This class owns the collection itself and the one rule that goes with it:
 * the user names a task by the number {@code list} showed, counting from 1,
 * while an {@code ArrayList} counts from 0. Converting between the two, and
 * refusing a number that names no task, used to be spread over three command
 * handlers; keeping it here means there is one place that can get it wrong, and
 * a caller cannot reach past the guard by accident.</p>
 *
 * <p>An {@code ArrayList} is used underneath because it grows as tasks are added
 * and closes the gap itself when one is removed, so there is no fixed limit on
 * how many tasks fit and no separate count to keep in step with the contents:
 * {@link #size()} is always the truth.</p>
 *
 * <p>The list is declared as holding {@link Task} but actually holds
 * {@link Todo}, {@link Deadline} and {@link Event} objects. This is polymorphism
 * at work: the tasks are stored and printed through the shared {@code Task}
 * type, and each object's own {@code toString()} decides how it appears.</p>
 */
public class TaskList {
    /** The stored tasks, first added first. */
    private final ArrayList<Task> tasks;

    /**
     * Creates an empty list, for a first run or when the save file could not be
     * read.
     */
    public TaskList() {
        this.tasks = new ArrayList<>();
    }

    /**
     * Creates a list holding the given tasks, e.g. the ones just read from disk.
     *
     * <p>The tasks are copied into a list of this object's own rather than the
     * given list being kept. That way nothing outside can still be holding a
     * reference through which the list could be changed behind this class's
     * back, which is the whole point of putting the collection in a class. With
     * a handful of tasks the copy costs nothing worth measuring.</p>
     *
     * @param initialTasks the tasks to start with, in the order to keep them
     */
    public TaskList(ArrayList<Task> initialTasks) {
        this.tasks = new ArrayList<>(initialTasks);
    }

    /**
     * Returns how many tasks are stored.
     *
     * @return the number of tasks
     */
    public int size() {
        return tasks.size();
    }

    /**
     * Adds a task to the end of the list.
     * It takes a {@code Task} rather than a description, so the same method
     * works for every kind of task without needing to know which one it got.
     *
     * @param task the task to remember
     */
    public void add(Task task) {
        tasks.add(task);
    }

    /**
     * Returns the task at the given position.
     *
     * @param taskNumber the task's position as shown by {@code list}, counting from 1
     * @return the task at that position
     * @throws PiplupBotException if no stored task has that number
     */
    public Task get(int taskNumber) throws PiplupBotException {
        requireTaskNumber(taskNumber);
        return tasks.get(taskNumber - 1);
    }

    /**
     * Removes the task at the given position and hands it back.
     *
     * <p>{@code ArrayList.remove} shifts the tasks that followed it one place
     * towards the front, so the numbering stays contiguous: after deleting task
     * 3, the old task 4 becomes task 3. It also returns the task it removed,
     * which is what the caller's confirmation shows.</p>
     *
     * @param taskNumber the task's position as shown by {@code list}, counting from 1
     * @return the task that was removed
     * @throws PiplupBotException if no stored task has that number
     */
    public Task remove(int taskNumber) throws PiplupBotException {
        requireTaskNumber(taskNumber);
        return tasks.remove(taskNumber - 1);
    }

    /**
     * Returns the tasks as a plain list, for the callers that need to walk the
     * whole thing -- printing them all, or writing them all to disk.
     *
     * <p>A copy is returned rather than the list itself, so that adding to or
     * removing from what a caller gets back cannot change what is stored: the
     * only ways in are {@link #add} and {@link #remove}, which is what keeps
     * the numbering rule above in one place.</p>
     *
     * @return a copy of the stored tasks, in the order the user sees them
     */
    public ArrayList<Task> asList() {
        return new ArrayList<>(tasks);
    }

    /**
     * Returns one line per stored task, each numbered the way the user refers
     * to it, e.g. {@code "2.[D][ ] return book (by: Oct 15 2019, 6:00 pm)"}.
     *
     * <p>The list numbers its own tasks so that the {@code + 1} which turns an
     * index into a task number sits beside the {@code - 1} in {@link #get} and
     * {@link #remove} that turns it back. They are two halves of one rule, and
     * keeping them in one class is what stops them drifting apart.</p>
     *
     * <p>Only the tasks are returned, without any heading above them: what to
     * call this list is the caller's decision, which is what lets the same lines
     * appear under different wording.</p>
     *
     * @return a line per task, in the order the user sees them; empty if there
     *         are no tasks
     */
    public String[] toNumberedLines() {
        String[] lines = new String[tasks.size()];
        for (int i = 0; i < tasks.size(); i++) {
            lines[i] = (i + 1) + "." + tasks.get(i);
        }
        return lines;
    }

    /**
     * Checks that a number names one of the stored tasks.
     * {@code ArrayList} would throw {@code IndexOutOfBoundsException} on a bad
     * index anyway; checking first lets the bot explain the problem in its own
     * words instead of ending the conversation with a stack trace.
     *
     * @param taskNumber the task's position as shown by {@code list}, counting from 1
     * @throws PiplupBotException if no stored task has that number
     */
    private void requireTaskNumber(int taskNumber) throws PiplupBotException {
        if (taskNumber < 1 || taskNumber > tasks.size()) {
            throw new PiplupBotException("There is no task numbered " + taskNumber + ".");
        }
    }
}
