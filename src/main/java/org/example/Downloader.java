package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class handles the entire flow of downloading a song with yt-dlp.
 * It does this by calling yt-dlp from the command line and logging its
 * progress in the application window.
 */
public class Downloader {

    private final Logger logger;

    /**
     * Generic constructor for Downloader. Only attaches the Logger singleton to the class instance.
     */
    public Downloader() {
        this.logger = Logger.getLogger();
    }

    /**
     * Uses the command line (does not visually appear) to download the song indicated by the
     * YouTube URL. It uses yt-dlp to perform this, and thus any errors it throws in its logs are
     * likely the case of a faulty installation of yt-dlp and/or ffmpeg.
     *
     * @param url The URL to the YouTube video (so not only a vId!) which represents the song.
     * @throws IOException If an I/O error occurs.
     * @throws InterruptedException if the current Thread is interrupted while waiting.
     */
    public void downloadSongs(String url) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("yt-dlp.exe",
                "--replace-in-metadata", "\"title\"", "\"[\\\"]\"", "\"\"",
                "-x",
                "--audio-format", "mp3",
                "-P \"%USERPROFILE%/Downloads\"",
                "-o \"%(title)s.%(ext)s\"",
                "\"" + url + "\"");
        Process process = pb.start();

        StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream(), "OUTPUT", this.logger);
        StreamGobbler errorGobbler = new StreamGobbler(process.getErrorStream(), "ERROR", this.logger);

        outputGobbler.start();
        errorGobbler.start();

        process.waitFor();
    }

    /**
     * Simple helper class that handles a command line stream and propagates its information
     * to the logger.
     */
    private static class StreamGobbler extends Thread {
        private final BufferedReader reader;
        private final String streamType;
        private final Logger logger;

        /**
         * Constructs a StreamGobbler instance with a given inputStream.
         *
         * @param inputStream The inputStream to be logged.
         * @param streamType The name of the inputStream type (typically OUTPUT or ERROR).
         * @param logger The Logger instance to be used.
         */
        public StreamGobbler(InputStream inputStream, String streamType, Logger logger) {
            this.reader = new BufferedReader(new InputStreamReader(inputStream));
            this.streamType = streamType;
            this.logger = logger;
        }

        /**
         * Run command to start a new Thread that performs logging on the given inputStream.
         */
        @Override
        public void run() {
            try {
                String line;
                Pattern pattern = Pattern.compile("Downloading item (\\d+) of (\\d+)");
                while ((line = reader.readLine()) != null) {
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.find()) {
                        String currentItem = matcher.group(1);
                        String totalItems = matcher.group(2);
                        this.logger.println("Song download progress: " + currentItem + " out of " + totalItems);
                    }
                    System.out.println(streamType + "> " + line);
                }
            } catch (IOException e) {
                ErrorLogger.runtimeExceptionOccurred(e);
                this.logger.println("Ran into IOException in StreamGobbler. See errorLog.log for more details.");
            }
        }
    }
}
