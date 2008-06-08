//
// $Id$

package com.threerings.msoy.peer.server;

import com.threerings.presents.client.Client;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.SetListener;

import com.threerings.presents.peer.data.ClientInfo;
import com.threerings.presents.peer.data.NodeObject;
import com.threerings.presents.peer.server.PeerNode;

import com.threerings.msoy.peer.data.MsoyClientInfo;

/**
 * Handles Whirled-specific peer bits.
 */
public class MsoyPeerNode extends PeerNode
    implements SetListener
{
    public MsoyPeerNode (int httpPort)
    {
        _httpPort = httpPort;
    }

    @Override // from PeerNode
    public void objectAvailable (NodeObject object)
    {
        super.objectAvailable(object);

        // map and issue a remoteMemberLoggedOn for all logged on members
        for (ClientInfo info : object.clients) {
            ((MsoyPeerManager)_peermgr).remoteMemberLoggedOn(this, (MsoyClientInfo)info);
        }
    }

    @Override // from PeerNode
    public void clientDidLogoff (Client client)
    {
        // issue a remoteMemberLoggedOff for all members that were on this peer
        for (ClientInfo info : nodeobj.clients) {
            ((MsoyPeerManager)_peermgr).remoteMemberLoggedOff(this, (MsoyClientInfo)info);
        }
        super.clientDidLogoff(client);
    }

    // from interface SetListener
    public void entryAdded (EntryAddedEvent event)
    {
        if (event.getName().equals(NodeObject.CLIENTS)) {
            ((MsoyPeerManager)_peermgr).remoteMemberLoggedOn(
                this, (MsoyClientInfo)event.getEntry());
        }
    }

    // from interface SetListener
    public void entryUpdated (EntryUpdatedEvent event)
    {
        // nada
    }

    // from interface SetListener
    public void entryRemoved (EntryRemovedEvent event)
    {
        if (event.getName().equals(NodeObject.CLIENTS)) {
            ((MsoyPeerManager)_peermgr).remoteMemberLoggedOff(
                this, (MsoyClientInfo)event.getOldEntry());
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
