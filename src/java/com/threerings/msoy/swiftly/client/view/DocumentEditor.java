//
// $Id$

package com.threerings.msoy.swiftly.client.view;

import com.threerings.msoy.swiftly.data.SwiftlyDocument;

/**
 * A component to edit/view SwiftlyDocuments.
 */
public interface DocumentEditor<T extends SwiftlyDocument, E extends DocumentEditor<T, E>>
{
    /**
     * Return the SwiftlyDocument loaded in this DocumentEditor
     */
    public T getSwiftlyDocument ();

    /**
     * Inform the DocumentEditor display the supplied SwiftlyDocument.
     */
    public void loadDocument (T doc);

    /**
     * Register an object to receive notification when this DocumentEditor is removed.
     */
    public void addDocumentEditorRemovalNotifier (RemovalNotifier<E> notifier);
}