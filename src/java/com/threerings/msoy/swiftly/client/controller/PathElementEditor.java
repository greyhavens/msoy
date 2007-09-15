//
// $Id$

package com.threerings.msoy.swiftly.client.controller;

import com.threerings.msoy.swiftly.client.view.PositionLocation;
import com.threerings.msoy.swiftly.data.PathElement;

/**
 * A component for working with PathElements.
 */
public interface PathElementEditor
{
    /**
     * Requests that the given path element be opened in the editor.
     * */
    public void openPathElement (final PathElement pathElement);

    /**
     * Requests that the given path element be opened in the editor, at the supplied
     * row and column.
     * @param highlight indicates whether the new location should be highlighted briefly
     */
    public void openPathElement (PathElement pathElement, PositionLocation location);

    /**
     * Renames a {@link PathElement} with the given String.
     */
    public void renamePathElement (final PathElement element, final String newName);
}