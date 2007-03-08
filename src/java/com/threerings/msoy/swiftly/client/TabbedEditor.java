//
// $Id$

package com.threerings.msoy.swiftly.client;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.HashMap;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.threerings.msoy.swiftly.data.PathElement;
import com.threerings.msoy.swiftly.data.SwiftlyCodes;
import com.threerings.msoy.swiftly.data.SwiftlyDocument;
import com.threerings.msoy.swiftly.util.SwiftlyContext;

import sdoc.Gutter;
import com.infosys.closeandmaxtabbedpane.CloseAndMaxTabbedPane;
import com.infosys.closeandmaxtabbedpane.CloseListener;

public class TabbedEditor extends CloseAndMaxTabbedPane
{
    public TabbedEditor (SwiftlyContext ctx, SwiftlyEditor editor)
    {
        super(false);
        _ctx = ctx;
        _editor = editor;

        addChangeListener(new TabChangedListener());
        addCloseListener(new CloseListener() {
           public void closeOperation(MouseEvent e) {
              closeTabAt(getOverTabIndex());
           }
        });
        setMaxIcon(false);
        setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
    }

    /**
     * Adds a {@link SwiftlyTextPane} to the tabbed panel.
     * @param the {@link PathElement} to load into the text panel.
     * @return the SwiftlyTextPane created or null if it was already created
     */
    public SwiftlyTextPane addEditorTab (PathElement pathElement)
    {
        if (_tabList.containsKey(pathElement)) {
            setSelectedComponent(_tabList.get(pathElement));
            return null;
        }

        SwiftlyTextPane textPane = new SwiftlyTextPane(_ctx, _editor, pathElement);
        JScrollPane scroller = new JScrollPane(textPane, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        // add line numbers
        scroller.setRowHeaderView(new Gutter(textPane, scroller));

        // add the tab
        add(scroller);
        // select the newly opened tab
        setSelectedComponent(scroller);

        // set the title
        updateCurrentTabTitle();
        // assign the mnemonic
        assignTabKeys();

        // add the scroller, which is the main component, to the tabList
        _tabList.put(pathElement, scroller);

        return textPane;
    }

    /**
     * Update the title on a tab, using the supplied {@link PathElement} as the index.
     * @param doc the PathElement to use as an index into the tabList
     */
    public void updateTabTitleAt (PathElement pathElement)
    {
        JScrollPane tab = _tabList.get(pathElement);
        if (tab != null) {
            updateTabTitleAt(indexOfComponent(tab));
        }
    }

    /**
     * Completely overwrite document contents when update is received from server.
     */
    public void setTabDocument (SwiftlyDocument doc)
    {
        JScrollPane tab = _tabList.get(doc.getPathElement());
        if (tab != null) {
            SwiftlyTextPane textPane = (SwiftlyTextPane)tab.getViewport().getView();
            getCurrentTextPane().setDocument(doc);
        }
    }

    /**
     * Update the title of the currently selected tab.
     */
    public void updateCurrentTabTitle ()
    {
        int selidx = getSelectedIndex();
        if (selidx >= 0) {
            updateTabTitleAt(selidx);
        }
    }

    /**
     * Adds an indicator(*) to the current tab showing if an unsaved change has occurred.
     * @param tabIndex the tab title needing updating
     */
    protected void updateTabTitleAt (int tabIndex)
    {
        JScrollPane pane = (JScrollPane)getComponentAt(tabIndex);
        SwiftlyTextPane textPane = (SwiftlyTextPane)pane.getViewport().getView();
        String title = textPane.getPathElement().getName();
        setTitleAt(tabIndex, title);
    }

    /**
     * Closes the current tab.
     */
    public void closeCurrentTab ()
    {
        closeTabAt(getSelectedIndex());
    }

    /**
     * Closes the tab at the given index.
     * @param tabIndex the tab title needing removal
     */
    public void closeTabAt (int tabIndex)
    {
        // Don't try to remove a tab if we have none.
        if (getTabCount() == 0) {
            // do nothing
            return;
        }

        JScrollPane pane = (JScrollPane)getComponentAt(tabIndex);
        SwiftlyTextPane textPane = (SwiftlyTextPane)pane.getViewport().getView();
        PathElement pathElement = textPane.getPathElement();
        remove(_tabList.get(pathElement));
        _tabList.remove(pathElement);
        assignTabKeys();
    }

    /**
     * Creates and returns an action to close the current tab.
     */
    public AbstractAction createCloseCurrentTabAction ()
    {
        return new AbstractAction(_ctx.xlate(SwiftlyCodes.SWIFTLY_MSGS, "m.action.close_tab")) {
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
        }
    }

    protected SwiftlyContext _ctx;
    protected SwiftlyEditor _editor;

    // maps the document of the loaded file to the componenet (scroller) holding that textpane
    protected HashMap<PathElement,JScrollPane> _tabList =
        new HashMap<PathElement,JScrollPane>();
}
