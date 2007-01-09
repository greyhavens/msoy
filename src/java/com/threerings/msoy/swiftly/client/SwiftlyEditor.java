package com.threerings.msoy.swiftly.client;

import java.awt.Container;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.HashMap;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import javax.swing.JTextPane;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class SwiftlyEditor extends JTabbedPane
{
    public SwiftlyEditor (SwiftlyApplet applet) 
    {
        super();
        _applet = applet;
        addChangeListener(new TabChangedListener());

        setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
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
        assignTabKeys();

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

    public SwiftlyApplet getApplet ()
    {
        return _applet;
    }

    // returns the JOptionPane option the user picked
    public int closeCurrentTab ()
    {
        // Don't try to remove a tab if we have none.
        if (getTabCount() == 0) {
            // We're doing nothing which is the same as picking cancel
            return JOptionPane.CANCEL_OPTION;
        }
        SwiftlyEditorScrollPane pane = (SwiftlyEditorScrollPane)getSelectedComponent();
        SwiftlyTextPane textPane = pane.getTextPane();

        int response = -1;
        if (textPane.hasUnsavedChanges()) {
            response = JOptionPane.showInternalConfirmDialog(_applet.getContentPane(),
                "Save changes?");
            // Choosing Cancel will return at this point and do nothing
            if (response == JOptionPane.YES_OPTION) {
                textPane.saveDocument();
            } else if (response == JOptionPane.NO_OPTION) {
                // continue on with the tab closing
            } else {
                // return cancel so something calling this knows to stop
                return response;
            }
        }

        SwiftlyDocument document = textPane.getSwiftlyDocument();
        remove(_tabList.get(document));
        _tabList.remove(document);
        assignTabKeys();
        return response;
    }


    public void removeTabs ()
    {
        int tabCount = getTabCount();
        for (int count = 0; count < tabCount; count++) {
            setSelectedIndex(0);
            int response = closeCurrentTab();
            if (response == JOptionPane.CANCEL_OPTION) {
                // do nothing more if the user picks cancel
                return;
            }
        }
    }

    public AbstractAction createNewTabAction ()
    {
        return new AbstractAction() {
            // from AbstractAction
            public void actionPerformed (ActionEvent e) {
                addEditorTab();
            }
        };
    }

    public AbstractAction createSaveCurrentTabAction ()
    {
        return new AbstractAction("Save") {
            // from AbstractAction
            public void actionPerformed (ActionEvent e) {
                saveCurrentTab();
            }
        };
    }

    public AbstractAction createCloseCurrentTabAction ()
    {
        return new AbstractAction() {
            // from AbstractAction
            public void actionPerformed (ActionEvent e) {
                closeCurrentTab();
            }
        };
    }

    protected void assignTabKeys ()
    {
        for (int tabIndex = 0; tabIndex < getTabCount(); tabIndex++) {
            // ALT+tabIndex selects this tab for the first 9 tabs
            if (tabIndex < 9) {
                setMnemonicAt(tabIndex, KeyEvent.VK_1 + tabIndex);
            }
        }
    }

    protected class TabChangedListener implements ChangeListener
    {
        // from interface ChangeListener
        public void stateChanged(ChangeEvent evt) {
            // clear the statusbar whenever a different tab is selected
            _applet.clearStatus();
        }
    }

    protected SwiftlyApplet _applet;

    // maps the document of the loaded file to the componenet (scroller) holding that textpane
    protected HashMap<SwiftlyDocument,SwiftlyEditorScrollPane> _tabList =
        new HashMap<SwiftlyDocument,SwiftlyEditorScrollPane>();
}
