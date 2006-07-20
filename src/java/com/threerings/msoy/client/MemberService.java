//
// $Id$

package com.threerings.msoy.client;

import com.threerings.util.Name;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

/**
 * Services for members.
 */
public interface MemberService extends InvocationService
{
    /**
     * Request to add the specified user to the client's friendlist.
     */
    public void alterFriend (
            Client client, int friendId, boolean add,
            InvocationListener listener);
}
