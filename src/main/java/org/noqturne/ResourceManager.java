package org.noqturne;

import org.apache.commons.io.FileUtils;
import org.noqturne.exceptions.TaggingFolderException;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.ExecutionException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Class used for interfacing with runtime dependency files.
 */
public class ResourceManager {

    private static final Path appDir = Paths.get(System.getenv("APPDATA"), "Noqturne");
    private static final Path binDir = appDir.resolve("bin");
    private static final Logger logger = Logger.getLogger();
    private static final String PY_FILE = "/coverArt.py";
    private static final String PY_FILE_PREFIX = "coverArt";
    private static final String PY_FILE_SUFFIX = ".py";
    private static final String TAG_FOLDER_KEY = "TAGGING_FOLDER=";
    private static Path tempPyFilePath;

    /**
     * Gets the Path to the yt-dlp binary used for downloading songs.
     * The binary will be located in its own folder in %APPDATA%/Roaming, but if it does not
     * exist there yet, it will download it promptly.
     * This method also checks whether the ffmpeg binaries needed for the functionality of yt-dlp
     * are present.
     *
     * @return Path object pointing directly to the yt-dlp binary
     * @throws IOException if an I/O errors occurs
     */
    public static Path getYtDlpPath() throws IOException {
        Files.createDirectories(binDir);
        Path ytDlpPath = binDir.resolve("yt-dlp.exe");

        // If yt-dlp has not previously been copied to %APPDATA%/Roaming
        if (!Files.exists(ytDlpPath)) {
            logger.println("yt-dlp dependency not found, downloading now...");
            FileDownloader ytDlpDownloader = new FileDownloader(Gui.getInstance(),
                    "https://github.com/yt-dlp/yt-dlp/releases/download/2025.03.31/yt-dlp.exe",
                    ytDlpPath,
                    "yt-dlp");
            ytDlpDownloader.execute();
            try {
                ytDlpDownloader.get();
            } catch (InterruptedException | ExecutionException e) {
                ErrorLogger.runtimeExceptionOccurred(e);
            }
            logger.println("yt-dlp.exe downloaded!");
            updateYtDlp(ytDlpPath);
        }

        // Checks whether ffmpeg dependencies are present
        Path ffmpegDependencyPath = binDir.resolve("ffmpeg.exe");
        if (!Files.exists(ffmpegDependencyPath)) {
            AbstractWorker ffmpegDownloader = downloadLatestFfmpeg(binDir);
            try {
                ffmpegDownloader.get();
            } catch (InterruptedException | ExecutionException e) {
                ErrorLogger.runtimeExceptionOccurred(e);
            }
            onFfmpegDownloaded(binDir.resolve("ffmpeg.zip"));
        }
        return ytDlpPath;
    }

    /**
     * Downloads latest ffmpeg builds and puts them in the right folder. Will replace existing builds
     * if they already exist in that location. Be sure to call {@link ResourceManager#onFfmpegDownloaded(Path)}
     * after the returned AbstractWorker has finished running.
     *
     * @param binDir Path object pointing to the binary folder of this app's AppData folder
     * @return AbstractWorker denoting the download progress
     * @throws IOException if an I/O error occurs
     */
    private static AbstractWorker downloadLatestFfmpeg(Path binDir) throws IOException {
        logger.println("Downloading ffmpeg now...");
        Files.createDirectories(binDir);
        Path ffmpegZipPath = binDir.resolve("ffmpeg.zip");

        FileDownloader ffmpegDownloader = new FileDownloader(Gui.getInstance(),
                "https://github.com/yt-dlp/FFmpeg-Builds/releases/download/latest/ffmpeg-master-latest-win64-gpl.zip",
                ffmpegZipPath,
                "ffmpeg");

        ffmpegDownloader.execute();
        return ffmpegDownloader;
    }

    /**
     * Called asynchronously after ffmpeg has been downloaded. Handles the file location and unzipping
     * of ffmpeg binaries.
     *
     * @param ffmpegZipPath Path where the zip file of ffmpeg is located
     */
    private static void onFfmpegDownloaded(Path ffmpegZipPath) {
        try {
            logger.println("ffmpeg.zip downloaded!");

            // Unzips downloaded zip file
            logger.println("Unzipping...");
            Path ffmpegUnzippedDirectory = binDir.resolve("ffmpeg_temp");
            unzip(ffmpegZipPath.toString(), ffmpegUnzippedDirectory.toString());
            logger.println("Unzipped!");

            // Move files from extracted zip to desired location for yt-dlp
            logger.println("Moving files...");
            searchAndMoveFile("ffmpeg.exe", ffmpegUnzippedDirectory, binDir.resolve("ffmpeg.exe"));
            searchAndMoveFile("ffprobe.exe", ffmpegUnzippedDirectory, binDir.resolve("ffprobe.exe"));
            searchAndMoveFile("ffplay.exe", ffmpegUnzippedDirectory, binDir.resolve("ffplay.exe"));
            logger.println("Moving files successful!");

            // Deleting files that will not be used anymore
            logger.println("Removing remnants...");
            Files.delete(ffmpegZipPath);
            FileUtils.deleteDirectory(ffmpegUnzippedDirectory.toFile());
            logger.println("Removed remnants! Ffmpeg successfully installed!");
        } catch (IOException e) {
            ErrorLogger.runtimeExceptionOccurred(e);
        }
    }

    /**
     * Updates the existing yt-dlp binary with its own update command.
     *
     * @param ytDlpPath Path object pointing to the yt-dlp binary
     */
    private static void updateYtDlp(Path ytDlpPath) {
        ProcessBuilder pb = new ProcessBuilder(ytDlpPath.toString(), "-U");
        pb.inheritIO();
        try {
            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                logger.println("yt-dlp updated successfully!");
            } else {
                logger.println("yt-dlp update failed with exit code " + exitCode);
            }
        } catch (InterruptedException | IOException e) {
            ErrorLogger.runtimeExceptionOccurred(e);
        }
    }

    /**
     * Slightly modified version of the unzip function found on
     * <a href="https://www.geeksforgeeks.org/how-to-zip-and-unzip-files-in-java/">GeeksForGeeks</a>
     * Used to unzip the ffmpeg zip folder after it is downloaded, but can be used to unzip
     * any arbitrary zip file.
     *
     * @param zipFile String representing the file path to the zip file
     * @param destFolder String representing the file path to the destination folder where the
     *                   zip file will get unzipped to
     * @throws IOException if an I/O error occurs
     */
    @SuppressWarnings("SameParameterValue")
    private static void unzip(String zipFile, String destFolder) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            byte[] buffer = new byte[1024];
            while ((entry = zis.getNextEntry()) != null) {
                File newFile = new File(destFolder + File.separator + entry.getName());
                if (entry.isDirectory()) {
                    if (!newFile.exists()) {
                        if (!newFile.mkdirs()) {
                            throw new IOException("Failed to create directory: " + newFile.getAbsolutePath());
                        }
                    }
                } else {
                    File parentDir = newFile.getParentFile();
                    if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
                        throw new IOException("Failed to create directory: " + parentDir.getAbsolutePath());
                    }
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        int length;
                        while ((length = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, length);
                        }
                    }
                }
            }
        }
    }

    /**
     * Uses an iterative approach to search for a specified file in a specified folder and
     * will move it to a specified path after finding it.
     *
     * @param fileName String denoting the filename of the file to be found
     * @param pathToSearch Path object representing the folder to search in
     * @param pathToMove After finding the file, moves it to this specified Path
     * @throws IOException if an I/O errors occurs
     */
    private static void searchAndMoveFile(String fileName, Path pathToSearch, Path pathToMove) throws IOException {
        File f = pathToSearch.toFile();
        Stack<File> stack = new Stack<>();
        stack.push(f);

        while (!stack.empty()) {
            f = stack.pop();
            File[] list = f.listFiles();
            if (list == null) {
                continue;
            }
            for (File file : list) {
                if (file.isFile() && (file.getName().equals(fileName))) {
                    Files.move(file.toPath(), pathToMove, StandardCopyOption.REPLACE_EXISTING);
                    return;
                }
                if (file.isDirectory()) {
                    stack.push(file);
                }
            }
        }
    }

    /**
     * Makes sure ytmusicapi is pip installed. If it isn't, it will automatically install it and log the results.
     */
    public static void ensureYtMusicApiInstallation() {
        try {
            Process process = new ProcessBuilder("python", "-m", "pip", "show", "ytmusicapi").start();
            int exitCode = process.waitFor();
            // If pip show returns an error, ytmusicapi is not installed yet
            if (exitCode != 0) {
                logger.println("Couldn't find Python dependency ytmusicapi, installing now...");
                process = new ProcessBuilder("python", "-m", "pip", "install", "ytmusicapi").start();
                exitCode = process.waitFor();
                if (exitCode == 0) {
                    logger.println("Successfully installed ytmusicapi Python dependency!");
                } else {
                    logger.println("Couldn't install ytmusicapi. Tagging will not work.");
                }
            }
        } catch (IOException | InterruptedException e) {
            ErrorLogger.runtimeExceptionOccurred(e);
        }
    }

    /**
     * Updates the ytmusicapi python dependency.
     * Before updating will ensure ytmusicapi has already been installed.
     */
    private static void updateYtMusicApi() {
        ensureYtMusicApiInstallation();
        try {
            Process process = new ProcessBuilder("python", "-m", "pip", "install", "ytmusicapi", "-U").start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                logger.println("Updating ytmusicapi failed!");
            } else {
                logger.println("Updating ytmusicapi successful!");
            }
        } catch (IOException | InterruptedException e) {
            ErrorLogger.runtimeExceptionOccurred(e);
        }

    }

    /**
     * Gets the Path to the coverArt.py file used for getting the vId of a song.
     * This file is located in the resources folder but for a compiled jar we have
     * to first extract it to a temporary file from which we can invoke it from the
     * command line.
     *
     * @return Path to coverArt.py file
     * @throws IOException if an I/O error occurs
     */
    public static Path getCoverArtPy() throws IOException {
        if (tempPyFilePath == null) {
            InputStream in = ResourceManager.class.getResourceAsStream(PY_FILE);
            if (in == null) {
                ErrorLogger.runtimeExceptionOccurred("Could not get python file resource.");
                throw new FileNotFoundException(PY_FILE);
            }
            File tempPyFile = File.createTempFile(PY_FILE_PREFIX, PY_FILE_SUFFIX);
            // Once Noqturne exits, this temporary python file gets deleted
            tempPyFile.deleteOnExit();
            Path tempPath = tempPyFile.toPath();
            Files.copy(in, tempPath, StandardCopyOption.REPLACE_EXISTING);
            tempPyFilePath = tempPath;
        }
        return tempPyFilePath;
    }

    /**
     * Updates the runtime dependencies of this application.
     * Specifically, this method will update:
     * <ul>
     *     <li>yt-dlp</li>
     *     <li>ffmpeg builds</li>
     *     <li>ytmusicapi</li>
     * </ul>
     */
    public static void updateDependencies() {
        new AbstractWorker(Gui.getInstance()) {
            @Override
            protected void beginTask() {
                logger.println("Starting update...");
            }
            @Override
            protected void executeTask() {
                try {
                    updateYtDlp(getYtDlpPath());
                    AbstractWorker ffmpegDownloader = downloadLatestFfmpeg(appDir.resolve("bin"));
                    updateYtMusicApi();

                    ffmpegDownloader.get();
                    onFfmpegDownloaded(binDir.resolve("ffmpeg.zip"));
                } catch (IOException | ExecutionException | InterruptedException e) {
                    ErrorLogger.runtimeExceptionOccurred(e);
                }
            }
            @Override
            protected void taskCompleted() {
                logger.println("Update complete!");
            }
        }.execute();
    }

    /**
     * Gets the folder to tag mp3 files in. This is saved in a config file at %APPDATA%/Roaming.
     *
     * @return File object denoting the directory of where the tagging directory is located
     * @throws IOException if an I/O error occurs
     * @throws TaggingFolderException if the tagging folder filepath does not exist
     */
    public static File getTaggingDirectory() throws IOException, TaggingFolderException {
        Path configFile = appDir.resolve("config.txt");
        Files.createDirectories(appDir);
        if (!Files.exists(configFile)) {
            setTaggingDirectory(null);
        }
        List<String> lines = Files.readAllLines(configFile);
        for (String line : lines) {
            if (line.startsWith(TAG_FOLDER_KEY)) {
                Path taggingFolderPath = Paths.get(line.substring(TAG_FOLDER_KEY.length()));
                if (!Files.exists(taggingFolderPath)) {
                    throw new TaggingFolderException();
                }
                return taggingFolderPath.toFile();
            }
        }
        throw new IOException("config file found but without expected key");
    }

    /**
     * Sets the directory of where to tag songs in. This will be saved in a config file at
     * %APPDATA%/Roaming for future use.
     *
     * @param directory Path object denoting where to tag songs in. If this parameter is null,
     *                  it will set the Windows Downloads folder as its default
     * @throws IOException if an I/O error occurs
     */
    public static void setTaggingDirectory(@Nullable Path directory) throws IOException {
        Path configFile = appDir.resolve("config.txt");
        Files.createDirectories(appDir);
        List<String> lines = new ArrayList<>();
        if (Files.exists(configFile)) {
            lines = Files.readAllLines(configFile);
        }

        boolean updated = false;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).startsWith(TAG_FOLDER_KEY)) {
                if (directory == null) {
                    lines.set(i, TAG_FOLDER_KEY + Paths.get(System.getProperty("user.home"), "Downloads"));
                } else {
                    lines.set(i, TAG_FOLDER_KEY + directory.toAbsolutePath());
                }
                updated = true;
                break;
            }
        }

        if (!updated) {
            if (directory == null) {
                lines.add(TAG_FOLDER_KEY + Paths.get(System.getProperty("user.home"), "Downloads"));
            } else {
                lines.add(TAG_FOLDER_KEY + directory.toAbsolutePath());
            }
        }

        Files.write(configFile, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
}
