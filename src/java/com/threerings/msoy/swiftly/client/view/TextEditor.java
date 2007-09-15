//
// $Id$

package com.threerings.msoy.swiftly.client.view;

import com.threerings.msoy.swiftly.data.SwiftlyTextDocument;

/**
 * A component to edit SwiftlyTextDocuments.
 */
public interface TextEditor
    extends PositionableComponent, AccessControlComponent
{
    /**
     * Return the SwiftlyTextDocument loaded in this TextEditor
     */
    public SwiftlyTextDocument getSwiftlyDocument ();

    /**
     * Inform the TextEditor display the supplied SwiftlyTextDocument.
     */
    public void loadDocument (SwiftlyTextDocument doc);

    /**
     * Inform the TextEditor that the contents of the SwiftlyTextDocument it is displaying
     * has changed.
     */
    public void documentTextChanged ();
}