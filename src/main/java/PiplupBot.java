import java.util.Scanner;

/**
 * ACKNOWLEDGEMENTS: This Java file was written with the help of Claude.
 */

/**
 * A simple command line chatbot.
 * It greets the user, stores whatever text is typed as a task,
 * lists the stored tasks on the {@code list} command,
 * marks a task as done on the {@code mark <number>} command,
 * and exits when the user types {@code bye}.
 */
public class PiplupBot {
    /** Command that ends the conversation. */
    private static final String EXIT_COMMAND = "bye";

    /** Command that displays everything stored so far. */
    private static final String LIST_COMMAND = "list";

    /** Command that marks a task as done; expects a task number after it, e.g. {@code mark 2}. */
    private static final String MARK_COMMAND = "mark";

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

    /**
     * Completion status of each task, kept in step with {@code tasks}:
     * {@code isDone[i]} describes {@code tasks[i]}.
     * A boolean array is used instead of a Task class because the requirements
     * do not allow new classes; a Task class would be the tidier design otherwise.
     */
    private static final boolean[] isDone = new boolean[MAX_TASKS];

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
     * Formats one task as a status box followed by its description, e.g. {@code [X] read book}.
     *
     * @param index zero-based position of the task in {@code tasks}
     * @return the task rendered for display
     */
    private static String formatTask(int index) {
        String statusBox = isDone[index] ? "[X]" : "[ ]";
        return statusBox + " " + tasks[index];
    }

    /**
     * Stores a task and confirms it to the user. New tasks start out not done.
     *
     * @param task the text to remember
     */
    private static void addTask(String task) {
        tasks[taskCount] = task;
        isDone[taskCount] = false;
        taskCount++;
        reply("added: " + task);
    }

    /**
     * Displays the stored tasks, numbered starting from 1, with their done status.
     */
    private static void listTasks() {
        // Build the reply one line per task (plus the heading), so reply() can frame
        // them all inside a single pair of dividers.
        String[] lines = new String[taskCount + 1];
        lines[0] = "Here are the tasks in your list:";
        for (int i = 0; i < taskCount; i++) {
            lines[i + 1] = (i + 1) + "." + formatTask(i);
        }
        reply(lines);
    }

    /**
     * Marks the task at the given position as done and confirms it to the user.
     *
     * @param taskNumber the task's position as shown by {@code list}, counting from 1
     */
    private static void markTask(int taskNumber) {
        // Guard against numbers that do not name a stored task, so a typo
        // reports a message instead of crashing the program.
        if (taskNumber < 1 || taskNumber > taskCount) {
            reply("There is no task numbered " + taskNumber + ".");
            return;
        }

        int index = taskNumber - 1;
        isDone[index] = true;
        reply("Nice! I've marked this task as done:", "  " + formatTask(index));
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
            } else if (input.startsWith(MARK_COMMAND + " ")) {
                // Everything after "mark " should be the task number.
                String argument = input.substring(MARK_COMMAND.length() + 1).trim();
                try {
                    markTask(Integer.parseInt(argument));
                } catch (NumberFormatException e) {
                    reply("Please give me a task number, e.g. mark 2.");
                }
            } else if (!input.isEmpty()) {
                // Anything that isn't a known command becomes a stored task.
                addTask(input);
            }
        }
    }
}
