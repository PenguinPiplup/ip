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
 * common lives here, so the bot can hold them all in one list and call
 * {@code toString()} without knowing which kind it is holding.</p>
 *
 * <p>It is {@code abstract} because "a task" on its own is not something the
 * user can add: every task the bot stores is a todo, a deadline or an event.
 * Declaring it so lets the compiler say the same thing, and lets this class
 * call {@link #getTypeLabel()} knowing some subclass must have answered it.</p>
 */
public abstract class Task {
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
     * Returns the label that says which kind of task this is.
     * Each subclass answers for itself, which is why this class does not need
     * to store the kind or ask what it is: calling this method on a
     * {@code Task} reaches the right subclass's answer on its own.
     *
     * @return the letter in square brackets, e.g. {@code "[T]"}
     */
    protected abstract String getTypeLabel();

    /**
     * Renders the task the way the task list displays it, e.g. {@code [T][X] read book}.
     * Java calls this automatically whenever a Task is used where text is expected,
     * such as in string concatenation.
     *
     * <p>Deciding the order -- label, status box, description -- here rather
     * than in each subclass means the three kinds of task cannot drift into
     * three different shapes; a subclass supplies only its own label, plus
     * anything peculiar to it such as a deadline's due date.</p>
     *
     * @return the type label, the status box and the description
     */
    @Override
    public String toString() {
        return getTypeLabel() + "[" + getStatusIcon() + "] " + description;
    }
}
