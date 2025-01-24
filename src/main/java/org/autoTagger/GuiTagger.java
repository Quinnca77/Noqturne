package org.autoTagger;

import com.mpatric.mp3agic.NotSupportedException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.autoTagger.Tagger.getAllMp3Files;

/**
 * Class for the GUI of the app. Everything is made with Java swing.
 */
public class GuiTagger extends JFrame {
    protected JButton tagAllFilesInButton;
    protected JPanel MainPanel;
    protected JCheckBox fileRename1;
    protected JCheckBox fileRename2;
    protected JCheckBox fileRename3;
    protected JTextField songPlaylistURLTextField;
    protected JButton downloadAndTagSongButton;
    protected JTextPane loadingText;
    protected JTextField filePathSong;
    protected JTextField vIdThumbnail;
    protected JButton addCoverForIndividualButton;
    protected JTabbedPane tabbedPane;
    protected JTextField artistNameInput = new JTextField(10);
    protected JTextField songNameInput = new JTextField(10);
    protected final Logger logger;
    protected final Tagger tagger;
    protected final Downloader downloader;
    protected boolean renameState = true;

    /**
     * Calling this constructor will create and show the GUI of the auto-tagger.
     *
     * @param testing boolean value of whether this class instance is being tested or not.
     *                if it is set to true, the GUI will not be shown, and instead only the
     *                necessary fields are initialized.
     */
    public GuiTagger(boolean testing) {
        new Logger(this);
        this.logger = Logger.getLogger();
        this.tagger = new Tagger();
        this.downloader = new Downloader();
        if (!testing) {
            initializeGUI();
        }
    }

    /**
     * Initialize and show GUI while binding its buttons to the relevant functions.
     */
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

        tagAllFilesInButton.addActionListener(e -> invokeTagAllFiles(null));
        downloadAndTagSongButton.addActionListener(e -> invokeDownloadAndTag());
        addCoverForIndividualButton.addActionListener(e -> invokeIndividualTag());
        linkCheckboxes();
        setVisible(true);
    }

    /**
     * Makes sure the "Rename file(s)?" checkboxes will update the persistent state field of this class.
     */
    private void linkCheckboxes() {
        fileRename1.addActionListener(e -> {
            renameState = fileRename1.isSelected();
            refreshCheckboxes();
        });
        fileRename2.addActionListener(e -> {
            renameState = fileRename2.isSelected();
            refreshCheckboxes();
        });
        fileRename3.addActionListener(e -> {
            renameState = fileRename3.isSelected();
            refreshCheckboxes();
        });
    }

    /**
     * Called when a checkbox changes state. Will set the other checkboxes to the same state.
     */
    private void refreshCheckboxes() {
        fileRename1.setSelected(renameState);
        fileRename2.setSelected(renameState);
        fileRename3.setSelected(renameState);
    }

    /**
     * Tags a single file, even if it isn't in the user's "Downloads" folder.
     * Prerequisites: <ul>
     *     <li>Absolute filepath to file on computer</li>
     *     <li>vId of the cover you want the individual file to have. If this isn't
     *     specified, it will instead use automatic cover art searching.</li>
     * </ul>
     */
    protected void addCoverForIndividualFile() {
        String filePath = filePathSong.getText().replaceAll("\"", "");
        if (filePath.isEmpty()) {
            showMD(GuiTagger.this, "Please put in a file path when using this option");
            return;
        }
        String vId = vIdThumbnail.getText();
        File song = new File(filePath);
        if (renameState) {
            song = renameSong(song);
        }
        try {
            if (vId.isEmpty()) {
                tagger.genericTagFile(song.getPath());
            } else {
                tagger.tagIndividualFile(song.getPath(), vId);
            }
            showMD(GuiTagger.this, "Tagging successful!");
        } catch (IOException | NotSupportedException | InterruptedException e) {
            ErrorLogger.runtimeExceptionOccurred(e);
            showMD(GuiTagger.this, "Something went wrong, please contact the developer.\nError code 01");
            throw new RuntimeException(e);
        }
    }

    /**
     * Tags all mp3 files in user's "Downloads" folder. Different file locations can be specified in the
     * parameter by making each file to be tagged a <code>File</code> object.
     *
     * @param arrayOfSongs array of <code>File</code> objects to tag.
     *                    <code>null</code> to tag all files in "Downloads" folder.
     */
    protected void tagAllFiles(@Nullable File[] arrayOfSongs) {
        File[] songs = arrayOfSongs;
        if (renameState) {
            if (arrayOfSongs == null) {
                songs = getAllMp3Files();
            }
            for (int i = 0; i < songs.length; i++) {
                songs[i] = renameSong(songs[i]);
            }
        }
        try {
            if (renameState || arrayOfSongs != null) {
                this.tagger.tagAllFiles(songs);
            } else {
                this.tagger.tagAllFiles(null);
            }
            showMD(GuiTagger.this, "Tagging successful!");
        } catch (IOException |
                 InterruptedException | NotSupportedException e) {
            ErrorLogger.runtimeExceptionOccurred(e);
            showMD(GuiTagger.this, "Something went wrong, please contact the developer.\nError code 02");
            throw new RuntimeException(e);
        } catch (NoSongFoundException e) {
            showMD(GuiTagger.this, "No songs found in Downloads folder!");
        }
    }

    /**
     * Helper function to rename a song by the user when they checked "Rename file(s)?".
     * This function will prompt the user with the artist and song name, after which it will rename
     * the mp3 file in the file system.
     *
     * @param song a File object that points to the mp3 file that is the song to be renamed.
     * @return a File object representing the new renamed song file.
     */
    private File renameSong(File song) {
        JPanel fields = getFieldsRenameDialog(song.getName());
        Path songPath = song.toPath();

        int result = JOptionPane.showConfirmDialog(GuiTagger.this, fields, "Rename file", JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
        switch (result) {
            case JOptionPane.OK_OPTION:
                if (!(artistNameInput.getText().isEmpty() && songNameInput.getText().isEmpty())) {
                    try {
                        song = Files.move(songPath, songPath.resolveSibling(artistNameInput.getText() + " - " + songNameInput.getText() + ".mp3")).toFile();
                    } catch (IOException e) {
                        this.logger.println("Renaming song failed!");
                    }
                }
                break;

            case JOptionPane.CANCEL_OPTION:
                break;
        }
        return song;
    }

    /**
     * Initializes the UI for the rename prompt box.
     *
     * @param fileName the name of the mp3 file to be renamed.
     * @return a JPanel which is perfect for a prompt box.
     */
    private @NotNull JPanel getFieldsRenameDialog(String fileName) {
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

    /**
     * Method runs when the "tag all files" button is pressed. This runs the associated
     * functionality {@link #tagAllFiles(File[])} but also makes sure the user is informed on the progress
     * via the console on the right of the UI.
     *
     * @param arrayOfSongs if applicable, the array of <code>File</code> objects that points to
     *                    the songs to be tagged. <code>null</code> to tag all songs in "Downloads" folder.
     */
    protected void invokeTagAllFiles(@Nullable File[] arrayOfSongs) {
        new AbstractWorker(this) {
            @Override
            protected void beginTask() {
                logger.println("Starting tagging...");
            }
            @Override
            protected void executeTask() {
                tagAllFiles(arrayOfSongs);
            }
            @Override
            protected void taskCompleted() {
                logger.println("Tagging complete!");
            }
        }.execute();
    }

    /**
     * Method runs when the "download and tag" button is pressed. This downloads all the songs by
     * calling the associated function, and afterward calls {@link #invokeTagAllFiles(File[])} to
     * tag the recently downloaded files. While doing this, it will report its progress via
     * the console on the right of the UI.
     * <p>
     * Note that if a user already has mp3 files in their "Downloads" folder before calling this function,
     * they will be re-tagged!!!
     */
    protected void invokeDownloadAndTag() {
        new AbstractWorker(this) {
            private File[] arrayOfSongs;
            @Override
            protected void beginTask() {
                logger.println("Starting download...");
            }
            @Override
            protected void executeTask() {
                try {
                    arrayOfSongs = downloader.downloadSongs(songPlaylistURLTextField.getText());
                } catch (IOException | InterruptedException e) {
                    ErrorLogger.runtimeExceptionOccurred(e);
                    showMD(GuiTagger.this,
                            "Something went wrong, please contact the developer.\nError code 03");
                    throw new RuntimeException(e);
                }
            }
            @Override
            protected void taskCompleted() {
                logger.println("Download complete.");
                invokeTagAllFiles(arrayOfSongs);
            }
        }.execute();
    }

    /**
     * Method runs when the "add cover for individual file" button is pressed. This will simply run
     * the associated functionality {@link #addCoverForIndividualFile()} and report its progress
     * via the console on the right of the UI.
     */
    protected void invokeIndividualTag() {
        new AbstractWorker(this) {
            @Override
            protected void beginTask() {
                logger.println("Starting tagging...");
            }
            @Override
            protected void executeTask() {
                addCoverForIndividualFile();
            }
            @Override
            protected void taskCompleted() {
                logger.println("Tagging complete!");
            }
        }.execute();
    }

    /**
     * Shortcut function to make code less cluttered. Simply calls {@link JOptionPane#showMessageDialog(Component, Object)}.
     */
    protected void showMD(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message);
    }

    /**
     * Displays the given string into the console on the right of the UI.
     * This function should <b>only</b> be used for informing the user of relevant progress.
     * No technical details should be shared via this function for a better UX.
     *
     * @param string the text to be displayed to the user.
     */
    public void displayText(String string) {
        SwingUtilities.invokeLater(() -> {
            StyledDocument doc = loadingText.getStyledDocument();
            try {
                doc.insertString(doc.getLength(), string, null);
            } catch (BadLocationException e) {
                ErrorLogger.runtimeExceptionOccurred(e);
                showMD(GuiTagger.this, "Something went wrong, please contact the developer.\nError code 04");
            }
        });
    }
}
