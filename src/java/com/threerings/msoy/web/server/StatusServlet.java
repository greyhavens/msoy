//
// $Id$

package com.threerings.msoy.web.server;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.samskivert.io.StreamUtil;

import com.threerings.presents.dobj.RootDObjectManager;
import com.threerings.presents.peer.data.ClientInfo;
import com.threerings.presents.peer.data.NodeObject;

import com.threerings.msoy.data.MemberLocation;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.peer.data.MsoyClientInfo;
import com.threerings.msoy.peer.data.MsoyNodeObject;
import com.threerings.msoy.peer.server.MsoyPeerManager;
import com.threerings.msoy.web.data.ServiceException;

import static com.threerings.msoy.Log.log;

/**
 * Reports server status in plain text.
 */
public class StatusServlet extends HttpServlet
{
    @Override // from HttpServlet
    protected void doGet (HttpServletRequest req, HttpServletResponse rsp)
        throws IOException
    {
        final Map<String,ServerInfo> info = Maps.newHashMap();
        final Function<NodeObject,Void> collector = new Function<NodeObject,Void>() {
            public Void apply (NodeObject nodeobj) {
                info.put(nodeobj.nodeName, collectInfo((MsoyNodeObject)nodeobj));
                return null;
            }
        };

        try {
            final ServletWaiter<Void> waiter = new ServletWaiter<Void>("collectStats");
            _omgr.postRunnable(new Runnable() {
                public void run () {
                    _peerMan.applyToNodes(collector);
                    waiter.postSuccess(null);
                }
            });
            waiter.waitForResult();

        } catch (ServiceException se) {
            log.warning("Failed to gather stats.", se);
            rsp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        boolean details = "/details".equals(req.getPathInfo());
        PrintStream out = null;
        try {
            out = new PrintStream(rsp.getOutputStream());
            for (ServerInfo sinfo : info.values()) {
                out.println(sinfo);
                if (details) {
                    out.println(sinfo.details);
                }
            }
        } finally {
            StreamUtil.close(out);
        }
    }

    protected ServerInfo collectInfo (MsoyNodeObject mnobj)
    {
        ServerInfo info = new ServerInfo();
        info.name = mnobj.nodeName;
        info.rooms = mnobj.hostedScenes.size();
        info.games = mnobj.hostedGames.size();
        info.channels = mnobj.hostedChannelz.size();

        for (ClientInfo cinfo : mnobj.clients) {
            if (MemberName.isGuest(((MsoyClientInfo)cinfo).getMemberId())) {
                info.guests++;
            } else {
                info.members++;
            }
        }

        for (MemberLocation mloc : mnobj.memberLocs) {
            if (mloc.sceneId != 0) {
                info.inScene++;
            }
            if (mloc.gameId != 0) {
                info.inGame++;
            }
            info.details.append("- ").append(mloc).append("\n");
        }

        return info;
    }

    protected static class ServerInfo
    {
        public String name;

        public int members;
        public int guests;

        public int rooms;
        public int games;
        public int channels;

        public int inScene;
        public int inGame;

        public StringBuilder details = new StringBuilder();

        public String toString () {
            return name + " [members=" + members + ", guests=" + guests +
                ", inScene=" + inScene + ", inGame=" + inGame +
                ", rooms=" + rooms + ", games=" + games + ", channels=" + channels + "] ";
        }
    }

    // our dependencies
    @Inject protected RootDObjectManager _omgr;
    @Inject protected MsoyPeerManager _peerMan;
}
