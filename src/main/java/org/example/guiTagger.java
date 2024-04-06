package org.example;

import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.NotSupportedException;
import com.mpatric.mp3agic.UnsupportedTagException;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URISyntaxException;

public class guiTagger extends JFrame {
    private JTextField filePathSong;
    private JButton addCoverForIndividualButton;
    private JButton tagAllFilesInButton;
    private JTextField vIDThumbnail;
    private JPanel MainPanel;

    public guiTagger() {
        setContentPane(MainPanel);
        setTitle("Auto-Tagger by Quinn Caris");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(700, 500);
        setLocationRelativeTo(null);
        setVisible(true);
        tagAllFilesInButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    Tagger.tagAllFiles();
                    JOptionPane.showMessageDialog(guiTagger.this, "Tagging successful!");
                } catch (InvalidDataException | UnsupportedTagException | IOException | URISyntaxException |
                         InterruptedException | NotSupportedException ex) {
                    JOptionPane.showMessageDialog(guiTagger.this, "Something went wrong, sowwy");
                    throw new RuntimeException(ex);
                }
            }
        });

        addCoverForIndividualButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String filePath = filePathSong.getText();
                String vID = vIDThumbnail.getText();
                try {
                    Tagger.tagFile(filePath, true, vID);
                    JOptionPane.showMessageDialog(guiTagger.this, "Tagging successful!");
                } catch (InvalidDataException | UnsupportedTagException | IOException | URISyntaxException |
                         InterruptedException | NotSupportedException ex) {
                    JOptionPane.showMessageDialog(guiTagger.this, "Something went wrong, sowwy");
                    throw new RuntimeException(ex);
                }
            }
        });
    }
}
