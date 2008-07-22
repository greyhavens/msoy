//
// $Id$

package com.threerings.msoy.swiftly.client.event;

import com.threerings.msoy.swiftly.data.PathElement;

public interface PathElementListener
{
    /** Notify the listener that a PathElement was added to the project */
    void elementAdded (PathElement element);

    /** Notify the listener that a PathElement was updated in a project */
    void elementUpdated (PathElement element);

    /** Notify the listener that a PathElement was removed from a project */
    void elementRemoved (PathElement element);
}
