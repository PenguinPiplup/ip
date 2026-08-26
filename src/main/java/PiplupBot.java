import java.util.ArrayList;
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
 * removes a task on the {@code delete <number>} command,
 * and exits when the user types {@code bye}.
 *
 * <p>Every change to the list is written straight to the hard disk by
 * {@link Storage}, and read back when the program starts, so the tasks survive
 * the program being closed.</p>
 */
public class PiplupBot {
    /**
     * Keywords that separate the parts of a deadline or an event.
     * They are surrounded by spaces so that a description containing the word
     * "from" or the characters "/by" is not mistaken for a separator.
     */
    private static final String BY_SEPARATOR = " /by ";
    private static final String FROM_SEPARATOR = " /from ";
    private static final String TO_SEPARATOR = " /to ";

    /** Horizontal line that separates the bot's replies from the user's input. */
    private static final String DIVIDER =
            "    ____________________________________________________________";

    /**
     * Stored tasks, in the order the user added them.
     * An {@code ArrayList} grows as tasks are added and closes the gap itself
     * when one is removed, so there is no fixed limit on how many tasks fit and
     * no separate count to keep in step with the contents: {@code tasks.size()}
     * is always the truth.
     *
     * <p>The list is declared as holding {@code Task} but actually holds
     * {@link Todo}, {@link Deadline} and {@link Event} objects. This is
     * polymorphism at work: the bot stores and prints them all through the
     * shared {@code Task} type, and each object's own {@code toString()}
     * decides how it appears.</p>
     */
    private static final ArrayList<Task> tasks = new ArrayList<>();

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
        tasks.add(task);
        reply("Got it. I've added this task:",
                "  " + task,
                "Now you have " + tasks.size() + " tasks in the list.");
        saveTasks();
    }

    /**
     * Writes the list to disk, telling the user if it could not be written.
     *
     * <p>Saving is a side errand rather than the command the user asked for, so
     * a failure must not swallow the confirmation or end the conversation. It is
     * reported after the confirmation instead, because the confirmation is true
     * as far as this session goes -- the task really was added -- and the warning
     * is what qualifies it.</p>
     */
    private static void saveTasks() {
        try {
            Storage.save(tasks);
        } catch (PiplupBotException e) {
            reply(e.getMessageLines());
        }
    }

    /**
     * Handles {@code todo <description>}.
     *
     * @param input the whole line the user typed
     * @throws PiplupBotException if no description follows the command word
     */
    private static void handleTodo(String input) throws PiplupBotException {
        String description = Command.TODO.argumentOf(input);
        if (description.isEmpty()) {
            throw new PiplupBotException("A todo needs a description, e.g. todo borrow book.");
        }
        addTask(new Todo(description));
    }

    /**
     * Handles {@code deadline <description> /by <when>}.
     * The date is stored as plain text; turning it into a real date is a later step.
     *
     * @param input the whole line the user typed
     * @throws PiplupBotException if the description or the {@code /by} part is missing
     */
    private static void handleDeadline(String input) throws PiplupBotException {
        String details = Command.DEADLINE.argumentOf(input);
        String hint = "A deadline needs a /by part, e.g. deadline return book /by Sunday.";

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
        addTask(new Deadline(description, by));
    }

    /**
     * Handles {@code event <description> /from <start> /to <end>}.
     * The times are stored as plain text, as for deadlines.
     *
     * @param input the whole line the user typed
     * @throws PiplupBotException if the description, the {@code /from} part
     *                            or the {@code /to} part is missing
     */
    private static void handleEvent(String input) throws PiplupBotException {
        String details = Command.EVENT.argumentOf(input);
        String hint = "An event needs a /from and a /to part, "
                + "e.g. event project meeting /from Mon 2pm /to 4pm.";

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
        addTask(new Event(description, from, to));
    }

    /**
     * Displays the stored tasks, numbered starting from 1, with their done status.
     */
    private static void listTasks() {
        // Build the reply one line per task (plus the heading), so reply() can frame
        // them all inside a single pair of dividers.
        String[] lines = new String[tasks.size() + 1];
        lines[0] = "Here are the tasks in your list:";
        for (int i = 0; i < tasks.size(); i++) {
            lines[i + 1] = (i + 1) + "." + tasks.get(i);
        }
        reply(lines);
    }

    /**
     * Checks that a number names one of the stored tasks.
     * Both {@code mark}/{@code unmark} and {@code delete} need this same guard.
     * {@code ArrayList} would throw {@code IndexOutOfBoundsException} on a bad
     * index anyway; checking first lets the bot explain the problem in its own
     * words instead of ending the conversation with a stack trace.
     *
     * @param taskNumber the task's position as shown by {@code list}, counting from 1
     * @throws PiplupBotException if no stored task has that number
     */
    private static void requireTaskNumber(int taskNumber) throws PiplupBotException {
        if (taskNumber < 1 || taskNumber > tasks.size()) {
            throw new PiplupBotException("There is no task numbered " + taskNumber + ".");
        }
    }

    /**
     * Removes the task at the given position and confirms it to the user.
     * {@code ArrayList.remove} shifts the tasks that followed it one place
     * towards the front, so the numbering stays contiguous: after deleting
     * task 3, the old task 4 becomes task 3. It also hands back the task it
     * removed, which is what the confirmation shows.
     *
     * @param taskNumber the task's position as shown by {@code list}, counting from 1
     * @throws PiplupBotException if no stored task has that number
     */
    private static void deleteTask(int taskNumber) throws PiplupBotException {
        requireTaskNumber(taskNumber);

        Task removedTask = tasks.remove(taskNumber - 1);
        reply("Noted. I've removed this task:",
                "  " + removedTask,
                "Now you have " + tasks.size() + " tasks in the list.");
        saveTasks();
    }

    /**
     * Sets the done status of the task at the given position and confirms it to the user.
     * One method covers both directions because marking and unmarking differ only
     * in the value stored and the wording of the confirmation.
     *
     * @param taskNumber the task's position as shown by {@code list}, counting from 1
     * @param isTaskDone {@code true} to mark the task done, {@code false} to reverse it
     * @throws PiplupBotException if no stored task has that number
     */
    private static void setTaskStatus(int taskNumber, boolean isTaskDone) throws PiplupBotException {
        requireTaskNumber(taskNumber);

        Task task = tasks.get(taskNumber - 1);
        if (isTaskDone) {
            task.markAsDone();
        } else {
            task.markAsNotDone();
        }

        String confirmation = isTaskDone
                ? "Nice! I've marked this task as done:"
                : "OK, I've marked this task as not done yet:";
        reply(confirmation, "  " + task);
        saveTasks();
    }

    /**
     * Reads the task number that follows a command such as {@code mark},
     * {@code unmark} or {@code delete}.
     * It only reads the number; what happens to the task it names is left to the
     * caller, which is why all three commands can share this one method.
     *
     * @param input   the whole line the user typed
     * @param command the command the line names
     * @return the number typed after the command word
     * @throws PiplupBotException if what follows the command word is not a number
     */
    private static int parseTaskNumber(String input, Command command) throws PiplupBotException {
        // Everything after the command word should be the task number.
        // argumentOf() copes with the word on its own, e.g. a bare "mark", which
        // leaves an empty argument that parseInt rejects like any other non-number.
        String argument = command.argumentOf(input);

        try {
            return Integer.parseInt(argument);
        } catch (NumberFormatException e) {
            // Translate Java's own exception into the bot's own kind, so that the
            // main loop has just one kind of error to report.
            throw new PiplupBotException(
                    "Please give me a task number, e.g. " + command.getKeyword() + " 2.");
        }
    }

    public static void main(String[] args) {
        // Pick up where the last run left off, before a word is printed: the
        // greeting should already be true when it appears. addAll() fills the
        // existing list rather than replacing it, which is what lets the field
        // stay final -- one list for the whole run, whatever ends up in it.
        Storage.LoadResult loaded = Storage.load();
        tasks.addAll(loaded.tasks());

        String banner = " ____  _       _             ____        _   \n" +
                        "|  _ \\(_)_ __ | |_   _ _ __ | __ )  ___ | |_ \n" +
                        "| |_) | | '_ \\| | | | | '_ \\|  _ \\ / _ \\| __|\n" +
                        "|  __/| | |_) | | |_| | |_) | |_) | (_) | |_ \n" +
                        "|_|   |_| .__/|_|\\__,_| .__/|____/ \\___/ \\__|\n" +
                        "        |_|           |_|                    \n";
        System.out.println(banner);

        reply("Hello! I'm PiplupBot.", "What can I do for you?");

        // Anything wrong with the save file is said after the greeting rather
        // than before it, so the bot introduces itself first and the warning
        // reads as its own words rather than as a crash on startup.
        if (loaded.hasWarning()) {
            reply(loaded.warningLines());
        }

        Scanner scanner = new Scanner(System.in);
        // Keep reading commands until "bye" clears this flag, or until the
        // input runs out (e.g. Ctrl-D / piped input).
        boolean isChatting = true;
        while (isChatting && scanner.hasNextLine()) {
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                // A blank line names no command, so there is nothing to report.
                continue;
            }

            // One try/catch for the whole conversation: the handlers below decide
            // *what* went wrong and throw, while this block is the single place
            // that decides *how* the problem is shown. A new command therefore
            // gets its error reporting for free.
            try {
                // Recognising the command and acting on it are now two steps:
                // Command.fromInput() works out *which* command was typed, or
                // reports that the line names none, and the switch decides what
                // to do about it. Since the switch names every constant, adding
                // a command to the enum leaves a gap here that IntelliJ's
                // "missing branches in enum switch" inspection points straight at.
                Command command = Command.fromInput(input);
                switch (command) {
                case TODO -> handleTodo(input);
                case DEADLINE -> handleDeadline(input);
                case EVENT -> handleEvent(input);
                case LIST -> listTasks();
                case MARK -> setTaskStatus(parseTaskNumber(input, command), true);
                case UNMARK -> setTaskStatus(parseTaskNumber(input, command), false);
                case DELETE -> deleteTask(parseTaskNumber(input, command));
                case BYE -> {
                    reply("Bye. Hope to see you again soon!");
                    isChatting = false;
                }
                }
            } catch (PiplupBotException e) {
                // The bot explains the problem and carries on with the next line,
                // instead of letting the error stop the conversation.
                reply(e.getMessageLines());
            }
        }
    }
}
