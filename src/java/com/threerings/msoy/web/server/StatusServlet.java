//
// $Id$

package com.threerings.msoy.web.server;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

import com.samskivert.io.StreamUtil;
import com.samskivert.util.Callables;
import com.samskivert.util.IntIntMap;
import com.samskivert.util.Tuple;

import com.threerings.presents.client.Client;
import com.threerings.presents.dobj.DSet;
import com.threerings.presents.dobj.RootDObjectManager;
import com.threerings.presents.peer.data.ClientInfo;
import com.threerings.presents.peer.data.NodeObject;
import com.threerings.presents.server.ReportManager;
import com.threerings.presents.util.FutureResult;

import com.threerings.msoy.data.MemberLocation;
import com.threerings.msoy.data.MsoyAuthName;
import com.threerings.msoy.game.data.GameAuthName;
import com.threerings.msoy.party.data.PartyAuthName;
import com.threerings.msoy.peer.data.HostedPlace;
import com.threerings.msoy.peer.data.MemberGame;
import com.threerings.msoy.peer.data.MemberScene;
import com.threerings.msoy.peer.data.MsoyNodeObject;
import com.threerings.msoy.peer.server.MsoyPeerManager;
import com.threerings.msoy.server.MemberManager;
import com.threerings.msoy.server.MsoyEventLogger;

import com.threerings.msoy.party.data.PartyInfo;
import com.threerings.msoy.party.data.PartySummary;

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
        final Details details = parseDetails(req.getPathInfo());
        FutureTask<Map<String, ServerInfo>> task =
            new FutureTask<Map<String, ServerInfo>>(new Callable<Map<String, ServerInfo>>() {
            public Map<String, ServerInfo> call () throws Exception {
                return collectInfo(details);
            }
        });
        _omgr.postRunnable(task);

        try {
            Map<String, ServerInfo> info = task.get();
            PrintStream out = null;
            try {
                out = new PrintStream(rsp.getOutputStream());
                for (ServerInfo sinfo : info.values()) {
                    out.println(sinfo);
                    if (sinfo.details != null) {
                        try {
                            out.println(sinfo.details.call());
                            out.flush();
                        } catch (Exception e) {
                            out.println("Failed to get details: " + e.getMessage());
                            e.printStackTrace(out);
                        }
                    }
                }
            } finally {
                StreamUtil.close(out);
            }

        } catch (Throwable t) {
            if (t instanceof ExecutionException) {
                t = t.getCause();
            }
            log.warning("Failed to gather stats.", t);
            rsp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
    }

    protected static Details parseDetails (String pathinfo)
    {
        try {
            if (pathinfo != null && pathinfo.startsWith("/")) {
                return Enum.valueOf(Details.class, pathinfo.substring(1).toUpperCase());
            }
        } catch (Exception e) {
            log.info("Ignoring invalid status details", "pinfo", pathinfo);
        }
        return Details.NONE;
    }

    protected Map<String, ServerInfo> collectInfo (final Details details)
    {
        final Map<String,ServerInfo> info = Maps.newLinkedHashMap();

        // collect info on this server
        MsoyNodeObject nodeobj = (MsoyNodeObject)_peerMan.getNodeObject();
        info.put(nodeobj.nodeName, collectInfo(details, null, nodeobj));

        // collect info on our peers
        _peerMan.invokeOnNodes(new Function<Tuple<Client,NodeObject>,Boolean>() {
            public Boolean apply (Tuple<Client, NodeObject> args) {
                info.put(args.right.nodeName,
                         collectInfo(details, args.left, (MsoyNodeObject)args.right));
                return true;
            }
        });

        return info;
    }

    protected ServerInfo collectInfo (Details details, Client client, MsoyNodeObject nodeobj)
    {
        final ServerInfo info = new ServerInfo();
        info.name = nodeobj.nodeName;
        info.rooms = nodeobj.hostedScenes.size();
        info.games = nodeobj.hostedGames.size();
        info.channels = nodeobj.hostedChannels.size();
        info.parties = nodeobj.hostedParties.size();

        info.clients += nodeobj.clients.size();
        for (ClientInfo cinfo : nodeobj.clients) {
            if (cinfo.username instanceof MsoyAuthName) {
                info.members++;
            } else if (cinfo.username instanceof GameAuthName) {
                info.gamers++;
            } else if (cinfo.username instanceof PartyAuthName) {
                info.partiers++;
            } else {
                info.others++;
            }
        }

        info.inScene = nodeobj.memberScenes.size();
        info.inGame = nodeobj.memberGames.size();

        switch (details) {
//         case MEMBERS:
//             info.details = makeDetails(nodeobj.memberScenes);
//             break;
// TODO: correlate memberScenes with hostedScenes, and memberGames with hostedGames
        case ROOMS: {
            IntIntMap pops = new IntIntMap();
            for (MemberScene scene : nodeobj.memberScenes) {
                pops.increment(scene.sceneId, 1);
            }
            info.details = makeDetails(nodeobj.hostedScenes, pops);
            break;
        }
        case GAMES: {
            IntIntMap pops = new IntIntMap();
            for (MemberGame game : nodeobj.memberGames) {
                pops.increment(game.gameId, 1);
            }
            info.details = makeDetails(nodeobj.hostedGames, pops);
            break;
        }
        case CHANNELS:
            info.details = makeDetails(nodeobj.hostedChannels, null);
            break;
        case PARTIES:
            info.details = makePartyDetails(nodeobj.hostedParties, nodeobj.partyInfos);
            break;
        case REPORT:
            collectReportInfo(info, client, nodeobj, ReportManager.DEFAULT_TYPE);
            break;
        case PROFILE:
            collectReportInfo(info, client, nodeobj, ReportManager.PROFILE_TYPE);
            break;
        case CLIENTS:
            collectReportInfo(info, client, nodeobj, MemberManager.CLIENTS_REPORT_TYPE);
            break;
        case PANOPTICON:
            collectReportInfo(info, client, nodeobj, MsoyEventLogger.PANOPTICON_REPORT_TYPE);
            break;
        case NONE:
            // leave details as null in this case
            break;
        }

        return info;
    }

    protected Callable<String> makeDetails (Iterable<? extends Object> data, IntIntMap pops)
    {
        StringBuilder buf = new StringBuilder();
        for (Object value : data) {
            buf.append("- ").append(value);
            if (pops != null) {
                buf.append(": ").append(Math.max(0, pops.get(((HostedPlace)value).placeId)));
            }
            buf.append("\n");
        }
        return Callables.asCallable(buf.toString());
    }

    protected Callable<String> makePartyDetails (
        DSet<PartySummary> summaries, DSet<PartyInfo> infos)
    {
        StringBuilder buf = new StringBuilder();
        for (PartySummary summary : summaries) {
            PartyInfo info = infos.get(summary.getKey());
            buf.append("- size=").append(info.population).append(" ")
                .append("name=\"").append(summary.name).append("\" ")
                .append("group=\"").append(summary.group).append("\" ")
                .append("groupId=").append(summary.group.getGroupId()).append(" ")
                .append("status=\"").append(info.statusType)
                .append(": ").append(info.status).append("\"\n");
        }
        return Callables.asCallable(buf.toString());
    }

    protected void collectReportInfo (ServerInfo info, Client client, MsoyNodeObject nodeobj,
                                      final String type)
    {
        if (client == null) { // nextgen narya will make this less hacky
            final FutureResult<String> report = new FutureResult<String>();
            try {
                _peerMan.generateReport(null, type, report);
                info.details = new Callable<String>() {
                    public String call () throws Exception {
                        return report.get();
                    }
                };
            } catch (Exception e) {
                // will not happen; if it does, details will just be null
                log.warning("Mission impossible!", e);
            }

        } else {
            final FutureResult<String> report = new FutureResult<String>();
            nodeobj.peerService.generateReport(client, type, report);
            info.details = new Callable<String>() {
                public String call () throws Exception {
                    return report.get();
                }
            };
        }
    }

    protected static enum Details {
        NONE, ROOMS, GAMES, CHANNELS, PARTIES, REPORT, PROFILE, CLIENTS, PANOPTICON
    };

    protected static class ServerInfo
    {
        public String name;
        public int clients;
        public int members;
        public int gamers;
        public int partiers;
        public int others;

        public int rooms;
        public int games;
        public int channels;
        public int parties;

        public int inScene;
        public int inGame;

        public Callable<String> details;

        public StringBuilder modeinfo = new StringBuilder();

        public String toString () {
            return name + " [clients=" + clients + ", members=" + members + ", gamers=" + gamers +
                ", partiers=" + partiers + ", others=" + others +
                ", inScene=" + inScene + ", inGame=" + inGame +
                ", rooms=" + rooms + ", games=" + games + ", channels=" + channels +
                ", parties=" + parties + "] ";
        }
    }

    // our dependencies
    @Inject protected MsoyPeerManager _peerMan;
    @Inject protected RootDObjectManager _omgr;
}
