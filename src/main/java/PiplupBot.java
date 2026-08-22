import java.util.Scanner;

/**
 * ACKNOWLEDGEMENTS: This Java file was written with the help of Claude.
 */

/**
 * A simple command line chatbot.
 * It greets the user, stores three kinds of task -- {@code todo},
 * {@code deadline} and {@code event} -- lists the stored tasks on the
 * {@code list} command, marks a task as done on the {@code mark <number>}
 * command, reverses that on the {@code unmark <number>} command,
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

    /** Command that adds a task with no date attached, e.g. {@code todo borrow book}. */
    private static final String TODO_COMMAND = "todo";

    /** Command that adds a task with a due date, e.g. {@code deadline return book /by Sunday}. */
    private static final String DEADLINE_COMMAND = "deadline";

    /** Command that adds a task with a start and an end, e.g. {@code event meeting /from Mon 2pm /to 4pm}. */
    private static final String EVENT_COMMAND = "event";

    /**
     * Keywords that separate the parts of a deadline or an event.
     * They are surrounded by spaces so that a description containing the word
     * "from" or the characters "/by" is not mistaken for a separator.
     */
    private static final String BY_SEPARATOR = " /by ";
    private static final String FROM_SEPARATOR = " /from ";
    private static final String TO_SEPARATOR = " /to ";

    /** Maximum number of tasks that can be stored, as allowed by the requirements. */
    private static final int MAX_TASKS = 100;

    /** Horizontal line that separates the bot's replies from the user's input. */
    private static final String DIVIDER =
            "    ____________________________________________________________";

    /**
     * Stored tasks. Only the first {@code taskCount} slots hold real data;
     * the rest are still {@code null}.
     * The array is declared as {@code Task[]} but holds {@link Todo},
     * {@link Deadline} and {@link Event} objects. This is polymorphism at work:
     * the bot stores and prints them all through the shared {@code Task} type,
     * and each object's own {@code toString()} decides how it appears.
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
     * Stores a task and confirms it to the user.
     * It takes a {@code Task} rather than a description, so the same method
     * works for every kind of task without needing to know which one it got.
     *
     * @param task the task to remember
     */
    private static void addTask(Task task) {
        tasks[taskCount] = task;
        taskCount++;
        reply("Got it. I've added this task:",
                "  " + task,
                "Now you have " + taskCount + " tasks in the list.");
    }

    /**
     * Returns whatever the user typed after a command word, with the spaces
     * around it removed, e.g. {@code "borrow book"} from {@code "todo borrow book"}.
     *
     * @param input   the whole line the user typed
     * @param command the command word at the start of {@code input}
     * @return the rest of the line, possibly empty
     */
    private static String argumentOf(String input, String command) {
        return input.substring(command.length()).trim();
    }

    /**
     * Handles {@code todo <description>}.
     *
     * @param input the whole line the user typed
     */
    private static void handleTodo(String input) {
        String description = argumentOf(input, TODO_COMMAND);
        if (description.isEmpty()) {
            reply("A todo needs a description, e.g. todo borrow book.");
            return;
        }
        addTask(new Todo(description));
    }

    /**
     * Handles {@code deadline <description> /by <when>}.
     * The date is stored as plain text; turning it into a real date is a later step.
     *
     * @param input the whole line the user typed
     */
    private static void handleDeadline(String input) {
        String details = argumentOf(input, DEADLINE_COMMAND);
        String hint = "A deadline needs a /by part, e.g. deadline return book /by Sunday.";

        int separator = details.indexOf(BY_SEPARATOR);
        if (separator < 0) {
            reply(hint);
            return;
        }

        String description = details.substring(0, separator).trim();
        String by = details.substring(separator + BY_SEPARATOR.length()).trim();
        // Reject the command unless there is text on both sides of "/by", so a
        // half-typed line reports a hint instead of storing a nameless task.
        if (description.isEmpty() || by.isEmpty()) {
            reply(hint);
            return;
        }
        addTask(new Deadline(description, by));
    }

    /**
     * Handles {@code event <description> /from <start> /to <end>}.
     * The times are stored as plain text, as for deadlines.
     *
     * @param input the whole line the user typed
     */
    private static void handleEvent(String input) {
        String details = argumentOf(input, EVENT_COMMAND);
        String hint = "An event needs a /from and a /to part, "
                + "e.g. event project meeting /from Mon 2pm /to 4pm.";

        int fromSeparator = details.indexOf(FROM_SEPARATOR);
        // Look for "/to" only after "/from", so that a description containing
        // "/to" earlier in the line cannot be mistaken for the separator.
        int toSeparator = fromSeparator < 0
                ? -1
                : details.indexOf(TO_SEPARATOR, fromSeparator + FROM_SEPARATOR.length());
        if (fromSeparator < 0 || toSeparator < 0) {
            reply(hint);
            return;
        }

        String description = details.substring(0, fromSeparator).trim();
        String from = details.substring(fromSeparator + FROM_SEPARATOR.length(), toSeparator).trim();
        String to = details.substring(toSeparator + TO_SEPARATOR.length()).trim();
        if (description.isEmpty() || from.isEmpty() || to.isEmpty()) {
            reply(hint);
            return;
        }
        addTask(new Event(description, from, to));
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
        // argumentOf() copes with the word on its own, e.g. a bare "mark", which
        // leaves an empty argument that parseInt rejects like any other non-number.
        String argument = argumentOf(input, command);
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
            } else if (input.equals(MARK_COMMAND) || input.startsWith(MARK_COMMAND + " ")) {
                // Accept the word on its own as well, so a forgotten number is
                // answered with a hint rather than an unknown-command message.
                handleStatusCommand(input, MARK_COMMAND, true);
            } else if (input.equals(UNMARK_COMMAND) || input.startsWith(UNMARK_COMMAND + " ")) {
                handleStatusCommand(input, UNMARK_COMMAND, false);
            } else if (input.equals(TODO_COMMAND) || input.startsWith(TODO_COMMAND + " ")) {
                handleTodo(input);
            } else if (input.equals(DEADLINE_COMMAND) || input.startsWith(DEADLINE_COMMAND + " ")) {
                handleDeadline(input);
            } else if (input.equals(EVENT_COMMAND) || input.startsWith(EVENT_COMMAND + " ")) {
                handleEvent(input);
            } else if (!input.isEmpty()) {
                // Every task now has a type, so a line naming no command is a typo
                // rather than a task. Saying so beats silently storing it.
                reply("Sorry, I don't know what \"" + input + "\" means.",
                        "Try: todo, deadline, event, list, mark, unmark, or bye.");
            }
        }
    }
}
