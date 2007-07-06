//
// $Id$

package com.threerings.msoy.swiftly.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

import com.threerings.msoy.swiftly.data.PathElement;

/**
 * Provides invocation services pertaining to a particular project room.
 */
public interface ProjectRoomService extends InvocationService
{
    /** Requests to add a path element to the project. */
    public void addPathElement (Client client, PathElement element, InvocationListener listener);

    /** Requests that the specified path element be updated (wholesale). */
    public void updatePathElement (Client client, PathElement element,
                                   InvocationListener listener);

    /** Requests that the specified path element be removed from the project. */
    public void deletePathElement (Client client, int elementId, ConfirmListener listener);

    /** Requests that the specified path element be renamed. */
    public void renamePathElement (Client client, int elementId, String newName,
                                   ConfirmListener listener);

    /** Requests to add a document to the project. */
    public void addDocument (Client client, String fileName, PathElement parent, String mimeType,
                             InvocationListener listener);

    /** Requests that the specified document be updated (currently wholesale but some day with
     * diffs). */
    public void updateDocument (Client client, int elementId, String text,
                                InvocationListener listener);

    /** Requests that the specified document be removed from the project. */
    public void deleteDocument (Client client, int elementId, InvocationListener listener);

    /** Requests that the project be build and the artifacts be published to the game object. */
    public void buildProject (Client client, ConfirmListener listener);
    
    /** Load the build result into this users inventory. */
    public void buildAndExportProject (Client client, ConfirmListener listener);

    /** Requests a document be loaded. */
    public void loadDocument (Client client, PathElement element, ConfirmListener listener);
}
