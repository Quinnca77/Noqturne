package org.example;

import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.NotSupportedException;
import com.mpatric.mp3agic.UnsupportedTagException;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static org.example.Tagger.PATH_TO_SONGS;
import static org.example.Tagger.getAllFiles;

public class guiTagger extends JFrame {
    private JTextField filePathSong;
    private JButton addCoverForIndividualButton;
    private JButton tagAllFilesInButton;
    private JTextField vIDThumbnail;
    private JPanel MainPanel;
    private JCheckBox fileRename;
    private static final JTextField artistNameInput = new JTextField(10);
    private static final JTextField songNameInput = new JTextField(10);

    public guiTagger() {
        setContentPane(MainPanel);
        setTitle("Auto-Tagger by Quinn Caris");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(700, 500);
        setLocationRelativeTo(null);
        setVisible(true);
        tagAllFilesInButton.addActionListener(e -> {
            if (fileRename.isSelected()) {
                File[] songs = getAllFiles();
                for (File song : songs) {
                    JPanel fields = getFields(song.getName());

                    int result = JOptionPane.showConfirmDialog(guiTagger.this, fields, "Rename file", JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
                    switch (result) {
                        case JOptionPane.OK_OPTION:
                            if (!(artistNameInput.getText().isEmpty() && songNameInput.getText().isEmpty())) {
                                song.renameTo(new File(PATH_TO_SONGS + artistNameInput.getText() + " - " + songNameInput.getText() + ".mp3"));
                            }
                            break;

                        case JOptionPane.CANCEL_OPTION:
                            break;
                    }
                }
            }
            try {
                Tagger.tagAllFiles();
                JOptionPane.showMessageDialog(guiTagger.this, "Tagging successful!");
            } catch (InvalidDataException | UnsupportedTagException | IOException | URISyntaxException |
                     InterruptedException | NotSupportedException ex) {
                JOptionPane.showMessageDialog(guiTagger.this, "Something went wrong, sowwy");
                throw new RuntimeException(ex);
            } catch (NoSongFoundException exc) {
                JOptionPane.showMessageDialog(guiTagger.this, "No songs found in Downloads folder!");
            } catch (VideoIdEmptyException exce) {
                JOptionPane.showMessageDialog(guiTagger.this, "No song online found that corresponds with these fields!");
            }
        });

        addCoverForIndividualButton.addActionListener(e -> {
            String filePath = filePathSong.getText().replaceAll("\"", "");
            String vID = vIDThumbnail.getText();
            if (fileRename.isSelected()) {
                File song = new File(filePath);
                JPanel fields = getFields(song.getName());

                int result = JOptionPane.showConfirmDialog(guiTagger.this, fields, "Rename file", JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
                switch (result) {
                    case JOptionPane.OK_OPTION:
                        if (!(artistNameInput.getText().isEmpty() && songNameInput.getText().isEmpty())) {
                            filePath = PATH_TO_SONGS + artistNameInput.getText() + " - " + songNameInput.getText() + ".mp3";
                            song.renameTo(new File(filePath));
                        }
                        break;

                    case JOptionPane.CANCEL_OPTION:
                        break;
                }
            }
            try {
                Tagger.tagFile(filePath, true, vID);
                JOptionPane.showMessageDialog(guiTagger.this, "Tagging successful!");
            } catch (InvalidDataException | UnsupportedTagException | IOException | URISyntaxException |
                     InterruptedException | NotSupportedException ex) {
                JOptionPane.showMessageDialog(guiTagger.this, "Something went wrong, sowwy");
                throw new RuntimeException(ex);
            } catch (VideoIdEmptyException exce) {
                JOptionPane.showMessageDialog(guiTagger.this, "No song online found that corresponds with these fields!");
            }
        });
    }

    private static @NotNull JPanel getFields(String fileName) {
        JPanel mainPanel = new JPanel(new BorderLayout());
        JPanel title = new JPanel();
        JPanel fields = new JPanel(new GridLayout(2, 2));
        JLabel artistName = new JLabel("Artist name:");
        JLabel songName = new JLabel("Song name:");
        JTextField song = new JTextField(fileName);
        song.setBorder(null);
        song.setOpaque(false);
        song.setEditable(false);

        title.add(song);
        fields.add(artistName);
        fields.add(artistNameInput);
        fields.add(songName);
        fields.add(songNameInput);
        mainPanel.add(title, BorderLayout.NORTH);
        mainPanel.add(fields, BorderLayout.CENTER);
        return mainPanel;
    }
}
