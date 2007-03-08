//
// $Id$

package com.threerings.msoy.swiftly.server;

import com.threerings.msoy.swiftly.client.ProjectRoomService;
import com.threerings.msoy.swiftly.data.PathElement;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

/**
 * Defines the server-side of the {@link ProjectRoomService}.
 */
public interface ProjectRoomProvider extends InvocationProvider
{
    /**
     * Handles a {@link ProjectRoomService#addDocument} request.
     */
    public void addDocument (ClientObject caller, PathElement arg1, InvocationService.InvocationListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link ProjectRoomService#addPathElement} request.
     */
    public void addPathElement (ClientObject caller, PathElement arg1);

    /**
     * Handles a {@link ProjectRoomService#buildProject} request.
     */
    public void buildProject (ClientObject caller);

    /**
     * Handles a {@link ProjectRoomService#commitProject} request.
     */
    public void commitProject (ClientObject caller, String arg1, InvocationService.ConfirmListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link ProjectRoomService#deleteDocument} request.
     */
    public void deleteDocument (ClientObject caller, int arg1);

    /**
     * Handles a {@link ProjectRoomService#deletePathElement} request.
     */
    public void deletePathElement (ClientObject caller, int arg1);

    /**
     * Handles a {@link ProjectRoomService#finishFileUpload} request.
     */
    public void finishFileUpload (ClientObject caller, InvocationService.ConfirmListener arg1)
        throws InvocationException;

    /**
     * Handles a {@link ProjectRoomService#loadDocument} request.
     */
    public void loadDocument (ClientObject caller, PathElement arg1);

    /**
     * Handles a {@link ProjectRoomService#startFileUpload} request.
     */
    public void startFileUpload (ClientObject caller, PathElement arg1, InvocationService.ConfirmListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link ProjectRoomService#updateDocument} request.
     */
    public void updateDocument (ClientObject caller, int arg1, String arg2);

    /**
     * Handles a {@link ProjectRoomService#updatePathElement} request.
     */
    public void updatePathElement (ClientObject caller, PathElement arg1);

    /**
     * Handles a {@link ProjectRoomService#uploadFile} request.
     */
    public void uploadFile (ClientObject caller, byte[] arg1);
}
