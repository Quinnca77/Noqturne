package org.autoTagger;

import com.mpatric.mp3agic.NotSupportedException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
    protected JTextField vIdThumbnail;
    protected JButton tagIndividualButton;
    protected JTabbedPane tabbedPane;
    protected JButton openFileButton;
    protected JTextField filePathSong;
    private JScrollPane filePathScrollPane;
    private JSplitPane splitPane;
    private JButton settingsButton;
    protected JTextField artistNameInput = new JTextField();
    protected JTextField songNameInput = new JTextField();
    protected final Logger logger;
    protected final Tagger tagger;
    protected final Downloader downloader;
    protected boolean renameState = true;
    protected File chosenSongFile;

    private static final int SETTINGS_BUTTON_SIZE = 32;
    private static final float BRIGHTNESS_FACTOR = 1.2f;

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
        setSize(800, 600);
        setLocationRelativeTo(null);

        URL iconURL = getClass().getResource("/icon.png");
        if (iconURL != null) {
            ImageIcon icon = new ImageIcon(iconURL);
            this.setIconImage(icon.getImage());
        }

        tagAllFilesInButton.addActionListener(e -> invokeTagAllFiles(null));
        downloadAndTagSongButton.addActionListener(e -> invokeDownloadAndTag());
        tagIndividualButton.addActionListener(e -> invokeIndividualTag());
        ToolTipManager.sharedInstance().setInitialDelay(0);
        ToolTipManager.sharedInstance().setDismissDelay(1000 * 60 * 10);

        JFileChooser chooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "MP3 Songs", "mp3");
        chooser.setFileFilter(filter);
        openFileButton.addActionListener(e -> {
            int returnVal = chooser.showOpenDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                chosenSongFile = chooser.getSelectedFile();
                filePathSong.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });
        splitPane.setDividerLocation(0.5); // Ensure the split is down the middle at start-up

        // This line is used to hide the scrollbar since it overlays the text and looked ugly
        filePathScrollPane.getHorizontalScrollBar().setPreferredSize(new Dimension(0,0));
        // Without this line, the scroll pane shows that its size is slightly bigger than the JTextField
        // it encompasses. This depended on the UI resizing, and thus I got rid of it entirely
        filePathScrollPane.setBorder(null);

        linkCheckboxes();
        settingsButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        try {
            InputStream imageStream = getClass().getResourceAsStream("/settings_gear_icon.png");
            if (imageStream == null) {
                ErrorLogger.runtimeExceptionOccurred("Image not found in resources");
                throw new RuntimeException("Image not found in resources");
            }
            BufferedImage originalImage = ImageIO.read(imageStream);
            Image resizedImage = originalImage.getScaledInstance(SETTINGS_BUTTON_SIZE, SETTINGS_BUTTON_SIZE, Image.SCALE_SMOOTH);
            BufferedImage resizedBufferedImage = toBufferedImage(resizedImage);
            settingsButton.setIcon(new ImageIcon(resizedBufferedImage));
            BufferedImage hoverImage = new BufferedImage(
                    resizedBufferedImage.getWidth(), resizedBufferedImage.getHeight(), BufferedImage.TYPE_INT_ARGB);

            RescaleOp op = new RescaleOp(
                    new float[] {BRIGHTNESS_FACTOR, BRIGHTNESS_FACTOR, BRIGHTNESS_FACTOR, 1f }, // R, G, B, A scale
                    new float[] { 0f, 0f, 0f, 0f }, // no offset
                    null);

            op.filter(resizedBufferedImage, hoverImage);
            settingsButton.setRolloverIcon(new ImageIcon(hoverImage));
        } catch (IOException e) {
            ErrorLogger.runtimeExceptionOccurred(e);
            this.logger.println("An error occurred while setting the settings button icon, " +
                    "see errorLog.log for more details");
        }

        setVisible(true);
        ResourceManager.ensureYtMusicApiInstallation();
    }

    /**
     * Simple helper method to convert an Image object to a BufferedImage the size of a default icon.
     *
     * @param img Image object to be converted
     * @return BufferedImage representation of the input
     */
    private BufferedImage toBufferedImage(Image img) {
        BufferedImage buffered = new BufferedImage(SETTINGS_BUTTON_SIZE, SETTINGS_BUTTON_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = buffered.createGraphics();
        g2.drawImage(img, 0, 0, null);
        g2.dispose();
        return buffered;
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
     *     <li>Absolute filepath to file on computer or choosing it through the file chooser button</li>
     *     <li>vId of the cover you want the individual file to have. If this isn't
     *     specified, it will instead use automatic cover art searching.</li>
     * </ul>
     */
    protected void addCoverForIndividualFile() {
        File song = chosenSongFile;
        if (song == null && !(new File(filePathSong.getText()).exists())) {
            showMD(GuiTagger.this, "Please choose a valid file");
            return;
        }
        if (song == null) {
            song = new File(filePathSong.getText());
        }
        String vId = vIdThumbnail.getText();
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
                    String artistText = artistNameInput.getText().replaceAll("[\\\\/:*?\"<>|]", "_");
                    String songText = songNameInput.getText().replaceAll("[\\\\/:*?\"<>|]", "_");
                    try {
                        song = Files.move(songPath, songPath.resolveSibling(artistText + " - " + songText + ".mp3")).toFile();
                    } catch (IOException e) {
                        ErrorLogger.runtimeExceptionOccurred(e);
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
        JPanel fields = new JPanel(new GridLayout(2, 2, 5, 5));
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
        mainPanel.setPreferredSize(new Dimension(800,80));
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
