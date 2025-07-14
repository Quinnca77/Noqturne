package org.noqturne;

import com.mpatric.mp3agic.NotSupportedException;
import org.noqturne.exceptions.NoSongFoundException;
import org.noqturne.exceptions.TaggingFolderException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;

import static org.noqturne.Tagger.getAllMp3Files;

/**
 * Class for the GUI of the app. Everything is made with Java Swing.
 */
public class Gui extends JFrame {

    private static Gui instance;
    protected JButton tagAllFilesInButton;
    protected JPanel MainPanel;
    protected JCheckBox fileRename1;
    protected JCheckBox fileRename2;
    protected JCheckBox fileRename3;
    protected JTextField songPlaylistURLTextField;
    protected JButton downloadAndTagSongButton;
    protected JTextPane consoleText;
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
    protected final SongDownloader songDownloader;
    protected boolean renameState = true;
    protected File chosenSongFile;

    private static final int SETTINGS_BUTTON_SIZE = 32;
    private static final float BRIGHTNESS_FACTOR = 1.2f;

    /**
     * Calling this constructor will create and show the GUI of Noqturne.
     *
     * @param testing boolean value of whether this class instance is being tested or not.
     *                if it is set to true, the GUI will not be shown, and instead only the
     *                necessary fields are initialized.
     */
    public Gui(boolean testing) {
        new Logger(this);
        this.logger = Logger.getLogger();
        this.tagger = new Tagger();
        this.songDownloader = new SongDownloader();
        instance = this;
        if (!testing) {
            initializeGUI();
        }
    }

    /**
     * Initialize and show GUI while binding its buttons to the relevant functions.
     */
    private void initializeGUI() {
        setContentPane(MainPanel);
        setTitle("Noqturne by Quinn Caris");
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
        }
        settingsButton.addActionListener(e -> openSettings());

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
     * Tags a single file, even if it isn't in the user's tagging folder.
     * Prerequisites: <ul>
     *     <li>Absolute filepath to file on computer or choosing it through the file chooser button</li>
     *     <li>vId of the cover you want the individual file to have. If this isn't
     *     specified, it will instead use automatic cover art searching.</li>
     * </ul>
     */
    protected void addCoverForIndividualFile() {
        File song = chosenSongFile;
        if (song == null && !(new File(filePathSong.getText()).exists())) {
            showMD(Gui.this, "Please choose a valid file");
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
            showMD(Gui.this, "Tagging successful!");
        } catch (IOException | NotSupportedException | InterruptedException e) {
            ErrorLogger.runtimeExceptionOccurred(e);
        }
    }

    /**
     * Tags all mp3 files in user's tagging folder. Different file locations can be specified in the
     * parameter by making each file to be tagged a <code>File</code> object.
     *
     * @param arrayOfSongs array of <code>File</code> objects to tag.
     *                    <code>null</code> to tag all files in tagging folder.
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
            showMD(Gui.this, "Tagging successful!");
        } catch (IOException |
                 InterruptedException | NotSupportedException e) {
            ErrorLogger.runtimeExceptionOccurred(e);
        } catch (NoSongFoundException e) {
            showMD(Gui.this, "No songs found in tagging folder!");
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

        int result = JOptionPane.showConfirmDialog(Gui.this, fields, "Rename file", JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
        switch (result) {
            case JOptionPane.OK_OPTION:
                if (!(artistNameInput.getText().isEmpty() && songNameInput.getText().isEmpty())) {
                    String artistText = artistNameInput.getText().replaceAll("[\\\\/:*?\"<>|]", "_");
                    String songText = songNameInput.getText().replaceAll("[\\\\/:*?\"<>|]", "_");
                    try {
                        song = Files.move(songPath, songPath.resolveSibling(artistText + " - " + songText + ".mp3")).toFile();
                    } catch (IOException e) {
                        ErrorLogger.runtimeExceptionOccurred(e);
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
     *                    the songs to be tagged. <code>null</code> to tag all songs in tagging folder.
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
                    arrayOfSongs = songDownloader.downloadSongs(songPlaylistURLTextField.getText());
                } catch (IOException | InterruptedException e) {
                    ErrorLogger.runtimeExceptionOccurred(e);
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
     * Error messages will be printed in red.
     * <p>
     * Called in {@link Logger#println(String)}.
     *
     * @param string the text to be displayed to the user.
     * @param isError determines whether the text to display is an error or not
     */
    public void displayText(String string, boolean isError) {
        SwingUtilities.invokeLater(() -> {
            StyledDocument doc = consoleText.getStyledDocument();
            Style style = consoleText.addStyle("Style", null);

            if (isError) {
                StyleConstants.setForeground(style, Color.RED);
            }

            try {
                doc.insertString(doc.getLength(), string, style);
            } catch (BadLocationException e) {
                ErrorLogger.runtimeExceptionOccurred(e);
            }
        });
    }

    /**
     * Will open the Settings dialog menu of Noqturne. This menu contains
     * a button to update the app's runtime dependencies, and contains the
     * functionality to choose the folder in which to download and tag mp3
     * files in.
     */
    public void openSettings() {
        JDialog settingsDialog = new JDialog(this, "Settings", true);
        settingsDialog.setSize(400, 300);

        JPanel settingsPanel = new JPanel();
        settingsPanel.setLayout(new GridLayout(2, 1));

        JButton updateDependenciesButton = new JButton("Update Dependencies");
        updateDependenciesButton.addActionListener(e -> ResourceManager.updateDependencies());
        JPanel dependenciesButtonPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        dependenciesButtonPanel.add(updateDependenciesButton, gbc);
        settingsPanel.add(dependenciesButtonPanel);

        JPanel filePathRowPanel = new JPanel(new GridLayout(1, 2));

        JPanel filePathLabelPanel = new JPanel(new GridBagLayout());
        JLabel filePathLabel = new JLabel("Filepath tagging folder");
        Properties stringProps = new Properties();
        try (InputStream in = this.getClass().getResourceAsStream("/string.properties")) {
            stringProps.load(in);
        } catch (IOException e) {
            ErrorLogger.runtimeExceptionOccurred(e);
            throw new RuntimeException(e);
        }
        JLabel questionMark = new JLabel((String) stringProps.get("helpText"));
        questionMark.setToolTipText((String) stringProps.get("taggingFolderToolTipText"));

        filePathLabelPanel.add(filePathLabel);
        filePathLabelPanel.add(questionMark);
        filePathRowPanel.add(filePathLabelPanel);

        JPanel filePathPanel = new JPanel(new GridBagLayout());

        String taggingFolder = null;
        try {
            taggingFolder = ResourceManager.getTaggingDirectory().toString();
        } catch (IOException e) {
            ErrorLogger.runtimeExceptionOccurred(e);
            throw new RuntimeException(e);
        } catch (TaggingFolderException e) {
            logger.printError("Could not find tagging folder");
            ErrorLogger.runtimeExceptionOccurred(e);
        }
        JTextField filePathTextField = new JTextField(taggingFolder);

        JScrollPane filePathTextScrollPane = new JScrollPane(filePathTextField);
        filePathTextScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        filePathTextScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);

        JButton fileChooserButton = new JButton();
        fileChooserButton.setIcon(new ImageIcon(
                Objects.requireNonNull(
                        this.getClass().getResource("/open_file_icon.png"))));

        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooserButton.addActionListener(e -> {
            int returnVal = chooser.showOpenDialog(settingsPanel);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                try {
                    ResourceManager.setTaggingDirectory(chooser.getSelectedFile().toPath());
                } catch (IOException ex) {
                    ErrorLogger.runtimeExceptionOccurred(ex);
                }
                filePathTextField.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });

        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        filePathPanel.add(filePathTextScrollPane, gbc);

        gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 1;
        gbc.weightx = 0;
        filePathPanel.add(fileChooserButton, gbc);

        filePathRowPanel.add(filePathPanel);
        settingsPanel.add(filePathRowPanel);

        settingsDialog.add(settingsPanel);
        settingsDialog.setLocationRelativeTo(this);
        settingsDialog.setVisible(true);
    }

    public void showProgressBar(AbstractWorker task, String progressName) {
        JDialog progressDialog = new JDialog(this, progressName);
        progressDialog.setSize(300, 100);
        progressDialog.setLayout(new FlowLayout());
        JProgressBar bar = new JProgressBar();
        bar.setIndeterminate(true);
        JLabel progressText = new JLabel("0/100");
        task.addPropertyChangeListener(e -> {
            switch (e.getPropertyName()) {
                case "progress" -> {
                    if (bar.isIndeterminate()) {
                        bar.setIndeterminate(false);
                    }
                    int progress = (Integer) e.getNewValue();
                    bar.setValue(progress);
                    progressText.setText(progress + "/100");
                }
                case "state" -> {
                    if (e.getNewValue() == SwingWorker.StateValue.DONE) {
                        progressDialog.dispose();
                    }
                }
            }
        });
        progressDialog.add(bar);
        progressDialog.add(progressText);
        progressDialog.setLocationRelativeTo(this);
        progressDialog.setAlwaysOnTop(true);
        progressDialog.setVisible(true);
    }

    public static Gui getInstance() {
        return instance;
    }
}
