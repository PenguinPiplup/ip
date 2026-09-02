package piplupbot.task;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import piplupbot.PiplupBotException;

/**
 * Tests what {@link Event} adds to {@link Task}: a start and an end.
 *
 * <p>Having two dates rather than one is what makes an event worth its own
 * cases. They are written to the file as two fields rather than as one combined
 * "2-4pm", so that reading the file back does not have to take them apart again
 * -- and they must not be able to swap places, which is what the cases below
 * check by giving the two ends different times.</p>
 *
 * <p>The behaviour every task shares is checked in {@link TodoTest} rather than
 * repeated here.</p>
 */
public class EventTest {

    // ---------- What the user sees ----------

    @Test
    public void toString_newEvent_showsDescriptionStartAndEnd() throws PiplupBotException {
        assertEquals("[E][ ] project meeting (from: Oct 2 2019 02:00 PM to: Oct 2 2019 04:00 PM)",
                new Event("project meeting", "2019-10-02 1400", "2019-10-02 1600").toString());
    }

    @Test
    public void toString_doneEvent_showsCrossInStatusBox() throws PiplupBotException {
        Event event = new Event("project meeting", "2019-10-02 1400", "2019-10-02 1600");
        event.markAsDone();
        assertEquals("[E][X] project meeting (from: Oct 2 2019 02:00 PM to: Oct 2 2019 04:00 PM)",
                event.toString());
    }

    /** An event may run across midnight into the following day. */
    @Test
    public void toString_eventSpanningTwoDays_showsBothDates() throws PiplupBotException {
        assertEquals("[E][ ] 24/7 shift (from: Oct 12 2019 12:00 AM to: Oct 13 2019 12:00 AM)",
                new Event("24/7 shift", "2019-10-12", "2019-10-13").toString());
    }

    // ---------- What the save file holds ----------

    /**
     * The start and the end are separate fields, in that order. Reversing them
     * would still produce a readable file, and every event in it would come back
     * running backwards.
     */
    @Test
    public void toFileFields_newEvent_returnsStartThenEndInIsoForm() throws PiplupBotException {
        assertArrayEquals(
                new String[] {"E", "0", "project meeting", "2019-10-02T14:00", "2019-10-02T16:00"},
                new Event("project meeting", "2019-10-02 1400", "2019-10-02 1600").toFileFields());
    }

    @Test
    public void toFileFields_doneEvent_returnsDoneFlagOfOne() throws PiplupBotException {
        Event event = new Event("project meeting", "2019-10-02 1400", "2019-10-02 1600");
        event.markAsDone();
        assertArrayEquals(
                new String[] {"E", "1", "project meeting", "2019-10-02T14:00", "2019-10-02T16:00"},
                event.toFileFields());
    }

    // ---------- A time the bot cannot read ----------

    /** Either end being unreadable is enough to refuse the whole event. */
    @Test
    public void constructor_unreadableStartTime_exceptionThrown() {
        assertThrows(PiplupBotException.class,
                () -> new Event("meeting", "sometime", "2019-10-02 1600"));
    }

    @Test
    public void constructor_unreadableEndTime_exceptionThrown() {
        assertThrows(PiplupBotException.class,
                () -> new Event("meeting", "2019-10-02 1400", "sometime"));
    }
}
