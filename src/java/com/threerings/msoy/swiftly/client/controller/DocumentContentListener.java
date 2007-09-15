//
// $Id$

package com.threerings.msoy.swiftly.client.controller;

import com.threerings.msoy.swiftly.data.SwiftlyTextDocument;

public interface DocumentContentListener
{
    /**
     * Inform the listener that the contents of the supplied SwiftlyTextDocument have changed.
     */
    public void documentContentsChanged (SwiftlyTextDocument doc);
}