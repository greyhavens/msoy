//
// $Id$

package com.threerings.msoy.person.server;

import com.samskivert.util.ResultListener;
import com.threerings.msoy.server.MsoyServer;

/**
 * Resolves a person's friend information.
 */
public class HoodResolver extends BlurbResolver
{
    @Override // from BlurbResolver
    protected void resolve ()
    {
        MsoyServer.memberMan.serializeNeighborhood(_memberId, false, new ResultListener<String>() {
            public void requestCompleted (String hood) {
                resolutionCompleted(hood);
            }
            public void requestFailed (Exception cause) {
                resolutionFailed(cause);
            }
        });
    }

}
