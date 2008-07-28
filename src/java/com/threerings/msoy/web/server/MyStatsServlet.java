//
// $Id$

package com.threerings.msoy.web.server;

import static com.threerings.msoy.Log.log;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.inject.Inject;

import com.samskivert.io.StreamUtil;
import com.samskivert.servlet.util.CookieUtil;
import com.samskivert.util.IntSet;

import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.web.data.MemberCard;
import com.threerings.msoy.web.data.WebCreds;
import com.threerings.msoy.web.data.WebIdent;

/**
 * Reports server status in plain text.
 */
public class MyStatsServlet extends HttpServlet
{
    @Override // from HttpServlet
    protected void doGet (HttpServletRequest req, HttpServletResponse rsp)
        throws IOException
    {
        try {
            // pull out session token from the request header
            String token = CookieUtil.getCookieValue(req, WebCreds.CREDS_COOKIE);
            if (token == null) {
                rsp.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            // make sure the user is authenticated, and pull out their record object
            WebIdent ident = new WebIdent(_mhelper.getMemberId(token), token);
            MemberRecord member = _mhelper.getAuthedUser(ident);

            if (member == null) {
                rsp.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            // now get their friend ids
            IntSet friendIds = _memberRepo.loadFriendIds(member.memberId);
            List<MemberCard> friends = _mhelper.resolveMemberCards(friendIds, true, friendIds);

            // and print out the response
            String results = makeResults(member, friends);
            rsp.getOutputStream().println(results);
            StreamUtil.close(rsp.getOutputStream());

        } catch (Exception e) {
            log.warning("Failed to gather user stats.", e);
            rsp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
    }

    protected String makeResults (MemberRecord member, List<MemberCard> friends)
        throws JSONException, UnsupportedEncodingException
    {
        JSONObject result = new JSONObject();

        result.put("name", URLEncoder.encode(member.name, "UTF-8"));
        result.put("coins", member.flow);
        result.put("level", member.level);
        result.put("friendsOnline", friends.size());

        return result.toString();
    }

    // our dependencies
    @Inject protected MemberHelper _mhelper;
    @Inject protected MemberRepository _memberRepo;
}
