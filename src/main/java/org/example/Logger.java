package org.example;

public class Logger {
    private final guiTagger gui;
    private static Logger logger;

    public Logger(guiTagger gui) {
        this.gui = gui;
        logger = this;
    }

    public static Logger getLogger() {
        return logger;
    }

    public void println(String string) {
        gui.displayText(string + "\n");
    }
}
