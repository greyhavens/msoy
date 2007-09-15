//
// $Id$

package com.threerings.msoy.swiftly.client.event;

import com.threerings.msoy.swiftly.data.PathElement;

public interface PathElementListener
{
    /** Notify the listener that a PathElement was added to the project */
    public void elementAdded (PathElement element);

    /** Notify the listener that a PathElement was updated in a project */
    public void elementUpdated (PathElement element);

    /** Notify the listener that a PathElement was removed from a project */
    public void elementRemoved (PathElement element);
}
