//
// $Id$

package com.threerings.msoy.swiftly.client.view;

import com.threerings.msoy.swiftly.data.PathElement;

/**
 * A tabbed component that works with TabbedEditorComponents and wraps a JTabbedPane.
 */
public interface TabbedEditor
{
    /**
     * Adds a {@link TabbedEditorComponent} to the tabbed panel.
     * @param tab the {@link TabbedEditorComponent} to load into a new editor tab.
     * @param pathElement the {@link PathElement} being displayed in a new tab.
     */
    void addEditorTab (TabbedEditorComponent tab, PathElement pathElement);

    /**
     * Selects the component editing the supplied {@link PathElement}.
     * @param the {@link PathElement} to check
     * @return the TabbedEditorComponent if open, null otherwise.
     */
    TabbedEditorComponent selectTab (PathElement pathElement);

    /**
     * Update the title on a tab, using the supplied {@link PathElement} as the index.
     * @param pathElement the PathElement to use as an index into the tabList
     */
    void updateTabTitleAt (PathElement pathElement);

    /**
     * Closes the tab holding the provided PathElement. Does nothing if the element is not open.
     */
    void closePathElementTab (PathElement pathElement);

    /**
     * Closes the current tab.
     */
    void closeCurrentTab ();
}