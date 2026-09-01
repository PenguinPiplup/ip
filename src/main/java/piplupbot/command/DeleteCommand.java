package piplupbot.command;

import piplupbot.PiplupBotException;
import piplupbot.Storage;
import piplupbot.Ui;
import piplupbot.task.Task;
import piplupbot.task.TaskList;

/**
 * ACKNOWLEDGEMENTS: This Java file was written with the help of Claude.
 */

/**
 * Removes a task from the list and confirms it to the user.
 *
 * <p>{@link TaskList#remove} does the removing and hands back the task it
 * removed, which is what the confirmation shows; this command's own job is only
 * to word that confirmation and have the change saved.</p>
 */
public class DeleteCommand extends Command {
    /** The task's position as shown by {@code list}, counting from 1. */
    private final int taskNumber;

    /**
     * Creates a command that will remove the task at the given position.
     *
     * @param taskNumber the task's position as shown by {@code list}, counting from 1
     */
    public DeleteCommand(int taskNumber) {
        this.taskNumber = taskNumber;
    }

    @Override
    public void execute(TaskList tasks, Ui ui, Storage storage) throws PiplupBotException {
        Task removedTask = tasks.remove(taskNumber);
        ui.show("Noted. I've removed this task:",
                "  " + removedTask,
                "Now you have " + tasks.size() + " tasks in the list.");
        save(tasks, ui, storage);
    }
}
