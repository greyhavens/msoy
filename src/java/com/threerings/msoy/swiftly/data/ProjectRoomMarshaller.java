//
// $Id$

package com.threerings.msoy.swiftly.data;

import com.threerings.msoy.swiftly.client.ProjectRoomService;
import com.threerings.msoy.swiftly.data.PathElement;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.InvocationMarshaller;

/**
 * Provides the implementation of the {@link ProjectRoomService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class ProjectRoomMarshaller extends InvocationMarshaller
    implements ProjectRoomService
{
    /** The method id used to dispatch {@link #addDocument} requests. */
    public static final int ADD_DOCUMENT = 1;

    // from interface ProjectRoomService
    public void addDocument (Client arg1, String arg2, PathElement arg3, String arg4, InvocationService.InvocationListener arg5)
    {
        ListenerMarshaller listener5 = new ListenerMarshaller();
        listener5.listener = arg5;
        sendRequest(arg1, ADD_DOCUMENT, new Object[] {
            arg2, arg3, arg4, listener5
        });
    }

    /** The method id used to dispatch {@link #addPathElement} requests. */
    public static final int ADD_PATH_ELEMENT = 2;

    // from interface ProjectRoomService
    public void addPathElement (Client arg1, PathElement arg2, InvocationService.InvocationListener arg3)
    {
        ListenerMarshaller listener3 = new ListenerMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, ADD_PATH_ELEMENT, new Object[] {
            arg2, listener3
        });
    }

    /** The method id used to dispatch {@link #buildAndExportProject} requests. */
    public static final int BUILD_AND_EXPORT_PROJECT = 3;

    // from interface ProjectRoomService
    public void buildAndExportProject (Client arg1, InvocationService.ConfirmListener arg2)
    {
        InvocationMarshaller.ConfirmMarshaller listener2 = new InvocationMarshaller.ConfirmMarshaller();
        listener2.listener = arg2;
        sendRequest(arg1, BUILD_AND_EXPORT_PROJECT, new Object[] {
            listener2
        });
    }

    /** The method id used to dispatch {@link #buildProject} requests. */
    public static final int BUILD_PROJECT = 4;

    // from interface ProjectRoomService
    public void buildProject (Client arg1, InvocationService.ConfirmListener arg2)
    {
        InvocationMarshaller.ConfirmMarshaller listener2 = new InvocationMarshaller.ConfirmMarshaller();
        listener2.listener = arg2;
        sendRequest(arg1, BUILD_PROJECT, new Object[] {
            listener2
        });
    }

    /** The method id used to dispatch {@link #deleteDocument} requests. */
    public static final int DELETE_DOCUMENT = 5;

    // from interface ProjectRoomService
    public void deleteDocument (Client arg1, int arg2, InvocationService.InvocationListener arg3)
    {
        ListenerMarshaller listener3 = new ListenerMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, DELETE_DOCUMENT, new Object[] {
            Integer.valueOf(arg2), listener3
        });
    }

    /** The method id used to dispatch {@link #deletePathElement} requests. */
    public static final int DELETE_PATH_ELEMENT = 6;

    // from interface ProjectRoomService
    public void deletePathElement (Client arg1, int arg2, InvocationService.ConfirmListener arg3)
    {
        InvocationMarshaller.ConfirmMarshaller listener3 = new InvocationMarshaller.ConfirmMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, DELETE_PATH_ELEMENT, new Object[] {
            Integer.valueOf(arg2), listener3
        });
    }

    /** The method id used to dispatch {@link #loadDocument} requests. */
    public static final int LOAD_DOCUMENT = 7;

    // from interface ProjectRoomService
    public void loadDocument (Client arg1, PathElement arg2, InvocationService.ConfirmListener arg3)
    {
        InvocationMarshaller.ConfirmMarshaller listener3 = new InvocationMarshaller.ConfirmMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, LOAD_DOCUMENT, new Object[] {
            arg2, listener3
        });
    }

    /** The method id used to dispatch {@link #renamePathElement} requests. */
    public static final int RENAME_PATH_ELEMENT = 8;

    // from interface ProjectRoomService
    public void renamePathElement (Client arg1, int arg2, String arg3, InvocationService.ConfirmListener arg4)
    {
        InvocationMarshaller.ConfirmMarshaller listener4 = new InvocationMarshaller.ConfirmMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, RENAME_PATH_ELEMENT, new Object[] {
            Integer.valueOf(arg2), arg3, listener4
        });
    }

    /** The method id used to dispatch {@link #updateDocument} requests. */
    public static final int UPDATE_DOCUMENT = 9;

    // from interface ProjectRoomService
    public void updateDocument (Client arg1, int arg2, String arg3, InvocationService.InvocationListener arg4)
    {
        ListenerMarshaller listener4 = new ListenerMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, UPDATE_DOCUMENT, new Object[] {
            Integer.valueOf(arg2), arg3, listener4
        });
    }

    /** The method id used to dispatch {@link #updatePathElement} requests. */
    public static final int UPDATE_PATH_ELEMENT = 10;

    // from interface ProjectRoomService
    public void updatePathElement (Client arg1, PathElement arg2, InvocationService.InvocationListener arg3)
    {
        ListenerMarshaller listener3 = new ListenerMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, UPDATE_PATH_ELEMENT, new Object[] {
            arg2, listener3
        });
    }
}
