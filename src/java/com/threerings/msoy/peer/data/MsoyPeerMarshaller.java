//
// $Id$

package com.threerings.msoy.peer.data;

import javax.annotation.Generated;

import com.threerings.io.Streamable;

import com.threerings.util.Name;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationMarshaller;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.peer.client.MsoyPeerService;

/**
 * Provides the implementation of the {@link MsoyPeerService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           comments="Derived from MsoyPeerService.java.")
public class MsoyPeerMarshaller extends InvocationMarshaller<ClientObject>
    implements MsoyPeerService
{
    /** The method id used to dispatch {@link #forwardMemberObject} requests. */
    public static final int FORWARD_MEMBER_OBJECT = 1;

    // from interface MsoyPeerService
    public void forwardMemberObject (MemberObject arg1, Streamable[] arg2)
    {
        sendRequest(FORWARD_MEMBER_OBJECT, new Object[] {
            arg1, arg2
        });
    }

    /** The method id used to dispatch {@link #reclaimItem} requests. */
    public static final int RECLAIM_ITEM = 2;

    // from interface MsoyPeerService
    public void reclaimItem (int arg1, int arg2, ItemIdent arg3, InvocationService.ConfirmListener arg4)
    {
        InvocationMarshaller.ConfirmMarshaller listener4 = new InvocationMarshaller.ConfirmMarshaller();
        listener4.listener = arg4;
        sendRequest(RECLAIM_ITEM, new Object[] {
            Integer.valueOf(arg1), Integer.valueOf(arg2), arg3, listener4
        });
    }

    /** The method id used to dispatch {@link #transferRoomOwnership} requests. */
    public static final int TRANSFER_ROOM_OWNERSHIP = 3;

    // from interface MsoyPeerService
    public void transferRoomOwnership (int arg1, byte arg2, int arg3, Name arg4, boolean arg5, InvocationService.ConfirmListener arg6)
    {
        InvocationMarshaller.ConfirmMarshaller listener6 = new InvocationMarshaller.ConfirmMarshaller();
        listener6.listener = arg6;
        sendRequest(TRANSFER_ROOM_OWNERSHIP, new Object[] {
            Integer.valueOf(arg1), Byte.valueOf(arg2), Integer.valueOf(arg3), arg4, Boolean.valueOf(arg5), listener6
        });
    }
}
