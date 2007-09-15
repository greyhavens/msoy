//
// $Id$

package com.threerings.msoy.swiftly.client.controller;

import com.threerings.msoy.swiftly.data.SwiftlyTextDocument;

/**
 * Handles dispatch document update events.
 */
public interface DocumentUpdateDispatcher
{
    /**
     * The contents of the supplied SwiftlyTextDocument have changed to the supplied text.
     */
    public void documentTextChanged (SwiftlyTextDocument doc, String text);
}
