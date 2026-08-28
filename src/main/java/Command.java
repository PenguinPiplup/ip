/**
 * ACKNOWLEDGEMENTS: This Java file was written with the help of Claude.
 */

/**
 * One thing the user asked the bot to do, ready to be carried out.
 *
 * <p>A command is created by {@link Parser#parse} once the line has been read
 * and found to make sense, and carried out later by {@link #execute}. Splitting
 * those two moments apart is the point of this class: reading a line and acting
 * on it used to happen in one step, in a {@code switch} that had to name every
 * command in the same place. Now each command is an object that carries whatever
 * it needs -- the task to add, the number to delete -- and knows how to do its
 * own work.</p>
 *
 * <p>The gain is that adding a command becomes an addition rather than an edit.
 * A new command is a new subclass plus one line in {@link Parser#parse};
 * {@link PiplupBot#run} does not change at all, because it only ever asks a
 * command to execute itself and does not care which kind it got. That is
 * polymorphism doing the dispatching the {@code switch} used to do by hand.</p>
 *
 * <p>This is an abstract class rather than an interface because it carries
 * shared behaviour as well as a shape: {@link #isExit()} answers {@code false}
 * for every command but one, and {@link #save} is the same three lines in every
 * command that changes the list. Subclasses inherit both instead of repeating
 * them.</p>
 */
public abstract class Command {
    /**
     * Carries out this command.
     *
     * <p>Every command is handed the same three things, whether or not it uses
     * them, so that {@link PiplupBot#run} can call this method without knowing
     * which command it holds. {@link ListCommand}, for instance, never touches
     * the storage. The alternative -- a different set of parameters per command
     * -- would put the {@code switch} back, since the caller would have to know
     * which command it had in order to know what to pass it.</p>
     *
     * @param tasks   the task list to read or change
     * @param ui      what the command says to the user about what it did
     * @param storage where the list is kept between runs
     * @throws PiplupBotException if the command cannot be carried out, e.g. it
     *                            names a task number that does not exist
     */
    public abstract void execute(TaskList tasks, Ui ui, Storage storage) throws PiplupBotException;

    /**
     * Reports whether the conversation should end once this command has run.
     *
     * <p>Answering {@code false} here means only {@link ExitCommand} has to say
     * anything on the subject, and a newly written command ends the conversation
     * only if it deliberately says so.</p>
     *
     * @return {@code true} if the bot should stop reading commands
     */
    public boolean isExit() {
        return false;
    }

    /**
     * Writes the list to disk, telling the user if it could not be written.
     *
     * <p>Saving is a side errand rather than the command the user asked for, so
     * a failure must not swallow the confirmation or end the conversation. It is
     * reported after the confirmation instead, because the confirmation is true
     * as far as this session goes -- the task really was added -- and the warning
     * is what qualifies it.</p>
     *
     * <p>It lives here rather than in each subclass because the three commands
     * that change the list would otherwise hold three copies of it. It is
     * {@code protected} so that only commands can call it: saving is part of
     * carrying out a command, not something the rest of the program should be
     * able to ask for.</p>
     *
     * @param tasks   the list to write
     * @param ui      how a failure is reported
     * @param storage where the list is written to
     */
    protected void save(TaskList tasks, Ui ui, Storage storage) {
        try {
            storage.save(tasks.asList());
        } catch (PiplupBotException e) {
            ui.showError(e);
        }
    }
}
