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

public class Tagger {

    public static final String PATH_TO_SONGS = "C:\\Users\\" + System.getProperty("user.name") + "\\Downloads\\";
    // file filter for sort mp3 files
    static FileFilter filter = file -> file.getName().endsWith(".mp3");
    Logger logger;

    public Tagger() {
        this.logger = Logger.getLogger();
    }

    public static File[] getAllFiles() {
        File file = new File(PATH_TO_SONGS);
        return file.listFiles(filter);
    }

    public void tagAllFiles() throws InvalidDataException, UnsupportedTagException, IOException, URISyntaxException, InterruptedException, NotSupportedException, NoSongFoundException, VideoIdEmptyException {
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

    public void tagFile(String filePath, boolean individual, String vID) throws InvalidDataException, UnsupportedTagException, IOException, URISyntaxException, InterruptedException, NotSupportedException, VideoIdEmptyException {
        ID3v2 id3v2Tag;
        Mp3File mp3file = new Mp3File(filePath);
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
        if (individual) {
            img = getCroppedImageFromVID(vID);
        } else {
            img = getCoverArt(songName);
        }
        byte[] bytes = FileUtils.readFileToByteArray(img);
        String mimeType = Files.probeContentType(img.toPath());
        id3v2Tag.setAlbumImage(bytes, mimeType);

        File tempMp3File = File.createTempFile("temp", ".mp3");
        mp3file.save(tempMp3File.getAbsolutePath()); // Save to temporary file

        // Replace original file with the temporary file
        Files.move(tempMp3File.toPath(), new File(filePath).toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    private static ID3v2 addArtistAndSongname(String[] splitSong, Mp3File mp3file) {
        ID3v2 id3v2Tag = null;
        if (splitSong.length == 2) {
            if (mp3file.hasId3v2Tag()) {
                id3v2Tag = mp3file.getId3v2Tag();
            } else {
                // mp3 does not have an ID3v2 tag, let's create one
                id3v2Tag = new ID3v24Tag();
                mp3file.setId3v1Tag(id3v2Tag);
            }
            id3v2Tag.setArtist(splitSong[0]);
            id3v2Tag.setTitle(splitSong[1]);
        }
        return id3v2Tag;
    }

    // TODO if no decent match is found (hamming distance song title), return special value and get artist picture instead
    public static File getCoverArt(String songName) throws IOException, InterruptedException, VideoIdEmptyException {
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
        for (String vID : fullOutput) {
            try {
                return getCroppedImageFromVID(vID);
            } catch (IOException ignored) {

            }
        }
        throw new IOException("No vID found without error-causing image");
    }

    private static @NotNull File getCroppedImageFromVID(String vID) throws IOException {
        URL url = new URL("https://i.ytimg.com/vi/" + vID + "/maxresdefault.jpg");
        File img = new File("img.jpg");
        try {
            FileUtils.copyURLToFile(url, img);
        } catch (FileNotFoundException e) {
            try {
                url = new URL("https://i.ytimg.com/vi/" + vID + "/hq720.jpg");
                FileUtils.copyURLToFile(url, img);
            } catch (FileNotFoundException ex) {
                url = new URL("https://i.ytimg.com/vi/" + vID + "/hqdefault.jpg");
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