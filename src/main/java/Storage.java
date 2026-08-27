import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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
 * that hard to forget. The {@link Task} classes hand over and receive their
 * fields as plain text and know nothing of separators or escaping, so the file
 * format lives in this one class alone.</p>
 *
 * <p>Dates are held in the file in ISO form, e.g. {@code 2019-10-15T18:00},
 * rather than the way the bot shows them. Being the form {@code LocalDateTime}
 * writes and reads by default, it survives the round trip exactly, and it keeps
 * the file independent of any later change to the wording on screen. A file
 * written by an earlier version, which stored whatever text the user typed, no
 * longer parses: those lines are skipped and the file copied aside, exactly as
 * for any other line this version cannot read.</p>
 *
 * <p>Because the file is ordinary text that anything on the machine can edit,
 * move or damage, reading it is written defensively: a file that is missing,
 * empty, unreadable or partly nonsense must still leave the bot usable, and must
 * never be thrown away without a copy being kept first.</p>
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
     * Where a file that could not be read in full is copied before the bot
     * overwrites it. Without this, one damaged line would be destroyed by the
     * very next command the user typed.
     */
    private static final Path DAMAGED_PATH =
            FILE_PATH.resolveSibling(FILE_PATH.getFileName() + ".damaged");

    /**
     * The character sequence that separates the fields of one saved task.
     * The spaces around the bar make the file readable by eye.
     */
    private static final String FIELD_SEPARATOR = " | ";

    /**
     * The characters that cannot appear in a field as themselves. A bar would be
     * read back as a separator, and the backslash is what marks an escaped
     * character, so it has to be escapable in its own right -- otherwise a
     * description ending in a backslash would swallow the separator after it.
     */
    private static final char ESCAPE = '\\';
    private static final char SEPARATOR_MARK = '|';

    /** What a load reports when nothing went wrong: nothing. */
    private static final String[] NO_WARNING = new String[0];

    /**
     * What a load produced: the tasks, and anything the user should be told
     * about the file they came from.
     *
     * <p>A record is used because this is what a record is for -- a value that
     * carries a few related fields and no behaviour of its own. It exists
     * because {@code load()} has two things to report, and being able to return
     * only the tasks is what let an earlier version lose a damaged line in
     * silence.</p>
     *
     * @param tasks        the tasks that were read, in the order the file held them
     * @param warningLines what to tell the user, or an empty array if all is well
     */
    public record LoadResult(ArrayList<Task> tasks, String[] warningLines) {
        /**
         * Reports whether anything went wrong while reading the file.
         *
         * @return {@code true} if there is something the user should be told
         */
        public boolean hasWarning() {
            return warningLines.length > 0;
        }
    }

    /** Prevents instances being created; every member of this class is static. */
    private Storage() {
    }

    /**
     * Writes the whole task list to the save file, replacing whatever was there.
     * The enclosing directory is created first if it does not exist yet, so a
     * fresh clone of the project needs no manual setup.
     *
     * @param tasks the tasks to write, in the order the user sees them
     * @throws PiplupBotException if the file could not be written, so that the
     *                            caller can tell the user their change is only
     *                            in this session
     */
    public static void save(ArrayList<Task> tasks) throws PiplupBotException {
        // Each task hands over its own fields: the list does not need to know
        // which kind of task it is holding, exactly as when the tasks are
        // printed. Turning those fields into a line is this class's job.
        List<String> lines = new ArrayList<>();
        for (Task task : tasks) {
            lines.add(encodeLine(task.toFileFields()));
        }

        try {
            // getParent() is the "data" directory. createDirectories() is happy
            // if it already exists, so there is no need to check first.
            Files.createDirectories(FILE_PATH.getParent());
            // The lines are joined with "\n" rather than left to
            // Files.write(Path, Iterable), which would use the platform's own
            // line ending. A file written on Windows and one written on macOS
            // are then byte for byte the same, which matters as soon as the file
            // is shared between machines or compared against a recorded copy.
            // An empty list writes an empty file, not a file holding one blank line.
            String text = lines.isEmpty() ? "" : String.join("\n", lines) + "\n";
            Files.writeString(FILE_PATH, text);
        } catch (IOException e) {
            throw new PiplupBotException(
                    "I could not save your tasks to " + displayPath(FILE_PATH) + " (" + e + ").",
                    "Your change is in this session only, and will be lost when I close.");
        }
    }

    /**
     * Reads the saved tasks back, so a run of the bot starts where the last one
     * left off.
     *
     * <p>Four things can go wrong, and none of them may stop the bot starting:
     * the file may be absent (a first run -- not an error at all), it may be
     * unreadable, or it may hold lines that cannot be parsed. In the last two
     * cases the bot is about to overwrite what it could not understand, so the
     * file is copied aside first and the user is told both what was lost and
     * where the copy is.</p>
     *
     * @return the tasks that were read, together with anything the user should
     *         be told about the file
     */
    public static LoadResult load() {
        ArrayList<Task> tasks = new ArrayList<>();

        // A file that is not there is what the very first run sees, and an empty
        // list is the right answer for it -- so this is not worth a word.
        if (!Files.exists(FILE_PATH)) {
            return new LoadResult(tasks, NO_WARNING);
        }

        List<String> lines;
        try {
            lines = Files.readAllLines(FILE_PATH);
        } catch (IOException e) {
            // The file is there but cannot be read at all: the wrong permissions,
            // a directory in its place, or bytes that are not text.
            return new LoadResult(tasks, new String[] {
                "I could not read " + displayPath(FILE_PATH) + " (" + e + ").",
                "I have started with an empty list, so your next command would overwrite it.",
                preserveDamagedFile()
            });
        }

        int skipped = 0;
        for (String line : lines) {
            try {
                tasks.add(parseTask(line));
            } catch (PiplupBotException e) {
                // Skip just this line: losing one task is better than refusing to
                // start, and the copy taken below is what makes it recoverable.
                skipped++;
            }
        }

        if (skipped == 0) {
            return new LoadResult(tasks, NO_WARNING);
        }
        return new LoadResult(tasks, new String[] {
            "I could not understand " + skipped + (skipped == 1 ? " line" : " lines")
                    + " in " + displayPath(FILE_PATH) + ", so I skipped "
                    + (skipped == 1 ? "it" : "them") + ".",
            "Those tasks are not in the list, and your next command would overwrite them.",
            preserveDamagedFile()
        });
    }

    /**
     * Copies the save file aside before the bot has a chance to overwrite it.
     * This is called only when the file could not be read in full, which is
     * exactly when saving over it would destroy something the user cannot get
     * back by any other means.
     *
     * @return a line telling the user where the copy is, or why there is none
     */
    private static String preserveDamagedFile() {
        // Files.copy() of a directory succeeds by quietly creating an empty
        // directory, so without this check the bot would promise a rescue copy
        // that holds nothing at all -- the very kind of false reassurance the
        // rest of this method exists to avoid. Only a real file can be rescued.
        if (!Files.isRegularFile(FILE_PATH)) {
            return "There is nothing there for me to copy: " + displayPath(FILE_PATH)
                    + " is not a file.";
        }

        try {
            Files.copy(FILE_PATH, DAMAGED_PATH, StandardCopyOption.REPLACE_EXISTING);
            return "I have kept the file as it was in " + displayPath(DAMAGED_PATH) + ".";
        } catch (IOException e) {
            // Even the copy failed. Say so plainly rather than implying a safety
            // net that is not there.
            return "I could not keep a copy of it (" + e + "), so please back it up yourself.";
        }
    }

    /**
     * Returns a path written the same way on every operating system.
     * {@code Path.toString()} uses backslashes on Windows and forward slashes
     * elsewhere, so quoting it directly would make the bot's wording -- and every
     * test case that records that wording -- differ from machine to machine.
     *
     * @param path the path to describe
     * @return the path with forward slashes, e.g. {@code ./data/piplupbot.txt}
     */
    private static String displayPath(Path path) {
        return "./" + path.toString().replace('\\', '/');
    }

    /**
     * Joins one task's fields into the line that represents it in the file,
     * e.g. {@code T | 1 | read book}.
     *
     * <p>This is deliberately a different rendering from {@link Task#toString()}:
     * the screen format is written for a person to read, while this one is
     * written to be read back by the program, so it keeps the fields separate
     * instead of dressing them up with brackets and words. Were the two ever
     * merged, a change to the wording on screen would silently invalidate every
     * saved file.</p>
     *
     * @param fields the task's fields, in the order they are written
     * @return the line to write to the file
     */
    private static String encodeLine(String[] fields) {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                line.append(FIELD_SEPARATOR);
            }
            line.append(encodeField(fields[i]));
        }
        return line.toString();
    }

    /**
     * Protects the characters that would otherwise change how a line is read.
     * A description such as {@code buy milk | eggs} would be split into an extra
     * field when read back, and the task dropped as damaged -- so a bar is
     * written as {@code \|}, and a real backslash as {@code \\}.
     *
     * <p>Because every bar inside a field is written after a backslash, the three
     * characters {@code " | "} can no longer occur except between two fields.
     * That is what lets {@link #parseTask} go on splitting a line on the plain
     * separator instead of needing a character-by-character parser of its own.</p>
     *
     * @param field one field's text, exactly as the user typed it
     * @return the text with its escapes added
     */
    private static String encodeField(String field) {
        StringBuilder encoded = new StringBuilder();
        for (int i = 0; i < field.length(); i++) {
            char character = field.charAt(i);
            if (character == ESCAPE || character == SEPARATOR_MARK) {
                encoded.append(ESCAPE);
            }
            encoded.append(character);
        }
        return encoded.toString();
    }

    /**
     * Undoes {@link #encodeField}, turning {@code \|} back into {@code |}.
     * A backslash followed by anything else, or one at the very end of a field,
     * was never written by this program, so the line is rejected rather than
     * guessed at.
     *
     * @param field one field as it appears in the file
     * @return the text the user originally typed
     * @throws PiplupBotException if the field holds an escape this program would
     *                            never have written
     */
    private static String decodeField(String field) throws PiplupBotException {
        StringBuilder decoded = new StringBuilder();
        for (int i = 0; i < field.length(); i++) {
            char character = field.charAt(i);
            if (character != ESCAPE) {
                decoded.append(character);
                continue;
            }

            // The backslash escapes whatever follows it, so step past it and take
            // the next character literally -- if there is one, and if it is a
            // character this program would ever have escaped.
            i++;
            if (i >= field.length()) {
                throw new PiplupBotException("Field ends in a stray escape: " + field);
            }
            char escaped = field.charAt(i);
            if (escaped != ESCAPE && escaped != SEPARATOR_MARK) {
                throw new PiplupBotException("Unknown escape in field: " + field);
            }
            decoded.append(escaped);
        }
        return decoded.toString();
    }

    /**
     * Turns one line of the save file back into the task it was written from.
     * This is the exact reverse of {@link #encodeLine}: the fields are read in
     * the order that method writes them, and the type code chooses which kind of
     * task to rebuild -- the one place in the program that has to decide a task's
     * kind from data rather than letting the object answer for itself.
     *
     * <p>Every check here rejects a line the bot could not itself have written.
     * They matter because the file is ordinary text that anything can edit:
     * without them a hand-edited line could produce a task with no description,
     * or a deadline with no date, which no command would ever let a user create.
     * The dates themselves are checked by the {@link Deadline} and {@link Event}
     * constructors, which reject a date they cannot read with the same
     * {@link PiplupBotException} the checks here throw -- so a damaged date is
     * skipped along with every other kind of damaged line.</p>
     *
     * @param line one line of the save file
     * @return the task the line describes
     * @throws PiplupBotException if the line is not in the saved format
     */
    private static Task parseTask(String line) throws PiplupBotException {
        // split() takes a regular expression, and "|" means "or" in one, so the
        // separator must be quoted to be matched literally -- unquoted, " | "
        // would match any single space. The -1 keeps empty fields at the end, so
        // a line stopping after a separator is rejected below instead of quietly
        // losing its last field.
        String[] fields = line.split(Pattern.quote(FIELD_SEPARATOR), -1);
        if (fields.length < 3) {
            throw new PiplupBotException("Not enough fields: " + line);
        }

        String typeCode = fields[0];
        String doneFlag = fields[1];
        String description = requireText(decodeField(fields[2]), "description", line);

        // Each kind of task writes a known number of fields, so the count is
        // checked before reading a field that a shorter line would not have.
        Task task = switch (typeCode) {
        case "T" -> {
            requireFieldCount(fields, 3, line);
            yield new Todo(description);
        }
        case "D" -> {
            requireFieldCount(fields, 4, line);
            yield new Deadline(description, requireText(decodeField(fields[3]), "date", line));
        }
        case "E" -> {
            requireFieldCount(fields, 5, line);
            yield new Event(description,
                    requireText(decodeField(fields[3]), "start time", line),
                    requireText(decodeField(fields[4]), "end time", line));
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
     * Checks that a field the user must have typed something into is not empty.
     * {@code isBlank()} rather than {@code isEmpty()}, because a field of spaces
     * is just as impossible to have typed: every command trims its argument
     * before the task is created.
     *
     * @param text the decoded field
     * @param name what the field is, for the error message
     * @param line the whole line, for the error message
     * @return the same text, when it has something in it
     * @throws PiplupBotException if the field is empty or holds only spaces
     */
    private static String requireText(String text, String name, String line)
            throws PiplupBotException {
        if (text.isBlank()) {
            throw new PiplupBotException("Empty " + name + " in: " + line);
        }
        return text;
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
