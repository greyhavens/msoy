//
// $Id$

package com.threerings.msoy.peer.server;

import com.threerings.presents.client.Client;
import com.threerings.presents.dobj.DSet;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.SetListener;
import com.threerings.presents.server.PresentsDObjectMgr;

import com.threerings.presents.peer.data.ClientInfo;
import com.threerings.presents.peer.data.NodeObject;
import com.threerings.presents.peer.server.PeerManager;
import com.threerings.presents.peer.server.PeerNode;
import com.threerings.presents.peer.server.persist.NodeRecord;

import com.threerings.msoy.data.MemberLocation;
import com.threerings.msoy.peer.data.MsoyClientInfo;
import com.threerings.msoy.peer.data.MsoyNodeObject;
import com.threerings.msoy.server.ServerConfig;

/**
 * Handles Whirled-specific peer bits.
 */
public class MsoyPeerNode extends PeerNode
    implements SetListener<DSet.Entry>
{
    @Override // from PeerNode
    public void init (PeerManager peermgr, PresentsDObjectMgr omgr, NodeRecord record)
    {
        super.init(peermgr, omgr, record);
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

    // from interface SetListener
    public void entryAdded (EntryAddedEvent<DSet.Entry> event)
    {
        if (event.getName().equals(MsoyNodeObject.MEMBER_LOCS)) {
            ((MsoyPeerManager)_peermgr).memberEnteredScene(
                getNodeName(), (MemberLocation)event.getEntry());
        }
        if (event.getName().equals(NodeObject.CLIENTS)) {
            ((MsoyPeerManager)_peermgr).memberLoggedOn(
                getNodeName(), (MsoyClientInfo)event.getEntry());
        }
    }

    // from interface SetListener
    public void entryUpdated (EntryUpdatedEvent<DSet.Entry> event)
    {
        if (event.getName().equals(MsoyNodeObject.MEMBER_LOCS)) {
            ((MsoyPeerManager)_peermgr).memberEnteredScene(
                getNodeName(), (MemberLocation)event.getEntry());
        }
    }

    // from interface SetListener
    public void entryRemoved (EntryRemovedEvent<DSet.Entry> event)
    {
        if (event.getName().equals(NodeObject.CLIENTS)) {
            ((MsoyPeerManager)_peermgr).memberLoggedOff(
                getNodeName(), (MsoyClientInfo)event.getOldEntry());
        }
    }

    /**
     * Return the HTTP port this Whirled node is listening on.
     */
    public int getHttpPort ()
    {
        return _httpPort;
    }

    /** The HTTP port this Whirled node is listening on.  */
    protected int _httpPort;
}
