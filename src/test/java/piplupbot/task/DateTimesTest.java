package piplupbot.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import piplupbot.PiplupBotException;

/**
 * Tests {@link DateTimes#parse(String)}.
 *
 * <p>{@code parse} is tested rather than the rest of the bot because it is a
 * pure function: a piece of text goes in, and either a {@link LocalDateTime} or
 * a {@link PiplupBotException} comes out. Nothing is printed, read from disk or
 * remembered between calls, so a test needs no task list, no save file and no
 * stand-in for the user -- which is what makes the failures it reports easy to
 * place.</p>
 *
 * <p>The cases below are grouped by what they check rather than by layout: that
 * each accepted layout is read as the same moment in time, that a date given
 * without a time means midnight, that a date which does not exist on the
 * calendar is refused rather than quietly moved, and that a refusal explains
 * itself. The refusals matter as much as the successes here: a lenient
 * formatter would turn {@code 31/2/2019} into 28 February and store a date the
 * user never typed.</p>
 */
public class DateTimesTest {

    // ---------- The layouts a user may type, and the one the save file holds ----------

    /**
     * The form {@link DateTimes#toFileString} writes, which {@code parse} must
     * accept so that a saved file can be read back.
     */
    @Test
    public void parse_isoDateTimeFromSaveFile_returnsSameDateTime() throws PiplupBotException {
        assertEquals(LocalDateTime.of(2019, 10, 15, 18, 0), DateTimes.parse("2019-10-15T18:00"));
    }

    @Test
    public void parse_dashDateWithCompactTime_returnsDateTime() throws PiplupBotException {
        assertEquals(LocalDateTime.of(2019, 10, 15, 18, 0), DateTimes.parse("2019-10-15 1800"));
    }

    @Test
    public void parse_dashDateWithColonTime_returnsDateTime() throws PiplupBotException {
        assertEquals(LocalDateTime.of(2019, 10, 15, 18, 0), DateTimes.parse("2019-10-15 18:00"));
    }

    /** The day comes first in the slash layout, so this is 2 December, not 12 February. */
    @Test
    public void parse_slashDateWithCompactTime_returnsDateTime() throws PiplupBotException {
        assertEquals(LocalDateTime.of(2019, 12, 2, 18, 0), DateTimes.parse("2/12/2019 1800"));
    }

    @Test
    public void parse_slashDateWithColonTime_returnsDateTime() throws PiplupBotException {
        assertEquals(LocalDateTime.of(2019, 12, 2, 18, 0), DateTimes.parse("2/12/2019 18:00"));
    }

    /** A padded day or month is still accepted, since people write dates both ways. */
    @Test
    public void parse_slashDateWithLeadingZeros_returnsDateTime() throws PiplupBotException {
        assertEquals(LocalDateTime.of(2019, 12, 2, 18, 0), DateTimes.parse("02/12/2019 1800"));
    }

    /** Midnight and noon are where a 12-hour reading of the clock would go wrong. */
    @Test
    public void parse_midnightAndNoon_returnsDateTime() throws PiplupBotException {
        assertEquals(LocalDateTime.of(2019, 10, 15, 0, 0), DateTimes.parse("2019-10-15 0000"));
        assertEquals(LocalDateTime.of(2019, 10, 15, 12, 0), DateTimes.parse("2019-10-15 1200"));
    }

    // ---------- A date with no time of day ----------

    /**
     * A deadline is often a whole day, so a date on its own is taken to mean the
     * start of that day. The user is never shown that choice being made for
     * them, which is why it is pinned down here.
     */
    @Test
    public void parse_isoDateOnly_returnsMidnight() throws PiplupBotException {
        assertEquals(LocalDateTime.of(2019, 10, 15, 0, 0), DateTimes.parse("2019-10-15"));
    }

    @Test
    public void parse_slashDateOnly_returnsMidnight() throws PiplupBotException {
        assertEquals(LocalDateTime.of(2019, 12, 2, 0, 0), DateTimes.parse("2/12/2019"));
    }

    // ---------- Surrounding spaces ----------

    /**
     * The text arrives from a split on {@code /by} or {@code /to}, so it may
     * still carry spaces the user typed around it.
     */
    @Test
    public void parse_surroundingWhitespace_returnsDateTime() throws PiplupBotException {
        assertEquals(LocalDateTime.of(2019, 10, 15, 18, 0),
                DateTimes.parse("   2019-10-15 1800   "));
    }

    // ---------- Dates that do not exist on the calendar ----------

    @Test
    public void parse_leapDayInLeapYear_returnsDateTime() throws PiplupBotException {
        assertEquals(LocalDateTime.of(2020, 2, 29, 0, 0), DateTimes.parse("29/2/2020"));
    }

    /** 2019 has no 29 February, so this must be refused rather than shifted to the 28th. */
    @Test
    public void parse_leapDayInNonLeapYear_exceptionThrown() {
        assertThrows(PiplupBotException.class, () -> DateTimes.parse("29/2/2019"));
    }

    /** February never has 31 days, in any year. */
    @Test
    public void parse_dayOutsideMonth_exceptionThrown() {
        assertThrows(PiplupBotException.class, () -> DateTimes.parse("31/2/2019 1800"));
    }

    @Test
    public void parse_monthOutOfRange_exceptionThrown() {
        assertThrows(PiplupBotException.class, () -> DateTimes.parse("2019-13-01 1800"));
    }

    // ---------- Times that do not exist on the clock ----------

    @Test
    public void parse_hourOutOfRange_exceptionThrown() {
        assertThrows(PiplupBotException.class, () -> DateTimes.parse("2019-10-15 2500"));
    }

    @Test
    public void parse_minuteOutOfRange_exceptionThrown() {
        assertThrows(PiplupBotException.class, () -> DateTimes.parse("2019-10-15 1860"));
    }

    // ---------- Text that names no layout the bot accepts ----------

    /**
     * The dashed layout wants a two-digit month and day, so a date written
     * without the padding is refused. Recording that here means a later decision
     * to accept {@code 2019-1-5} shows up as this test failing, rather than as a
     * silent change in what the bot takes.
     */
    @Test
    public void parse_dashDateWithoutLeadingZeros_exceptionThrown() {
        assertThrows(PiplupBotException.class, () -> DateTimes.parse("2019-1-5 1800"));
    }

    /** Month-first with dashes is a layout the bot does not offer. */
    @Test
    public void parse_monthFirstDashDate_exceptionThrown() {
        assertThrows(PiplupBotException.class, () -> DateTimes.parse("10-15-2019 1800"));
    }

    /** The bot does not read dates written in words, however common they are. */
    @Test
    public void parse_wordDate_exceptionThrown() {
        assertThrows(PiplupBotException.class, () -> DateTimes.parse("tomorrow"));
    }

    /**
     * An empty {@code /by} part is caught by {@link Parser} first, but a date is
     * still refused here rather than becoming some default moment in time.
     */
    @Test
    public void parse_emptyText_exceptionThrown() {
        assertThrows(PiplupBotException.class, () -> DateTimes.parse(""));
    }

    @Test
    public void parse_blankText_exceptionThrown() {
        assertThrows(PiplupBotException.class, () -> DateTimes.parse("     "));
    }

    // ---------- What the user is told when the date cannot be read ----------

    /**
     * The refusal has to be usable: it quotes the text that was rejected, so the
     * user can see what the bot actually received, and follows it with the hint
     * listing the layouts that would have worked.
     */
    @Test
    public void parse_unreadableDate_messageQuotesTextAndGivesHint() {
        PiplupBotException exception =
                assertThrows(PiplupBotException.class, () -> DateTimes.parse("  next Friday  "));

        String[] lines = exception.getMessageLines();
        assertEquals(2, lines.length);
        // The quoted text is trimmed, so the user is shown the date without the
        // spaces that surrounded it in the command they typed.
        assertTrue(lines[0].contains("\"next Friday\""),
                "The first line should quote the rejected date, but was: " + lines[0]);
        assertEquals(DateTimes.FORMAT_HINT, lines[1]);
    }

    // ---------- Saving and loading are the exact reverse of each other ----------

    /**
     * Checks the promise the two methods make together: whatever
     * {@link DateTimes#toFileString} writes into the save file, {@code parse}
     * reads back as the same moment. If that did not hold, every task with a
     * date would come back wrong -- or not at all -- the next time the bot
     * started.
     */
    @Test
    public void parse_textFromToFileString_returnsSameDateTime() throws PiplupBotException {
        LocalDateTime original = LocalDateTime.of(2019, 10, 15, 18, 0);
        assertEquals(original, DateTimes.parse(DateTimes.toFileString(original)));
    }
}
