//
// $Id$

package com.threerings.msoy.swiftly.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

import com.threerings.msoy.swiftly.data.PathElement;
import com.threerings.msoy.swiftly.data.SwiftlyDocument;

/**
 * Provides invocation services pertaining to a particular project room.
 */
public interface ProjectRoomService extends InvocationService
{
    /** Requests to add a path element to the project. */
    public void addPathElement (Client client, PathElement element);

    /** Requests that the specified path element be updated (wholesale). */
    public void updatePathElement (Client client, PathElement element);

    /** Requests that the specified path element be removed from the project. */
    public void deletePathElement (Client client, int elementId);

    /** Requests to add a document to the project. element is the "parent" of the new document. */
    public void addDocument (Client client, PathElement element, InvocationListener listener);

    /** Requests that the specified document be updated (currently wholesale but some day with
     * diffs). */
    public void updateDocument (Client client, int elementId, String text);

    /** Requests that the specified document be removed from the project. */
    public void deleteDocument (Client client, int elementId);

    /** Requests that the project be build and the artifacts be published to the game object. */
    public void buildProject (Client client);

    /** Requests pending project modifications be committed. */
    public void commitProject (Client client, String commitMsg, ConfirmListener listener);

    /** Requests a document be loaded. */
    public void loadDocument (Client client, PathElement element, ConfirmListener listener);

    /** Reqests that an upload file session be started */
    public void startFileUpload (Client client, PathElement parent, ConfirmListener listener);

    /** Uploads some file data */
    public void uploadFile (Client client, byte[] data);

    /** Finalizes a file upload */
    public void finishFileUpload (Client client, ConfirmListener listener);
}
