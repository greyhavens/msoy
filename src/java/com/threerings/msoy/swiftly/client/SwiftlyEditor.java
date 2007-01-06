package com.threerings.msoy.swiftly.client;

import java.awt.Color;
import java.awt.Container;
import java.awt.BorderLayout;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.HashMap;

import javax.swing.JTextPane;
import javax.swing.JTabbedPane;
import javax.swing.text.StyledDocument;
import javax.swing.text.BadLocationException;

public class SwiftlyEditor extends JTabbedPane
{
    public SwiftlyEditor (SwiftlyApplet applet) 
    {
        super();
        _applet = applet;
    }

    public void addEditorTab (SwiftlyDocument document)
    {
        if (_tabList.containsKey(document)) {
            setSelectedComponent(_tabList.get(document));
            return;
        }

        SwiftlyTextPane textPane = new SwiftlyTextPane(this, document);
        StyledDocument styledDoc = textPane.getStyledDocument();
        try {
            styledDoc.insertString(0, document.getText(), null);
        } catch (BadLocationException e) {
        }

        // TODO make these colors setable by the user?
        textPane.setForeground(Color.white);
        textPane.setBackground(Color.black);
        SwiftlyEditorScrollPane scroller = new SwiftlyEditorScrollPane(textPane);

        addTab(document.getFilename(), scroller);
        int tabIndex = getTabCount() - 1;
        // ALT+tabIndex selects this tab. TODO don't let this go beyond 10.
        setMnemonicAt(tabIndex, KeyEvent.VK_1 + tabIndex);

        // add the scroller, which is the main component, to the tabList
        _tabList.put(document, scroller);

        // select the newly opened tab
        setSelectedComponent(scroller);
    }

    public void closeEditorTab (SwiftlyDocument document) 
    {
        Container container = _tabList.get(document);
        remove(container);
        _tabList.remove(document);
    }

    protected SwiftlyApplet _applet;

    // maps the url of the loaded file to the componenet (scroller) holding that textpane
    // TODO this needs to map SwiftlyDocuments to scrollers, IF a document has been opened
    protected HashMap<SwiftlyDocument,SwiftlyEditorScrollPane> _tabList =
        new HashMap<SwiftlyDocument,SwiftlyEditorScrollPane>();
}
