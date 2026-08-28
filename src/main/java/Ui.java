import java.util.Scanner;

/**
 * ACKNOWLEDGEMENTS: This Java file was written with the help of Claude.
 */

/**
 * Everything the bot says to the user and everything it hears back.
 *
 * <p>Gathering the input and output here means the rest of the program never
 * touches {@code System.out} or {@code System.in} directly. The benefit is that
 * the look of a reply -- the dividers, the indentation, the blank line after --
 * is decided in one place, so changing it is one edit here rather than an edit
 * at every message; and the classes that decide <em>what</em> to say no longer
 * have to know <em>how</em> it is shown.</p>
 *
 * <p>Unlike {@link Storage}, this is a normal class with instance methods rather
 * than a collection of static ones, because it owns a {@link Scanner} over
 * standard input: that scanner is state that has to live as long as the
 * conversation, and one object holding it is simpler than a static field that
 * every method has to assume was set up first.</p>
 */
public class Ui {
    /** Horizontal line that separates the bot's replies from the user's input. */
    private static final String DIVIDER =
            "    ____________________________________________________________";

    /** The name the bot draws for itself before it says anything. */
    private static final String BANNER =
            " ____  _       _             ____        _   \n" +
            "|  _ \\(_)_ __ | |_   _ _ __ | __ )  ___ | |_ \n" +
            "| |_) | | '_ \\| | | | | '_ \\|  _ \\ / _ \\| __|\n" +
            "|  __/| | |_) | | |_| | |_) | |_) | (_) | |_ \n" +
            "|_|   |_| .__/|_|\\__,_| .__/|____/ \\___/ \\__|\n" +
            "        |_|           |_|                    \n";

    /**
     * Reads the user's lines. It is created once and kept, because a
     * {@code Scanner} buffers what it has read ahead; making a new one per line
     * could drop input that the previous one had already taken from the stream.
     */
    private final Scanner scanner = new Scanner(System.in);

    /**
     * Prints one or more lines wrapped between horizontal lines.
     * Each line is indented so the bot's replies stand out from what the user typed.
     *
     * @param lines the lines of text to display
     */
    public void show(String... lines) {
        System.out.println(DIVIDER);
        for (String line : lines) {
            System.out.println("     " + line);
        }
        System.out.println(DIVIDER);
        System.out.println();
    }

    /**
     * Shows a heading with items listed beneath it, all inside one pair of
     * dividers rather than one pair per item.
     *
     * <p>Pasting the heading on top of the items is a question of layout, so it
     * belongs here beside {@link #show} rather than in the class that chose the
     * words. The heading is a parameter because the same items can be introduced
     * differently depending on why they are being shown.</p>
     *
     * @param heading the line that introduces the items
     * @param items   the lines to show beneath it, possibly none
     */
    public void showList(String heading, String[] items) {
        String[] lines = new String[items.length + 1];
        lines[0] = heading;
        System.arraycopy(items, 0, lines, 1, items.length);
        show(lines);
    }

    /**
     * Draws the banner and greets the user.
     */
    public void showWelcome() {
        System.out.println(BANNER);
        show("Hello! I'm PiplupBot.", "What can I do for you?");
    }

    /**
     * Says goodbye, in answer to the {@code bye} command.
     */
    public void showGoodbye() {
        show("Bye. Hope to see you again soon!");
    }

    /**
     * Explains an error the bot recognised.
     *
     * <p>Taking the exception itself, rather than the lines inside it, keeps the
     * caller from having to know that the message is stored as separate lines.</p>
     *
     * @param e the error to explain
     */
    public void showError(PiplupBotException e) {
        show(e.getMessageLines());
    }

    /**
     * Reports whether there is another line of input to read.
     * This is {@code false} once the input runs out, e.g. on Ctrl-D or at the
     * end of a piped file, which lets the main loop stop rather than block.
     *
     * @return {@code true} if {@link #readCommand()} has a line to return
     */
    public boolean hasNextCommand() {
        return scanner.hasNextLine();
    }

    /**
     * Reads the next line the user typed, with the spaces around it removed.
     *
     * @return the trimmed line, which may be empty if the user pressed Enter alone
     */
    public String readCommand() {
        return scanner.nextLine().trim();
    }
}
