//
// $Id$
package com.threerings.msoy.swiftly.server;

import static com.threerings.msoy.Log.log;

import java.util.ArrayList;
import java.util.logging.Level;

import com.samskivert.util.SerialExecutor;
import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.swiftly.data.SwiftlyDocument;
import com.threerings.presents.client.InvocationService.ResultListener;

/** Handles a request to commit our project. */
public class CommitProjectTask
    implements SerialExecutor.ExecutorTask
{
    /**
     * Only commit the project, do not perform a build.
     */
    public CommitProjectTask (ProjectRoomManager manager, ResultListener listener)
    {
        this(manager, null, listener);
    }

    /**
     * Commit the project, then perform a build.
     */
    public CommitProjectTask (ProjectRoomManager manager, AbstractBuildTask buildTask,
                              ResultListener listener)
    {
        _manager = manager;
        // take a snapshot of certain items while we're on the dobj thread
        _allDocs = manager.getRoomObj().documents.toArray(
            new SwiftlyDocument[manager.getRoomObj().documents.size()]);

        _buildTask = buildTask;
        _listener = listener;

    }

    public boolean merge (SerialExecutor.ExecutorTask other)
    {
        return true;
    }

    public long getTimeout ()
    {
        return 60 * 1000L; // 60 seconds is all you get kid
    }

    // this is called on the executor thread and can go hog wild with the blocking
    public void executeTask ()
    {
        try {
            // commit each swiftly document in the project that has changed
            for (SwiftlyDocument doc : _allDocs) {
                if (doc.isDirty()) {
                    _manager.getStorage().putDocument(doc, "Automatic Swiftly Commit");
                    doc.commit();
                    _modDocs.add(doc);
                }
            }

        } catch (Throwable error) {
            // we'll report this on resultReceived()
            _error = error;
        }
    }

    // this is called back on the dobj thread and must only report results
    public void resultReceived ()
    {
        // update the documents that were committed (any that got added to this list got
        // committed, even if we later failed)
        for (SwiftlyDocument doc : _modDocs) {
            _manager.getRoomObj().updateDocuments(doc);
        }

        if (_error != null) {
            log.log(Level.WARNING, "Project storage commit failed.", _error);
            _listener.requestFailed("e.commit_failed_unexpected");
            return;
        }

        // if the commit worked, run the build if instructed
        if (buildRequested()) {
            MsoyServer.swiftlyMan.buildExecutor.execute(_buildTask);

        } else {
            // TODO: can we avoid using null here?
            _listener.requestProcessed(null);
        }
    }

    // this is called back on the dobj thread and must only report failure
    public void timedOut ()
    {
        _listener.requestFailed("e.commit_timed_out");
    }

    protected boolean buildRequested ()
    {
        return (_buildTask != null);
    }

    protected final SwiftlyDocument[] _allDocs;
    protected final ArrayList<SwiftlyDocument> _modDocs = new ArrayList<SwiftlyDocument>();

    protected final ProjectRoomManager _manager;
    protected final AbstractBuildTask _buildTask;
    protected final ResultListener _listener;
    protected Throwable _error;
}