package org.noqturne;

import com.formdev.flatlaf.FlatDarkLaf;

public class Main {
    public static void main(String[] args) {
        FlatDarkLaf.setup();
        new Gui(false);
    }
}
