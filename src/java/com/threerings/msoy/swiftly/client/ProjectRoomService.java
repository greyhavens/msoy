//
// $Id$

package com.threerings.msoy.swiftly.client;

import com.threerings.msoy.swiftly.data.PathElement;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

/**
 * Provides invocation services pertaining to a particular project room.
 */
public interface ProjectRoomService extends InvocationService
{
    /** Requests a document be loaded. */
    public void loadDocument (Client client, PathElement element, ConfirmListener listener);

    /** Requests to add a document to the project. */
    public void addDocument (Client client, String fileName, PathElement parent, String mimeType,
                             ConfirmListener listener);

    /**
     * Requests that the specified document be updated (currently wholesale but some day with
     * diffs).
     */
    public void updateTextDocument (Client client, int documentId, String text,
                                    ConfirmListener listener);
    /** Requests that the specified path element be removed from the project. */
    public void deletePathElement (Client client, int elementId, ConfirmListener listener);

    /** Requests that the specified path element be renamed. */
    public void renamePathElement (Client client, int elementId, String newName,
                                   ConfirmListener listener);

    /** Requests that the project be build and the artifacts be published to the game object. */
    public void buildProject (Client client, ResultListener listener);

    /** Load the build result into this users inventory. */
    public void buildAndExportProject (Client client, ResultListener listener);
}
