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
import java.util.Objects;

import static org.autoTagger.Tagger.PATH_TO_SONGS;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class GuiTaggerTest {

    private guiTagger guiTagger;
    private JTextField mockSongPlaylistURLTextField;
    private JTextField mockFilePathSong;
    private JTextField mockVIdThumbnail;
    private static final String TEST_SONG_URL = "https://youtu.be/ISjNj_-4_QI?si=RlNW2TaIEU_SuvOn";
    private static final String TEST_SONG_NAME = "Noa Klay - I Need Your Love (feat. Dayana).mp3";
    private static final String TEST_SONG_ARTIST = "Noa Klay";
    private static final String TEST_SONG_TITLE = "I Need Your Love (feat. Dayana)";
    private static final String TEST_SONG_VID = "U0TXIXTzJEY";
    private static final String TEST_COVER_ART = "Coverart.jpg";
    private static final String TEST_SPECIFIED_COVER_ART = "SpecifiedCoverArt.jpg";

    @BeforeEach
    void setUp() throws BadLocationException {
        guiTagger = spy(new guiTagger(true));

        // Mock dependencies
        mockSongPlaylistURLTextField = Mockito.mock(JTextField.class);
        mockFilePathSong = Mockito.mock(JTextField.class);
        mockVIdThumbnail = Mockito.mock(JTextField.class);
        JTextPane mockLoadingText = Mockito.mock(JTextPane.class);
        StyledDocument mockStyledDocument = Mockito.mock(StyledDocument.class);
        JCheckBox mockFileRename = Mockito.mock(JCheckBox.class);

        // Set mock objects in guiTagger
        guiTagger.songPlaylistURLTextField = mockSongPlaylistURLTextField;
        guiTagger.filePathSong = mockFilePathSong;
        guiTagger.vIdThumbnail = mockVIdThumbnail;
        guiTagger.loadingText = mockLoadingText;
        guiTagger.fileRename1 = mockFileRename;

        Mockito.doNothing().when(guiTagger).showMessageDialog(any(), anyString());
        when(mockFileRename.isSelected()).thenReturn(false);
        when(mockLoadingText.getStyledDocument()).thenReturn(mockStyledDocument);
        Mockito.doNothing().when(mockStyledDocument).insertString(anyInt(), anyString(), any());
    }

    @Test
    void individualTagTest() throws IOException, InvalidDataException, UnsupportedTagException {
        // Copy test mp3 file to Downloads folder to test application
        File copied = copyTestFileToTagFolder();

        // Set up mock behavior
        when(mockFilePathSong.getText()).thenReturn(PATH_TO_SONGS + TEST_SONG_NAME);
        when(mockVIdThumbnail.getText()).thenReturn(TEST_SONG_VID);

        // Test if mp3 file is as expected after method is called
        guiTagger.addCoverForIndividualFile();
        testResultingFile(TEST_SPECIFIED_COVER_ART);

        // Clean up
        if (!copied.delete()) {
            System.out.println("Deleting the test file failed!");
        }
    }

    @Test
    void batchTagTest() throws InvalidDataException, UnsupportedTagException, IOException {
        // Copy test mp3 file to Downloads folder to test application
        File copied = copyTestFileToTagFolder();

        // Test if mp3 file is as expected after method is called
        guiTagger.tagAllFiles();
        testResultingFile(TEST_COVER_ART);

        // Clean up
        if (!copied.delete()) {
            System.out.println("Deleting the test file failed!");
        }
    }

    @Test
    void downloadAndTagTest() throws InvalidDataException, UnsupportedTagException, IOException, InterruptedException {
        when(mockSongPlaylistURLTextField.getText()).thenReturn(TEST_SONG_URL);
        guiTagger.downloadSongs();

        // Test if mp3 file is as expected after method is called
        File song = new File(PATH_TO_SONGS + TEST_SONG_TITLE + ".mp3");
        if (!song.renameTo(new File(PATH_TO_SONGS + TEST_SONG_ARTIST + " - " + TEST_SONG_TITLE + ".mp3"))) {
            throw new RuntimeException("Song rename failed!");
        }
        guiTagger.tagAllFiles();
        testResultingFile(TEST_COVER_ART);

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

    private void testResultingFile(String coverArt) throws InvalidDataException, UnsupportedTagException, IOException {
        Mp3File song = new Mp3File(PATH_TO_SONGS + TEST_SONG_NAME);
        if (song.hasId3v2Tag()){
            ID3v2 id3v2tag = song.getId3v2Tag();
            byte[] img = id3v2tag.getAlbumImage();
            String title = id3v2tag.getTitle();
            String artist = id3v2tag.getArtist();
            File coverArtFile = new File(URLDecoder.decode(
                    Objects.requireNonNull(getClass().getResource("/" + coverArt)).getPath(),
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