package piplupbot.task;

import java.time.LocalDateTime;

import piplupbot.PiplupBotException;

/**
 * ACKNOWLEDGEMENTS: This Java file was written with the help of Claude.
 */

/**
 * A task that runs from one point in time to another,
 * e.g. {@code team project meeting 2019-10-02 1400 to 1600}.
 */
public class Event extends Task {
    /** When the event starts, as a real point in time. */
    protected LocalDateTime from;

    /** When the event ends, as a real point in time. */
    protected LocalDateTime to;

    /**
     * Creates an event that is not done yet.
     *
     * @param description what the task is
     * @param from        when it starts, in any of the layouts {@link DateTimes}
     *                    accepts
     * @param to          when it ends, in the same layouts
     * @throws PiplupBotException if either time is not a date this bot understands
     */
    public Event(String description, String from, String to) throws PiplupBotException {
        super(description);
        this.from = DateTimes.parse(from);
        this.to = DateTimes.parse(to);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@code "E"}, the code for an event
     */
    @Override
    protected String getTypeCode() {
        return "E";
    }

    /**
     * Renders the event as
     * {@code [E][ ] project meeting (from: Oct 2 2019 02:00 PM to: Oct 2 2019 04:00 PM)}.
     *
     * @return the shared task text, which already carries the {@code [E]}
     *         label, followed by the start and end times
     */
    @Override
    public String toString() {
        return super.toString() + " (from: " + DateTimes.format(from)
                + " to: " + DateTimes.format(to) + ")";
    }

    /**
     * {@inheritDoc}
     *
     * <p>An event is saved as
     * {@code E | 0 | project meeting | 2019-10-02T14:00 | 2019-10-02T16:00}.
     * The start and the end are separate fields, so reading the file back does
     * not have to split a combined "2-4pm" apart again, and both are written in
     * ISO form for the reasons given in {@link Deadline#toFileFields()}.</p>
     *
     * @return the shared task fields followed by the start and the end
     */
    @Override
    public String[] toFileFields() {
        return withExtraFields(DateTimes.toFileString(from), DateTimes.toFileString(to));
    }
}
