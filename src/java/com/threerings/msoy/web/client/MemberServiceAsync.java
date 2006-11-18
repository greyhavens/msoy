//
// $Id$

package com.threerings.msoy.web.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.threerings.msoy.web.data.WebCreds;

/**
 * The asynchronous (client-side) version of {@link MemberService}.
 */
public interface MemberServiceAsync
{
    /**
     * The asynchronous version of {@link MemberService#getName}.
     */
    public void getName (int memberId, AsyncCallback callback);

    /**
     * The asynchronous version of {@link MemberService#acceptFriend}.
     */
    public void acceptFriend (WebCreds creds, int friendId, AsyncCallback callback);

    /**
     * The asynchronous version of {@link MemberService#declineFriend}.
     */
    public void declineFriend (WebCreds creds, int friendId, AsyncCallback callback);

    /**
     * The asynchronous version of {@link MemberService#getNeighborhood}.
     */
    public void getNeighborhood (WebCreds creds, int memberId, AsyncCallback callback);
}
