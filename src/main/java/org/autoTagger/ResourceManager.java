package org.autoTagger;

import org.apache.commons.io.FileUtils;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Stack;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Class used for interfacing with runtime dependency files.
 */
public class ResourceManager {

    private static final Path appDir = Paths.get(System.getenv("APPDATA"), "AutoTagger");
    private static final Logger logger = Logger.getLogger();
    private static final String PY_FILE = "/coverArt.py";
    private static final String PY_FILE_PREFIX = "coverArt";
    private static final String PY_FILE_SUFFIX = ".py";
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
        Path binDir = appDir.resolve("bin");
        Files.createDirectories(binDir);
        Path ytDlpPath = binDir.resolve("yt-dlp.exe");

        // If yt-dlp has not previously been copied to %APPDATA%/Roaming
        if (!Files.exists(ytDlpPath)) {
            logger.println("yt-dlp dependency not found, downloading now...");
            downloadFromUrl("https://github.com/yt-dlp/yt-dlp/releases/download/2025.03.31/yt-dlp.exe", ytDlpPath);
            logger.println("yt-dlp.exe downloaded!");

            updateYtDlp(ytDlpPath);
        }

        // Checks whether ffmpeg dependencies are present
        Path ffmpegDependencyPath = binDir.resolve("ffmpeg.exe");
        if (!Files.exists(ffmpegDependencyPath)) {
            downloadLatestFfmpeg(binDir);
        }
        return ytDlpPath;
    }

    /**
     * Downloads latest ffmpeg builds and puts them in the right folder. Will replace existing builds
     * if they already exist in that location.
     *
     * @param binDir Path object pointing to the binary folder of this app's AppData folder
     * @throws IOException if an I/O error occurs
     */
    private static void downloadLatestFfmpeg(Path binDir) throws IOException {
        logger.println("Downloading ffmpeg now...");
        Path ffmpegZipPath = binDir.resolve("ffmpeg.zip");
        downloadFromUrl("https://github.com/yt-dlp/FFmpeg-Builds/releases/download/latest/ffmpeg-master-latest-win64-gpl.zip",
                ffmpegZipPath);
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
    }

    /**
     * Updates the existing yt-dlp binary with its own update command.
     *
     * @param ytDlpPath Path object pointing to the yt-dlp binary
     * @throws IOException if an I/O error occurs
     */
    private static void updateYtDlp(Path ytDlpPath) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(ytDlpPath.toString(), "-U");
        pb.inheritIO();
        Process process = pb.start();
        try {
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                logger.println("yt-dlp updated successfully!");
            } else {
                logger.println("yt-dlp update failed with exit code " + exitCode);
            }
        } catch (InterruptedException e) {
            ErrorLogger.runtimeExceptionOccurred(e);
            logger.println("Something went wrong while updating yt-dlp!");
        }
    }

    /**
     * Downloads file from a website to a specified filepath
     *
     * @param url website to download from. Should directly point to the download link
     * @param path Path object to download the file to
     * @throws IOException if an I/O error occurs
     */
    private static void downloadFromUrl(String url, Path path) throws IOException {
        URL urlObject = new URL(url);
        try (InputStream in = urlObject.openStream()) {
            Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
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
                    System.out.println(file.getAbsolutePath());
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
            logger.println("Something went wrong while checking Python dependency ytmusicapi.");
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
            logger.println("Something went wrong while checking Python dependency ytmusicapi.");
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
            // Once Auto-Tagger exits, this temporary python file gets deleted
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
        try {
            updateYtDlp(getYtDlpPath());
            downloadLatestFfmpeg(appDir.resolve("bin"));
            updateYtMusicApi();
        } catch (IOException e) {
            logger.println("Could not update dependencies due to an I/O error");
            ErrorLogger.runtimeExceptionOccurred(e);
            return;
        }
        logger.println("Dependencies successfully updated!");

    }
}
