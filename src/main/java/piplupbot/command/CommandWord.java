package piplupbot.command;

import piplupbot.PiplupBotException;

// ACKNOWLEDGEMENTS: This Java file was written with the help of Claude.

/**
 * The words PiplupBot recognises at the start of a line, one for each command
 * it understands.
 *
 * <p>The enum is named for the word rather than for the command because the
 * word is all it decides: which one was typed, whether anything may follow it,
 * and how to spell it back to the user in a hint. What the command then
 * <em>does</em> is settled elsewhere, by whoever is handed the answer.</p>
 *
 * <p>Holding the keywords here rather than as loose {@code String} constants
 * means the bot has one list of commands instead of several: the word to match,
 * whether the command expects anything after it, and the hint shown for an
 * unknown command all come from these constants. Adding a command is therefore
 * one edit here plus one branch in the main loop.</p>
 *
 * <p>The constants are declared in the order the hint lists them, because the
 * hint is built from {@link #values()}.</p>
 */
public enum CommandWord {
    /** Adds a task with no date attached, e.g. {@code todo borrow book}. */
    TODO("todo", true),

    /** Adds a task with a due date, e.g. {@code deadline return book /by 2019-10-15 1800}. */
    DEADLINE("deadline", true),

    /**
     * Adds a task with a start and an end,
     * e.g. {@code event meeting /from 2019-10-02 1400 /to 2019-10-02 1600}.
     */
    EVENT("event", true),

    /** Displays everything stored so far. */
    LIST("list", false),

    /** Marks a task as done, e.g. {@code mark 2}. */
    MARK("mark", true),

    /** Reverses a task's done status, e.g. {@code unmark 2}. */
    UNMARK("unmark", true),

    /** Removes a task from the list, e.g. {@code delete 3}. */
    DELETE("delete", true),

    /** Ends the conversation. */
    BYE("bye", false);

    /** The word the user types to invoke this command. */
    private final String keyword;

    /**
     * Whether anything may follow the keyword on the same line.
     *
     * <p>This decides how strictly a line is matched. A command that takes an
     * argument also matches when the keyword is followed by a space, so a bare
     * {@code mark} is still recognised and can be answered with a hint about
     * the missing number. A command that takes none matches the keyword alone,
     * which is why {@code list now} and {@code bye now} are reported as
     * unknown commands rather than quietly ignoring the extra word.</p>
     */
    private final boolean takesArgument;

    /**
     * Creates a command word from the keyword the user types for it.
     * An enum's constructor is implicitly private, so the constants declared
     * above are the only instances there will ever be.
     *
     * @param keyword       the word the user types, e.g. {@code "mark"}
     * @param takesArgument whether anything may follow the keyword on the same line
     */
    CommandWord(String keyword, boolean takesArgument) {
        this.keyword = keyword;
        this.takesArgument = takesArgument;
    }

    /**
     * Returns the word the user types for this command, for use in hints such
     * as {@code "Please give me a task number, e.g. mark 2."}.
     *
     * @return the keyword, e.g. {@code "mark"}
     */
    public String getKeyword() {
        return keyword;
    }

    /**
     * Returns whatever the user typed after the keyword, with the spaces around
     * it removed, e.g. {@code "borrow book"} from {@code "todo borrow book"}.
     *
     * <p>Because the command matched the start of the line, the keyword's own
     * length is exactly how much to skip -- there is no second copy of the word
     * that could disagree with the one that matched.</p>
     *
     * @param input the whole line the user typed
     * @return the rest of the line, possibly empty
     */
    public String argumentOf(String input) {
        return input.substring(keyword.length()).trim();
    }

    /**
     * Reports whether this command is the one the given line names.
     *
     * @param input the whole line the user typed, already trimmed
     * @return {@code true} if the line starts with this command's keyword
     */
    private boolean matches(String input) {
        return input.equals(keyword)
                || (takesArgument && input.startsWith(keyword + " "));
    }

    /**
     * Returns the command the given line names.
     *
     * @param input the whole line the user typed, already trimmed and not empty
     * @return the matching command
     * @throws PiplupBotException if the line names no command
     */
    public static CommandWord fromInput(String input) throws PiplupBotException {
        for (CommandWord commandWord : values()) {
            if (commandWord.matches(input)) {
                return commandWord;
            }
        }
        throw new PiplupBotException("Sorry, I don't know what \"" + input + "\" means.",
                "Try: " + keywordList() + ".");
    }

    /**
     * Lists every keyword the way the hint reads them, e.g.
     * {@code "todo, deadline, event, list, mark, unmark, delete, or bye"}.
     *
     * <p>Building the list from {@link #values()} rather than writing it out
     * means a newly added command appears in the hint by itself, so the hint
     * cannot fall out of step with the commands it advertises.</p>
     *
     * @return the keywords in declaration order, separated by commas
     */
    private static String keywordList() {
        CommandWord[] commandWords = values();
        StringBuilder list = new StringBuilder();
        for (int i = 0; i < commandWords.length; i++) {
            if (i > 0) {
                list.append(", ");
            }
            if (i == commandWords.length - 1) {
                list.append("or ");
            }
            list.append(commandWords[i].keyword);
        }
        return list.toString();
    }
}
