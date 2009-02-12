//
// $Id$

package com.threerings.msoy.web.server;

import java.io.IOException;
import java.io.PrintStream;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.inject.Inject;

import com.samskivert.io.StreamUtil;
import com.samskivert.util.StringUtil;

import com.threerings.msoy.data.all.MediaDesc;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.server.persist.GameRepository;
import com.threerings.msoy.item.server.persist.GameRecord;

import com.threerings.msoy.room.data.RoomCodes;
import com.threerings.msoy.room.server.persist.MsoySceneRepository;
import com.threerings.msoy.room.server.persist.SceneRecord;

import static com.threerings.msoy.Log.log;

/**
 * Handles requests to assign an affiliate cookie to a user:
 * /welcome/[affiliate]/[page_tokens_and_args]
 */
public class WelcomeServlet extends HttpServlet
{
    @Override
    protected void doGet (HttpServletRequest req, HttpServletResponse rsp)
        throws IOException
    {
        String path = StringUtil.deNull(req.getPathInfo());
        String affiliate;
        // the path will now either be "", "/<affiliate>", or "/<affiliate>/<token>".
        // <affiliate> may be 0 to indicate "no affiliate" (we just want the redirect through
        // this servlet).
        if (path.startsWith("/")) {
            int nextSlash = path.indexOf("/", 1);
            if (nextSlash == -1) {
                affiliate = path.substring(1);
                path = "";
            } else {
                affiliate = path.substring(1, nextSlash);
                path = path.substring(nextSlash + 1);
            }
            // Set up the affiliate for this welcomed user.
            if (!StringUtil.isBlank(affiliate) && !"0".equals(affiliate)) {
                AffiliateCookie.set(rsp, affiliate);
            }

        } else {
            affiliate = null;
        }

        // possibly hand out special content to certain requesters
        String agent = StringUtil.deNull(req.getHeader("User-Agent"));
        if (agent.startsWith("facebookexternalhit")) {
            if (serveFacebook(req, rsp, path)) {
                return;
            }
        }

        // satisfy a normal request by a user (or if there were problems serving special)
        if (!StringUtil.isBlank(affiliate) && !"0".equals(affiliate)) {
            AffiliateCookie.set(rsp, affiliate);
        }
        rsp.sendRedirect("/#" + path);
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

        try {
            if (path.startsWith(SHARE_ROOM_PREFIX)) {
                int sceneId = Integer.parseInt(path.substring(SHARE_ROOM_PREFIX.length()));
                SceneRecord scene = _sceneRepo.loadScene(sceneId);
                if (scene == null) {
                    log.warning("Facebook requested share of nonexistant room?", "path", path);
                    return false;
                }
                image = scene.getSnapshot();
                if (image == null) {
                    image = RoomCodes.DEFAULT_ROOM_SNAPSHOT;
                }
                title = "Visit this room on Whirled: " /* TODO */ + scene.name;
                desc = "This user-created room is so cool!"; /* TODO */

            } else if (path.startsWith(SHARE_GAME_PREFIX)) {
                int gameId = Integer.parseInt(path.substring(SHARE_GAME_PREFIX.length()));
                GameRecord game = _gameRepo.loadItem(gameId); // TODO: development games???
                if (game == null) {
                    log.warning("Facebook requested share of nonexistant game?", "path", path);
                    return false;
                }
                if (game.shotMediaHash != null) {
                    image = new MediaDesc(game.shotMediaHash, game.shotMimeType);
                } else if (game.thumbMediaHash != null) {
                    image = new MediaDesc(
                        game.thumbMediaHash, game.thumbMimeType, game.thumbConstraint);
                } else {
                    image = Item.getDefaultThumbnailMediaFor(Item.GAME);
                }
                title = "Play this game on Whirled: " /* TODO */ + game.name;
                desc = "This user-created game is so cool!"; /* TODO */

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
    protected String deQuote (String input)
    {
        return input.replace('\"', '\'');
    }

    protected static final String SHARE_ROOM_PREFIX = "world-s";
    protected static final String SHARE_GAME_PREFIX = "world-game_l_";

    // our dependencies
    @Inject protected MsoySceneRepository _sceneRepo;
    @Inject protected GameRepository _gameRepo;
}
