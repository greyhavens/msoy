//
// $Id$

package com.threerings.msoy.person.server;

import java.util.ArrayList;
import java.util.List;

import com.samskivert.util.ResultListener;

import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.web.data.FriendEntry;

/**
 * Resolves a person's friend information.
 */
public class FriendsResolver extends BlurbResolver
{
    @Override // from BlurbResolver
    protected void resolve ()
    {
        MsoyServer.memberMan.loadFriends(_memberId, new ResultListener<List<FriendEntry>>() {
            public void requestCompleted (List<FriendEntry> friends) {
                ArrayList<FriendEntry> info = new ArrayList<FriendEntry>();
                for (FriendEntry entry : friends) {
                    info.add(entry);
                }
                resolutionCompleted(info);
            }
            public void requestFailed (Exception cause) {
                resolutionFailed(cause);
            }
        });
    }
}
