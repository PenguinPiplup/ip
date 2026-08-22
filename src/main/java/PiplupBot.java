import java.util.Scanner;

/**
 * ACKNOWLEDGEMENTS: This Java file was written with the help of Claude.
 */

/**
 * A simple command line chatbot.
 * It greets the user, stores whatever text is typed as a task,
 * lists the stored tasks on the {@code list} command,
 * marks a task as done on the {@code mark <number>} command,
 * reverses that on the {@code unmark <number>} command,
 * and exits when the user types {@code bye}.
 */
public class PiplupBot {
    /** Command that ends the conversation. */
    private static final String EXIT_COMMAND = "bye";

    /** Command that displays everything stored so far. */
    private static final String LIST_COMMAND = "list";

    /** Command that marks a task as done; expects a task number after it, e.g. {@code mark 2}. */
    private static final String MARK_COMMAND = "mark";

    /** Command that reverses a task's done status, e.g. {@code unmark 2}. */
    private static final String UNMARK_COMMAND = "unmark";

    /** Maximum number of tasks that can be stored, as allowed by the requirements. */
    private static final int MAX_TASKS = 100;

    /** Horizontal line that separates the bot's replies from the user's input. */
    private static final String DIVIDER =
            "    ____________________________________________________________";

    /**
     * Stored tasks. Only the first {@code taskCount} slots hold real data;
     * the rest are still {@code null}.
     * Each Task carries its own description and done status, so there is no
     * longer a second array to keep in step with this one.
     */
    private static final Task[] tasks = new Task[MAX_TASKS];

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
     * Stores a task and confirms it to the user. New tasks start out not done.
     *
     * @param description the text to remember
     */
    private static void addTask(String description) {
        tasks[taskCount] = new Task(description);
        taskCount++;
        reply("added: " + description);
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
            lines[i + 1] = (i + 1) + "." + tasks[i];
        }
        reply(lines);
    }

    /**
     * Sets the done status of the task at the given position and confirms it to the user.
     * One method covers both directions because marking and unmarking differ only
     * in the value stored and the wording of the confirmation.
     *
     * @param taskNumber the task's position as shown by {@code list}, counting from 1
     * @param done       {@code true} to mark the task done, {@code false} to reverse it
     */
    private static void setTaskStatus(int taskNumber, boolean done) {
        // Guard against numbers that do not name a stored task, so a typo
        // reports a message instead of crashing the program.
        if (taskNumber < 1 || taskNumber > taskCount) {
            reply("There is no task numbered " + taskNumber + ".");
            return;
        }

        Task task = tasks[taskNumber - 1];
        if (done) {
            task.markAsDone();
        } else {
            task.markAsNotDone();
        }

        String confirmation = done
                ? "Nice! I've marked this task as done:"
                : "OK, I've marked this task as not done yet:";
        reply(confirmation, "  " + task);
    }

    /**
     * Reads the task number that follows a {@code mark} or {@code unmark} command
     * and applies the requested status to that task.
     *
     * @param input   the whole line the user typed
     * @param command the command word at the start of {@code input}
     * @param done    the status to apply to the task named by the number
     */
    private static void handleStatusCommand(String input, String command, boolean done) {
        // Everything after the command word should be the task number.
        String argument = input.substring(command.length() + 1).trim();
        try {
            setTaskStatus(Integer.parseInt(argument), done);
        } catch (NumberFormatException e) {
            reply("Please give me a task number, e.g. " + command + " 2.");
        }
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
                handleStatusCommand(input, MARK_COMMAND, true);
            } else if (input.startsWith(UNMARK_COMMAND + " ")) {
                handleStatusCommand(input, UNMARK_COMMAND, false);
            } else if (!input.isEmpty()) {
                // Anything that isn't a known command becomes a stored task.
                addTask(input);
            }
        }
    }
}
