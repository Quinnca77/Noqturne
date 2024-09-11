package org.example;

import com.mpatric.mp3agic.*;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;

/**
 * Class for all functionalities associated with tagging an mp3 file in the Downloads folder.
 */
public class Tagger {

    public static final String PATH_TO_SONGS = "C:\\Users\\" + System.getProperty("user.name") + "\\Downloads\\";
    // File filter for sorting mp3 files
    static FileFilter filter = file -> file.getName().endsWith(".mp3");
    Logger logger;

    public Tagger() {
        this.logger = Logger.getLogger();
    }

    /**
     * Gets all mp3 files in the Downloads folder.
     * @return a File array with mp3 files
     */
    public static File[] getAllFiles() {
        File file = new File(PATH_TO_SONGS);
        return file.listFiles(filter);
    }

    /**
     * Most important function of this application. When this function is called,
     * it iterates over all mp3 files in the Downloads folder and tags them with
     * an artist tag, title tag, and cover art, of which the last is always performed
     * automatically.
     *
     * @throws IOException if file permissions are not configured as expected
     * @throws URISyntaxException if input is not a valid URI
     * @throws NoSongFoundException if there is no mp3 file in the Downloads folder
     * @throws VideoIdEmptyException if the cover art finder fails and find
     * no video IDs with an appropriate cover art
     */
    public void tagAllFiles() throws IOException, URISyntaxException, InterruptedException, NotSupportedException, NoSongFoundException, VideoIdEmptyException {
        File file = new File(PATH_TO_SONGS);
        File[] songs = file.listFiles(filter);
        if (songs != null && songs.length != 0) {
            for (File mp3 : songs) {
                tagFile(mp3.getAbsolutePath(), false, null);
            }
        } else {
            this.logger.println("There are no songs in your downloads folder!");
            throw new NoSongFoundException();
        }
    }

    // TODO split this function into two because doing it with a boolean variable is not optimal
    /**
     * Tags a single mp3 file with an artist name, song name and cover art.
     * @param filePath file path to the mp3 file to be tagged
     * @param individual whether this function is called for an individual file or not
     * @param vId vId of the cover art the file is to be tagged with (only used if
     *            "individual" is true)
     * @throws IOException if file permissions are not configured as expected
     * @throws URISyntaxException if input is not a valid URI
     * @throws VideoIdEmptyException if the cover art finder fails and find
     * no video IDs with an appropriate cover art
     */
    public void tagFile(String filePath, boolean individual, String vId) throws IOException, URISyntaxException, InterruptedException, NotSupportedException, VideoIdEmptyException {
        ID3v2 id3v2Tag;
        Mp3File mp3file;
        try {
            mp3file = new Mp3File(filePath);
        } catch (InvalidDataException | UnsupportedTagException e) {
            // This should never happen, as this function is only ever called with MP3 files.
            ErrorLogger.runtimeExceptionOccurred(e);
            throw new RuntimeException(e);
        }
        String songName = mp3file.getFilename().substring(PATH_TO_SONGS.length(), mp3file.getFilename().length() - 4);
        this.logger.println("Tagging " + songName + " now...");
        String[] splitSong = songName.split(" - ");
        id3v2Tag = addArtistAndSongname(splitSong, mp3file);
        if (id3v2Tag == null) {
            if (mp3file.hasId3v2Tag()) {
                id3v2Tag = mp3file.getId3v2Tag();
            } else {
                // mp3 does not have an ID3v2 tag, let's create one
                id3v2Tag = new ID3v24Tag();
                mp3file.setId3v1Tag(id3v2Tag);
            }
        }
        File img;
        try {
            if (individual) {
                img = getCroppedImageFromVID(vId);
            } else {
                img = getCoverArt(songName);
            }
            byte[] bytes = FileUtils.readFileToByteArray(img);
            String mimeType = Files.probeContentType(img.toPath());
            id3v2Tag.setAlbumImage(bytes, mimeType);
        } catch (VIdException e) {
            this.logger.println("Couldn't find valid cover art, skipping cover art for " + songName);
        }
        
        File tempMp3File = File.createTempFile("temp", ".mp3");
        mp3file.save(tempMp3File.getAbsolutePath()); // Save to temporary file

        // Replace original file with the temporary file
        Files.move(tempMp3File.toPath(), new File(filePath).toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Tags an mp3 file with the artist name and song name in their metadata.
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
        }
        return id3v2Tag;
    }

    // TODO if no decent match is found (hamming distance song title), return special value and get artist picture instead
    /**
     * Finds cover art for a song.
     * @param songName the name of the song you want to find a cover art of
     * @return cover art File associated to the corresponding song name
     * @throws VideoIdEmptyException if the cover art finder fails and find
     * no video IDs with an appropriate cover art
     * @throws VIdException if the cover art finder would error on a cover art instance
     */
    public File getCoverArt(String songName) throws IOException, InterruptedException, VideoIdEmptyException, VIdException {
        String filePath = "coverArt.py";
        ProcessBuilder pb = new ProcessBuilder()
                .command("python", "-u", filePath, songName);
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
            throw new VideoIdEmptyException();
        }
        in.close();
        for (String vId : fullOutput) {
            try {
                return getCroppedImageFromVID(vId);
            } catch (IOException ignored) {

            }
        }
        this.logger.println("No vId found without error-causing image, skipping this song.");
        throw new VIdException();
    }

    /**
     * Given a vId, returns the cropped cover art corresponding to it.
     * @param vId the vId of the cover art to be extracted
     * @return an image File with the cropped cover art
     * @throws IOException if file permissions are not configured as expected
     */
    private @NotNull File getCroppedImageFromVID(String vId) throws IOException {
        URL url = new URL("https://i.ytimg.com/vi/" + vId + "/maxresdefault.jpg");
        File img = new File("img.jpg");
        try {
            FileUtils.copyURLToFile(url, img);
        } catch (FileNotFoundException e) {
            try {
                url = new URL("https://i.ytimg.com/vi/" + vId + "/hq720.jpg");
                FileUtils.copyURLToFile(url, img);
            } catch (FileNotFoundException ex) {
                url = new URL("https://i.ytimg.com/vi/" + vId + "/hqdefault.jpg");
                FileUtils.copyURLToFile(url, img);
            }
        }
        BufferedImage bufferedImg = ImageIO.read(img);
        int targetWidth = bufferedImg.getHeight();
        int startX = (bufferedImg.getWidth() / 2) - (targetWidth / 2);
        BufferedImage croppedImage = bufferedImg.getSubimage(startX, 0, targetWidth, targetWidth);
        File outputFile = new File("croppedImage.jpg");
        ImageIO.write(croppedImage, "jpg", outputFile);
        return outputFile;
    }
}