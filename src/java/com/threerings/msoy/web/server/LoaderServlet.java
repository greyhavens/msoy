//
// $Id$

package com.threerings.msoy.web.server;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.inject.Inject;

import com.samskivert.io.StreamUtil;

import com.threerings.msoy.data.all.HashMediaDesc;
import com.threerings.msoy.edgame.gwt.GameCode;
import com.threerings.msoy.game.server.persist.MsoyGameRepository;
import com.threerings.msoy.item.server.persist.DecorRecord;
import com.threerings.msoy.item.server.persist.DecorRepository;
import com.threerings.msoy.room.data.MsoySceneModel;
import com.threerings.msoy.room.server.persist.MsoySceneRepository;
import com.threerings.msoy.room.server.persist.SceneRecord;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;

import static com.threerings.msoy.Log.log;

/**
 * Tells a client which Loader to use while loading whirled.
 */
public class LoaderServlet extends HttpServlet
{
    @Override
    protected void doGet (HttpServletRequest req, HttpServletResponse rsp)
        throws IOException
    {
        String loader = getSceneLoader(req.getParameter("sceneId"), req.getParameter("memberHome"));
        if (loader == null) {
            loader = getGameLoader(req.getParameter("gameId"));
        }

        if (loader == null) {
            // TODO: return a real default. Presently this is used to signal to not loaderate
            loader = "ooo"; //
        }

        // simply print out the loader
        PrintWriter writer = rsp.getWriter();
        try {
            writer.print(loader);
        } finally {
            StreamUtil.close(writer);
        }
    }

    /**
     * Get the loader that should be used for the specified scene.
     */
    protected String getSceneLoader (String sceneId, String memberHome)
    {
        if (sceneId != null) {
            try {
                return getSceneLoader(Integer.parseInt(sceneId));
            } catch (NumberFormatException nfe) {
                // fall to return null, at bottom
            }
        }
        if (memberHome != null) {
            try {
                MemberRecord memrec = _memberRepo.loadMember(Integer.parseInt(memberHome));
                if (memrec != null) {
                    return getSceneLoader(memrec.homeSceneId);
                } else {
                    log.info("Asked to go to non-existant member's home", "memberId", memberHome);
                }
            } catch (NumberFormatException nfe) {
                // fall to return null, at bottom
            }
        }
        return null;
    }

    /**
     * Get the loader that should be used for the specified scene.
     */
    protected String getSceneLoader (int sceneId)
    {
        SceneRecord scene = _sceneRepo.loadScene(sceneId);
        // TODO: check access? Ensure scene is public? Ferfuck's sake
        if (scene == null) {
            log.info("Could not find scene", "sceneId", sceneId);
            return null;
        }
        if (scene.decorId == 0) {
            return MsoySceneModel.DEFAULT_DECOR_MEDIA.getMediaPath();
        }

        DecorRecord backdrop = _decorRepo.loadItem(scene.decorId);
        if (backdrop == null) {
            // how embarassing!
            log.info("Could not find backdrop", "sceneId", sceneId, "decorId", scene.decorId);
            return null;
        }

        // TODO: if the background is a swf, we probably need to do some spayshul stuffz.

        return HashMediaDesc.getMediaPath(backdrop.furniMediaHash, backdrop.furniMimeType);
    }

    /**
     * Get the loader that should be used for the specified game.
     */
    protected String getGameLoader (String gameId)
    {
        if (gameId != null) {
            try {
                return getGameLoader(Integer.parseInt(gameId));
            } catch (NumberFormatException nfe) {
                // fall through to null return
            }
        }
        return null;
    }

    /**
     * Get the loader that should be used for the specified game.
     */
    protected String getGameLoader (int gameId)
    {
        GameCode game = _mgameRepo.loadGameCode(gameId, false);
        if (game != null) {
            if (game.splashMedia != null) {
                return game.splashMedia.getMediaPath();
            } // else: no error
        } else {
            log.info("Asked to go to non-existent game", "gameId", gameId);
        }
        return null;
    }

    @Inject protected DecorRepository _decorRepo;
    @Inject protected MemberRepository _memberRepo;
    @Inject protected MsoyGameRepository _mgameRepo;
    @Inject protected MsoySceneRepository _sceneRepo;
}
