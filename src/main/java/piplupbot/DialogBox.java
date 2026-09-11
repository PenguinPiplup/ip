package piplupbot;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.shape.Circle;

// ACKNOWLEDGEMENTS: This Java file was written with the help of Claude.

/**
 * One message in the window's conversation: the words in a speech bubble,
 * beside a round picture of whoever said them.
 *
 * <p>The user's messages sit on the right with the picture last, as in most chat
 * apps, and the bot's sit on the left with the picture first. That is the only
 * difference in how they are built, so one class serves both. Its constructor
 * is private, and the three factory methods -- {@link #getUserDialog},
 * {@link #getBotDialog} and {@link #getErrorDialog} -- say which kind of message
 * is wanted, which reads better at the call than a {@code true} or
 * {@code false} would.</p>
 *
 * <p>This class decides only what a message is made of and which side it sits
 * on. How it looks -- the bubble's colour, its rounded corners, the gaps around
 * it -- is decided by the window's stylesheet, {@code main.css}, which finds
 * each part by the style class given to it here: {@code dialog-box} for the
 * whole message and {@code bubble} for the words, plus {@code user},
 * {@code bot} or {@code error} for whose message it is.</p>
 *
 * <p>It is an {@link HBox} -- a row of nodes, laid out left to right -- so the
 * window can add it to the conversation like any other node.</p>
 */
public class DialogBox extends HBox {
    /** Width and height of the picture beside each message, in pixels. */
    private static final double PICTURE_SIZE = 40;

    /**
     * Creates a message with the given words and picture, on the user's side of
     * the conversation or on the bot's.
     *
     * @param text       the words of the message
     * @param picture    a picture of whoever said them
     * @param isFromUser whether the user said them, rather than the bot
     */
    private DialogBox(String text, Image picture, boolean isFromUser) {
        Label bubble = new Label(text);
        bubble.setWrapText(true);
        // A label can be squeezed shorter than its text needs, and then cuts the
        // text off with "...". Never letting it be shorter than it would like to
        // be keeps every line of a long reply.
        bubble.setMinHeight(Region.USE_PREF_SIZE);
        bubble.getStyleClass().add("bubble");

        ImageView pictureView = new ImageView(picture);
        pictureView.setFitWidth(PICTURE_SIZE);
        pictureView.setFitHeight(PICTURE_SIZE);
        // Only the part of the picture inside this circle is drawn, which makes it round.
        pictureView.setClip(new Circle(PICTURE_SIZE / 2, PICTURE_SIZE / 2, PICTURE_SIZE / 2));

        if (isFromUser) {
            getChildren().addAll(bubble, pictureView);
            setAlignment(Pos.TOP_RIGHT);
        } else {
            getChildren().addAll(pictureView, bubble);
            setAlignment(Pos.TOP_LEFT);
        }
        getStyleClass().add("dialog-box");
    }

    /**
     * Returns a message from the user, for the right-hand side of the
     * conversation.
     *
     * @param text    what the user typed
     * @param picture the user's picture
     * @return the message, ready to add to the conversation
     */
    public static DialogBox getUserDialog(String text, Image picture) {
        DialogBox box = new DialogBox(text, picture, true);
        box.getStyleClass().add("user");
        return box;
    }

    /**
     * Returns a reply from the bot, for the left-hand side of the conversation.
     *
     * @param text    the reply, with a line break between its lines
     * @param picture the bot's picture
     * @return the reply, ready to add to the conversation
     */
    public static DialogBox getBotDialog(String text, Image picture) {
        DialogBox box = new DialogBox(text, picture, false);
        box.getStyleClass().add("bot");
        return box;
    }

    /**
     * Returns the bot's explanation of an error. It sits where the bot's other
     * replies do, but the stylesheet gives it a colour of its own, so that a
     * mistake stands out from an ordinary reply.
     *
     * @param text    the explanation, with a line break between its lines
     * @param picture the bot's picture
     * @return the explanation, ready to add to the conversation
     */
    public static DialogBox getErrorDialog(String text, Image picture) {
        DialogBox box = new DialogBox(text, picture, false);
        box.getStyleClass().add("error");
        return box;
    }
}
