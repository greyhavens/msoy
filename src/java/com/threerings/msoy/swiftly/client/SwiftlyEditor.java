package com.threerings.msoy.swiftly.client;

import java.awt.Color;
import java.awt.Container;
import java.awt.BorderLayout;
import java.awt.event.KeyEvent;
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

        SwiftlyTextPane textPane = new SwiftlyTextPane(this, url);
        // TODO make these colors setable by the user?
        textPane.setForeground(Color.white);
        textPane.setBackground(Color.black);
        SwiftlyEditorScrollPane scroller = new SwiftlyEditorScrollPane(textPane);

        addTab(tabName, scroller);
        int tabIndex = getTabCount() - 1;
        // ALT+tabIndex selects this tab. TODO don't let this go beyond 10.
        setMnemonicAt(tabIndex, KeyEvent.VK_1 + tabIndex);
        setTabPage(tabIndex, url);

        // add the scroller, which is the main component, to the tabList
        _tabList.put(url, scroller);

        // select the newly opened tab
        setSelectedComponent(scroller);
    }

    public void closeEditorTab (String url) {
        Container container = _tabList.get(url);
        remove(container);
    }

    public void setTabPage (int tabIndex, String url) {
        SwiftlyEditorScrollPane pane = (SwiftlyEditorScrollPane)getComponentAt(tabIndex);
        pane.setPage(url);
    }

    protected SwiftlyApplet _applet;

    // maps the url of the loaded file to the componenet (scroller) holding that textpane
    protected HashMap<String,SwiftlyEditorScrollPane> _tabList =
        new HashMap<String,SwiftlyEditorScrollPane>();
}
