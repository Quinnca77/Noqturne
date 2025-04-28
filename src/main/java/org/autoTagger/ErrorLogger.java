package org.autoTagger;

import java.io.*;
import java.util.Date;

/**
 * Class that gets called whenever a critical error occurs and reviewing a stacktrace
 * is necessary
 */
public class ErrorLogger {

    /**
     * Called when a critical Exception occurs. Dumps the stack trace of the Exception to a log
     * file for debugging purposes.
     * Also calls {@link GuiTagger#displayText(String, boolean)} to tell the user an error occurred.
     * <p>
     * When calling this function, be sure not to duplicate the error logging to the UI console!
     *
     * @param e the Exception to be logged.
     */
    public static void runtimeExceptionOccurred(Throwable e) {
        logToFile(e);
        Logger.getLogger().printError("A runtime error occurred, check errorLog.log for more details");
    }

    /**
     * Called when a critical error occurs. Dumps the details of the error to a log file
     * for debugging purposes.
     * Also calls {@link GuiTagger#displayText(String, boolean)} to tell the user an error occurred.
     * <p>
     * When calling this function, be sure not to duplicate the error logging to the UI console!
     *
     * @param text the description of the error that occurred.
     */
    public static void runtimeExceptionOccurred(String text) {
        logToFile(text);
        Logger.getLogger().printError("The following error occurred: " + text);
    }

    private static void logToFile(Object e) {
        try {
            File errorLog = new File("errorLog.log");
            //noinspection ResultOfMethodCallIgnored
            errorLog.createNewFile();
            FileWriter fileWriter = new FileWriter("errorLog.log");
            StringWriter sw = new StringWriter();
            if (e instanceof Throwable) {
                ((Throwable) e).printStackTrace(new PrintWriter(sw));
            } else if (e instanceof String) {
                sw.write(e + "\n");
            }
            sw.write("\nThis log has been generated at " + new Date());
            fileWriter.write(sw.toString());
            fileWriter.close();
        } catch (IOException ex) {
            Logger.getLogger().printError("A runtime error occurred, but could not be logged due to an IOException");
        }
    }

}
