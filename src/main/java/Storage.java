import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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
 * <p>This class only <em>writes</em> at present; reading the file back on
 * startup is the next step.</p>
 */
public class Storage {
    /**
     * Where the tasks are kept, relative to the directory the bot is run from
     * (the project root).
     */
    private static final Path FILE_PATH = Path.of("data", "duke.txt");

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
}
