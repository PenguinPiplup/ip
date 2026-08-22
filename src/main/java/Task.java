/**
 * ACKNOWLEDGEMENTS: This Java file was written with the help of Claude.
 */

/**
 * A single task in the user's list: what the task is, and whether it is done.
 * Keeping the two together means they can no longer fall out of step,
 * which was a risk while they lived in two separate arrays.
 *
 * <p>This is also the base class for the three kinds of task the bot supports:
 * {@link Todo}, {@link Deadline} and {@link Event}. Everything they have in
 * common lives here, so the bot can hold them all in one {@code Task[]} and
 * call {@code toString()} without knowing which kind it is holding.</p>
 */
public class Task {
    /**
     * What the user typed, e.g. {@code read book}.
     * {@code protected} rather than {@code private} so that future subclasses
     * (deadlines, events, and so on) can read it directly.
     */
    protected String description;

    /** Whether the task has been completed. */
    protected boolean isDone;

    /**
     * Creates a task that is not done yet.
     *
     * @param description what the task is
     */
    public Task(String description) {
        this.description = description;
        this.isDone = false;
    }

    /**
     * Returns the character shown inside the status box in the task list.
     *
     * @return {@code "X"} if the task is done, or a single space if it is not
     */
    public String getStatusIcon() {
        return (isDone ? "X" : " "); // mark done task with X
    }

    /** Records that the task has been completed. */
    public void markAsDone() {
        this.isDone = true;
    }

    /** Records that the task has not been completed after all. */
    public void markAsNotDone() {
        this.isDone = false;
    }

    /**
     * Renders the task the way the task list displays it, e.g. {@code [X] read book}.
     * Java calls this automatically whenever a Task is used where text is expected,
     * such as in string concatenation.
     *
     * @return the status box followed by the description
     */
    @Override
    public String toString() {
        return "[" + getStatusIcon() + "] " + description;
    }
}
