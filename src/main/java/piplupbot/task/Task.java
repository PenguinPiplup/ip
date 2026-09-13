package piplupbot.task;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import piplupbot.Storage;

// ACKNOWLEDGEMENTS: This Java file was written with the help of Claude.

/**
 * A single task in the user's list: what the task is, and whether it is done.
 * Keeping the two together means they can no longer fall out of step,
 * which was a risk while they lived in two separate arrays.
 *
 * <p>This is also the base class for the three kinds of task the bot supports:
 * {@link Todo}, {@link Deadline} and {@link Event}. Everything they have in
 * common lives here, so the bot can hold them all in one list and call
 * {@code toString()} without knowing which kind it is holding.</p>
 *
 * <p>It is {@code abstract} because "a task" on its own is not something the
 * user can add: every task the bot stores is a todo, a deadline or an event.
 * Declaring it so lets the compiler say the same thing, and lets this class
 * call {@link #getTypeLabel()} knowing some subclass must have answered it.</p>
 */
public abstract class Task {
    /**
     * What the user typed, e.g. {@code read book}.
     *
     * <p>It is {@code private}, not {@code protected}: no subclass reads it.
     * {@link Todo}, {@link Deadline} and {@link Event} hand their description to
     * this class's constructor and then leave it alone, so opening the field up
     * to them would buy nothing and cost the guarantee below.</p>
     *
     * <p>It is {@code final} because the constructor is the one place that checks
     * a description is not blank. A field a subclass could reassign afterwards
     * would make that check a hope rather than a promise -- and a blank
     * description is one this bot would save happily and then refuse to load
     * back, losing the task.</p>
     */
    private final String description;

    /**
     * Whether the task has been completed.
     * {@code private} for the same reason as the description, but not
     * {@code final}: {@link #markAsDone} and {@link #markAsNotDone} are the two
     * ways it is meant to change.
     */
    private boolean isDone;

    /**
     * Creates a task that is not done yet.
     *
     * @param description what the task is
     */
    public Task(String description) {
        assert description != null && !description.isBlank()
                : "A task was created with no description";
        this.description = description;
        this.isDone = false;
    }

    /**
     * Reports whether this task's description contains the given text, ignoring
     * the difference between capital and small letters.
     *
     * <p>The question is asked of the task rather than answered by a caller
     * reading a description out of it, so what counts as a match is decided in
     * one place, beside the field it is decided about. That matters because the
     * rule is not the obvious one: the search is deliberately more forgiving
     * than the rest of the bot, matching part of a word and ignoring capitals,
     * so that {@code find boo} and {@code find BOOK} both find "read book".
     * Someone hunting for a task they half remember should not have to type it
     * exactly.</p>
     *
     * <p>Only the description is searched, never the dates a deadline or an
     * event adds, so {@code find oct} does not match a task merely due in
     * October. Searching the whole rendered line would be one character's
     * change here and would quietly make every date, type label and status box
     * searchable too.</p>
     *
     * <p>{@code Locale.ROOT} rather than the machine's own language, for the
     * reason {@link DateTimes} pins {@code Locale.ENGLISH}: a Turkish machine
     * lower-cases "I" to a dotless letter, which would make a keyword stop
     * matching a description that plainly contains it.</p>
     *
     * @param text what to look for, as the user typed it
     * @return {@code true} if the description contains it
     */
    public boolean descriptionContains(String text) {
        return description.toLowerCase(Locale.ROOT).contains(text.toLowerCase(Locale.ROOT));
    }

    /**
     * Returns the character shown inside the status box in the task list.
     *
     * @return {@code "X"} if the task is done, or a single space if it is not
     */
    public String getStatusIcon() {
        return (isDone ? "X" : " "); // mark done task with X
    }

    /** Records that the task has been completed. */
    public void markAsDone() {
        this.isDone = true;
    }

    /** Records that the task has not been completed after all. */
    public void markAsNotDone() {
        this.isDone = false;
    }

    /**
     * Returns which kind of task this is.
     * Each subclass answers for itself, which is why this class does not need
     * to store the kind or ask what it is: calling this method on a
     * {@code Task} reaches the right subclass's answer on its own.
     *
     * <p>The kind is the one piece of type information a subclass has to supply,
     * because both the label shown on screen and the code written to the save
     * file are built from it. Keeping one source for the two means they cannot
     * drift apart.</p>
     *
     * @return the kind of task, e.g. {@link TaskType#TODO}
     */
    protected abstract TaskType getType();

    /**
     * Returns the label that says which kind of task this is, as the task list
     * displays it.
     *
     * @return the type code in square brackets, e.g. {@code "[T]"}
     */
    protected String getTypeLabel() {
        return "[" + getType().getCode() + "]";
    }

    /**
     * Returns the fields that describe this task in the save file.
     *
     * <p>The task hands over its parts as plain text and leaves {@link Storage}
     * to join them into a line. That division matters: a description may contain
     * any character at all, including the one the file uses to separate fields,
     * and protecting it is the file format's problem rather than the task's. A
     * task that built its own line would have to know about separators and
     * escaping, and every subclass would have to get that right again.</p>
     *
     * <p>The done status is given as {@code 1} or {@code 0} rather than
     * {@code true}/{@code false}, following the format in the requirements.</p>
     *
     * @return the type code, the done status and the description
     */
    public String[] toFileFields() {
        return withExtraFields();
    }

    /**
     * Builds the field list every task begins with, followed by whatever the
     * subclass adds. Having the shared three in one place means a subclass
     * cannot accidentally write them in a different order.
     *
     * @param extras the subclass's own fields, in the order they are written
     * @return the shared fields followed by {@code extras}
     */
    protected String[] withExtraFields(String... extras) {
        // The parts are added in order rather than assigned to numbered slots,
        // so this method says which shared fields there are and which comes
        // first without also having to state how many -- a count only the
        // reading end needs, and which Storage now keeps on its own.
        List<String> fields = new ArrayList<>();
        fields.add(getType().getCode());
        fields.add(isDone ? "1" : "0");
        fields.add(description);
        fields.addAll(List.of(extras));
        return fields.toArray(new String[0]);
    }

    /**
     * Renders the task the way the task list displays it, e.g. {@code [T][X] read book}.
     * Java calls this automatically whenever a Task is used where text is expected,
     * such as in string concatenation.
     *
     * <p>Deciding the order -- label, status box, description -- here rather
     * than in each subclass means the three kinds of task cannot drift into
     * three different shapes; a subclass supplies only its own label, plus
     * anything peculiar to it such as a deadline's due date.</p>
     *
     * @return the type label, the status box and the description
     */
    @Override
    public String toString() {
        return getTypeLabel() + "[" + getStatusIcon() + "] " + description;
    }
}
