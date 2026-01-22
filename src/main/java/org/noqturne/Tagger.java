package org.noqturne;

import com.mpatric.mp3agic.*;
import org.noqturne.exceptions.CoverArtSearchEmptyException;
import org.noqturne.exceptions.NoSongFoundException;
import org.noqturne.exceptions.TaggingFolderException;
import org.noqturne.exceptions.VIdException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * Class for all functionalities associated with tagging an mp3 file in the user's tagging folder.
 */
public class Tagger {

    // File filter for sorting mp3 files
    private final static FileFilter filter = file -> file.getName().endsWith(".mp3");
    public static final String MIME_TYPE = "image/jpeg";
    private final Logger logger;

    public Tagger() {
        this.logger = Logger.getLogger();
    }

    /**
     * Gets all mp3 files in the tagging folder.
     *
     * @return a File array with mp3 files
     */
    public static File[] getAllMp3Files() {
        File file;
        try {
            file = ResourceManager.getTaggingDirectory();
        } catch (IOException e) {
            ErrorLogger.runtimeExceptionOccurred(e);
            throw new RuntimeException(e);
        } catch (TaggingFolderException e) {
            ErrorLogger.runtimeExceptionOccurred("Could not find folder to tag mp3 files in");
            throw new RuntimeException(e);
        }
        return file.listFiles(filter);
    }

    /**
     * When this function is called, it iterates over all mp3 files in the tagging folder and tags them with
     * an artist tag, title tag, and cover art, of which the last is always performed automatically.
     *
     * @param arrayOfSongs <code>null</code> in case you simply want all files in the tagging folder to
     *                     be tagged, otherwise they can be specified as a <code>File</code> array and
     *                     then only those files will be tagged
     * @throws IOException if an I/O error occurs
     * @throws NoSongFoundException if there is no mp3 file in the tagging folder
     */
    public void tagAllFiles(@Nullable File[] arrayOfSongs) throws IOException, InterruptedException, NotSupportedException, NoSongFoundException {
        File[] songs;
        if (arrayOfSongs == null) {
            File file;
            try {
                file = ResourceManager.getTaggingDirectory();
            } catch (IOException e) {
                ErrorLogger.runtimeExceptionOccurred(e);
                throw new RuntimeException(e);
            } catch (TaggingFolderException e) {
                ErrorLogger.runtimeExceptionOccurred("Could not find folder to tag mp3 files in");
                throw new RuntimeException(e);
            }
            songs = file.listFiles(filter);
        } else {
            songs = arrayOfSongs;
        }
        if (songs != null && songs.length != 0) {
            for (File mp3 : songs) {
                genericTagFile(mp3.getAbsolutePath());
            }
        } else {
            this.logger.println("There are no songs in your tagging folder!");
            throw new NoSongFoundException();
        }
    }

    /**
     * Tags a single generic mp3 file with an artist name, song name and cover art automatically
     * based on the name of the song.
     *
     * @param filePath file path to the mp3 file to be tagged
     * @throws IOException if an I/O error occurs
     */
    public void genericTagFile(String filePath) throws IOException, InterruptedException, NotSupportedException {
        Mp3File mp3file;
        try {
            mp3file = loadMp3File(filePath);
        } catch (Exception e) {
            this.logger.printError("Couldn't find valid cover art, skipping cover art for this file.");
            return;
        }

        ID3v2 id3v2Tag = getId3v2Tag(filePath, mp3file);
        String songName = getSongName(filePath);
        try {
            CoverArtResult coverArtResult = getCoverArt(songName);
            byte[] img = coverArtResult.coverArt();
            id3v2Tag.setAlbumImage(img, MIME_TYPE);
            String vId = coverArtResult.vId();
            id3v2Tag.setComment("vId of cover art:" + vId);
        } catch (VIdException | CoverArtSearchEmptyException e) {
            this.logger.printError("Couldn't find valid cover art, skipping cover art for " + songName);
        }

        saveMP3FileWithCover(filePath, mp3file);
    }

    /**
     * Tags a single mp3 file with an artist name, song name and cover art with the provided vId
     * of the video that should become its cover art.
     *
     * @param filePath file path to the mp3 file to be tagged
     * @param vId vId of the cover art the file is to be tagged with
     * @throws IOException if an I/O error occurs
     */
    public void tagIndividualFile(String filePath, String vId) throws IOException, NotSupportedException {
        Mp3File mp3file = loadMp3File(filePath);
        ID3v2 id3v2Tag = getId3v2Tag(filePath, mp3file);

        byte[] img = getCroppedImageFromVID(vId);
        id3v2Tag.setAlbumImage(img, MIME_TYPE);
        id3v2Tag.setComment("vId of cover art:" + vId);

        saveMP3FileWithCover(filePath, mp3file);
    }

    private static void saveMP3FileWithCover(String filePath, Mp3File mp3file) throws IOException, NotSupportedException {
        File tempMp3File = File.createTempFile("temp", ".mp3");
        mp3file.save(tempMp3File.getAbsolutePath()); // Save to temporary file

        // Replace original file with the temporary file
        Files.move(tempMp3File.toPath(), new File(filePath).toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    private static ID3v2 getId3v2Tag(String filePath, Mp3File mp3file) {
        String songName = getSongName(filePath);

        Logger.getLogger().println("Tagging " + songName + " now...");
        String[] splitSong = songName.split(" - ");
        ID3v2 id3v2Tag = addArtistAndSongname(splitSong, mp3file);
        if (id3v2Tag == null) {
            if (mp3file.hasId3v2Tag()) {
                id3v2Tag = mp3file.getId3v2Tag();
            } else {
                // Mp3 does not have an ID3v2 tag, let's create one
                id3v2Tag = new ID3v24Tag();
                mp3file.setId3v1Tag(id3v2Tag);
            }
        }
        return id3v2Tag;
    }

    private static @NotNull String getSongName(String filePath) {
        String fullSongName = Paths.get(filePath).getFileName().toString();
        return fullSongName.substring(0, fullSongName.length() - 4);
    }

    private static @NotNull Mp3File loadMp3File(String filePath) throws IOException {
        Mp3File mp3file;
        try {
            mp3file = new Mp3File(filePath);
        } catch (InvalidDataException | UnsupportedTagException e) {
            // This should never happen, as this function is only ever called with MP3 files.
            ErrorLogger.runtimeExceptionOccurred(e);
            throw new RuntimeException();
        }
        return mp3file;
    }

    /**
     * Tags an mp3 file with the artist name and song name in their metadata.
     *
     * @param splitSong array of 2 elements. First element is the artist, second
     *                  element is the song title
     * @param mp3file Mp3File object of the mp3 file to be tagged
     * @return ID3v2 tag of the associated mp3 file, ready for more tags to be added
     * to it
     */
    private static ID3v2 addArtistAndSongname(String[] splitSong, Mp3File mp3file) {
        ID3v2 id3v2Tag = null;
        if (splitSong.length == 2) {
            if (mp3file.hasId3v2Tag()) {
                id3v2Tag = mp3file.getId3v2Tag();
            } else {
                // mp3 does not have an ID3v2 tag, let's create one
                id3v2Tag = new ID3v24Tag();
                mp3file.setId3v2Tag(id3v2Tag);
            }
            id3v2Tag.setArtist(splitSong[0]);
            id3v2Tag.setTitle(splitSong[1]);
        } else {
            Logger.getLogger().printError("Could not tag artist and song fields of "
                    + mp3file.getFilename()
                    + " because filename is not in the format of artist - songname");
        }
        return id3v2Tag;
    }

    /**
     * Return type of cover art finding.
     * @param coverArt Cover art used for the song in byte[] format.
     * @param vId vId of the cover art of the song on YouTube.
     */
    private record CoverArtResult(byte[] coverArt, String vId) {}

    /**
     * Finds cover art for a song.
     *
     * @param songName the name of the song you want to find a cover art of
     * @return {@link Tagger.CoverArtResult} cover art byte[] (mimeType jpeg) associated to the corresponding song name
     * @throws CoverArtSearchEmptyException if the cover art finder fails and find
     * no video IDs with an appropriate cover art
     * @throws VIdException if the cover art finder would error on a cover art instance
     */
    private CoverArtResult getCoverArt(String songName) throws IOException, InterruptedException, CoverArtSearchEmptyException, VIdException {
        Path filePath = ResourceManager.getCoverArtPy();
        ProcessBuilder pb = new ProcessBuilder()
                .command("python", "-u", filePath.toString(), songName);
        Process p = pb.start();
        BufferedReader in = new BufferedReader(
                new InputStreamReader(p.getInputStream()));
        String buffer;
        ArrayList<String> fullOutput = new ArrayList<>();
        while ((buffer = in.readLine()) != null){
            fullOutput.add(buffer);
        }
        p.waitFor();
        if (fullOutput.isEmpty()) {
            ErrorLogger.runtimeExceptionOccurred("Cover art searching failed with the Python script returning no stdout output, " +
                    "have you pip installed ytmusicapi?");
            throw new CoverArtSearchEmptyException();
        }
        in.close();
        for (String vId : fullOutput) {
            try {
                byte[] coverArt = getCroppedImageFromVID(vId);
                return new CoverArtResult(coverArt, vId);
            } catch (IOException e) {
                ErrorLogger.runtimeExceptionOccurred(e);
            }
        }
        this.logger.println("No vId found without error-causing image, skipping this song.");
        throw new VIdException();
    }

    /**
     * Given a vId, returns the cropped cover art corresponding to it.
     *
     * @param vId the vId of the cover art to be extracted
     * @return byte[] with the cropped cover art (mimeType jpeg)
     * @throws IOException if an I/O error occurs
     */
    private byte[] getCroppedImageFromVID(String vId) throws IOException {
        final String BASE_URL = "https://i.ytimg.com/vi/" + vId;
        String[] urls = {
                BASE_URL + "/maxresdefault.jpg",
                BASE_URL + "/hq720.jpg",
                BASE_URL + "/hqdefault.jpg"
        };

        IOException last = null;
        BufferedImage img = null;
        for (String u : urls) {
            try {
                img = ImageIO.read(new URL(u));
            } catch (IOException e) {
                last = e;
            }
        }

        if (last != null || img == null) {
            ErrorLogger.runtimeExceptionOccurred(last, "Could not get image from YouTube URL");
            throw new IOException(last);
        }

        int targetWidth = img.getHeight();
        int startX = (img.getWidth() / 2) - (targetWidth / 2);
        BufferedImage croppedImg = img.getSubimage(startX, 0, targetWidth, targetWidth);
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ImageIO.write(croppedImg, "jpg", byteStream);
        return byteStream.toByteArray();
    }
}