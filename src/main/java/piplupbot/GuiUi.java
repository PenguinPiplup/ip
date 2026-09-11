package piplupbot;

import java.net.URL;

import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

// ACKNOWLEDGEMENTS: This Java file was written with the help of Claude.

/**
 * The bot's face in a window: the conversation so far, as a column of speech
 * bubbles, above a box to type in and a button to send what is typed.
 *
 * <p>It plays two parts at once. As a JavaFX {@link Application}, it builds the
 * window and hands each line typed in the box to {@link PiplupBot#respondTo} --
 * the same method the console uses. As a {@link Ui}, it is where the bot's
 * replies go: {@link #show} adds each one to the conversation as a
 * {@link DialogBox}, just as {@link TextUi#show} prints it in the console. So
 * every command behaves the same in both faces, and a new command appears here
 * without any change to this class.</p>
 *
 * <p>This program never creates a {@code GuiUi} itself. {@link Launcher}
 * gives JavaFX the class, and JavaFX starts up, creates the object with the
 * no-argument constructor, and calls {@link #start}. Since nothing can be passed
 * to that constructor, the window makes its own bot, keeping its tasks in the
 * same file as the console -- which is what lets the two faces share one list.
 * It is also why the controls and pictures can be made along with the object:
 * by the time JavaFX makes it, JavaFX is already running.</p>
 *
 * <p>The window is a {@link BorderPane}, which has a region for each edge and
 * one for the center: the conversation fills the center, and the input bar sits
 * at the bottom. The regions left empty are free for later -- a list of tasks
 * on the right, say. How the parts look is kept out of the Java code, in the
 * stylesheet {@code main.css}, so the colours can change without this class
 * changing.</p>
 */
public class GuiUi extends Application implements Ui {
    /** Width of the window when it opens, in pixels. */
    private static final double WINDOW_WIDTH = 480;

    /** Height of the window when it opens, in pixels. */
    private static final double WINDOW_HEIGHT = 560;

    /** How long the goodbye stays on screen before the window closes. */
    private static final Duration GOODBYE_DELAY = Duration.seconds(1.5);

    /** Where the window's stylesheet is, among the program's own files. */
    private static final String STYLESHEET_PATH = "/css/main.css";

    /** Where the picture shown beside the user's messages is. */
    private static final String USER_IMAGE_PATH = "/images/user.png";

    /** Where the picture shown beside the bot's replies is. */
    private static final String BOT_IMAGE_PATH = "/images/piplupbot.png";

    /** The bot, keeping its tasks in the same file the console uses. */
    private final PiplupBot bot = new PiplupBot(PiplupBot.DEFAULT_FILE_PATH);

    /** The picture shown beside the user's messages. */
    private final Image userImage = new Image(getResourceUrl(USER_IMAGE_PATH));

    /** The picture shown beside the bot's replies. */
    private final Image botImage = new Image(getResourceUrl(BOT_IMAGE_PATH));

    /** Everything said so far, one {@link DialogBox} per message, oldest at the top. */
    private final VBox conversation = new VBox();

    /** The box the user types each command into. */
    private final TextField inputBox = new TextField();

    /** The button that sends what is in the input box, as pressing Enter does. */
    private final Button sendButton = new Button("Send");

    /**
     * Lays out the window, greets the user, and shows the window.
     *
     * @param stage the window JavaFX has created for this application
     */
    @Override
    public void start(Stage stage) {
        Scene scene = new Scene(buildLayout(stage), WINDOW_WIDTH, WINDOW_HEIGHT);
        scene.getStylesheets().add(getResourceUrl(STYLESHEET_PATH));

        // This window is the Ui, so the greeting arrives through show() below.
        bot.greet(this);

        stage.setTitle("PiplupBot");
        stage.setScene(scene);
        stage.show();
        inputBox.requestFocus();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Here that means adding a speech bubble from the bot to the
     * conversation, with the lines one under another. There is no need for the
     * console's dividers or indentation, as the bubble already marks where the
     * reply begins and ends.</p>
     */
    @Override
    public void show(String... lines) {
        conversation.getChildren().add(DialogBox.getBotDialog(String.join("\n", lines), botImage));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Here the explanation gets a bubble of a different colour from an
     * ordinary reply, so that a mistake stands out. The console, which has no
     * colours, shows the same words in the same way as any other reply.</p>
     */
    @Override
    public void showError(PiplupBotException e) {
        String text = String.join("\n", e.getMessageLines());
        conversation.getChildren().add(DialogBox.getErrorDialog(text, botImage));
    }

    /**
     * Returns the window's content: the conversation, able to scroll, above the
     * input box and the Send button.
     *
     * @param stage the window to close after {@code bye}
     * @return the whole of the window's content
     */
    private BorderPane buildLayout(Stage stage) {
        // The conversation soon outgrows the window, so it scrolls. Fitting it to
        // the width makes a long reply wrap onto more lines instead of making the
        // window scroll sideways.
        ScrollPane scrollPane = new ScrollPane(conversation);
        scrollPane.setFitToWidth(true);
        conversation.getStyleClass().add("conversation");
        // Whenever a new message makes the conversation taller, scroll to the
        // bottom, so the newest message is always the one in view.
        conversation.heightProperty().addListener(observable -> scrollPane.setVvalue(1.0));

        inputBox.setPromptText("Type a command, e.g. todo read book");
        // The input box takes whatever width the button leaves over.
        HBox.setHgrow(inputBox, Priority.ALWAYS);
        // Pressing Enter in the box and clicking the button both send the line.
        inputBox.setOnAction(event -> handleInput(stage));
        sendButton.setOnAction(event -> handleInput(stage));
        HBox inputBar = new HBox(inputBox, sendButton);
        inputBar.getStyleClass().add("input-bar");

        BorderPane layout = new BorderPane();
        layout.setCenter(scrollPane);
        layout.setBottom(inputBar);
        return layout;
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
            // Sending an empty box sends nothing, as a blank line does in the console.
            return;
        }

        conversation.getChildren().add(DialogBox.getUserDialog(input.trim(), userImage));
        // Handing over this window as the Ui is what makes the reply appear in it.
        boolean isExit = bot.respondTo(input, this);

        if (isExit) {
            // Close after a pause rather than at once, so the goodbye can be read.
            inputBox.setDisable(true);
            sendButton.setDisable(true);
            PauseTransition pause = new PauseTransition(GOODBYE_DELAY);
            pause.setOnFinished(event -> stage.close());
            pause.play();
        }
    }

    /**
     * Returns the address of one of the files that come with the program, such
     * as a picture, in the form JavaFX asks for.
     *
     * <p>The path is looked up among the program's own files -- the ones in
     * {@code src/main/resources} -- rather than in the folder the program was
     * started from, so it works the same from IntelliJ, from Gradle and from the
     * JAR file. Inside the JAR file, though, the lookup is case-sensitive even on
     * Windows: {@code /images/User.png} would not find {@code user.png}.</p>
     *
     * @param path where the file is, starting with {@code /},
     *             e.g. {@code "/images/user.png"}
     * @return the file's address
     * @throws IllegalStateException if the program has no such file
     */
    private static String getResourceUrl(String path) {
        URL url = GuiUi.class.getResource(path);
        if (url == null) {
            // Say which file is missing, instead of failing later with a bare NullPointerException.
            throw new IllegalStateException("Missing resource: " + path);
        }
        return url.toExternalForm();
    }
}
