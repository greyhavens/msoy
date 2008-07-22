//
// $Id$

package com.threerings.msoy.swiftly.client.view;

import com.threerings.msoy.swiftly.data.PathElement;

/**
 * Holds various components, notably a tree, for displaying project documents and tools.
 */
public interface ProjectPanel
{
    /**
     * Tell the JTree to start editing the name of the selected node.
     */
    void renameCurrentElement ();

    /**
     * Enable editing of tree nodes.
     */
    void enableEditing ();

    /**
     * Disable editing of tree nodes.
     */
    void disableEditing ();

    /**
     * Return the parent of the currently selected element.
     */
    PathElement getCurrentParent ();

    /**
     * Return the currently selected PathElement.
     */
    PathElement getSelectedPathElement ();
}