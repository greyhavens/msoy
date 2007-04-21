//
// $Id$

package com.threerings.msoy.server;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.util.Invoker;

import com.threerings.presents.server.PresentsClient;

import com.threerings.presents.peer.data.ClientInfo;
import com.threerings.presents.peer.data.NodeObject;
import com.threerings.presents.peer.server.PeerManager;
import com.threerings.presents.peer.server.persist.NodeRecord;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyClientInfo;
import com.threerings.msoy.data.MsoyNodeObject;

/**
 * Manages communication with our peer servers, coordinates services that must work across peers.
 */
public class MsoyPeerManager extends PeerManager
{
    public MsoyPeerManager (ConnectionProvider conprov, Invoker invoker)
        throws PersistenceException
    {
        super(conprov, invoker);
    }

    @Override // from PeerManager
    protected PeerNode createPeerNode (NodeRecord record)
    {
        return new MsoyPeerNode(record);
    }

    @Override // from CrowdPeerManager
    protected NodeObject createNodeObject ()
    {
        return new MsoyNodeObject();
    }

    @Override // from CrowdPeerManager
    protected ClientInfo createClientInfo ()
    {
        return new MsoyClientInfo();
    }

    @Override // from CrowdPeerManager
    protected void initClientInfo (PresentsClient client, ClientInfo info)
    {
        super.initClientInfo(client, info);
        MsoyClientInfo minfo = (MsoyClientInfo)info;
        MemberObject user = (MemberObject)client.getClientObject();
        minfo.memberId = user.getMemberId();
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
}
