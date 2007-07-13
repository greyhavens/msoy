//
// $Id$

package com.threerings.msoy.peer.server;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.util.Invoker;

import com.threerings.presents.server.PresentsClient;
import com.threerings.crowd.peer.server.CrowdPeerManager;

import com.threerings.presents.peer.data.ClientInfo;
import com.threerings.presents.peer.data.NodeObject;
import com.threerings.presents.peer.server.persist.NodeRecord;

import com.threerings.msoy.data.MemberObject;

import com.threerings.msoy.peer.data.HostedScene;
import com.threerings.msoy.peer.data.MsoyClientInfo;
import com.threerings.msoy.peer.data.MsoyNodeObject;

import static com.threerings.msoy.Log.log;

/**
 * Manages communication with our peer servers, coordinates services that must work across peers.
 */
public class MsoyPeerManager extends CrowdPeerManager
{
    public MsoyPeerManager (ConnectionProvider conprov, Invoker invoker)
        throws PersistenceException
    {
        super(conprov, invoker);
    }

    /**
     * Called by the RoomManager when it is hosting a scene.
     */
    public void roomDidStartup (int sceneId, String name)
    {
        log.info("Hosting scene [id=" + sceneId + ", name=" + name + "].");
        _mnobj.addToHostedScenes(new HostedScene(sceneId, name));
    }

    /**
     * Called by the RoomManager when it is no longer hosting a scene.
     */
    public void roomDidShutdown (int sceneId)
    {
        log.info("No longer hosting scene [id=" + sceneId + "].");
        _mnobj.removeFromHostedScenes(sceneId);
    }

    @Override // from PeerManager
    protected PeerNode createPeerNode (NodeRecord record)
    {
        return new MsoyPeerNode(record);
    }

    @Override // from CrowdPeerManager
    protected NodeObject createNodeObject ()
    {
        return (_mnobj = new MsoyNodeObject());
    }

    @Override // from CrowdPeerManager
    protected ClientInfo createClientInfo ()
    {
        return new MsoyClientInfo();
    }

    @Override // from CrowdPeerManager
    protected void didInit ()
    {
        super.didInit();

//         // set up our node object
//         MsoyNodeObject mnodeobj = (MsoyNodeObject)_nodeobj;
//         mnodeobj.setMsoyPeerService(
//             (MsoyPeerMarshaller)MsoyServer.invmgr.registerDispatcher(new MsoyPeerDispatcher(this)));
    }

    protected class MsoyPeerNode extends PeerNode
    {
        public MsoyPeerNode (NodeRecord record) {
            super(record);
        }

        // TODO: stuff!
    }

    /** A casted reference to our node object. */
    protected MsoyNodeObject _mnobj;
}
