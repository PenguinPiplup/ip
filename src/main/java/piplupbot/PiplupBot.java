package piplupbot;

import java.nio.file.Path;

import piplupbot.command.Command;
import piplupbot.task.TaskList;

// ACKNOWLEDGEMENTS: This Java file was written with the help of Claude.

/**
 * A simple command line chatbot.
 * It greets the user, stores three kinds of task -- {@code todo},
 * {@code deadline} and {@code event} -- lists the stored tasks on the
 * {@code list} command, marks a task as done on the {@code mark <number>}
 * command, reverses that on the {@code unmark <number>} command,
 * removes a task on the {@code delete <number>} command,
 * shows the tasks whose description contains some text on the
 * {@code find <keyword>} command,
 * and exits when the user types {@code bye}.
 *
 * <p>Every change to the list is written straight to the hard disk by
 * {@link Storage}, and read back when the program starts, so the tasks survive
 * the program being closed.</p>
 *
 * <p>This class is the conversation and nothing else. It hands each line to
 * {@link Parser}, asks the {@link Command} that comes back to carry itself out,
 * and stops when one of them says the conversation is over. What a command
 * <em>does</em> is that command's own business, holding the tasks is
 * {@link TaskList}'s, and saying things out loud -- and hearing what the user
 * typed -- is {@link Ui}'s.</p>
 *
 * <p>So nothing here knows how a command is spelled, what any of them do, how a
 * list is indexed, or how a reply is laid out on screen. The measure of that is
 * that this file no longer names a single command: teaching the bot a new one
 * leaves it untouched.</p>
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

        // Keep reading commands until one of them says the conversation is over,
        // or until the input runs out (e.g. Ctrl-D / piped input).
        boolean isExit = false;
        while (!isExit && ui.hasNextCommand()) {
            String input = ui.readCommand();
            if (input.isEmpty()) {
                // A blank line names no command, so there is nothing to report.
                continue;
            }

            // One try/catch for the whole conversation: the parser and the
            // commands decide *what* went wrong and throw, while this block is
            // the single place that decides *how* the problem is shown. A new
            // command therefore gets its error reporting for free.
            try {
                // Reading the line and acting on it are two separate steps, and
                // this method takes no part in either. Parser.parse() turns the
                // line into a command -- or refuses it -- and the command carries
                // itself out, so nothing here names a single one of them.
                Command command = Parser.parse(input);
                command.execute(tasks, ui, storage);

                // Only the command knows whether it was the last one, so it is
                // asked rather than recognised: this loop never mentions "bye".
                isExit = command.isExit();
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
