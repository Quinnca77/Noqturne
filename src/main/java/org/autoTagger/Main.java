package org.autoTagger;

import com.formdev.flatlaf.FlatDarkLaf;

// TODO make it tag only the downloaded songs and not everything in the directory
// TODO make directory choice user for downloaded songs
// TODO use a different way of selecting an mp3 file instead of a filepath
public class Main {
    public static void main(String[] args) {
        FlatDarkLaf.setup();
        new GuiTagger(false);
    }
}
