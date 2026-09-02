package piplupbot.task;

import java.time.LocalDateTime;

import piplupbot.PiplupBotException;
import piplupbot.Storage;

// ACKNOWLEDGEMENTS: This Java file was written with the help of Claude.

/**
 * A task that must be done before a given point in time,
 * e.g. {@code submit report by 2019-10-11 1700}.
 */
public class Deadline extends Task {
    /**
     * When the task is due, as a real point in time rather than as the text the
     * user typed. Storing it this way means the date is checked once, when the
     * task is created, so every later use of it -- showing it, saving it, and in
     * a later version comparing or sorting by it -- can rely on it being a date
     * at all.
     */
    protected LocalDateTime by;

    /**
     * Creates a deadline that is not done yet.
     *
     * @param description what the task is
     * @param by          when it is due, in any of the layouts {@link DateTimes}
     *                    accepts
     * @throws PiplupBotException if {@code by} is not a date this bot understands
     */
    public Deadline(String description, String by) throws PiplupBotException {
        super(description);
        this.by = DateTimes.parse(by);
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
     * Renders the deadline as {@code [D][ ] return book (by: Oct 15 2019 06:00 PM)}.
     *
     * @return the shared task text, which already carries the {@code [D]}
     *         label, followed by the due date
     */
    @Override
    public String toString() {
        return super.toString() + " (by: " + DateTimes.format(by) + ")";
    }

    /**
     * {@inheritDoc}
     *
     * <p>A deadline is saved as {@code D | 0 | return book | 2019-10-15T18:00}:
     * the fields every task has, followed by the due date as a field of its own.
     * The date is written in ISO form rather than the way it is shown on screen,
     * so that {@link Storage} can read it back exactly and so that changing the
     * display wording later cannot make saved files unreadable.</p>
     *
     * @return the shared task fields followed by the due date
     */
    @Override
    public String[] toFileFields() {
        return withExtraFields(DateTimes.toFileString(by));
    }
}
