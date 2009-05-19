//
// $Id$

package com.threerings.msoy.web.server;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.samskivert.io.StreamUtil;
import com.samskivert.servlet.util.FriendlyException;
import com.samskivert.servlet.util.ParameterUtil;
import com.samskivert.util.StringUtil;

// import com.google.code.facebookapi.FacebookJaxbRestClient;
// import com.google.code.facebookapi.schema.FriendsGetResponse;
// import com.google.code.facebookapi.schema.User;
// import com.google.code.facebookapi.schema.UsersGetInfoResponse;

import com.threerings.msoy.server.FacebookLogic;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.persist.MemberRepository;

import com.threerings.msoy.game.gwt.FacebookInfo;
import com.threerings.msoy.game.server.persist.GameInfoRecord;
import com.threerings.msoy.game.server.persist.MsoyGameRepository;
import com.threerings.msoy.group.server.persist.GroupRecord;
import com.threerings.msoy.group.server.persist.GroupRepository;

import com.threerings.msoy.web.gwt.ExternalAuther;
import com.threerings.msoy.web.gwt.Pages;

import static com.threerings.msoy.Log.log;

/**
 * Handles Facebook callback requests.
 */
public class FacebookServlet extends HttpServlet
{
    protected void doHead (HttpServletRequest req, HttpServletResponse rsp)
        throws ServletException, IOException
    {
        log.info("Got HEAD request " + req.getRequestURL());
        dumpParameters(req);
    }

    protected void doGet (HttpServletRequest req, HttpServletResponse rsp)
        throws ServletException, IOException
    {
//         log.info("Got GET request " + req.getRequestURL());
//         dumpParameters(req);

        // determine whether we're in game mode or Whirled mode
        try {
            AppInfo info = parseAppInfo(req.getPathInfo());

            // make sure we have signed facebook data
            validateRequest(req, info.secret);

            // we should either have 'canvas_user' or 'user'
            String fbuid = ParameterUtil.getParameter(req, FBKEY_PREFIX + "canvas_user", "");
            if (StringUtil.isBlank(fbuid)) {
                fbuid = ParameterUtil.getParameter(req, FBKEY_PREFIX + "user", "");
            }
            if (StringUtil.isBlank(fbuid)) {
                rsp.sendRedirect(getLoginURL(info.key));
                return;
            }

            // see if this external user already has an account
            int memberId = _memberRepo.lookupExternalAccount(ExternalAuther.FACEBOOK, fbuid);
            if (memberId == 0) {
                // TODO: create an account
                throw new FriendlyException("TODO: create this man an account!");
            }

            // activate a session for them
            String authtok = _memberRepo.startOrJoinSession(memberId, FBAUTH_DAYS);
            SwizzleServlet.setCookie(req, rsp, authtok);

            // and send them to the appropriate page
            if (info.sceneId != 0) {
                rsp.sendRedirect("/#" + Pages.WORLD.makeToken("s" + info.sceneId));
            } else if (info.gameId != 0) {
                rsp.sendRedirect("/#" + Pages.WORLD.makeToken("game", "p", info.gameId));
            } else {
                rsp.sendRedirect("/#" + Pages.GAMES.makeToken());
            }

        } catch (FriendlyException fe) {
            PrintStream out = null;
            try {
                out = new PrintStream(rsp.getOutputStream());
                out.println(fe.getMessage());
            } finally {
                StreamUtil.close(out);
            }
        }
    }

    protected void doPost (HttpServletRequest req, HttpServletResponse rsp)
        throws ServletException, IOException
    {
        log.info("Got POST request " + req.getRequestURL());
        dumpParameters(req);
    }

    protected void dumpParameters (HttpServletRequest req)
    {
        for (String pname : ParameterUtil.getParameterNames(req)) {
            for (String value : req.getParameterValues(pname)) {
                log.info("  " + pname + " -> " + value);
            }
        }
    }

    protected void validateRequest (HttpServletRequest req, String secret)
        throws FriendlyException
    {
        String sig = ParameterUtil.getParameter(req, "fb_sig", true);
        if (StringUtil.isBlank(sig)) {
            throw new FriendlyException("Missing fb_sig parameter");
        }

        // obtain a list of all fb_sig_ keys and sort them alphabetically by key
        List<String> params = Lists.newArrayList();
        for (String pname : ParameterUtil.getParameterNames(req)) {
            if (pname.startsWith(FBKEY_PREFIX)) {
                params.add(pname.substring(FBKEY_PREFIX.length()) + "=" +
                           req.getParameterValues(pname)[0]);
            }
        }
        Collections.sort(params);

        // concatenate them all together (no separator) and MD5 this plus our secret key
        String sigdata = StringUtil.join(params.toArray(new String[params.size()]), "");
        if (!sig.equals(StringUtil.md5hex(sigdata + secret))) {
            throw new FriendlyException("Invalid fb_sig parameter");
        }
    }

    protected AppInfo parseAppInfo (String path)
        throws FriendlyException
    {
        if (path == null || !path.startsWith(GAME_PATH)) {
            return DEF_APP_INFO;
        }

        int gameId;
        try {
            gameId = Integer.parseInt(path.substring(GAME_PATH.length()));
        } catch (Exception e) {
            throw new FriendlyException("Invalid game URL: " + path);
        }

        GameInfoRecord ginfo = _mgameRepo.loadGame(gameId);
        if (ginfo == null) {
            throw new FriendlyException("Unknown game: " + gameId);
        }

        AppInfo info = new AppInfo();
        info.gameId = ginfo.gameId;

        FacebookInfo fbinfo = _mgameRepo.loadFacebookInfo(ginfo.gameId);
        if (fbinfo.key == null) {
            throw new FriendlyException("Game missing Facebook info: " + ginfo.name);
        }

        if (ginfo.isAVRG) {
            GroupRecord grec = _groupRepo.loadGroup(ginfo.groupId);
            if (grec == null) {
                throw new FriendlyException("Game missing group: " + ginfo.name);
            }
            info.sceneId = grec.homeSceneId;
        }

        info.key = fbinfo.key;
        info.secret = fbinfo.secret;
        return info;
    }

    protected static String getLoginURL (String key)
    {
        return "http://www.facebook.com/login.php?api_key=" + key + "&v=1.0";
    }

    protected static class AppInfo
    {
        public int gameId;
        public int sceneId;
        public String key;
        public String secret;
    }

    @Inject protected MemberRepository _memberRepo;
    @Inject protected MsoyGameRepository _mgameRepo;
    @Inject protected GroupRepository _groupRepo;
    @Inject protected FacebookLogic _faceLogic;

    protected static final AppInfo DEF_APP_INFO = new AppInfo();
    static {
        DEF_APP_INFO.key = ServerConfig.config.getValue("facebook.api_key", "");
        DEF_APP_INFO.secret = ServerConfig.config.getValue("facebook.secret", "");
    }

    protected static final String FBKEY_PREFIX = "fb_sig_";
    protected static final int FBAUTH_DAYS = 2;
    protected static final String GAME_PATH = "/game/";
}
