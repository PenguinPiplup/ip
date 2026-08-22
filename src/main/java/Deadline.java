/**
 * ACKNOWLEDGEMENTS: This Java file was written with the help of Claude.
 */

/**
 * A task that must be done before a given point in time,
 * e.g. {@code submit report by 11/10/2019 5pm}.
 */
public class Deadline extends Task {
    /**
     * When the task is due, exactly as the user typed it after {@code /by}.
     * It is kept as text because the requirements at this stage do not ask for
     * real dates; a later version can parse it into a {@code LocalDateTime}.
     */
    protected String by;

    /**
     * Creates a deadline that is not done yet.
     *
     * @param description what the task is
     * @param by          when it is due, as typed by the user
     */
    public Deadline(String description, String by) {
        super(description);
        this.by = by;
    }

    /**
     * Renders the deadline as {@code [D][ ] return book (by: Sunday)}.
     *
     * @return the type label, the shared task text, and the due date
     */
    @Override
    public String toString() {
        return "[D]" + super.toString() + " (by: " + by + ")";
    }
}
