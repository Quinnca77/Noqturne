package org.autoTagger;

import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.UnsupportedTagException;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;
import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static org.autoTagger.Tagger.PATH_TO_SONGS;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class GuiTaggerTest {

    private GuiTagger guiTagger;
    private JTextField mockFilePathSong;
    private JTextField mockVIdThumbnail;
    private static final String TEST_SONG_URL = "https://www.youtube.com/watch?v=U0TXIXTzJEY";
    private static final String TEST_SONG_NAME = "il vento d'oro.mp3";
    private static final String TEST_SONG_ARTIST = "Yugo Kanno";
    private static final String TEST_SONG_TITLE = "il vento d'oro";
    private static final String TEST_SONG_VID = "U0TXIXTzJEY";
    private static final String TEST_COVER_ART = "CoverArt.jpg";

    @BeforeEach
    void setUp() throws BadLocationException {
        guiTagger = spy(new GuiTagger(true));

        // Mock dependencies
        JTextField mockSongPlaylistURLTextField = Mockito.mock(JTextField.class);
        mockFilePathSong = Mockito.mock(JTextField.class);
        mockVIdThumbnail = Mockito.mock(JTextField.class);
        JTextPane mockLoadingText = Mockito.mock(JTextPane.class);
        StyledDocument mockStyledDocument = Mockito.mock(StyledDocument.class);

        // Set mock objects in guiTagger
        guiTagger.songPlaylistURLTextField = mockSongPlaylistURLTextField;
        guiTagger.filePathSong = mockFilePathSong;
        guiTagger.vIdThumbnail = mockVIdThumbnail;
        guiTagger.loadingText = mockLoadingText;

        Mockito.doNothing().when(guiTagger).showMD(any(), anyString());
        guiTagger.renameState = false;
        when(mockLoadingText.getStyledDocument()).thenReturn(mockStyledDocument);
        Mockito.doNothing().when(mockStyledDocument).insertString(anyInt(), anyString(), any());
    }

    @Test
    void individualTagTest() throws IOException, InvalidDataException, UnsupportedTagException {
        // Copy test mp3 file to Downloads folder to test application
        File copied = copyTestFileToTagFolder();

        // Set up mock behavior
        when(mockFilePathSong.getText()).thenReturn(PATH_TO_SONGS + TEST_SONG_ARTIST + " - " + TEST_SONG_TITLE + ".mp3");
        when(mockVIdThumbnail.getText()).thenReturn(TEST_SONG_VID);

        copied = Files.move(copied.toPath(), copied.toPath().resolveSibling(TEST_SONG_ARTIST + " - " + TEST_SONG_TITLE + ".mp3")).toFile();

        // Test if mp3 file is as expected after method is called
        guiTagger.addCoverForIndividualFile();
        testResultingFile();

        // Clean up
        if (!copied.delete()) {
            System.out.println("Deleting the test file failed!");
        }
    }

    @Test
    void batchTagTest() throws InvalidDataException, UnsupportedTagException, IOException {
        // Copy test mp3 file to Downloads folder to test application
        File copied = copyTestFileToTagFolder();
        copied = Files.move(copied.toPath(), copied.toPath().resolveSibling(TEST_SONG_ARTIST + " - " + TEST_SONG_TITLE + ".mp3")).toFile();

        // Test if mp3 file is as expected after method is called
        guiTagger.tagAllFiles(new File[]{copied});
        testResultingFile();

        // Clean up
        if (!copied.delete()) {
            System.out.println("Deleting the test file failed!");
        }
    }

    @Test
    void downloadAndTagTest() throws InvalidDataException, UnsupportedTagException, IOException, InterruptedException {
        new Downloader().downloadSongs(TEST_SONG_URL);

        // Test if mp3 file is as expected after method is called
        File song = new File(PATH_TO_SONGS + TEST_SONG_TITLE + ".mp3");
        Path songPath = song.toPath();
        song = Files.move(songPath, songPath.resolveSibling(TEST_SONG_ARTIST + " - " + TEST_SONG_TITLE + ".mp3")).toFile();
        guiTagger.tagAllFiles(new File[]{song});
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
        if (song.hasId3v2Tag()){
            ID3v2 id3v2tag = song.getId3v2Tag();
            byte[] img = id3v2tag.getAlbumImage();
            String title = id3v2tag.getTitle();
            String artist = id3v2tag.getArtist();
            File coverArtFile = new File(URLDecoder.decode(
                    Objects.requireNonNull(getClass().getResource("/" + GuiTaggerTest.TEST_COVER_ART)).getPath(),
                    StandardCharsets.UTF_8));
            byte[] correctCoverArt = FileUtils.readFileToByteArray(coverArtFile);
            Assertions.assertArrayEquals(img, correctCoverArt);
            Assertions.assertEquals(title, TEST_SONG_TITLE);
            Assertions.assertEquals(artist, TEST_SONG_ARTIST);
        } else {
            throw new RuntimeException("mp3 file didn't have a tag");
        }
    }
}