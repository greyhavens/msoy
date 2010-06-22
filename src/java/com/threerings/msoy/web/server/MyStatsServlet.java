//
// $Id$

package com.threerings.msoy.web.server;

import static com.threerings.msoy.Log.log;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.inject.Inject;
import com.samskivert.io.StreamUtil;
import com.samskivert.servlet.util.CookieUtil;
import com.threerings.msoy.money.data.all.MemberMoney;
import com.threerings.msoy.money.server.MoneyLogic;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.web.gwt.MemberCard;
import com.threerings.msoy.web.gwt.WebCreds;

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
            String token = CookieUtil.getCookieValue(req, WebCreds.credsCookie());
            if (token == null) {
                rsp.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            // make sure the user is authenticated, and pull out their record object
            MemberRecord member = _mhelper.getAuthedUser(token);
            if (member == null) {
                rsp.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            // get their money
            MemberMoney money = _moneyLogic.getMoneyFor(member.memberId);

            // now get their friend ids
            Set<Integer> friendIds = _memberRepo.loadFriendIds(member.memberId);
            List<MemberCard> friends = _mhelper.resolveMemberCards(friendIds, true, null);

            // and print out the response
            String results = makeResults(member, friends, money);
            rsp.getOutputStream().println(results);
            StreamUtil.close(rsp.getOutputStream());

        } catch (final Exception e) {
            log.warning("Failed to gather user stats.", e);
            rsp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
    }

    protected String makeResults (MemberRecord member, List<MemberCard> friends, MemberMoney money)
        throws JSONException, UnsupportedEncodingException
    {
        JSONObject result = new JSONObject();

        result.put("name", URLEncoder.encode(member.name, "UTF-8"));
        result.put("coins", money.coins);
        result.put("level", member.level);
        result.put("friendsOnline", friends.size());

        return result.toString();
    }

    // our dependencies
    @Inject protected MemberHelper _mhelper;
    @Inject protected MemberRepository _memberRepo;
    @Inject protected MoneyLogic _moneyLogic;
}
