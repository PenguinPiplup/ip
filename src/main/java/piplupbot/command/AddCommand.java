package piplupbot.command;

import piplupbot.Parser;
import piplupbot.Storage;
import piplupbot.Ui;
import piplupbot.task.Deadline;
import piplupbot.task.Event;
import piplupbot.task.Task;
import piplupbot.task.TaskList;
import piplupbot.task.Todo;

/**
 * ACKNOWLEDGEMENTS: This Java file was written with the help of Claude.
 */

/**
 * Stores a task and confirms it to the user.
 *
 * <p>One class covers {@code todo}, {@code deadline} and {@code event} because
 * by the time a command exists the differences between them are spent: the
 * parser has already worked out which kind was typed and built the matching
 * {@link Todo}, {@link Deadline} or {@link Event}. All that is left is to add it
 * and say so, which is identical for all three -- even the confirmation, since
 * the task prints itself through its own {@code toString()}.</p>
 */
public class AddCommand extends Command {
    /** The task to store, already built and checked by {@link Parser}. */
    private final Task task;

    /**
     * Creates a command that will store the given task.
     *
     * @param task the task to remember
     */
    public AddCommand(Task task) {
        this.task = task;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The confirmation shows the task through its own {@code toString()}, so
     * the same three lines serve a todo, a deadline and an event alike -- by the
     * time this runs, which kind was typed has already been settled by
     * {@link Parser}.</p>
     */
    @Override
    public void execute(TaskList tasks, Ui ui, Storage storage) {
        tasks.add(task);
        ui.show("Got it. I've added this task:",
                "  " + task,
                "Now you have " + tasks.size() + " tasks in the list.");
        save(tasks, ui, storage);
    }
}
