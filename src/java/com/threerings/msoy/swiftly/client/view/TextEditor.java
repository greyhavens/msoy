//
// $Id$

package com.threerings.msoy.swiftly.client.view;

import com.threerings.msoy.swiftly.data.SwiftlyTextDocument;

/**
 * A component to edit SwiftlyTextDocuments.
 */
public interface TextEditor
    extends PositionableComponent, AccessControlComponent,
            DocumentEditor<SwiftlyTextDocument>
{
    /**
     * Inform the TextEditor that the contents of the SwiftlyTextDocument it is displaying
     * has changed.
     */
    public void documentTextChanged ();
}