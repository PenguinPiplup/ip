import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * ACKNOWLEDGEMENTS: This Java file was written with the help of Claude.
 */

/**
 * Keeps the task list on the hard disk, so the tasks outlive one run of the bot.
 *
 * <p>The whole list is rewritten every time it changes. Appending only the new
 * task would be faster, but it could not express a {@code mark} or a
 * {@code delete}, both of which alter a line that is already in the file.
 * Rewriting is therefore the simplest approach that is correct for every kind of
 * change, and with a handful of tasks the cost is not worth optimising away.</p>
 *
 * <p>Everything here is {@code static} because there is only ever one file, at
 * one hard-coded path. If the path ever has to vary -- a different file per user,
 * say, or a temporary file in a test -- this becomes a normal class whose
 * constructor takes the path.</p>
 *
 * <p>{@link #save} and {@link #load} are two halves of one agreement about what
 * a line in the file looks like, so they are kept in the same class: a change to
 * the format has to be made in both, and having them side by side is what makes
 * that hard to forget.</p>
 */
public class Storage {
    /**
     * Where the tasks are kept.
     *
     * <p>The path is relative, so it is resolved against whatever directory the
     * bot is started in -- normally the project root, giving
     * {@code ./data/piplupbot.txt}. The text-UI tests rely on this: they start
     * the program in a scratch directory of their own, so the file they write is
     * not the one holding real tasks, and no test-only setting is needed here to
     * arrange it.</p>
     */
    private static final Path FILE_PATH = Path.of("data", "piplupbot.txt");

    /**
     * The character sequence that separates the fields of one saved task.
     * Spaces around the bar make the file readable, and are stripped again when
     * the file is read back.
     */
    private static final String FIELD_SEPARATOR = " | ";

    /** Prevents instances being created; every member of this class is static. */
    private Storage() {
    }

    /**
     * Returns the separator that goes between two fields of a saved task.
     * The {@link Task} classes build their own lines, so they ask for the
     * separator here rather than each spelling out {@code " | "} for themselves.
     *
     * @return the field separator used in the save file
     */
    public static String getFieldSeparator() {
        return FIELD_SEPARATOR;
    }

    /**
     * Writes the whole task list to the save file, replacing whatever was there.
     * The enclosing directory is created first if it does not exist yet, so a
     * fresh clone of the project needs no manual setup.
     *
     * @param tasks the tasks to write, in the order the user sees them
     */
    public static void save(ArrayList<Task> tasks) {
        // Each task renders its own line: the list does not need to know which
        // kind of task it is holding, exactly as when the tasks are printed.
        List<String> lines = new ArrayList<>();
        for (Task task : tasks) {
            lines.add(task.toFileFormat());
        }

        try {
            // getParent() is the "data" directory. createDirectories() is happy
            // if it already exists, so there is no need to check first.
            Files.createDirectories(FILE_PATH.getParent());
            Files.write(FILE_PATH, lines);
        } catch (IOException e) {
            // Saving is a side errand, not the command the user asked for, so a
            // failure is reported and the conversation carries on. It goes to
            // the error stream to keep it out of the bot's own replies.
            System.err.println("Warning: I could not save your tasks (" + e.getMessage() + ").");
        }
    }

    /**
     * Reads the saved tasks back, so a run of the bot starts where the last one
     * left off.
     *
     * <p>A file that is not there yet is not an error: it is what the very first
     * run sees, and an empty list is the right answer for it. Neither is a line
     * the parser cannot understand -- it is skipped and the rest of the file is
     * still loaded, because losing one task is better than refusing to start.
     * Nothing is said about a skipped line, which is a real shortcoming: the
     * next change to the list rewrites the file, and the skipped line is gone
     * for good. Reporting it is worth doing, and is left as a later step.</p>
     *
     * @return the saved tasks in the order they were written, or an empty list
     *         if there is nothing to read
     */
    public static ArrayList<Task> load() {
        ArrayList<Task> tasks = new ArrayList<>();

        if (!Files.exists(FILE_PATH)) {
            return tasks;
        }

        try {
            for (String line : Files.readAllLines(FILE_PATH)) {
                try {
                    tasks.add(parseTask(line));
                } catch (PiplupBotException e) {
                    // Skip just this line. See the note above: the rest of the
                    // file is still worth loading.
                }
            }
        } catch (IOException e) {
            // The file exists but could not be read at all. Starting with an
            // empty list is the only thing left to do, so say so and carry on.
            System.err.println("Warning: I could not read your saved tasks (" + e.getMessage() + ").");
        }

        return tasks;
    }

    /**
     * Turns one line of the save file back into the task it was written from.
     * This is the exact reverse of {@link Task#toFileFormat()}: the fields are
     * read in the order that method writes them, and the type code chooses which
     * kind of task to rebuild -- the one place in the program that has to decide
     * a task's kind from data rather than letting the object answer for itself.
     *
     * @param line one line of the save file
     * @return the task the line describes
     * @throws PiplupBotException if the line is not in the saved format
     */
    private static Task parseTask(String line) throws PiplupBotException {
        // split() takes a regular expression, and "|" means "or" in one, so the
        // separator must be quoted to be matched literally -- unquoted, " | "
        // would match any single space. The -1 keeps empty fields at the end,
        // so a line stopping after a separator is rejected below instead of
        // quietly losing its last field.
        String[] fields = line.split(Pattern.quote(FIELD_SEPARATOR), -1);
        if (fields.length < 3) {
            throw new PiplupBotException("Not enough fields: " + line);
        }

        String typeCode = fields[0];
        String doneFlag = fields[1];
        String description = fields[2];
        if (description.isEmpty()) {
            throw new PiplupBotException("Empty description: " + line);
        }

        // Each kind of task writes a known number of fields, so the count is
        // checked here rather than reading past the end of a short line.
        Task task = switch (typeCode) {
        case "T" -> {
            requireFieldCount(fields, 3, line);
            yield new Todo(description);
        }
        case "D" -> {
            requireFieldCount(fields, 4, line);
            yield new Deadline(description, fields[3]);
        }
        case "E" -> {
            requireFieldCount(fields, 5, line);
            yield new Event(description, fields[3], fields[4]);
        }
        default -> throw new PiplupBotException("Unknown task type: " + line);
        };

        // A new task starts off not done, so only "1" needs acting on -- but
        // anything other than the two flags the file is meant to hold means the
        // line was not written by this program, so it is rejected.
        if (doneFlag.equals("1")) {
            task.markAsDone();
        } else if (!doneFlag.equals("0")) {
            throw new PiplupBotException("Unknown done status: " + line);
        }

        return task;
    }

    /**
     * Checks that a saved line has exactly the number of fields its type needs.
     *
     * @param fields   the fields the line was split into
     * @param expected how many fields this kind of task is written with
     * @param line     the whole line, for the error message
     * @throws PiplupBotException if the line has any other number of fields
     */
    private static void requireFieldCount(String[] fields, int expected, String line)
            throws PiplupBotException {
        if (fields.length != expected) {
            throw new PiplupBotException(
                    "Expected " + expected + " fields but found " + fields.length + ": " + line);
        }
    }
}
