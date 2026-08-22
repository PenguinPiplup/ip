/**
 * ACKNOWLEDGEMENTS: This Java file was written with the help of Claude.
 */

/**
 * A task with no date or time attached to it, e.g. {@code visit new theme park}.
 * It adds nothing to {@link Task} except the {@code [T]} label, because a todo
 * really is just a description and a done status.
 */
public class Todo extends Task {
    /**
     * Creates a todo that is not done yet.
     *
     * @param description what the task is
     */
    public Todo(String description) {
        super(description);
    }

    /**
     * Renders the todo as {@code [T][ ] visit new theme park}.
     * The {@code [T]} says which kind of task this is; {@code super.toString()}
     * supplies the status box and description that every task shares.
     *
     * @return the type label followed by the shared task text
     */
    @Override
    public String toString() {
        return "[T]" + super.toString();
    }
}
