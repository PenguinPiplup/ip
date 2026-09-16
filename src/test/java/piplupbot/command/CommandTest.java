package piplupbot.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Tests the behavior {@link Command} shares with every command.
 *
 * <p>Only {@link Command#describeTaskCount} is tested here. {@link Command#save}
 * is covered by the tests that run a whole command, and {@link Command#isExit}
 * is a constant.</p>
 */
public class CommandTest {

    /**
     * The singular is the whole reason this method exists: before it, the bot
     * said "1 tasks". This case fails if the check is removed.
     */
    @Test
    public void describeTaskCount_oneTask_singular() {
        assertEquals("Now you have 1 task in the list.", Command.describeTaskCount(1));
    }

    /**
     * Zero is the easiest count to get wrong, since a rule such as
     * {@code taskCount <= 1} would read naturally and still be wrong. This is
     * what a user sees after deleting the last task.
     */
    @Test
    public void describeTaskCount_zeroTasks_plural() {
        assertEquals("Now you have 0 tasks in the list.", Command.describeTaskCount(0));
    }

    /**
     * Eleven is checked as well as two, so a rule that looks at the last digit
     * rather than the whole number would fail.
     */
    @Test
    public void describeTaskCount_severalTasks_plural() {
        assertEquals("Now you have 2 tasks in the list.", Command.describeTaskCount(2));
        assertEquals("Now you have 11 tasks in the list.", Command.describeTaskCount(11));
    }
}
