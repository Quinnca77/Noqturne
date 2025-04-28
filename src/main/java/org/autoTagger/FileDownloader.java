package org.autoTagger;

import javax.swing.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class FileDownloader extends AbstractWorker {

    private final String url;
    private final Path path;
    private final String name;

    /**
     * Instantiates AbstractWorker with the right parameters
     *
     * @param url website to download from. Should directly point to the download link
     * @param path Path object to download the file to
     * @param name String denoting the object that is being downloaded
     */
    public FileDownloader(JFrame frame, String url, Path path, String name) {
        super(frame);
        this.url = url;
        this.path = path;
        this.name = name;
    }

    @Override
    protected void beginTask() {

    }

    @Override
    protected void executeTask() {
        try {
            GuiTagger.getInstance().showProgressBar(this, "Downloading " + name + "...");
            downloadFromUrl(url, path);
        } catch (IOException e) {
            ErrorLogger.runtimeExceptionOccurred(e);
        }
    }

    @Override
    protected void taskCompleted() {

    }

    /**
     * Downloads file from a website to a specified filepath.
     *
     * @param url website to download from. Should directly point to the download link
     * @param path Path object to download the file to
     * @throws IOException if an I/O error occurs
     */
    private void downloadFromUrl(String url, Path path) throws IOException {
        URL urlObject = new URL(url);
        URLConnection connection = urlObject.openConnection();
        long contentLength = connection.getContentLengthLong(); // Total file size

        try (InputStream in = connection.getInputStream();
             OutputStream out = Files.newOutputStream(path, StandardOpenOption.CREATE)) {

            byte[] buffer = new byte[8192]; // 8KB buffer
            long bytesRead = 0;
            int read;

            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
                bytesRead += read;

                if (contentLength > 0) {
                    int percent = (int) ((bytesRead * 100) / contentLength);
                    setProgress(percent);
                }
            }
        }
    }
}
