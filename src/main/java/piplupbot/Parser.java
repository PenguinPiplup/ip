package piplupbot;

import piplupbot.command.AddCommand;
import piplupbot.command.Command;
import piplupbot.command.CommandWord;
import piplupbot.command.DeleteCommand;
import piplupbot.command.ExitCommand;
import piplupbot.command.FindCommand;
import piplupbot.command.ListCommand;
import piplupbot.command.MarkCommand;
import piplupbot.task.DateTimes;
import piplupbot.task.Deadline;
import piplupbot.task.Event;
import piplupbot.task.Task;
import piplupbot.task.TaskList;
import piplupbot.task.Todo;

/**
 * ACKNOWLEDGEMENTS: This Java file was written with the help of Claude.
 */

/**
 * Turns a line the user typed into the {@link Command} it asks for.
 *
 * <p>Every command that carries more than its own keyword needs its line pulled
 * apart before anything can be done with it: a deadline hides a description and
 * a date on either side of {@code /by}, an event hides three parts, and
 * {@code mark} hides a number. That work used to sit in {@link PiplupBot}
 * alongside the decisions about what to do with the result, which meant the
 * class that decides <em>what</em> a command means also had to know the exact
 * spelling of every separator.</p>
 *
 * <p>Splitting the two apart leaves this class with one job: read the line, or
 * explain why it cannot be read. Nothing here stores a task, prints anything, or
 * knows that a task list exists -- it hands back a command and lets the caller
 * decide when to run it. That is also what makes it easy to test:
 * {@link #parse} is a line of text in, a command or a {@link PiplupBotException}
 * out.</p>
 *
 * <p>Unlike {@link Ui} and {@link TaskList}, this is a class of static methods
 * rather than an object to create, because it has nothing to remember between
 * calls. Reading one line tells it nothing it would want to know while reading
 * the next, so there is no state for an object to hold and no reason to make one.</p>
 *
 * <p>Recognising the word itself is still {@link CommandWord#fromInput}'s
 * question, not this class's: the enum holds the keywords, so matching them
 * belongs with them. This class asks that question and then does what the enum
 * cannot -- build the command object, filled in with whatever the rest of the
 * line said.</p>
 */
public class Parser {
    /**
     * Keywords that separate the parts of a deadline or an event.
     * They are surrounded by spaces so that a description containing the word
     * "from" or the characters "/by" is not mistaken for a separator.
     */
    private static final String BY_SEPARATOR = " /by ";
    private static final String FROM_SEPARATOR = " /from ";
    private static final String TO_SEPARATOR = " /to ";

    /**
     * Reads a whole line and returns the command it asks for, ready to run.
     *
     * <p>The command is built but not carried out, so a line that cannot be
     * understood is refused before anything has happened: no task is stored, no
     * confirmation is printed, and the list is exactly as it was. That is why
     * every method below throws rather than returning something half-filled.</p>
     *
     * <p>The {@code switch} names every constant, so adding a command to
     * {@link CommandWord} leaves a gap here that the compiler reports -- a
     * {@code switch} expression over an enum must cover all of them. This is the
     * one place left that lists the commands; {@link PiplupBot#run} no longer
     * does, because it asks whatever it is given to execute itself.</p>
     *
     * <p>All three task keywords produce an {@link AddCommand}, differing only in
     * which kind of {@link Task} was built for it -- the same polymorphism that
     * lets one list hold all three.</p>
     *
     * @param input the whole line the user typed, already trimmed and not empty
     * @return the command the line asks for
     * @throws PiplupBotException if the line names no command, or names one but
     *                            is missing or mistaking what should follow it
     */
    public static Command parse(String input) throws PiplupBotException {
        CommandWord commandWord = CommandWord.fromInput(input);
        return switch (commandWord) {
        case TODO -> new AddCommand(parseTodo(input));
        case DEADLINE -> new AddCommand(parseDeadline(input));
        case EVENT -> new AddCommand(parseEvent(input));
        case LIST -> new ListCommand();
        case FIND -> new FindCommand(parseKeyword(input));
        case MARK -> new MarkCommand(parseTaskNumber(input, commandWord), true);
        case UNMARK -> new MarkCommand(parseTaskNumber(input, commandWord), false);
        case DELETE -> new DeleteCommand(parseTaskNumber(input, commandWord));
        case BYE -> new ExitCommand();
        };
    }

    /**
     * Reads {@code todo <description>}.
     *
     * @param input the whole line the user typed
     * @return the task the line describes
     * @throws PiplupBotException if no description follows the command word
     */
    private static Todo parseTodo(String input) throws PiplupBotException {
        String description = CommandWord.TODO.argumentOf(input);
        if (description.isEmpty()) {
            throw new PiplupBotException("A todo needs a description, e.g. todo borrow book.");
        }
        return new Todo(description);
    }

    /**
     * Reads {@code deadline <description> /by <when>}.
     *
     * <p>This method only splits the line apart; whether the {@code /by} part is
     * a date at all is {@link DateTimes}'s question, asked by the
     * {@link Deadline} constructor. Both kinds of mistake reach the user the
     * same way, as a {@link PiplupBotException} the main loop turns into a
     * reply.</p>
     *
     * @param input the whole line the user typed
     * @return the task the line describes
     * @throws PiplupBotException if the description or the {@code /by} part is
     *                            missing, or the date cannot be understood
     */
    private static Deadline parseDeadline(String input) throws PiplupBotException {
        String details = CommandWord.DEADLINE.argumentOf(input);
        String hint = "A deadline needs a /by part, "
                + "e.g. deadline return book /by 2019-10-15 1800.";

        int separator = details.indexOf(BY_SEPARATOR);
        if (separator < 0) {
            throw new PiplupBotException(hint);
        }

        String description = details.substring(0, separator).trim();
        String by = details.substring(separator + BY_SEPARATOR.length()).trim();
        // Reject the command unless there is text on both sides of "/by", so a
        // half-typed line reports a hint instead of storing a nameless task.
        if (description.isEmpty() || by.isEmpty()) {
            throw new PiplupBotException(hint);
        }
        return new Deadline(description, by);
    }

    /**
     * Reads {@code event <description> /from <start> /to <end>}.
     * The two times are read the same way a deadline's date is.
     *
     * @param input the whole line the user typed
     * @return the task the line describes
     * @throws PiplupBotException if the description, the {@code /from} part
     *                            or the {@code /to} part is missing, or either
     *                            time cannot be understood
     */
    private static Event parseEvent(String input) throws PiplupBotException {
        String details = CommandWord.EVENT.argumentOf(input);
        String hint = "An event needs a /from and a /to part, "
                + "e.g. event project meeting /from 2019-10-02 1400 /to 2019-10-02 1600.";

        int fromSeparator = details.indexOf(FROM_SEPARATOR);
        // Look for "/to" only after "/from", so that a description containing
        // "/to" earlier in the line cannot be mistaken for the separator.
        int toSeparator = fromSeparator < 0
                ? -1
                : details.indexOf(TO_SEPARATOR, fromSeparator + FROM_SEPARATOR.length());
        if (fromSeparator < 0 || toSeparator < 0) {
            throw new PiplupBotException(hint);
        }

        String description = details.substring(0, fromSeparator).trim();
        String from = details.substring(fromSeparator + FROM_SEPARATOR.length(), toSeparator).trim();
        String to = details.substring(toSeparator + TO_SEPARATOR.length()).trim();
        if (description.isEmpty() || from.isEmpty() || to.isEmpty()) {
            throw new PiplupBotException(hint);
        }
        return new Event(description, from, to);
    }

    /**
     * Reads {@code find <keyword>}.
     *
     * <p>What follows the command word is taken whole, spaces and all, rather
     * than being split into words: {@code find read book} looks for the phrase
     * "read book", not for either word on its own. That is the simpler rule and
     * the one a user is likely to expect from a single line of text; searching
     * for several words at once would need a way to say whether all of them or
     * any of them must match, which nothing in the requirements asks for.</p>
     *
     * @param input the whole line the user typed
     * @return the text to look for
     * @throws PiplupBotException if nothing follows the command word
     */
    private static String parseKeyword(String input) throws PiplupBotException {
        String keyword = CommandWord.FIND.argumentOf(input);
        // A bare "find" would otherwise match every task, since every string
        // contains the empty string -- a confusing way to answer a line that
        // never said what to look for.
        if (keyword.isEmpty()) {
            throw new PiplupBotException("Please tell me what to look for, e.g. find book.");
        }
        return keyword;
    }

    /**
     * Reads the task number that follows a command such as {@code mark},
     * {@code unmark} or {@code delete}.
     * It only reads the number; whether any task has that number is
     * {@link TaskList}'s question, and what happens to the task is the caller's,
     * which is why all three commands can share this one method.
     *
     * @param input       the whole line the user typed
     * @param commandWord the command the line names
     * @return the number typed after the command word
     * @throws PiplupBotException if what follows the command word is not a number
     */
    private static int parseTaskNumber(String input, CommandWord commandWord)
            throws PiplupBotException {
        // Everything after the command word should be the task number.
        // argumentOf() copes with the word on its own, e.g. a bare "mark", which
        // leaves an empty argument that parseInt rejects like any other non-number.
        String argument = commandWord.argumentOf(input);

        try {
            return Integer.parseInt(argument);
        } catch (NumberFormatException e) {
            // Translate Java's own exception into the bot's own kind, so that the
            // main loop has just one kind of error to report.
            throw new PiplupBotException(
                    "Please give me a task number, e.g. " + commandWord.getKeyword() + " 2.");
        }
    }
}
