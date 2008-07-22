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
    void openPathElement (PathElement element);

    /**
     * Requests that the given path element be opened in the editor, at the supplied
     * PositionLocation.
     */
    void openPathElementAt (PathElement element, PositionLocation location);

    /**
     * Renames a {@link PathElement} with the given String.
     */
    void renamePathElement (PathElement element, String newName);
}