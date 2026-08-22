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
     * Renders the event as {@code [E][ ] project meeting (from: Mon 2pm to: 4pm)}.
     *
     * @return the type label, the shared task text, and the start and end times
     */
    @Override
    public String toString() {
        return "[E]" + super.toString() + " (from: " + from + " to: " + to + ")";
    }
}
