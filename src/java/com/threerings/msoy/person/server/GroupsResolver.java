//
// $Id$

package com.threerings.msoy.person.server;

import java.util.List;

import com.samskivert.util.ResultListener;

import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.web.data.GroupMembership;

/**
 * Resolves a person's friend information.
 */
public class GroupsResolver extends BlurbResolver
{
    @Override // from BlurbResolver
    protected void resolve ()
    {
        MsoyServer.groupMan.getMembershipGroups(
            _memberId, false, new ResultListener<List<GroupMembership>>() {
                public void requestCompleted (List<GroupMembership> groups) {
                    resolutionCompleted(groups);
                }
                public void requestFailed (Exception cause) {
                    resolutionFailed(cause);
                }
            });
    }
}
