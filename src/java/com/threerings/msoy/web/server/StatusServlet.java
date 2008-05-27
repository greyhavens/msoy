//
// $Id$

package com.threerings.msoy.web.server;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;
import java.util.logging.Level;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.collect.Maps;
import com.samskivert.io.StreamUtil;

import com.threerings.presents.peer.data.ClientInfo;
import com.threerings.presents.peer.data.NodeObject;
import com.threerings.presents.peer.server.PeerManager;

import com.threerings.msoy.data.MemberLocation;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.peer.data.MsoyClientInfo;
import com.threerings.msoy.peer.data.MsoyNodeObject;
import com.threerings.msoy.server.MsoyServer;
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
        final PeerManager.Operation collector = new PeerManager.Operation() {
            public void apply (NodeObject nodeobj) {
                info.put(nodeobj.nodeName, collectInfo((MsoyNodeObject)nodeobj));
            }
        };

        try {
            final ServletWaiter<Void> waiter = new ServletWaiter<Void>("collectStats");
            MsoyServer.omgr.postRunnable(new Runnable() {
                public void run () {
                    MsoyServer.peerMan.applyToNodes(collector);
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
        info.channels = mnobj.hostedChannels.size();

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
}
