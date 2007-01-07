package com.threerings.msoy.swiftly.client;

import java.awt.Container;
import java.awt.BorderLayout;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.HashMap;

import javax.swing.JTextPane;
import javax.swing.JTabbedPane;

public class SwiftlyEditor extends JTabbedPane
{
    public SwiftlyEditor (SwiftlyApplet applet) 
    {
        super();
        _applet = applet;
    }

    public void addEditorTab ()
    {
        addEditorTab (new SwiftlyDocument("new file", "This is a new file"));
    }

    public void addEditorTab (SwiftlyDocument document)
    {
        if (_tabList.containsKey(document)) {
            setSelectedComponent(_tabList.get(document));
            return;
        }

        SwiftlyTextPane textPane = new SwiftlyTextPane(this, document);
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

    public void saveCurrentTab () 
    {
        SwiftlyEditorScrollPane pane = (SwiftlyEditorScrollPane)getSelectedComponent();
        // TODO show a progress bar in the status bar while Saving...
        if (pane.getTextPane().saveDocument()) {
            setTabTitleChanged(false);
            // TODO show the filename that just saved
            _applet.setStatus("Document saved.");
        }
    }

    public void setTabTitleChanged (boolean changed)
    {
        int tabIndex = getSelectedIndex();
        String title = getTitleAt(tabIndex);

        boolean hasAsterisk = false;
        if (title.charAt(0) == '*') {
            hasAsterisk = true;
        }

        if (changed && !hasAsterisk) {
            title = "*" + title;
            setTitleAt(tabIndex, title);
        } else if (hasAsterisk) {
            title = title.substring(1);
            setTitleAt(tabIndex, title);
        }
    }

    public void closeCurrentTab () 
    {
        SwiftlyEditorScrollPane pane = (SwiftlyEditorScrollPane)getSelectedComponent();
        closeEditorTab(pane.getTextPane().getSwiftlyDocument());
    }

    public void closeEditorTab (SwiftlyDocument document) 
    {
        remove(_tabList.get(document));
        _tabList.remove(document);
    }

    public SwiftlyApplet getApplet ()
    {
        return _applet;
    }

    protected SwiftlyApplet _applet;

    // maps the url of the loaded file to the componenet (scroller) holding that textpane
    // TODO this needs to map SwiftlyDocuments to scrollers, IF a document has been opened
    protected HashMap<SwiftlyDocument,SwiftlyEditorScrollPane> _tabList =
        new HashMap<SwiftlyDocument,SwiftlyEditorScrollPane>();
}
