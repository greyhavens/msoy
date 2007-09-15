//
//$Id$

package com.threerings.msoy.swiftly.client.event;

import com.threerings.msoy.swiftly.data.SwiftlyDocument;

public interface SwiftlyDocumentListener
{
    /** Notify the listener that a SwiftlyDocument was added to the project */
    public void documentAdded (SwiftlyDocument doc);

    /** Notify the listener that a SwiftlyDocument was updated in a project */
    public void documentUpdated (SwiftlyDocument doc);

    /** Notify the listener that a SwiftlyDocument was removed from a project */
    public void documentRemoved (SwiftlyDocument doc);
}