import java.nio.file.Path;

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
 * should do and what to say about it. Making sense of the line the user typed
 * is {@link Parser}'s work, holding the tasks is {@link TaskList}'s, and saying
 * things out loud -- and hearing what the user typed -- is {@link Ui}'s. So
 * nothing here needs to know how a command is spelled, how a list is indexed,
 * or how a reply is laid out on screen.</p>
 */
public class PiplupBot {
    /** Where the tasks are kept between runs. */
    private final Storage storage;

    /** The tasks the user has stored, as read from {@link #storage} at startup. */
    private final TaskList tasks;

    /**
     * Everything the bot says and hears. The bot keeps one of these for the
     * whole run, and asks it to do all the printing and reading, so no method
     * here touches {@code System.out} or {@code System.in} itself.
     */
    private final Ui ui;

    /**
     * What to tell the user about the save file, or an empty array if it was
     * read without trouble.
     *
     * <p>The file is read in the constructor but the warning cannot be shown
     * there, because the bot should introduce itself before it starts
     * complaining. Keeping the lines here is what lets the reading happen at
     * the earliest moment and the telling at the right one.</p>
     */
    private final String[] loadWarningLines;

    /**
     * Creates a bot whose tasks are kept in the given file, reading back
     * whatever the last run left there.
     *
     * <p>Reading in the constructor means that by the time the object exists it
     * is already usable, so no method has to wonder whether the list has been
     * filled in yet. It is safe to do here because {@link Storage#load} does not
     * throw: a file that is missing, unreadable or damaged still yields a list
     * -- an empty one if need be -- along with something to tell the user.</p>
     *
     * @param filePath where to keep the tasks, e.g.
     *                 {@code Path.of("data", "piplupbot.txt")}
     */
    public PiplupBot(Path filePath) {
        this.ui = new Ui();
        this.storage = new Storage(filePath);

        Storage.LoadResult loaded = storage.load();
        // The loaded tasks are handed to TaskList's constructor, so the list
        // starts out holding exactly what the file held.
        this.tasks = new TaskList(loaded.tasks());
        this.loadWarningLines = loaded.warningLines();
    }

    /**
     * Stores a task and confirms it to the user.
     * It takes a {@code Task} rather than a description, so the same method
     * works for every kind of task without needing to know which one it got.
     *
     * @param task the task to remember
     */
    private void addTask(Task task) {
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
    private void saveTasks() {
        try {
            storage.save(tasks.asList());
        } catch (PiplupBotException e) {
            ui.showError(e);
        }
    }

    /**
     * Displays the stored tasks, numbered starting from 1, with their done status.
     * All this method decides is what to call the list: the numbering is
     * {@link TaskList}'s and the layout is {@link Ui}'s.
     */
    private void listTasks() {
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
    private void deleteTask(int taskNumber) throws PiplupBotException {
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
    private void setTaskStatus(int taskNumber, boolean isTaskDone) throws PiplupBotException {
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
     * Holds the conversation, from the greeting to {@code bye}.
     * Everything it needs was settled by the constructor, so this method is the
     * conversation itself and nothing else.
     */
    public void run() {
        ui.showWelcome();

        // Anything wrong with the save file is said after the greeting rather
        // than before it, so the bot introduces itself first and the warning
        // reads as its own words rather than as a crash on startup.
        if (loadWarningLines.length > 0) {
            ui.show(loadWarningLines);
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
                case TODO -> addTask(Parser.parseTodo(input));
                case DEADLINE -> addTask(Parser.parseDeadline(input));
                case EVENT -> addTask(Parser.parseEvent(input));
                case LIST -> listTasks();
                case MARK -> setTaskStatus(Parser.parseTaskNumber(input, command), true);
                case UNMARK -> setTaskStatus(Parser.parseTaskNumber(input, command), false);
                case DELETE -> deleteTask(Parser.parseTaskNumber(input, command));
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

    /**
     * Starts the bot with its usual save file.
     *
     * <p>The path is relative, so it is resolved against whatever directory the
     * bot is started in -- normally the project root, giving
     * {@code ./data/piplupbot.txt}. The text-UI tests rely on this: they start
     * the program in a scratch directory of their own, so the file they write
     * is not the one holding real tasks, and no test-only setting is needed to
     * arrange it.</p>
     *
     * <p>The parts of the path are passed separately rather than as one string,
     * so the separator between them is never written down here: {@code Path.of}
     * joins them with whatever the machine uses, a backslash on Windows and a
     * forward slash elsewhere.</p>
     *
     * @param args ignored; the bot takes its instructions from the conversation
     */
    public static void main(String[] args) {
        new PiplupBot(Path.of("data", "piplupbot.txt")).run();
    }
}
