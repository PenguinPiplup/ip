package piplupbot;

import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

// ACKNOWLEDGEMENTS: This Java file was written with the help of Claude.

/**
 * The bot's face in a window: the conversation so far, above a box to type in.
 *
 * <p>It plays two parts at once. As a JavaFX {@link Application}, it builds the
 * window and hands each line typed in the box to {@link PiplupBot#respondTo} --
 * the same method the console uses. As a {@link Ui}, it is where the bot's
 * replies go: {@link #show} adds each one to the conversation, just as
 * {@link TextUi#show} prints it in the console. So every command behaves the
 * same in both faces, and a new command appears here without any change to
 * this class.</p>
 *
 * <p>This program never creates a {@code GuiUi} itself. {@link PiplupBot#launch}
 * gives JavaFX the class, and JavaFX starts up, creates the object with the
 * no-argument constructor, and calls {@link #start}. Since nothing can be passed
 * to that constructor, the window makes its own bot, keeping its tasks in the
 * same file as the console -- which is what lets the two faces share one list.
 * It is also why the text area and the input box can be made along with the
 * object: by the time JavaFX makes it, JavaFX is already running.</p>
 *
 * <p>The window is deliberately plain: it is the smallest thing that shows the
 * two faces sharing one bot. Dialog boxes, pictures or FXML can replace the
 * plain text later without the bot noticing, because all of that stays inside
 * this class.</p>
 */
public class GuiUi extends Application implements Ui {
    /** Width of the window when it opens, in pixels. */
    private static final double WINDOW_WIDTH = 480;

    /** Height of the window when it opens, in pixels. */
    private static final double WINDOW_HEIGHT = 560;

    /** How long the goodbye stays on screen before the window closes. */
    private static final Duration GOODBYE_DELAY = Duration.seconds(1.5);

    /** The bot, keeping its tasks in the same file the console uses. */
    private final PiplupBot bot = new PiplupBot(PiplupBot.DEFAULT_FILE_PATH);

    /** Everything said so far, by the user and by the bot. */
    private final TextArea conversation = new TextArea();

    /** The box the user types each command into. */
    private final TextField inputBox = new TextField();

    /**
     * Lays out the window, greets the user, and shows the window.
     *
     * @param stage the window JavaFX has created for this application
     */
    @Override
    public void start(Stage stage) {
        conversation.setEditable(false);
        conversation.setWrapText(true);
        // The conversation takes whatever height the input box leaves over.
        VBox.setVgrow(conversation, Priority.ALWAYS);

        inputBox.setPromptText("Type a command, e.g. todo read book");
        // Pressing Enter in the box is what sends the line.
        inputBox.setOnAction(event -> handleInput(stage));

        // This window is the Ui, so the greeting arrives through show() below.
        bot.greet(this);

        stage.setTitle("PiplupBot");
        stage.setScene(new Scene(new VBox(conversation, inputBox), WINDOW_WIDTH, WINDOW_HEIGHT));
        stage.show();
        inputBox.requestFocus();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Here that means adding the reply to the conversation, one line under
     * another, without the console's dividers or indentation, which a window
     * has no need for.</p>
     */
    @Override
    public void show(String... lines) {
        conversation.appendText("PiplupBot: " + String.join("\n", lines) + "\n\n");
    }

    /**
     * Sends the line in the input box to the bot, which answers through
     * {@link #show}, then closes the window if the line ended the conversation.
     *
     * @param stage the window to close after {@code bye}
     */
    private void handleInput(Stage stage) {
        String input = inputBox.getText();
        inputBox.clear();
        if (input.isBlank()) {
            // Enter on an empty box sends nothing, as a blank line does in the console.
            return;
        }

        conversation.appendText("You: " + input.trim() + "\n\n");
        // Handing over this window as the Ui is what makes the reply appear in it.
        boolean isExit = bot.respondTo(input, this);

        if (isExit) {
            // Close after a pause rather than at once, so the goodbye can be read.
            inputBox.setDisable(true);
            PauseTransition pause = new PauseTransition(GOODBYE_DELAY);
            pause.setOnFinished(event -> stage.close());
            pause.play();
        }
    }
}
