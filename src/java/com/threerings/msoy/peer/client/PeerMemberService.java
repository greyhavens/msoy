//
// $Id$

package com.threerings.msoy.peer.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

/**
 * Provides member-related peer services.
 */
public interface PeerMemberService extends InvocationService
{
    /**
     * Reports the number of unread mail messages the member has.
     */
    public void reportUnreadMail (Client client, int memberId, boolean hasNewMail);
}
