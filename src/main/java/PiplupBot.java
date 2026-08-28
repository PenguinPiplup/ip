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
 *
 * <p>This class is the bot's decision-maker: it works out what each command
 * should do and what to say about it. Holding the tasks is {@link TaskList}'s
 * work, and saying things out loud -- and hearing what the user typed -- is
 * {@link Ui}'s, so nothing here needs to know how a list is indexed or how a
 * reply is laid out on screen.</p>
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

    /**
     * The tasks the user has stored. It is filled in {@link #main} from whatever
     * the save file held, rather than started empty here, because the bot has
     * nothing useful to do with a list until it knows what was loaded.
     */
    private static TaskList tasks;

    /**
     * Everything the bot says and hears. The bot keeps one of these for the
     * whole run, and asks it to do all the printing and reading, so no method
     * here touches {@code System.out} or {@code System.in} itself.
     */
    private static final Ui ui = new Ui();

    /**
     * Stores a task and confirms it to the user.
     * It takes a {@code Task} rather than a description, so the same method
     * works for every kind of task without needing to know which one it got.
     *
     * @param task the task to remember
     */
    private static void addTask(Task task) {
        tasks.add(task);
        ui.show("Got it. I've added this task:",
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
            Storage.save(tasks.asList());
        } catch (PiplupBotException e) {
            ui.showError(e);
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
     *
     * <p>This method only splits the line apart; whether the {@code /by} part is
     * a date at all is {@link DateTimes}'s question, asked by the
     * {@link Deadline} constructor. Both kinds of mistake reach the user the
     * same way, as a {@link PiplupBotException} the main loop turns into a
     * reply.</p>
     *
     * @param input the whole line the user typed
     * @throws PiplupBotException if the description or the {@code /by} part is
     *                            missing, or the date cannot be understood
     */
    private static void handleDeadline(String input) throws PiplupBotException {
        String details = Command.DEADLINE.argumentOf(input);
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
        addTask(new Deadline(description, by));
    }

    /**
     * Handles {@code event <description> /from <start> /to <end>}.
     * The two times are read the same way a deadline's date is.
     *
     * @param input the whole line the user typed
     * @throws PiplupBotException if the description, the {@code /from} part
     *                            or the {@code /to} part is missing, or either
     *                            time cannot be understood
     */
    private static void handleEvent(String input) throws PiplupBotException {
        String details = Command.EVENT.argumentOf(input);
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
        addTask(new Event(description, from, to));
    }

    /**
     * Displays the stored tasks, numbered starting from 1, with their done status.
     * All this method decides is what to call the list: the numbering is
     * {@link TaskList}'s and the layout is {@link Ui}'s.
     */
    private static void listTasks() {
        ui.showList("Here are the tasks in your list:", tasks.toNumberedLines());
    }

    /**
     * Removes the task at the given position and confirms it to the user.
     * {@link TaskList#remove} does the removing and hands back the task it
     * removed, which is what the confirmation shows; this method's own job is
     * only to word that confirmation and have the change saved.
     *
     * @param taskNumber the task's position as shown by {@code list}, counting from 1
     * @throws PiplupBotException if no stored task has that number
     */
    private static void deleteTask(int taskNumber) throws PiplupBotException {
        Task removedTask = tasks.remove(taskNumber);
        ui.show("Noted. I've removed this task:",
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
        Task task = tasks.get(taskNumber);
        if (isTaskDone) {
            task.markAsDone();
        } else {
            task.markAsNotDone();
        }

        String confirmation = isTaskDone
                ? "Nice! I've marked this task as done:"
                : "OK, I've marked this task as not done yet:";
        ui.show(confirmation, "  " + task);
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
        // greeting should already be true when it appears. The loaded tasks are
        // handed to the TaskList's constructor, so the list starts out holding
        // exactly what the file held.
        Storage.LoadResult loaded = Storage.load();
        tasks = new TaskList(loaded.tasks());

        ui.showWelcome();

        // Anything wrong with the save file is said after the greeting rather
        // than before it, so the bot introduces itself first and the warning
        // reads as its own words rather than as a crash on startup.
        if (loaded.hasWarning()) {
            ui.show(loaded.warningLines());
        }

        // Keep reading commands until "bye" clears this flag, or until the
        // input runs out (e.g. Ctrl-D / piped input).
        boolean isChatting = true;
        while (isChatting && ui.hasNextCommand()) {
            String input = ui.readCommand();
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
                    ui.showGoodbye();
                    isChatting = false;
                }
                }
            } catch (PiplupBotException e) {
                // The bot explains the problem and carries on with the next line,
                // instead of letting the error stop the conversation.
                ui.showError(e);
            }
        }
    }
}
