package piplupbot.command;

import piplupbot.Storage;
import piplupbot.Ui;
import piplupbot.task.TaskList;

// ACKNOWLEDGEMENTS: This Java file was written with the help of Claude.

/**
 * Displays the stored tasks, numbered starting from 1, with their done status.
 *
 * <p>All this command decides is what to call the list: the numbering is
 * {@link TaskList}'s and the layout is {@link Ui}'s. It changes nothing, so it
 * has nothing to save.</p>
 */
public class ListCommand extends Command {
    @Override
    public void execute(TaskList tasks, Ui ui, Storage storage) {
        ui.showList("Here are the tasks in your list:", tasks.toNumberedLines());
    }
}
