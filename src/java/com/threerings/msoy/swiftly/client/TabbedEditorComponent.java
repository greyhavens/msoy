//
// $Id$

package com.threerings.msoy.swiftly.client;

import com.threerings.msoy.swiftly.data.PathElement;

/** 
 * Any {@link Component} wanting to be edited in the {@link TabbedEditor} must implement this
 * interface.
 */
public interface TabbedEditorComponent
{
    public PathElement getPathElement ();

    public void setPathElement (PathElement element);
}
