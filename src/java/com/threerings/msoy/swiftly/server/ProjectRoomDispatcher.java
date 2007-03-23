//
// $Id$

package com.threerings.msoy.swiftly.server;

import com.threerings.msoy.swiftly.client.ProjectRoomService;
import com.threerings.msoy.swiftly.data.PathElement;
import com.threerings.msoy.swiftly.data.ProjectRoomMarshaller;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.server.InvocationDispatcher;
import com.threerings.presents.server.InvocationException;

/**
 * Dispatches requests to the {@link ProjectRoomProvider}.
 */
public class ProjectRoomDispatcher extends InvocationDispatcher
{
    /**
     * Creates a dispatcher that may be registered to dispatch invocation
     * service requests for the specified provider.
     */
    public ProjectRoomDispatcher (ProjectRoomProvider provider)
    {
        this.provider = provider;
    }

    // from InvocationDispatcher
    public InvocationMarshaller createMarshaller ()
    {
        return new ProjectRoomMarshaller();
    }

    @SuppressWarnings("unchecked") // from InvocationDispatcher
    public void dispatchRequest (
        ClientObject source, int methodId, Object[] args)
        throws InvocationException
    {
        switch (methodId) {
        case ProjectRoomMarshaller.ADD_DOCUMENT:
            ((ProjectRoomProvider)provider).addDocument(
                source,
                (PathElement)args[0], (InvocationService.InvocationListener)args[1]
            );
            return;

        case ProjectRoomMarshaller.ADD_PATH_ELEMENT:
            ((ProjectRoomProvider)provider).addPathElement(
                source,
                (PathElement)args[0]
            );
            return;

        case ProjectRoomMarshaller.BUILD_PROJECT:
            ((ProjectRoomProvider)provider).buildProject(
                source                
            );
            return;

        case ProjectRoomMarshaller.COMMIT_PROJECT:
            ((ProjectRoomProvider)provider).commitProject(
                source,
                (String)args[0], (InvocationService.ConfirmListener)args[1]
            );
            return;

        case ProjectRoomMarshaller.DELETE_DOCUMENT:
            ((ProjectRoomProvider)provider).deleteDocument(
                source,
                ((Integer)args[0]).intValue()
            );
            return;

        case ProjectRoomMarshaller.DELETE_PATH_ELEMENT:
            ((ProjectRoomProvider)provider).deletePathElement(
                source,
                ((Integer)args[0]).intValue()
            );
            return;

        case ProjectRoomMarshaller.FINISH_FILE_UPLOAD:
            ((ProjectRoomProvider)provider).finishFileUpload(
                source,
                (InvocationService.ConfirmListener)args[0]
            );
            return;

        case ProjectRoomMarshaller.LOAD_DOCUMENT:
            ((ProjectRoomProvider)provider).loadDocument(
                source,
                (PathElement)args[0], (InvocationService.ConfirmListener)args[1]
            );
            return;

        case ProjectRoomMarshaller.START_FILE_UPLOAD:
            ((ProjectRoomProvider)provider).startFileUpload(
                source,
                (PathElement)args[0], (InvocationService.ConfirmListener)args[1]
            );
            return;

        case ProjectRoomMarshaller.UPDATE_DOCUMENT:
            ((ProjectRoomProvider)provider).updateDocument(
                source,
                ((Integer)args[0]).intValue(), (String)args[1]
            );
            return;

        case ProjectRoomMarshaller.UPDATE_PATH_ELEMENT:
            ((ProjectRoomProvider)provider).updatePathElement(
                source,
                (PathElement)args[0]
            );
            return;

        case ProjectRoomMarshaller.UPLOAD_FILE:
            ((ProjectRoomProvider)provider).uploadFile(
                source,
                (byte[])args[0]
            );
            return;

        default:
            super.dispatchRequest(source, methodId, args);
            return;
        }
    }
}
