//
// $Id$

package com.threerings.msoy.peer.data;

import com.threerings.io.Streamable;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.peer.client.MsoyPeerService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.InvocationMarshaller;

/**
 * Provides the implementation of the {@link MsoyPeerService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class MsoyPeerMarshaller extends InvocationMarshaller
    implements MsoyPeerService
{
    /** The method id used to dispatch {@link #forwardMemberObject} requests. */
    public static final int FORWARD_MEMBER_OBJECT = 1;

    // from interface MsoyPeerService
    public void forwardMemberObject (Client arg1, MemberObject arg2, Streamable[] arg3)
    {
        sendRequest(arg1, FORWARD_MEMBER_OBJECT, new Object[] {
            arg2, arg3
        });
    }

    /** The method id used to dispatch {@link #reclaimItem} requests. */
    public static final int RECLAIM_ITEM = 2;

    // from interface MsoyPeerService
    public void reclaimItem (Client arg1, int arg2, int arg3, ItemIdent arg4, InvocationService.ConfirmListener arg5)
    {
        InvocationMarshaller.ConfirmMarshaller listener5 = new InvocationMarshaller.ConfirmMarshaller();
        listener5.listener = arg5;
        sendRequest(arg1, RECLAIM_ITEM, new Object[] {
            Integer.valueOf(arg2), Integer.valueOf(arg3), arg4, listener5
        });
    }
}
