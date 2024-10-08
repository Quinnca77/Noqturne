package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Downloader {

    private final Logger logger;

    public Downloader() {
        this.logger = Logger.getLogger();
    }

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

    private static class StreamGobbler extends Thread {
        private final BufferedReader reader;
        private final String streamType;
        private final Logger logger;

        public StreamGobbler(InputStream inputStream, String streamType, Logger logger) {
            this.reader = new BufferedReader(new InputStreamReader(inputStream));
            this.streamType = streamType;
            this.logger = logger;
        }

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
                System.out.println("Ran into IOException in StreamGobbler. Stacktrace: " + Arrays.toString(e.getStackTrace()));
            }
        }
    }
}
