//
// $Id$

package com.threerings.msoy.swiftly.client;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.threerings.msoy.swiftly.data.PathElement;
import com.threerings.msoy.swiftly.data.SwiftlyCodes;
import com.threerings.msoy.swiftly.data.SwiftlyTextDocument;
import com.threerings.msoy.swiftly.util.SwiftlyContext;

import com.samskivert.util.HashIntMap;

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
     * Adds a {@link Component} to the tabbed panel.
     * @param the {@link Component} to load into a new editor tab.
     */
    public void addEditorTab (Component tab, PathElement pathElement)
    {
        // add the tab
        add(tab);
        // select the newly opened tab
        setSelectedComponent(tab);

        // assign the mnemonic
        assignTabKeys();

        // add the component to the tabList
        _tabList.put(pathElement.elementId, tab);

        // set the title
        updateTabTitleAt(pathElement);
    }

    /**
     * Selects the component editing the supplied {@link PathElement}.
     * @param the {@link PathElement} to check
     * @return true if the tab was open, false otherwise.
     */
    public boolean selectTab (PathElement pathElement)
    {
        if (_tabList.containsKey(pathElement.elementId)) {
            setSelectedComponent(_tabList.get(pathElement.elementId));
            return true;
        }
        return false;
    }

    /**
     * Update the title on a tab, using the supplied {@link PathElement} as the index.
     * @param pathElement the PathElement to use as an index into the tabList
     */
    public void updateTabTitleAt (PathElement pathElement)
    {
        Component tab = _tabList.get(pathElement.elementId);
        if (tab != null) {
            setTitleAt(indexOfComponent(tab), pathElement.getName());
        }
    }

    /**
     * Closes the tab holding the provided PathElement.
     */
    public void closePathElementTab (PathElement pathElement)
    {
        Component tab = _tabList.get(pathElement.elementId);
        if (tab == null) {
            return;
        }
        closeTabAt(indexOfComponent(tab));
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

        TabbedEditorComponent tab = (TabbedEditorComponent)getComponentAt(tabIndex);
        PathElement pathElement = tab.getPathElement();
        remove(_tabList.get(pathElement.elementId));
        _tabList.remove(pathElement.elementId);
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

    // TODO: if this remains unneeded, remove
    protected class TabChangedListener implements ChangeListener
    {
        // from interface ChangeListener
        public void stateChanged(ChangeEvent evt) {
            Component tab = getSelectedComponent();
        }
    }

    protected SwiftlyContext _ctx;
    protected SwiftlyEditor _editor;

    // maps the pathelement elementId to the component editing/viewing that pathelement
    protected HashIntMap<Component> _tabList = new HashIntMap<Component>();
}
