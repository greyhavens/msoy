package com.threerings.msoy.swiftly.client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.io.IOException;

import javax.swing.JTextPane;
import javax.swing.JScrollPane;

public class SwiftlyEditorScrollPane extends JScrollPane {
    public SwiftlyEditorScrollPane(SwiftlyTextPane pane) {
        super(pane, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
              JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        _textPane = pane;
    }
    
    public JTextPane getTextPane () {
        return _textPane; 
    }       

    public void setPage (String url) {
        try {
            _textPane.setPage(url);
        } catch (IOException ie) {
            String errorMessage = "This page could not be loaded.\n" + "URL: " + url + "\n" +
                "Reason: " + ie;
            _textPane.setText(errorMessage);
        }
    }       

    protected JTextPane _textPane;
}

