package com.threerings.msoy.swiftly.client;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.HashMap;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.JScrollPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class TabbedEditor extends JTabbedPane
{
    public TabbedEditor (SwiftlyEditor editor) 
    {
        _editor = editor;
        addChangeListener(new TabChangedListener());

        setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
    }

    /**
     * Adds a {@link SwiftlyTextPane} to the tabbed panel.
     * @param the {@link SwiftlyDocument} to load into the text panel.
     */
    public void addEditorTab (SwiftlyDocument document)
    {
        if (_tabList.containsKey(document)) {
            setSelectedComponent(_tabList.get(document));
            return;
        }

        SwiftlyTextPane textPane = new SwiftlyTextPane(_editor, document);
        JScrollPane scroller = new JScrollPane(textPane, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

        // add the tab
        add(scroller);
        // select the newly opened tab
        setSelectedComponent(scroller);

        // set the title
        updateCurrentTabTitle();
        // assign the mnemonic
        assignTabKeys();

        // add the scroller, which is the main component, to the tabList
        _tabList.put(document, scroller);
    }

    /**
     * Update the title on a tab, using the supplied {@link SwiftlyDocument} as the index.
     * @param doc the SwiftlyDocument to use as an index into the tabList
     */
    public void updateTabTitleAt (SwiftlyDocument document)
    {
        updateTabTitleAt(indexOfComponent(_tabList.get(document)));
    }

    /**
     * Update the title of the currently selected tab.
     */
    public void updateCurrentTabTitle ()
    {
        updateTabTitleAt(getSelectedIndex());
    }

    /**
     * Adds an indicator(*) to the current tab showing if an unsaved change has occurred.
     * @param tabIndex the tab title needing updating
     */
    protected void updateTabTitleAt (int tabIndex)
    {
        JScrollPane pane = (JScrollPane)getComponentAt(tabIndex);
        SwiftlyTextPane textPane = (SwiftlyTextPane)pane.getViewport().getView();
        String title = textPane.getSwiftlyDocument().getName();

        if (title.length() == 0) {
            title = "Untitled document";
        }

        if (textPane.hasUnsavedChanges()) {
            title = "*" + title;
        }

        setTitleAt(tabIndex, title);
    }

    /**
     * Closes the current tab. If the tab contains unsaved changes, display a dialog box.
     * @return the {@link JOptionPane} constant selected if a dialog was used.
     */
    public int closeCurrentTab ()
    {
        // Don't try to remove a tab if we have none.
        if (getTabCount() == 0) {
            // We're doing nothing which is the same as picking cancel
            return JOptionPane.CANCEL_OPTION;
        }
        SwiftlyTextPane textPane = getCurrentTextPane();

        int response = -1;
        if (textPane.hasUnsavedChanges()) {
            response = JOptionPane.showInternalConfirmDialog(_editor, "Save changes?");
            // Choosing Cancel will return at this point and do nothing
            if (response == JOptionPane.YES_OPTION) {
                textPane.saveDocument();
            } else if (response == JOptionPane.NO_OPTION) {
                // continue on with the tab closing
            } else {
                // return cancel so something calling this method knows to stop
                return response;
            }
        }

        SwiftlyDocument document = textPane.getSwiftlyDocument();
        remove(_tabList.get(document));
        _tabList.remove(document);
        assignTabKeys();
        return response;
    }

    /**
     * Removes all the tabs from the editor tab panel.
     */
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

    /**
     * Creates and returns an action to close the current tab.
     */
    public AbstractAction createCloseCurrentTabAction ()
    {
        return new AbstractAction("Close Tab") {
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

    /**
     * Gets the {@link SwiftlyEditorScrollPane} from the currently selected tab.
     */
    protected JScrollPane getCurrentPane ()
    {
        return (JScrollPane)getSelectedComponent();
    }

    /**
     * Gets the {@link SwiftlyTextPane} from the currently selected tab.
     */
    protected SwiftlyTextPane getCurrentTextPane ()
    {
        JScrollPane pane = getCurrentPane();
        if (pane != null) {
            return (SwiftlyTextPane)pane.getViewport().getView();
        }
        return null;
    }

    protected class TabChangedListener implements ChangeListener
    {
        // from interface ChangeListener
        public void stateChanged(ChangeEvent evt) {
            SwiftlyTextPane textPane = getCurrentTextPane();
            if (textPane != null) {
                _editor.getToolbar().updateEditorActions(textPane);
            }

            // clear the statusbar whenever a different tab is selected
            _editor.clearStatus();
        }
    }

    protected SwiftlyEditor _editor;

    // maps the document of the loaded file to the componenet (scroller) holding that textpane
    protected HashMap<SwiftlyDocument,JScrollPane> _tabList =
        new HashMap<SwiftlyDocument,JScrollPane>();
}
