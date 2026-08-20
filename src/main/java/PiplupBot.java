import java.util.Scanner;

/**
 * ACKNOWLEDGEMENTS: This Java file was written with the help of Claude.
 */

/**
 * A simple command line chatbot.
 * It greets the user, stores whatever text is typed as a task,
 * lists the stored tasks on the {@code list} command,
 * and exits when the user types {@code bye}.
 */
public class PiplupBot {
    /** Command that ends the conversation. */
    private static final String EXIT_COMMAND = "bye";

    /** Command that displays everything stored so far. */
    private static final String LIST_COMMAND = "list";

    /** Maximum number of tasks that can be stored, as allowed by the requirements. */
    private static final int MAX_TASKS = 100;

    /** Horizontal line that separates the bot's replies from the user's input. */
    private static final String DIVIDER =
            "    ____________________________________________________________";

    /**
     * Stored tasks. Only the first {@code taskCount} slots hold real data;
     * the rest are still {@code null}.
     */
    private static final String[] tasks = new String[MAX_TASKS];

    /** Number of tasks currently stored, and the index of the next free slot. */
    private static int taskCount = 0;

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

    /**
     * Stores a task and confirms it to the user.
     *
     * @param task the text to remember
     */
    private static void addTask(String task) {
        tasks[taskCount] = task;
        taskCount++;
        reply("added: " + task);
    }

    /**
     * Displays the stored tasks, numbered starting from 1.
     */
    private static void listTasks() {
        // Build the reply one line per task, so reply() can frame them all
        // inside a single pair of dividers.
        String[] lines = new String[taskCount];
        for (int i = 0; i < taskCount; i++) {
            lines[i] = (i + 1) + ". " + tasks[i];
        }
        reply(lines);
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
            } else if (input.equals(LIST_COMMAND)) {
                listTasks();
            } else if (!input.isEmpty()) {
                // Anything that isn't a known command becomes a stored task.
                addTask(input);
            }
        }
    }
}
