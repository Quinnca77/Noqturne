package org.autoTagger;

import com.formdev.flatlaf.FlatDarkLaf;

// TODO use a different way of selecting an mp3 file instead of a filepath
public class Main {
    public static void main(String[] args) {
        FlatDarkLaf.setup();
        new guiTagger(false);
    }
}
