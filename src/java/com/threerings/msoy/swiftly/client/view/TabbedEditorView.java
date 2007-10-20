//
// $Id$

package com.threerings.msoy.swiftly.client.view;

import java.awt.Component;
import java.awt.event.KeyEvent;
import java.util.Map;

import javax.swing.JTabbedPane;

import com.google.common.collect.Maps;

import org.jvnet.substance.SubstanceLookAndFeel;
import org.jvnet.substance.TabCloseListener;

import com.threerings.msoy.swiftly.data.PathElement;

/**
 * Implementation of TabbedEditor.
 */
public class TabbedEditorView extends JTabbedPane
    implements TabbedEditor
{
    public TabbedEditorView ()
    {
        setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        putClientProperty(
            SubstanceLookAndFeel.TABBED_PANE_CLOSE_BUTTONS_PROPERTY, Boolean.TRUE);

        // register tab close listener
        SubstanceLookAndFeel.registerTabCloseChangeListener(this, new TabCloseListener() {
            public void tabClosing(JTabbedPane tabbedPane, Component tabComponent) {
                tabWasClosed((TabbedEditorComponent)tabComponent);
            }

            public void tabClosed(JTabbedPane tabbedPane, Component tabComponent) {
                SubstanceLookAndFeel.unregisterTabCloseChangeListener(this);
            }
        });
    }

    // from TabbedEditorView
    public void addEditorTab (TabbedEditorComponent tab, PathElement pathElement)
    {
        Component comp = tab.getComponent();

        // add the tab
        add(comp);
        // select the newly opened tab
        setSelectedComponent(comp);

        // assign the mnemonic
        assignTabKeys();

        // add the component to the tabList
        _tabList.put(pathElement, tab);

        // set the title
        updateTabTitleAt(pathElement);
    }

    // from TabbedEditorView
    public TabbedEditorComponent selectTab (PathElement pathElement)
    {
        TabbedEditorComponent tab;
        if ((tab = _tabList.get(pathElement)) != null) {
            setSelectedComponent(tab.getComponent());
        }
        return tab;
    }

    // from TabbedEditorView
    public void updateTabTitleAt (PathElement pathElement)
    {
        TabbedEditorComponent tab;
        if ((tab = _tabList.get(pathElement)) != null) {
            setTitleAt(indexOfComponent(tab.getComponent()), pathElement.getName());
        }
    }

    // from TabbedEditorView
    public void closePathElementTab (PathElement pathElement)
    {
        TabbedEditorComponent tab = _tabList.get(pathElement);
        if (tab == null) {
            return;
        }
        closeTabAt(indexOfComponent(tab.getComponent()));
    }

    // from TabbedEditorView
    public void closeCurrentTab ()
    {
        closeTabAt(getSelectedIndex());
    }

    /**
     * Closes the tab at the given index.
     * @param tabIndex the tab title needing removal
     */
    private void closeTabAt (int tabIndex)
    {
        // Don't try to remove a tab if we have none.
        if (getTabCount() == 0) {
            // do nothing
            return;
        }

        TabbedEditorComponent tab = (TabbedEditorComponent)getComponentAt(tabIndex);
        remove(_tabList.get(tab.getPathElement()).getComponent());
        tabWasClosed(tab);
    }

    /**
     * Perform various bits of cleanup after a tabbed has been closed and removed from the UI.
     */
    private void tabWasClosed (TabbedEditorComponent tab)
    {
        _tabList.remove(tab.getPathElement());
        assignTabKeys();
    }

    /**
     * Assign the mnemonic shortcut keys to the currently displayed tabs.
     */
    private void assignTabKeys ()
    {
        for (int tabIndex = 0; tabIndex < getTabCount(); tabIndex++) {
            // ALT+tabIndex selects this tab for the first 9 tabs
            if (tabIndex < 9) {
                setMnemonicAt(tabIndex, KeyEvent.VK_1 + tabIndex);
            }
        }
    }

    /** Maps the PathElement to the component editing/viewing that PathElement */
    private final Map<PathElement, TabbedEditorComponent> _tabList = Maps.newHashMap();
}
