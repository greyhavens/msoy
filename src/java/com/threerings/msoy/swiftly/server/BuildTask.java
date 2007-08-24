//
// $Id$
package com.threerings.msoy.swiftly.server;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.swiftly.data.BuildResult;
import com.threerings.msoy.swiftly.server.build.BuildArtifact;
import com.threerings.presents.client.InvocationService.ConfirmListener;

/** Handles a request to build our project. */
public class BuildTask extends AbstractBuildTask
{
    public BuildTask (ProjectRoomManager manager, MemberName member, ConfirmListener listener)
    {
        super(manager, member, listener);
    }

    @Override // from CommonBuildTask
    public void publishResult (final BuildResult result)
    {
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run() {
                // Provide build output to the room
                _manager.getRoomObj().publishBuildResult(result);

                _listener.requestProcessed();
            }
        });
    }

    @Override // from CommonBuildTask
    public void processArtifact (final BuildArtifact artifact)
    {
        // BuildTask does nothing with the artifact
    }
}