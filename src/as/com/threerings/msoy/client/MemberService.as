//
// $Id$

package com.threerings.msoy.client {

import com.threerings.util.Name;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

/**
 * Services for members.
 */
public interface MemberService extends InvocationService
{
    /**
     * Request to add the specified friend to the user's buddylist.
     */
    function alterFriend (
            client :Client, friendId :int, add :Boolean,
            listener :InvocationService_InvocationListener);
}
}
