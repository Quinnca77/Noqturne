package org.example;

import javax.swing.*;
import java.awt.*;

public abstract class AbstractWorker extends SwingWorker<Void, Void> {
    private final JFrame frame;

    public AbstractWorker(JFrame frame) {
        this.frame = frame;
    }

    @Override
    protected Void doInBackground() {
        frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        beginTask();
        executeTask();
        return null;
    }

    @Override
    protected void done() {
        frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        taskCompleted();
    }

    // Abstract methods for specific task implementation
    protected abstract void beginTask();

    protected abstract void executeTask();

    protected abstract void taskCompleted();
}