//
// $Id$

package com.threerings.msoy.swiftly.client.view;

import com.threerings.msoy.swiftly.data.SwiftlyImageDocument;

/**
 * A component to view/edit SwiftlyImageDocuments.
 */
public interface ImageEditor
{
    /**
     * Inform the ImageEditor display the supplied SwiftlyImageDocument.
     */
    public void loadDocument (SwiftlyImageDocument doc);
}