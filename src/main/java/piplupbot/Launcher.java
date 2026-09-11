package piplupbot;

// ACKNOWLEDGEMENTS: This Java file was written with the help of Claude.

/**
 * The program's front door: the main class named in {@code build.gradle}, and
 * so the one that {@code ./gradlew run} and the JAR file start. It passes the
 * command-line arguments straight to {@link PiplupBot#launch}, which opens the
 * window, or the console when given {@code --cli}.
 *
 * <p>Why not start the window, {@link GuiUi}, directly? Because it extends
 * JavaFX's {@code Application}. When the class Java is asked to start is an
 * {@code Application}, Java insists on finding JavaFX as a module, and stops
 * with "JavaFX runtime components are missing" when JavaFX is on the classpath
 * instead -- which is where Gradle and the JAR file put it. Starting from an
 * ordinary class avoids that check.</p>
 *
 * <p>And why not start {@link PiplupBot} directly? It is an ordinary class too,
 * but its own {@code main} opens the console, as it always has: the text-UI
 * tests start it with no arguments and expect the console. So this class is
 * what makes the window the default.</p>
 */
public class Launcher {
    /**
     * Starts PiplupBot in the window, or in the console if the first argument
     * is {@code --cli}.
     *
     * @param args {@code --cli} for the console; nothing, for the window
     */
    public static void main(String[] args) {
        PiplupBot.launch(args);
    }
}
