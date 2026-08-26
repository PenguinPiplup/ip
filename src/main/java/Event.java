/**
 * ACKNOWLEDGEMENTS: This Java file was written with the help of Claude.
 */

/**
 * A task that runs from one point in time to another,
 * e.g. {@code team project meeting 2/10/2019 2-4pm}.
 */
public class Event extends Task {
    /** When the event starts, exactly as the user typed it after {@code /from}. */
    protected String from;

    /** When the event ends, exactly as the user typed it after {@code /to}. */
    protected String to;

    /**
     * Creates an event that is not done yet.
     *
     * @param description what the task is
     * @param from        when it starts, as typed by the user
     * @param to          when it ends, as typed by the user
     */
    public Event(String description, String from, String to) {
        super(description);
        this.from = from;
        this.to = to;
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
     * Renders the event as {@code [E][ ] project meeting (from: Mon 2pm to: 4pm)}.
     *
     * @return the shared task text, which already carries the {@code [E]}
     *         label, followed by the start and end times
     */
    @Override
    public String toString() {
        return super.toString() + " (from: " + from + " to: " + to + ")";
    }

    /**
     * {@inheritDoc}
     *
     * <p>An event is saved as {@code E | 0 | project meeting | Mon 2pm | 4pm}.
     * The start and the end are separate fields, so reading the file back does
     * not have to split a combined "2-4pm" apart again.</p>
     *
     * @return the shared task fields followed by the start and the end
     */
    @Override
    public String[] toFileFields() {
        return withExtraFields(from, to);
    }
}
