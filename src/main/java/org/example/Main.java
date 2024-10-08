package org.example;

import com.formdev.flatlaf.FlatDarkLaf;

// TODO make bash file for easier installation
// TODO rework UI with tabs, keep log screen at the same place but make three tabs for each functionality
public class Main {
    public static void main(String[] args) {
        FlatDarkLaf.setup();
        new guiTagger();
    }
}
