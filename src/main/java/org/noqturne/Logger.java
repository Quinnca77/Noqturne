package org.noqturne;

import javax.swing.*;

/**
 * Singleton class for handling all status updates in the program.
 */
public class Logger {
    private final Gui gui;
    private static Logger logger;

    /**
     * This constructor is never called except for the initialization of the GUI.
     * Keep it like that.
     *
     * @param gui the GUI for the logger to attach to
     */
    public Logger(Gui gui) {
        this.gui = gui;
        logger = this;
    }

    /**
     * @return an instance of logger meant for viewing status updates to the user.
     */
    public static Logger getLogger() {
        return logger;
    }

    /**
     * Prints a string to the GUI for the user to see. Only meant for user-friendly
     * language, no technical specifics.
     * <p>
     * Simply calls {@link Gui#displayText(String, boolean)}.
     *
     * @param string the string to be displayed to the user via the GUI textbox
     */
    public void println(String string) {
        SwingUtilities.invokeLater(() -> gui.displayText(string + "\n", false));
    }

    /**
     * Prints an error string to the GUI for the user to see. Only meant for user-friendly
     * language, no technical specifics. Will display the text in red.
     * <p>
     * Simply calls {@link Gui#displayText(String, boolean)}.
     *
     * @param string the string to be displayed to the user via the GUI textbox
     */
    public void printError(String string) {
        SwingUtilities.invokeLater(() -> gui.displayText(string + "\n", true));
    }
}
