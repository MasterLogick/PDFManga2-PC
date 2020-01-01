package net.ddns.logick;

import org.apache.commons.logging.impl.SimpleLog;

import javax.swing.*;

public class TextAreaLog extends SimpleLog {
    JTextArea logTextArea;

    public TextAreaLog(String name, JTextArea logTextArea) {
        super(name);
        this.logTextArea = logTextArea;
    }

    @Override
    protected void write(StringBuffer buffer) {
        String msg = buffer.toString();
        System.err.println(msg);
        logTextArea.append(msg + "\n");
    }
}
