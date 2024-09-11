package org.example;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ErrorLogger {

    public static void runtimeExceptionOccurred(Exception e) throws IOException {
        File errorLog = new File("errorLog.log");
        errorLog.createNewFile();
        FileWriter fileWriter = new FileWriter("errorLog.log");
        fileWriter.write(String.valueOf(e));
        fileWriter.close();
    }

}
