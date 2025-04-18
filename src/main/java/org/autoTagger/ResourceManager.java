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

public class ResourceManager {

    private static final Path appDir = Paths.get(System.getenv("APPDATA"), "AutoTagger");
    private static final Logger logger = Logger.getLogger();
    private static final String PY_FILE = "coverArt.py";
    private static final String PY_FILE_PREFIX = "coverArt";
    private static final String PY_FILE_SUFFIX = ".py";
    private static Path tempPyFilePath;

    public static Path getYtDlpPath() throws IOException {
        Path binDir = appDir.resolve("bin");
        Files.createDirectories(binDir);
        Path ytDlpPath = binDir.resolve("yt-dlp.exe");
        if (!Files.exists(ytDlpPath)) {
            logger.println("yt-dlp dependency not found, downloading now...");
            downloadFromUrl("https://github.com/yt-dlp/yt-dlp/releases/download/2025.03.31/yt-dlp.exe", ytDlpPath);
            logger.println("yt-dlp.exe downloaded!");

            // Update yt-dlp
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

        Path ffmpegDependencyPath = binDir.resolve("ffmpeg.exe");
        if (!Files.exists(ffmpegDependencyPath)) {
            logger.println("ffmpeg not found, downloading now...");
            Path ffmpegZipPath = binDir.resolve("ffmpeg.zip");
            downloadFromUrl("https://github.com/yt-dlp/FFmpeg-Builds/releases/download/latest/ffmpeg-master-latest-win64-gpl.zip",
                    ffmpegZipPath);
            logger.println("ffmpeg.zip downloaded!");

            logger.println("Unzipping...");
            unzip(ffmpegZipPath.toString(), "ffmpeg_temp");
            logger.println("Unzipped!");

            logger.println("Moving files...");
            Path ffmpegUnzippedDirectory = binDir.resolve("ffmpeg_temp");
            searchAndMoveFile("ffmpeg.exe", ffmpegUnzippedDirectory, binDir.resolve("ffmpeg.exe"));
            searchAndMoveFile("ffprobe.exe", ffmpegUnzippedDirectory, binDir.resolve("ffprobe.exe"));
            searchAndMoveFile("ffplay.exe", ffmpegUnzippedDirectory, binDir.resolve("ffplay.exe"));
            logger.println("Moving files successful!");

            logger.println("Removing remnants...");
            Files.delete(ffmpegZipPath);
            FileUtils.deleteDirectory(ffmpegUnzippedDirectory.toFile());
            logger.println("Removed remnants! Ffmpeg successfully installed!");
        }
        return ytDlpPath;
    }

    private static void downloadFromUrl(String url, Path path) throws IOException {
        URL ytDlpUrl = new URL(url);
        try (InputStream in = ytDlpUrl.openStream()) {
            Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void unzip(String zipFile, String destFolder) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            byte[] buffer = new byte[1024];
            while ((entry = zis.getNextEntry()) != null) {
                File newFile = new File(destFolder + File.separator + entry.getName());
                if (entry.isDirectory()) {
                    if (!newFile.mkdirs() && !newFile.isDirectory()) {
                        throw new IOException();
                    }
                } else {
                    if (!(new File(newFile.getParent()).mkdirs()) && !newFile.isDirectory()) {
                        throw new IOException();
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
                    Files.move(file.toPath(), pathToMove);
                    return;
                }
                if (file.isDirectory()) {
                    stack.push(file);
                }
            }
        }
    }

    public static void ensureYtMusicApiInstallation() {
        try {
            Process process = new ProcessBuilder("python", "-m", "pip", "show", "ytmusicapi").start();
            int exitCode = process.waitFor();
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

    public static Path getCoverArtPy() throws IOException {
        if (tempPyFilePath == null) {
            InputStream in = ResourceManager.class.getResourceAsStream(PY_FILE);
            if (in == null) {
                ErrorLogger.runtimeExceptionOccurred("Could not get python file resource.");
                throw new FileNotFoundException(PY_FILE);
            }
            File tempPyFile = File.createTempFile(PY_FILE_PREFIX, PY_FILE_SUFFIX);
            tempPyFile.deleteOnExit();
            Files.copy(in, tempPyFilePath, StandardCopyOption.REPLACE_EXISTING);
            tempPyFilePath = tempPyFile.toPath();
        }
        return tempPyFilePath;
    }
}
