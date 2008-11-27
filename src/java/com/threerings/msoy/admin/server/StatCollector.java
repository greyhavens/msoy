//
// $Id$

package com.threerings.msoy.admin.server;

import com.threerings.presents.util.FutureResult;

import com.threerings.msoy.admin.client.PeerAdminService;
import com.threerings.msoy.admin.gwt.StatsModel;

import static com.threerings.msoy.Log.log;

/**
 * Manages the process of collecting statistics.
 */
public abstract class StatCollector
{
    /** Used to merge stats from multiple nodes. */
    public abstract class Merger extends FutureResult<StatsModel>
    {
        public int pendingNodes;

        public PeerAdminService.ResultListener makeListener (final String nodeName) {
            return new PeerAdminService.ResultListener() {
                public void requestProcessed (Object result) {
                    mergeStats(nodeName, result);
                    maybeDeliverResults();
                }
                public void requestFailed (String cause) {
                    log.warning("Stat collection failed", "node", nodeName, "cause", cause);
                    maybeDeliverResults();
                }
            };
        }

        protected void maybeDeliverResults () {
            if (--pendingNodes == 0) {
                super.requestProcessed(finalizeModel());
            }
        }

        /**
         * Called on the initiating server node when results have been received from one of the
         * other nodes.
         */
        protected abstract void mergeStats (String nodeName, Object stats);

        /**
         * Called when stats from all nodes have been merged, finalizes and returns model.
         */
        protected abstract StatsModel finalizeModel ();
    }

    /**
     * Called on each server node to compile the desired statistics. The returned result must be a
     * {@link Stremaable} or a primitive type that can be sent over the wire.
     */
    public abstract Object compileStats ();

    /**
     * Creates a merger instance that can be used to merge stats collected from all nodes.
     */
    public abstract Merger createMerger ();
}
