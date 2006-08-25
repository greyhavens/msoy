//
// $Id$

package com.threerings.msoy.person.server;

import com.samskivert.util.ResultListener;

import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.web.data.Profile;

/**
 * Resolves a person's profile information.
 */
public class ProfileResolver extends BlurbResolver
{
    @Override // from BlurbResolver
    protected void resolve ()
    {
        MsoyServer.memberMan.loadProfile(
            _memberId, new ResultListener<Profile>() {
            public void requestCompleted (Profile profile) {
                resolutionCompleted(profile);
            }
            public void requestFailed (Exception cause) {
                resolutionFailed(cause);
            }
        });
    }
}
