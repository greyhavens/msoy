//
// $Id$

package com.threerings.msoy.peer.server;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.Communicator;
import com.threerings.presents.dobj.DSet;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.SetListener;
import com.threerings.presents.server.PresentsDObjectMgr;
import com.threerings.presents.server.net.ConnectionManager;
import com.threerings.presents.server.net.ServerCommunicator;

import com.threerings.presents.peer.data.ClientInfo;
import com.threerings.presents.peer.data.NodeObject;
import com.threerings.presents.peer.server.PeerManager;
import com.threerings.presents.peer.server.PeerNode;
import com.threerings.presents.peer.server.persist.NodeRecord;

import com.threerings.msoy.data.MemberLocation;
import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.peer.data.MsoyClientInfo;
import com.threerings.msoy.peer.data.MsoyNodeObject;
import com.threerings.msoy.server.ServerConfig;

/**
 * Handles Whirled-specific peer bits.
 */
public class MsoyPeerNode extends PeerNode
{
    @Override // from PeerNode
    public void init (PeerManager peermgr, PresentsDObjectMgr omgr, ConnectionManager conmgr,
                      NodeRecord record)
    {
        super.init(peermgr, omgr, conmgr, record);
        _httpPort = ServerConfig.getHttpPort(record.nodeName);
    }

    @Override // from PeerNode
    public void objectAvailable (NodeObject object)
    {
        super.objectAvailable(object);

        // map and issue a remoteMemberLoggedOn for all logged on members
        for (ClientInfo info : object.clients) {
            ((MsoyPeerManager)_peermgr).memberLoggedOn(getNodeName(), (MsoyClientInfo)info);
        }
    }

    @Override // from PeerNode
    public void clientDidLogoff (Client client)
    {
        // issue a remoteMemberLoggedOff for all members that were on this peer
        for (ClientInfo info : nodeobj.clients) {
            ((MsoyPeerManager)_peermgr).memberLoggedOff(getNodeName(), (MsoyClientInfo)info);
        }
        super.clientDidLogoff(client);
    }

    /**
     * Return the HTTP port this Whirled node is listening on.
     */
    public int getHttpPort ()
    {
        return _httpPort;
    }

    @Override // from PeerNode
    protected Communicator createCommunicator (Client client)
    {
        if (DeploymentConfig.devDeployment) {
            return new ServerCommunicator(client, _conmgr, _omgr);
        } else {
            return super.createCommunicator(client);
        }
    }

    @Override // from PeerNode
    protected NodeObjectListener createListener ()
    {
        return new MsoyNodeObjectListener();
    }

    /**
     * Extends the base NodeListener with Msoy-specific bits.
     */
    protected class MsoyNodeObjectListener extends NodeObjectListener
    {
        @Override
        public void entryAdded (EntryAddedEvent<DSet.Entry> event)
        {
            super.entryAdded(event);

            String name = event.getName();
            if (MsoyNodeObject.MEMBER_LOCS.equals(name)) {
                ((MsoyPeerManager)_peermgr).memberEnteredScene(
                    getNodeName(), (MemberLocation)event.getEntry());

            } else if (NodeObject.CLIENTS.equals(name)) {
                ((MsoyPeerManager)_peermgr).memberLoggedOn(
                    getNodeName(), (MsoyClientInfo)event.getEntry());
            }
        }

        @Override
        public void entryUpdated (EntryUpdatedEvent<DSet.Entry> event)
        {
            super.entryUpdated(event);

            String name = event.getName();
            if (MsoyNodeObject.MEMBER_LOCS.equals(name)) {
                ((MsoyPeerManager)_peermgr).memberEnteredScene(
                    getNodeName(), (MemberLocation)event.getEntry());
            }
        }

        @Override
        public void entryRemoved (EntryRemovedEvent<DSet.Entry> event)
        {
            super.entryRemoved(event);

            String name = event.getName();
            if (NodeObject.CLIENTS.equals(name)) {
                ((MsoyPeerManager)_peermgr).memberLoggedOff(
                    getNodeName(), (MsoyClientInfo)event.getOldEntry());
            }
        }
    } // END: class MsoyNodeObjectListener

    /** The HTTP port this Whirled node is listening on.  */
    protected int _httpPort;
}
