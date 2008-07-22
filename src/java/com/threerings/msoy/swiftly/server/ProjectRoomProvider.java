//
// $Id$

package com.threerings.msoy.swiftly.server;

import com.threerings.msoy.swiftly.client.ProjectRoomService;
import com.threerings.msoy.swiftly.data.PathElement;
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
    void addDocument (ClientObject caller, String arg1, PathElement arg2, String arg3, InvocationService.ConfirmListener arg4)
        throws InvocationException;

    /**
     * Handles a {@link ProjectRoomService#buildAndExportProject} request.
     */
    void buildAndExportProject (ClientObject caller, InvocationService.ResultListener arg1)
        throws InvocationException;

    /**
     * Handles a {@link ProjectRoomService#buildProject} request.
     */
    void buildProject (ClientObject caller, InvocationService.ResultListener arg1)
        throws InvocationException;

    /**
     * Handles a {@link ProjectRoomService#deletePathElement} request.
     */
    void deletePathElement (ClientObject caller, int arg1, InvocationService.ConfirmListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link ProjectRoomService#loadDocument} request.
     */
    void loadDocument (ClientObject caller, PathElement arg1, InvocationService.ConfirmListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link ProjectRoomService#renamePathElement} request.
     */
    void renamePathElement (ClientObject caller, int arg1, String arg2, InvocationService.ConfirmListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link ProjectRoomService#updateTextDocument} request.
     */
    void updateTextDocument (ClientObject caller, int arg1, String arg2, InvocationService.ConfirmListener arg3)
        throws InvocationException;
}
