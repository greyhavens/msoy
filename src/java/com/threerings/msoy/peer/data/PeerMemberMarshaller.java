//
// $Id$

package com.threerings.msoy.peer.data;

import com.threerings.msoy.peer.client.PeerMemberService;
import com.threerings.presents.client.Client;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.dobj.InvocationResponseEvent;

/**
 * Provides the implementation of the {@link PeerMemberService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class PeerMemberMarshaller extends InvocationMarshaller
    implements PeerMemberService
{
    /** The method id used to dispatch {@link #reportUnreadMail} requests. */
    public static final int REPORT_UNREAD_MAIL = 1;

    // from interface PeerMemberService
    public void reportUnreadMail (Client arg1, int arg2, boolean arg3)
    {
        sendRequest(arg1, REPORT_UNREAD_MAIL, new Object[] {
            Integer.valueOf(arg2), Boolean.valueOf(arg3)
        });
    }
}
