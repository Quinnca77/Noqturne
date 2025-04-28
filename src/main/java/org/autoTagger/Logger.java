package org.autoTagger;

import javax.swing.*;

/**
 * Singleton class for handling all status updates in the program.
 */
public class Logger {
    private final GuiTagger gui;
    private static Logger logger;

    /**
     * This constructor is never called except for the initialization of guiTagger.
     * Keep it like that.
     *
     * @param gui the GUI for the logger to attach to
     */
    public Logger(GuiTagger gui) {
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
     *
     * @param string the string to be displayed to the user via the GUI textbox
     */
    public void println(String string) {
        SwingUtilities.invokeLater(() -> gui.displayText(string + "\n"));
    }
}
