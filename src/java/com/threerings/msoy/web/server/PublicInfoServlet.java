//
// $Id$

package com.threerings.msoy.web.server;

import static com.threerings.msoy.Log.log;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.inject.Inject;
import com.samskivert.io.StreamUtil;

import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.game.server.persist.MsoyGameRepository;
import com.threerings.msoy.group.server.persist.GroupRepository;
import com.threerings.msoy.item.server.persist.GameRecord;

/**
 * Reports public information about games and whirleds as arrays of JSON objects.
 *
 * The URL must include parameters: <i>type</i> and <i>id</i>, with an optional
 * <i>count</i> parameter.
 *
 * Type should be one of: "game", or "whirled". Elements of that type will be looked up.
 *
 * If only id is specified, an array with one result will be returned, for that id.
 * If a count is specified as well, an array will be returned with all results whose
 * ids are in the range [id, id + count), where count is no larger than MAX_COUNT.
 */
public class PublicInfoServlet extends HttpServlet
{
    protected static final String TYPE_PARAM = "type";
    protected static final String ID_PARAM = "id";
    protected static final String COUNT_PARAM = "count";

    protected static final int MAX_COUNT = 100;

    @Override // from HttpServlet
    protected void doGet (HttpServletRequest req, HttpServletResponse rsp)
        throws IOException
    {
        RequestType type = RequestType.find(req.getParameter(TYPE_PARAM));
        String minParam = req.getParameter(ID_PARAM);
        String countParam = req.getParameter(COUNT_PARAM);

        if (type == null || minParam == null) {
            rsp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        try {
            Set<Integer> memberIds = new TreeSet<Integer>();
            Integer min = Integer.parseInt(minParam);
            Integer count =
                (countParam == null) ? 1 : Math.min(Integer.parseInt(countParam), MAX_COUNT);

            for (int ii = min; ii < min + count; ii++) {
                memberIds.add(ii);
            }

            rsp.getOutputStream().println(type.process(this, memberIds));
            StreamUtil.close(rsp.getOutputStream());

        } catch (NumberFormatException e) {
            log.warning("Badly formatted info request: " + req.getQueryString());
            rsp.sendError(HttpServletResponse.SC_NOT_FOUND);

        } catch (Exception e) {
            log.warning("Failed to process info url. ", e);
            rsp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    protected String processGame (Set<Integer> ids)
        throws Exception
    {
        JSONArray results = new JSONArray();
        for (int id : ids) {
            GameRecord game = _mgameRepo.loadGameRecord(id);
            if (game != null) {
                JSONObject m = new JSONObject();
                m.put("id", game.gameId);
                m.put("name", URLEncoder.encode(game.name, "UTF-8"));
                results.put(m);
            }
        }

        return results.toString();
    }

    protected String processWhirled (Set<Integer> ids)
        throws Exception
    {
        JSONArray results = new JSONArray();
        List<GroupName> groups = _whirledRepo.loadGroupNames(ids);

        for (GroupName group : groups) {
            if (group != null) {
                JSONObject m = new JSONObject();
                m.put("id", group.getGroupId());
                m.put("name", URLEncoder.encode(group.getNormal(), "UTF-8"));
                results.put(m);
            }
        }

        return results.toString();
    }

    protected enum RequestType {
        GAME    ("game")    {
                public String process (PublicInfoServlet s, Set<Integer> ids) throws Exception {
                    return s.processGame(ids); }
                },
        WHIRLED ("whirled") {
                public String process (PublicInfoServlet s, Set<Integer> ids) throws Exception {
                    return s.processWhirled(ids); }
                };

        abstract public String process (PublicInfoServlet s, Set<Integer> ids) throws Exception;

        /** Returns the enum for the specified parameter name. */
        public static RequestType find (String paramName) {
            for (RequestType request : RequestType.values()) {
                if (request._paramName.equals(paramName)) {
                    return request;
                }
            }

            return null;
        }

        private RequestType (String paramName) {
            _paramName = paramName;
        }

        private final String _paramName;
    }

    @Inject protected MsoyGameRepository _mgameRepo;
    @Inject protected GroupRepository _whirledRepo;
}
