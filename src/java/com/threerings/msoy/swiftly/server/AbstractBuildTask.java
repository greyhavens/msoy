//
// $Id$
package com.threerings.msoy.swiftly.server;

import static com.threerings.msoy.Log.log;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import com.samskivert.io.PersistenceException;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.swiftly.data.BuildResult;
import com.threerings.presents.client.InvocationService.ConfirmListener;

public abstract class AbstractBuildTask
    implements Runnable
{
    public AbstractBuildTask (ProjectRoomManager manager, MemberName member,
                            ConfirmListener listener)
    {
        _manager = manager;
        // snapshot the projectId, type and name while we are on the dobject thread
        _projectId = manager.getRoomObj().project.projectId;

        _member = member;
        _listener = listener;
    }

    // from Runnable
    public void run ()
    {
        try {
            BuildResult result = BuildUtil.buildProject(this, _manager.getBuilder());
            publishResult(result);

        } catch (Exception e) {
            processFailure(e, "e.build_failed_unexpected");
        }
    }

    /**
     * Returns the MemberName who requested this build.
     */
    public MemberName getMember ()
    {
        return _member;
    }

    /**
     * Returns the projectId this task is building.
     */
    public int getProjectId ()
    {
        return _projectId;
    }

    /**
     * Publishes the build result into the distributed object.
     */
    public abstract void publishResult (final BuildResult result);

    /**
     * Let any subclass process the build artifact before it is deleted.
     */
    public abstract void processArtifact (File artifact)
        throws IOException, PersistenceException;

    /**
     * Code to be run back on the calling thread, usually the dobject thread, to handle build
     * failure.
     */
    public void processFailure (Exception error, final String reason) {
        log.log(Level.WARNING, "Project build failed.", error);
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run() {
                _listener.requestFailed(reason);
            }
        });
    }

    protected final ProjectRoomManager _manager;
    protected final ConfirmListener _listener;
    protected final int _projectId;
    protected final MemberName _member;
}
