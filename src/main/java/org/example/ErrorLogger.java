package org.example;

import java.io.*;

/**
 * Class that gets called whenever a critical error occurs and reviewing a stacktrace
 * is necessary
 */
public class ErrorLogger {

    /**
     * Called when a critical Exception occurs. Dumps the stack trace of the Exception to a log
     * file for debugging purposes.
     *
     * @param e the Exception to be logged.
     */
    public static void runtimeExceptionOccurred(Throwable e) {
        try {
            File errorLog = new File("errorLog.log");
            //noinspection ResultOfMethodCallIgnored
            errorLog.createNewFile();
            FileWriter fileWriter = new FileWriter("errorLog.log");
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            fileWriter.write(sw.toString());
            fileWriter.close();
        } catch (IOException ex) {
            Logger.getLogger().println("A runtime error occurred, but could not be logged due to an IOException");
        }
    }

}
