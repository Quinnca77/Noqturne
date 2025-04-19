package org.autoTagger;

import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
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
     * @return an array of <code>File</code> objects that points to the downloaded songs.
     */
    public File[] downloadSongs(String url) throws IOException, InterruptedException {
        HashSet<File> filesNotToTag = new HashSet<>(Arrays.asList(Tagger.getAllMp3Files()));
        Process process = getProcess(url);

        StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream(), "OUTPUT", this.logger);
        StreamGobbler errorGobbler = new StreamGobbler(process.getErrorStream(), "ERROR", this.logger);

        outputGobbler.start();
        errorGobbler.start();

        process.waitFor();
        ArrayList<File> filesToTag = new ArrayList<>();
        for (File file : Tagger.getAllMp3Files()) {
            if (!filesNotToTag.contains(file)) {
                filesToTag.add(file);
            }
        }
        File[] output = new File[0];
        return filesToTag.toArray(output);
    }

    private static @NotNull Process getProcess(String url) throws IOException {
        Path ytDlpPath = ResourceManager.getYtDlpPath();
        ProcessBuilder pb = new ProcessBuilder(
                ytDlpPath.toString(),
                "--replace-in-metadata", "\"title\"", "\"[\\\"]\"", "\"\"",
                "-x",
                "--audio-format", "mp3",
                "-P \"%USERPROFILE%/Downloads\"", // TODO hard-coded for now, will change in later iterations
                "-o", "%(title)s.%(ext)s",
                "\"" + url + "\"");
        return pb.start();
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
                Pattern downloadPattern = Pattern.compile("Downloading item (\\d+) of (\\d+)");
                StringBuilder errorText = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    if (streamType.equals("ERROR") && line.contains("ERROR")) {
                        this.logger.println("yt-dlp ran into an error!!!");
                        errorText.append(line).append("\n");
                    }
                    Matcher matcher = downloadPattern.matcher(line);
                    if (matcher.find()) {
                        String currentItem = matcher.group(1);
                        String totalItems = matcher.group(2);
                        this.logger.println("Song download progress: " + currentItem + " out of " + totalItems);
                    }
                    System.out.println(streamType + "> " + line);
                }
                if (!errorText.isEmpty()) {
                    ErrorLogger.runtimeExceptionOccurred(errorText.toString());
                }
            } catch (IOException e) {
                ErrorLogger.runtimeExceptionOccurred(e);
                this.logger.println("Ran into IOException in StreamGobbler. See errorLog.log for more details.");
            }
        }
    }
}
