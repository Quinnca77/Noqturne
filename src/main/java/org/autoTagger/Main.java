package org.autoTagger;

import com.formdev.flatlaf.FlatDarkLaf;

// TODO make directory choice user for downloaded songs
public class Main {
    public static void main(String[] args) {
        FlatDarkLaf.setup();
        new GuiTagger(false);
    }
}
