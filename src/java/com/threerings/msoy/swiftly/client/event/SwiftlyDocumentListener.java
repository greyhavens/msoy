//
// $Id$

package com.threerings.msoy.swiftly.client.event;

import com.threerings.msoy.swiftly.data.SwiftlyDocument;
import com.threerings.msoy.swiftly.data.SwiftlyImageDocument;
import com.threerings.msoy.swiftly.data.SwiftlyTextDocument;

public interface SwiftlyDocumentListener
{
    /** Notify the listener that a SwiftlyDocument was added to the project */
    void documentAdded (SwiftlyDocument doc);

    /** Notify the listener that a SwiftlyTextDocument was updated in a project */
    void documentUpdated (SwiftlyTextDocument doc);

    /** Notify the listener that a SwiftlyImageDocument was updated in a project */
    void documentUpdated (SwiftlyImageDocument doc);

    /** Notify the listener that a SwiftlyDocument was removed from a project */
    void documentRemoved (SwiftlyDocument doc);
}