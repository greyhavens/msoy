//
// $Id$

package com.threerings.msoy.swiftly.data;

import com.threerings.presents.dobj.ChangeListener;

/**
 * Used to notify clients when a {@link DocumentUpdatedEvent} is received.
 */
public interface DocumentUpdateListener extends ChangeListener
{
    /**
     * Called when a document is updated.
     */
    public void documentUpdated (DocumentUpdatedEvent event);
}
