//
// $Id$

package com.threerings.msoy.web.server;

import java.io.IOException;
import java.io.PrintStream;

import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.samskivert.io.StreamUtil;
import com.samskivert.util.StringUtil;

import com.threerings.util.MessageBundle;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.VisitorInfo;
import com.threerings.msoy.server.MemberLogic;
import com.threerings.msoy.server.MsoyEventLogger;
import com.threerings.msoy.server.ServerMessages;

import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;
import com.threerings.msoy.web.gwt.ServiceException;

import com.threerings.msoy.item.server.ItemLogic;
import com.threerings.msoy.item.server.persist.CatalogRecord;

import com.threerings.msoy.game.gwt.GameGenre;
import com.threerings.msoy.game.server.persist.GameInfoRecord;
import com.threerings.msoy.game.server.persist.MsoyGameRepository;

import com.threerings.msoy.room.data.RoomCodes;
import com.threerings.msoy.room.server.persist.MsoySceneRepository;
import com.threerings.msoy.room.server.persist.SceneRecord;

import static com.threerings.msoy.Log.log;

/**
 * Handles a simple request to redirect: /go/[page_tokens_and_args]
 *
 * <p> Or a request to redirect with an optional assignment of affiliate:
 * /welcome/[affiliate]/[page_tokens_and_args]</p>
 * 
 * <p> Or a request to redirect with an optional assignment of affiliate and a friend request:
 * /friend/[affiliate]/[page_tokens_and_args]</p>
 */
public class GoServlet extends HttpServlet
{
    @Override
    protected void doGet (HttpServletRequest req, HttpServletResponse rsp)
        throws IOException
    {
        String path = StringUtil.deNull(req.getPathInfo());
        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        int affiliateId = 0;
        boolean autoFriend = req.getRequestURI().startsWith("/friend/");
        if (autoFriend || req.getRequestURI().startsWith("/welcome/")) {
            // the path will now either be "", "<affiliate>", or "<affiliate>/<token>".
            // <affiliate> may be 0 to indicate "no affiliate" (we just want the redirect).
            // NOTE: we allow negative affiliate ids here and the AffiliateCookie will eventually
            // convert these to "autoFriend" affiliates
            int nextSlash = path.indexOf("/");
            if (nextSlash == -1) {
                affiliateId = parseAffiliate(path);
                path = "";
            } else {
                affiliateId = parseAffiliate(path.substring(0, nextSlash));
                path = path.substring(nextSlash + 1);
            }
        }

        // after sorting out the actual page, see if we want to serve up something tricky
        if (serveCloakedPage(req, rsp, path, StringUtil.deNull(req.getHeader("User-Agent")))) {
            return;
        }

        // if this user appears to be brand new, create a visitor info for them
        VisitorInfo info = null;
        if (VisitorCookie.shouldCreate(req)) {
            VisitorCookie.set(rsp, info = new VisitorInfo());
            _eventLog.visitorInfoCreated(info, true);
        }

        // if the URL contains an entry vector, we extract it, obtain/assign the visitor id for the
        // requester and record all of the above
        if (path.indexOf(VEC_ARG) != -1) {
            try {
                Pages page = Pages.fromHistory(path);
                Args args = Args.fromHistory(path);

                String vec = null;
                for (int ii = 0; ii < args.getArgCount(); ii++) {
                    if (args.get(ii, "").equals(VEC_ARG)) {
                        vec = args.get(ii+1, (String)null);
                        path = page.makeToken(args.recomposeWithout(ii, 2));
                    }
                }
                if (vec == null) {
                    log.info("Passing through malformed entry vector", "path", path);

                } else if (info != null) {
                    // if we assigned this user a visitor info on this request, that means they are
                    // brand spanking new; in that case we note their entry vector
                    _memberLogic.trackVectorAssociation(info, vec);

                } else { // otherwise we don't
                    log.info("Ignoring entry by existing user", "path", path);
                }

            } catch (Exception e) {
                log.info("Failure looking for entry vector", "path", path, "error", e);
            }
        }

        // set their affiliate cookie if appropriate
        if (affiliateId > 0) {
            AffiliateCookie.set(rsp, affiliateId, autoFriend);
        }

        rsp.sendRedirect("/#" + path);
    }

    protected int parseAffiliate (String affiliate)
    {
        try {
            return Integer.parseInt(affiliate);
        } catch (Exception e) {
            log.info("Ignoring bogus affiliate", "aff", affiliate);
            return 0;
        }
    }

    /**
     * Check to see if we should serve up a cloaked page.
     */
    protected boolean serveCloakedPage (
        HttpServletRequest req, HttpServletResponse rsp, String path, String agent)
        throws IOException
    {
        if (agent.contains("Googlebot")) {
            if (serveGoogle(req, rsp, path)) {
                return true;
            }

        } else if (agent.startsWith("facebookexternalhit")) {
            if (serveFacebook(req, rsp, path)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Yeah.
     */
    protected boolean serveGoogle (HttpServletRequest req, HttpServletResponse rsp, String path)
        throws IOException
    {
        if (ALL_GAMES_PREFIX.equals(path)) {
            List<Object> args = Lists.newArrayList();
            // load the top 100 games
            for (GameInfoRecord game : _mgameRepo.loadGenre(GameGenre.ALL, 100)) {
                args.add(game.getShotMedia());
                args.add(GAME_DETAIL_PREFIX + game.gameId);
                args.add(game.name);
            }
            outputGoogle(rsp, "All games", "Whirled hosts many games", "", args);
            return true;

        } else if (path.startsWith(GAME_DETAIL_PREFIX)) {
            int gameId = Integer.parseInt(path.substring(GAME_DETAIL_PREFIX.length()));
            GameInfoRecord game = _mgameRepo.loadGame(gameId);
            if (game == null) {
                outputGoogle(rsp, "No such game", "No such game", ALL_GAMES_PREFIX,
                    GAME_DETAIL_PREFIX + (gameId - 1), "previous game",
                    GAME_DETAIL_PREFIX + (gameId + 1), "next game");
            } else {
                outputGoogle(rsp, game.name, game.description, ALL_GAMES_PREFIX,
                    game.getShotMedia(),
                    GAME_DETAIL_PREFIX + (gameId - 1), "previous game",
                    GAME_DETAIL_PREFIX + (gameId + 1), "next game");
            }
            return true;
        }

        return false;
    }

    /**
     * Service a request for the facebook share link.
     *
     * @return true on success
     */
    protected boolean serveFacebook (HttpServletRequest req, HttpServletResponse rsp, String path)
        throws IOException
    {
        MediaDesc image;
        String title;
        String desc;
        String gamePrefix;

        MessageBundle msgs = _serverMsgs.getBundle("server");
        try {
            if (path.startsWith(SHARE_ROOM_PREFIX)) {
                int sceneId = Integer.parseInt(path.substring(SHARE_ROOM_PREFIX.length()));
                SceneRecord scene = _sceneRepo.loadScene(sceneId);
                if (scene == null) {
                    log.warning("Facebook requested share of nonexistant room?", "path", path);
                    return false;
                }
                image = scene.getSnapshotThumb();
                if (image == null) {
                    image = RoomCodes.DEFAULT_SNAPSHOT_THUMB;
                }
                title = msgs.get("m.room_share_title", scene.name);
                desc = msgs.get("m.room_share_desc");

            } else if (path.startsWith(gamePrefix = SHARE_GAME_PREFIX) ||
                       path.startsWith(gamePrefix = GAME_DETAIL_PREFIX)) {
                int gameId = Integer.parseInt(path.substring(gamePrefix.length()));
                GameInfoRecord game = _mgameRepo.loadGame(gameId);
                if (game == null) {
                    log.warning("Facebook requested share of nonexistant game?", "path", path);
                    return false;
                }
                image = game.getShotMedia();
                title = msgs.get("m.game_share_title", game.name);
                desc = game.description;

            } else if (path.startsWith(SHARE_ITEM_PREFIX)) {
                String spec = path.substring(SHARE_ITEM_PREFIX.length());
                String[] pieces = spec.split("_");
                byte itemType = Byte.parseByte(pieces[0]);
                int catalogId = Integer.parseInt(pieces[1]);
                CatalogRecord listing;
                try {
                    listing = _itemLogic.requireListing(itemType, catalogId, true);
                } catch (ServiceException se) {
                    log.warning("Facebook requested share of nonexistant listing?", "path", path);
                    return false;
                }
                image = listing.item.getThumbMediaDesc();
                title = msgs.get("m.item_share_title", listing.item.name);
                desc = listing.item.description;

            } else {
                log.warning("Unknown facebook share request", "path", path);
                return false;
            }

        } catch (NumberFormatException nfe) {
            log.warning("Could not parse page for facebook sharing.", "path", path);
            return false;
        }

        outputFacebook(rsp, title, desc, image);
        return true;
    }

    protected void outputGoogle (
        HttpServletResponse rsp, String title, String desc, String upLink, List<Object> args)
        throws IOException
    {
        Object[] argArray = new Object[args.size()];
        args.toArray(argArray);
        outputGoogle(rsp, title, desc, upLink, argArray);
        if (++_googlePages <= MAX_GOOGLE_PAGES_TO_LOG) {
            log.info("Served google bot page", "title", title);
        }
    }

    /**
     * Output a generated page for google.
     *
     * @param args :
     *         MediaDesc - an image. Output directly.
     *         String - a /go/-based url, always followed by another String: link text
     */
    protected void outputGoogle (
        HttpServletResponse rsp, String title, String desc, String upLink, Object... args)
        throws IOException
    {
        // TODO: some sort of html templating? Ah, Pfile, you rocked, little guy!
        PrintStream out = new PrintStream(rsp.getOutputStream());
        try {
            out.println("<html><head>");
            out.println("<title>" + title + "</title>");
            out.println("<body>");
            out.println("<h1>" + title + "</h1>");
            out.println(desc);
            out.println("<a href=\"/go/" + upLink + "\">Go back</a>");

            for (int ii = 0; ii < args.length; ii++) {
                if (args[ii] instanceof MediaDesc) {
                    out.println("<img src=\"" + ((MediaDesc) args[ii]).getMediaPath() + "\">");

                } else if (args[ii] instanceof String) {
                    String link = (String) args[ii];
                    String text = (String) args[++ii];
                    out.println("<a href=\"/go/" + link + "\">" + text + "</a>");

                } else {
                    log.warning("Don't undertand arg: " + args[ii]);
                }
            }
            out.println("</body></html>");
        } finally {
            StreamUtil.close(out);
        }
    }

    /**
     * Output a generated page for facebook.
     */
    protected void outputFacebook (
        HttpServletResponse rsp, String title, String desc, MediaDesc image)
        throws IOException
    {
        // TODO: some sort of html templating? Ah, Pfile, you rocked, little guy!
        PrintStream out = new PrintStream(rsp.getOutputStream());
        try {
            out.println("<html><head>");
            out.println("<meta name=\"title\" content=\"" + deQuote(title) + "\"/>");
            out.println("<meta name=\"description\" content=\"" + deQuote(desc) + "\"/>");
            out.println("<link rel=\"image_src\" href=\"" + image.getMediaPath() + "\"/>");
            out.println("</head><body></body></html>");
        } finally {
            StreamUtil.close(out);
        }
    }

    /**
     * Replace quotes with ticks (" -> ')
     */
    protected static String deQuote (String input)
    {
        return input.replace('\"', '\'');
    }

    /** Number of google bot pages served. */
    protected int _googlePages;

    protected static final String ALL_GAMES_PREFIX = Pages.GAMES.getPath();
    protected static final String GAME_DETAIL_PREFIX = Pages.GAMES.getPath() + "-d_";

    protected static final String SHARE_ROOM_PREFIX = Pages.WORLD.getPath() + "-s";
    protected static final String SHARE_GAME_PREFIX = Pages.WORLD.getPath() + "-game_p_";
    protected static final String SHARE_ITEM_PREFIX = Pages.SHOP.getPath() + "-l_";

    protected static final String VEC_ARG = "vec";

    /** Maximum number of google bot pages to log. */
    protected static final int MAX_GOOGLE_PAGES_TO_LOG = 50;

    // our dependencies
    @Inject protected MsoyEventLogger _eventLog;
    @Inject protected MemberLogic _memberLogic;
    @Inject protected ItemLogic _itemLogic;
    @Inject protected MsoyGameRepository _mgameRepo;
    @Inject protected MsoySceneRepository _sceneRepo;
    @Inject protected ServerMessages _serverMsgs;
}
