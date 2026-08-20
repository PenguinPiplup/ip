import java.util.Scanner;

/**
 * A simple command line chatbot.
 * For now it greets the user, echoes back whatever is typed,
 * and exits when the user types the command {@code bye}.
 */
public class PiplupBot {
    /** Command that ends the conversation. */
    private static final String EXIT_COMMAND = "bye";

    /** Horizontal line that separates the bot's replies from the user's input. */
    private static final String DIVIDER =
            "    ____________________________________________________________";

    /**
     * Prints one or more lines wrapped between horizontal lines.
     * Each line is indented so the bot's replies stand out from what the user typed.
     *
     * @param lines the lines of text to display
     */
    private static void reply(String... lines) {
        System.out.println(DIVIDER);
        for (String line : lines) {
            System.out.println("     " + line);
        }
        System.out.println(DIVIDER);
        System.out.println();
    }

    public static void main(String[] args) {
        String banner = " ____  _       _             ____        _   \n" +
                        "|  _ \\(_)_ __ | |_   _ _ __ | __ )  ___ | |_ \n" +
                        "| |_) | | '_ \\| | | | | '_ \\|  _ \\ / _ \\| __|\n" +
                        "|  __/| | |_) | | |_| | |_) | |_) | (_) | |_ \n" +
                        "|_|   |_| .__/|_|\\__,_| .__/|____/ \\___/ \\__|\n" +
                        "        |_|           |_|                    \n";
        System.out.println(banner);

        reply("Hello! I'm PiplupBot.", "What can I do for you?");

        Scanner scanner = new Scanner(System.in);
        // Keep reading commands until the user types "bye",
        // or until the input runs out (e.g. Ctrl-D / piped input).
        while (scanner.hasNextLine()) {
            String input = scanner.nextLine().trim();

            if (input.equals(EXIT_COMMAND)) {
                reply("Bye. Hope to see you again soon!");
                break;
            }

            reply(input);
        }
    }
}
