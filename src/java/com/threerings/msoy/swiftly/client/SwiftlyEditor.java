package com.threerings.msoy.swiftly.client;

import java.awt.Color;
import java.awt.BorderLayout;
import java.awt.event.KeyEvent;
import java.io.IOException;

import javax.swing.JTextPane;
import javax.swing.JTabbedPane;
import javax.swing.JScrollPane;

public class SwiftlyEditor extends JTabbedPane {

    public SwiftlyEditor(SwiftlyApplet applet) {
        super();
        _applet = applet;
    }

    public class SwiftlyEditorScrollPane extends JScrollPane {
        public SwiftlyEditorScrollPane(SwiftlyTextPane pane) {
            super(pane, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                  JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
            _textPane = pane;
        }
        
        public JTextPane getTextPane() {
            return _textPane; 
        }       

        public void setPage(String url) {
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

    public void addEditorTab(String tabName, String url) {
        SwiftlyTextPane textPane = new SwiftlyTextPane();
        // TODO make these colors setable by the user?
        textPane.setForeground(Color.white);
        textPane.setBackground(Color.black);
        SwiftlyEditorScrollPane scroller = new SwiftlyEditorScrollPane(textPane);

        addTab(tabName, scroller);
        int tabIndex = getTabCount() - 1;
        // ALT+tabIndex selects this tab. TODO don't let this go beyond 10.
        setMnemonicAt(tabIndex, KeyEvent.VK_1 + tabIndex);
        setTabPage(tabIndex, url);
    }

    public void setTabPage(int tabIndex, String url) {
        SwiftlyEditorScrollPane pane = (SwiftlyEditorScrollPane)getComponentAt(tabIndex);
        pane.setPage(url);
    }

    protected SwiftlyApplet _applet;
}
