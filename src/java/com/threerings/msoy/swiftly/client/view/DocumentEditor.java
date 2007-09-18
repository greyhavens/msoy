//
// $Id$

package com.threerings.msoy.swiftly.client.view;

import com.threerings.msoy.swiftly.data.SwiftlyDocument;

/**
 * A component to edit/view SwiftlyDocuments.
 */
public interface DocumentEditor<D extends SwiftlyDocument>
{
    /**
     * Return the SwiftlyDocument loaded in this DocumentEditor
     */
    public D getSwiftlyDocument ();

    /**
     * Inform the DocumentEditor to display the supplied SwiftlyDocument.
     */
    public void loadDocument (D doc);
}