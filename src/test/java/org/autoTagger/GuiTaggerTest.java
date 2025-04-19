package org.autoTagger;

import com.mpatric.mp3agic.*;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static org.autoTagger.Tagger.PATH_TO_SONGS;
import static org.mockito.ArgumentMatchers.anyString;

class GuiTaggerTest {

    private Tagger tagger;
    private Downloader downloader;
    private static final String TEST_SONG_URL = "https://www.youtube.com/watch?v=U0TXIXTzJEY";
    private static final String TEST_SONG_NAME = "il vento d'oro.mp3";
    private static final String TEST_SONG_ARTIST = "Yugo Kanno";
    private static final String TEST_SONG_TITLE = "il vento d'oro";
    private static final String TEST_SONG_VID = "U0TXIXTzJEY";
    private static final String TEST_COVER_ART = "CoverArt.jpg";

    @BeforeEach
    void setUp() {

        GuiTagger mockGui = Mockito.mock(GuiTagger.class);
        Logger mockLogger = new Logger(mockGui);
        this.tagger = new Tagger();
        this.downloader = new Downloader();

        Mockito.doNothing().when(mockGui).displayText(anyString());

    }

    @Test
    void individualTagTest() throws IOException, InvalidDataException, UnsupportedTagException, NotSupportedException {
        // Copy test mp3 file to Downloads folder to test application
        File copied = copyTestFileToTagFolder();

        copied = Files.move(copied.toPath(), copied.toPath().resolveSibling(TEST_SONG_ARTIST + " - " + TEST_SONG_TITLE + ".mp3")).toFile();

        // Test if mp3 file is as expected after method is called
        tagger.tagIndividualFile(PATH_TO_SONGS + TEST_SONG_ARTIST + " - " + TEST_SONG_TITLE + ".mp3", TEST_SONG_VID);
        testResultingFile();

        // Clean up
        if (!copied.delete()) {
            System.out.println("Deleting the test file failed!");
        }
    }

    @Test
    void batchTagTest() throws InvalidDataException, UnsupportedTagException, IOException, NoSongFoundException, NotSupportedException, InterruptedException {
        // Copy test mp3 file to Downloads folder to test application
        File copied = copyTestFileToTagFolder();
        copied = Files.move(copied.toPath(), copied.toPath().resolveSibling(TEST_SONG_ARTIST + " - " + TEST_SONG_TITLE + ".mp3")).toFile();

        // Test if mp3 file is as expected after method is called
        tagger.tagAllFiles(new File[]{copied});
        testResultingFile();

        // Clean up
        if (!copied.delete()) {
            System.out.println("Deleting the test file failed!");
        }
    }

    @DisabledIfEnvironmentVariable(named = "CI", matches = "true") // Due to bot-flagging of yt-dlp
    @Test
    void downloadAndTagTest() throws InvalidDataException, UnsupportedTagException, IOException, InterruptedException, NoSongFoundException, NotSupportedException {
        downloader.downloadSongs(TEST_SONG_URL);

        // Test if mp3 file is as expected after method is called
        File song = new File(PATH_TO_SONGS + TEST_SONG_TITLE + ".mp3");
        Path songPath = song.toPath();
        song = Files.move(songPath, songPath.resolveSibling(TEST_SONG_ARTIST + " - " + TEST_SONG_TITLE + ".mp3")).toFile();
        tagger.tagAllFiles(new File[]{song});
        testResultingFile();

        // Clean up
        if (!(new File(PATH_TO_SONGS + TEST_SONG_ARTIST + " - " + TEST_SONG_TITLE + ".mp3")).delete()) {
            System.out.println("Deleting the test file failed!");
        }
    }

    private @NotNull File copyTestFileToTagFolder() throws IOException {
        // Copy test mp3 file to Downloads folder to test application
        File original = new File(URLDecoder.decode(
                Objects.requireNonNull(getClass().getResource("/" + TEST_SONG_NAME)).getPath(),
                StandardCharsets.UTF_8));
        File copied = new File(PATH_TO_SONGS + TEST_SONG_NAME);
        FileUtils.copyFile(original, copied);
        return copied;
    }

    private void testResultingFile() throws InvalidDataException, UnsupportedTagException, IOException {
        Mp3File song = new Mp3File(PATH_TO_SONGS + TEST_SONG_ARTIST + " - " + TEST_SONG_TITLE + ".mp3");
        System.out.println(song);
        if (song.hasId3v2Tag()){
            ID3v2 id3v2tag = song.getId3v2Tag();
            byte[] img = id3v2tag.getAlbumImage();
            String title = id3v2tag.getTitle();
            String artist = id3v2tag.getArtist();
            System.out.println("ID3v2 Title: " + id3v2tag.getTitle());
            System.out.println("ID3v2 Artist: " + id3v2tag.getArtist());
            System.out.println("ID3v2 Album Image: " + (id3v2tag.getAlbumImage() != null));
            File coverArtFile = new File(URLDecoder.decode(
                    Objects.requireNonNull(getClass().getResource("/" + GuiTaggerTest.TEST_COVER_ART)).getPath(),
                    StandardCharsets.UTF_8));
            System.out.println("Cover art file exists? " + coverArtFile.exists());
            byte[] correctCoverArt = FileUtils.readFileToByteArray(coverArtFile);
            Assertions.assertArrayEquals(img, correctCoverArt);
            Assertions.assertEquals(title, TEST_SONG_TITLE);
            Assertions.assertEquals(artist, TEST_SONG_ARTIST);
        } else {
            throw new RuntimeException("mp3 file didn't have a tag");
        }
    }
}