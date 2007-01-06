package com.threerings.msoy.swiftly.client;

import java.awt.Color;
import java.awt.Container;
import java.awt.BorderLayout;
import java.awt.event.KeyEvent;

import javax.swing.JTextPane;
import javax.swing.JTabbedPane;

public class SwiftlyEditor extends JTabbedPane {

    public SwiftlyEditor (SwiftlyApplet applet) {
        super();
        _applet = applet;
    }

    public void addEditorTab (String tabName, String url) {
        SwiftlyTextPane textPane = new SwiftlyTextPane(this);
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

    public void closeEditorTab (Container container) {
        remove(container);
    }

    public void setTabPage (int tabIndex, String url) {
        SwiftlyEditorScrollPane pane = (SwiftlyEditorScrollPane)getComponentAt(tabIndex);
        pane.setPage(url);
    }

    protected SwiftlyApplet _applet;
}
