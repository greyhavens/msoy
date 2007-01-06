package com.threerings.msoy.swiftly.client;

import java.awt.Color;
import java.awt.Container;
import java.awt.BorderLayout;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.HashMap;

import javax.swing.JTextPane;
import javax.swing.JTabbedPane;

public class SwiftlyEditor extends JTabbedPane {

    public SwiftlyEditor (SwiftlyApplet applet) {
        super();
        _applet = applet;
    }

    public void addEditorTab (String tabName, String url) {
        if (_tabList.containsKey(url)) {
            setSelectedComponent(_tabList.get(url));
            return;
        }

        SwiftlyTextPane textPane = new SwiftlyTextPane(this);

        // load the url into the textpane
        try {
            textPane.setPage(url);
        } catch (IOException ie) {
            String errorMessage = "This page could not be loaded. " + "URL: " + url;
            _applet.setStatus(errorMessage);
            // TODO do something more intelligent here
            return;
        }

        // TODO make these colors setable by the user?
        textPane.setForeground(Color.white);
        textPane.setBackground(Color.black);
        SwiftlyEditorScrollPane scroller = new SwiftlyEditorScrollPane(textPane);

        addTab(tabName, scroller);
        int tabIndex = getTabCount() - 1;
        // ALT+tabIndex selects this tab. TODO don't let this go beyond 10.
        setMnemonicAt(tabIndex, KeyEvent.VK_1 + tabIndex);

        // add the scroller, which is the main component, to the tabList
        _tabList.put(url, scroller);

        // select the newly opened tab
        setSelectedComponent(scroller);
    }

    public void closeEditorTab (String url) {
        Container container = _tabList.get(url);
        remove(container);
        _tabList.remove(url);
    }

    protected SwiftlyApplet _applet;

    // maps the url of the loaded file to the componenet (scroller) holding that textpane
    protected HashMap<String,SwiftlyEditorScrollPane> _tabList =
        new HashMap<String,SwiftlyEditorScrollPane>();
}
