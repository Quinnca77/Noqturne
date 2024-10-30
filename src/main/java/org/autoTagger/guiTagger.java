package org.autoTagger;

import com.mpatric.mp3agic.NotSupportedException;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import static org.autoTagger.Tagger.PATH_TO_SONGS;
import static org.autoTagger.Tagger.getAllFiles;

// TODO document this class
public class guiTagger extends JFrame {
    protected JButton tagAllFilesInButton;
    protected JPanel MainPanel;
    protected JCheckBox fileRename;
    protected JTextField songPlaylistURLTextField;
    protected JButton downloadAndTagSongButton;
    protected JTextPane loadingText;
    protected JTextField filePathSong;
    protected JTextField vIdThumbnail;
    protected JButton addCoverForIndividualButton;
    protected JTextField artistNameInput = new JTextField(10);
    protected JTextField songNameInput = new JTextField(10);
    protected final Logger logger;
    protected final Tagger tagger;
    protected final Downloader downloader;

    // Constructor that considers testing
    public guiTagger(boolean testing) {
        new Logger(this);
        this.logger = Logger.getLogger();
        this.tagger = new Tagger();
        this.downloader = new Downloader();
        if (!testing) {
            initializeGUI();
        }
    }

    // Show GUI and initialize its fields
    private void initializeGUI() {
        setContentPane(MainPanel);
        setTitle("Auto-Tagger by Quinn Caris");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(700, 500);
        setLocationRelativeTo(null);

        URL iconURL = getClass().getResource("/icon.png");
        if (iconURL != null) {
            ImageIcon icon = new ImageIcon(iconURL);
            this.setIconImage(icon.getImage());
        }

        setVisible(true);
        tagAllFilesInButton.addActionListener(e -> invokeTagAllFiles());
        downloadAndTagSongButton.addActionListener(e -> invokeDownloadAndTag());
        addCoverForIndividualButton.addActionListener(e -> invokeIndividualTag());
    }

    protected void addCoverForIndividualFile() {
        String filePath = filePathSong.getText().replaceAll("\"", "");
        if (filePath.isEmpty()) {
            showMessageDialog(guiTagger.this, "Please put in a file path when using this option");
            return;
        }
        String vId = vIdThumbnail.getText();
        if (fileRename.isSelected()) {
            File song = new File(filePath);
            JPanel fields = getFields(song.getName());

            int result = JOptionPane.showConfirmDialog(guiTagger.this, fields, "Rename file", JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
            switch (result) {
                case JOptionPane.OK_OPTION:
                    if (!(artistNameInput.getText().isEmpty() && songNameInput.getText().isEmpty())) {
                        filePath = PATH_TO_SONGS + artistNameInput.getText() + " - " + songNameInput.getText() + ".mp3";
                        if (!song.renameTo(new File(filePath))) {
                            this.logger.println("Renaming song failed!");
                        }
                    }
                    break;

                case JOptionPane.CANCEL_OPTION:
                    break;
            }
        }
        try {
            tagger.tagIndividualFile(filePath, vId);
            showMessageDialog(guiTagger.this, "Tagging successful!");
        } catch (IOException | NotSupportedException e) {
            ErrorLogger.runtimeExceptionOccurred(e);
            showMessageDialog(guiTagger.this, "Something went wrong, please contact the developer.\nError code 01");
            throw new RuntimeException(e);
        }
    }

    private @NotNull JPanel getFields(String fileName) {
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
        mainPanel.setPreferredSize(new Dimension(800,60));
        return mainPanel;
    }

    protected void tagAllFiles() {
        if (fileRename.isSelected()) {
            File[] songs = getAllFiles();
            for (File song : songs) {
                JPanel fields = getFields(song.getName());

                int result = JOptionPane.showConfirmDialog(guiTagger.this, fields, "Rename file", JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
                switch (result) {
                    case JOptionPane.OK_OPTION:
                        if (!(artistNameInput.getText().isEmpty() && songNameInput.getText().isEmpty())) {
                            if (!song.renameTo(new File(PATH_TO_SONGS + artistNameInput.getText() + " - " + songNameInput.getText() + ".mp3"))) {
                                this.logger.println("Renaming song failed!");
                            }
                        }
                        break;

                    case JOptionPane.CANCEL_OPTION:
                        break;
                }
            }
        }
        try {
            this.tagger.tagAllFiles();
            showMessageDialog(guiTagger.this, "Tagging successful!");
        } catch (IOException |
                 InterruptedException | NotSupportedException e) {
            ErrorLogger.runtimeExceptionOccurred(e);
            showMessageDialog(guiTagger.this, "Something went wrong, please contact the developer.\nError code 02");
            throw new RuntimeException(e);
        } catch (NoSongFoundException e) {
            showMessageDialog(guiTagger.this, "No songs found in Downloads folder!");
        } catch (VideoIdEmptyException e) {
            showMessageDialog(guiTagger.this, "No song online found that corresponds with these fields!");
        }
    }

    protected void invokeTagAllFiles() {
        new AbstractWorker(this) {
            @Override
            protected void beginTask() {
                Logger.getLogger().println("Starting tagging...");
            }
            @Override
            protected void executeTask() {
                tagAllFiles();
            }
            @Override
            protected void taskCompleted() {
                Logger.getLogger().println("Tagging complete!");
            }
        }.execute();
    }

    protected void invokeDownloadAndTag() {
        new AbstractWorker(this) {
            @Override
            protected void beginTask() {
                Logger.getLogger().println("Starting download...");
            }
            @Override
            protected void executeTask() {
                try {
                    downloadSongs();
                } catch (IOException | InterruptedException e) {
                    showMessageDialog(guiTagger.this,
                            "Something went wrong, please contact the developer.\nError code 03");
                }
            }
            @Override
            protected void taskCompleted() {
                Logger.getLogger().println("Download complete.");
                invokeTagAllFiles();
            }
        }.execute();
    }

    protected void invokeIndividualTag() {
        new AbstractWorker(this) {
            @Override
            protected void beginTask() {
                Logger.getLogger().println("Starting tagging...");
            }
            @Override
            protected void executeTask() {
                addCoverForIndividualFile();
            }
            @Override
            protected void taskCompleted() {
                Logger.getLogger().println("Tagging complete!");
            }
        }.execute();
    }

    protected void showMessageDialog(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message);
    }

    protected void downloadSongs() throws IOException, InterruptedException {
        System.out.println(songPlaylistURLTextField.getText());
        downloader.downloadSongs(songPlaylistURLTextField.getText());
    }

    public void displayText(String string) {
        SwingUtilities.invokeLater(() -> {
            StyledDocument doc = loadingText.getStyledDocument();
            try {
                doc.insertString(doc.getLength(), string, null);
            } catch (BadLocationException exc) {
                showMessageDialog(guiTagger.this, "Something went wrong, please contact the developer.\nError code 04");
            }
        });
    }
}
