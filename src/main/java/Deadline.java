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
     * {@inheritDoc}
     *
     * @return {@code "D"}, the code for a deadline
     */
    @Override
    protected String getTypeCode() {
        return "D";
    }

    /**
     * Renders the deadline as {@code [D][ ] return book (by: Sunday)}.
     *
     * @return the shared task text, which already carries the {@code [D]}
     *         label, followed by the due date
     */
    @Override
    public String toString() {
        return super.toString() + " (by: " + by + ")";
    }

    /**
     * Renders the deadline for the save file as {@code D | 0 | return book | June 6th},
     * i.e. the fields every task has, followed by the due date as a field of its own.
     *
     * @return the shared task fields followed by the due date
     */
    @Override
    public String toFileFormat() {
        return super.toFileFormat() + Storage.getFieldSeparator() + by;
    }
}
