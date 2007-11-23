//
// $Id$

package com.threerings.msoy.peer.client;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

/**
 * Provides member-related peer services.
 */
public interface PeerMemberService extends InvocationService
{
    /**
     * Reports whether or not the member has unread mail, and whether or not any of them were
     * just delivered.
     */
    public void reportUnreadMail (Client client, int memberId, int newMailCount);

    /**
     * Reports a flow update on the given member hosted on this peer.
     */
    public void flowUpdated (Client client, int memberId, int flow, int accFlow);

    /**
     * Reports a display name change on a given member hosted on this peer.
     */
    public void displayNameChanged (Client client, MemberName name);
}
