//
// $Id$

package com.threerings.msoy.web.server;

import java.io.IOException;
import java.io.PrintStream;

import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.io.StreamUtil;
import com.samskivert.util.CollectionUtil;
import com.samskivert.util.StringUtil;

import com.threerings.util.MessageBundle;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.server.ServerMessages;

import com.threerings.msoy.web.gwt.MemberCard;
import com.threerings.msoy.web.gwt.Pages;
import com.threerings.msoy.web.gwt.ServiceException;

import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;

import com.threerings.msoy.item.server.ItemLogic;
import com.threerings.msoy.item.server.persist.CatalogRecord;

import com.threerings.msoy.game.gwt.GameGenre;
import com.threerings.msoy.game.server.persist.GameInfoRecord;
import com.threerings.msoy.game.server.persist.MsoyGameRepository;

import com.threerings.msoy.group.data.all.Group;
import com.threerings.msoy.group.server.persist.GroupRecord;
import com.threerings.msoy.group.server.persist.GroupRepository;

import com.threerings.msoy.person.server.persist.ProfileRecord;
import com.threerings.msoy.person.server.persist.ProfileRepository;

import com.threerings.msoy.room.data.RoomCodes;
import com.threerings.msoy.room.server.persist.MsoySceneRepository;
import com.threerings.msoy.room.server.persist.SceneRecord;

import static com.threerings.msoy.Log.log;

/**
 * Serves up cloaked pages for various 3rd party crawlers.
 */
@Singleton
public class CloakedPageLogic
{
    /**
     * See if we should serve up a cloaked page for the specified request.
     * If we serve it up, return true, otherwise false.
     */
    public boolean serveCloakedPage (
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
     * Do we want to serve a cloaked google page for the specified request?
     * Return true if it was served, false otherwise.
     */
    protected boolean serveGoogle (
        HttpServletRequest req, HttpServletResponse rsp, String path)
        throws IOException
    {
        List<Object> args = Lists.newArrayList();
        if (ALL_GAMES.equals(path)) {
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
            String name = (game != null) ? game.name : "No such game";
            String desc = (game != null) ? game.description : "No such game";
            if (game != null) {
                args.add(game.getShotMedia());
                MemberRecord mrec = _memberRepo.loadMember(game.creatorId);
                if (mrec != null) {
                    args.add(Pages.PEOPLE.makeToken(mrec.memberId));
                    args.add("Created by " + mrec.name);
                }
            }
            if (gameId > 1) {
                args.add(GAME_DETAIL_PREFIX + (gameId - 1));
                args.add("previous game");
            }
            args.add(GAME_DETAIL_PREFIX + (gameId + 1));
            args.add("next game");
            outputGoogle(rsp, name, desc, ALL_GAMES, args);
            return true;

        } else if (path.equals(PEOPLE)) {
            // show up to 100 greeters
            List<Integer> greeters = _memberRepo.loadGreeterIds();
            CollectionUtil.limit(greeters, 100);
            for (Integer id : greeters) {
                args.add(Pages.PEOPLE.makeToken(id));
                args.add("A friendly person");
            }
            outputGoogle(rsp, "Friendly people", "People who are friendly", "", args);
            return true;

        } else if (path.startsWith(PROFILE_PREFIX)) {
            int memberId = Integer.parseInt(path.substring(PROFILE_PREFIX.length()));
            MemberRecord mrec = _memberRepo.loadMember(memberId);
            String name = (mrec != null) ? mrec.name : "No such player";
            String desc;
            if (mrec != null) {
                ProfileRecord profile = _profileRepo.loadProfile(memberId);
                args.add((profile != null) ? profile.getPhoto() : MemberCard.DEFAULT_PHOTO);
                desc = (profile != null) ? profile.headline : "";
            } else {
                desc = "No such player";
            }
            if (memberId > 1) {
                args.add(Pages.PEOPLE.makeToken(memberId - 1));
                args.add("previous player");
            }
            args.add(Pages.PEOPLE.makeToken(memberId + 1));
            args.add("next player");
            outputGoogle(rsp, name, desc, PEOPLE, args);
            return true;

        } else if (path.equals(GROUPS)) {
            // show 100 new & hot groups
            for (GroupRecord group : _groupRepo.getGroups(100)) {
                MediaDesc logo = group.toLogo();
                if (logo != null) {
                    args.add(logo);
                }
                args.add(GROUP_DETAIL_PREFIX + group.groupId);
                args.add(group.name);
            }
            outputGoogle(rsp, "Top 100 groups", "Whirled has many community groups", "", args);
            return true;

        } else if (path.startsWith(GROUP_DETAIL_PREFIX)) {
            int groupId = Integer.parseInt(path.substring(GROUP_DETAIL_PREFIX.length()));
            GroupRecord group = _groupRepo.loadGroup(groupId);
            if (group.policy == Group.Policy.EXCLUSIVE) {
                group = null; // pretend it doesn't exist
            }
            String name = (group != null) ? group.name : "No such group";
            String desc = (group != null) ? group.blurb : "No such group";
            if (group != null) {
                MediaDesc logo = group.toLogo();
                if (logo != null) {
                    args.add(logo);
                }
            }
            if (groupId > 1) {
                args.add(GROUP_DETAIL_PREFIX + (groupId - 1));
                args.add("previous group");
            }
            args.add(GROUP_DETAIL_PREFIX + (groupId + 1));
            args.add("next group");
            outputGoogle(rsp, name, desc, GROUPS, args);
            return true;
        }

        return false;
    }

    /**
     * Service a request for the facebook share link.
     *
     * @return true on success
     */
    protected boolean serveFacebook (
        HttpServletRequest req, HttpServletResponse rsp, String path)
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

    /**
     * Output a generated page for google.
     *
     * @param args :
     *         MediaDesc - an image. Output directly.
     *         String - a /go/-based url, always followed by another String: link text
     */
    protected void outputGoogle (
        HttpServletResponse rsp, String title, String desc, String upLink, List<Object> args)
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

            for (Iterator<Object> itr = args.iterator(); itr.hasNext(); ) {
                Object arg = itr.next();
                if (arg instanceof MediaDesc) {
                    out.println("<img src=\"" + ((MediaDesc) arg).getMediaPath() + "\">");

                } else if (arg instanceof String) {
                    String link = (String) arg;
                    String text = (String) itr.next();
                    out.println("<a href=\"/go/" + link + "\">" + text + "</a>");

                } else {
                    log.warning("Don't undertand arg: " + arg);
                }
            }

            if (!StringUtil.isBlank(upLink)) {
                out.println("<a href=\"/go/" + upLink + "\">Go back</a>");
            }
            out.println("</body></html>");
        } finally {
            StreamUtil.close(out);
        }

        // TEMP
        if (++_googlePages <= MAX_GOOGLE_PAGES_TO_LOG) {
            log.info("Served google bot page", "title", title);
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

    protected static final String ALL_GAMES = Pages.GAMES.makeToken();
    protected static final String GAME_DETAIL_PREFIX = Pages.GAMES.makeToken("d", "");

    protected static final String PEOPLE = Pages.PEOPLE.makeToken();
    protected static final String PROFILE_PREFIX = Pages.PEOPLE.makeToken() + "-"; // hack!

    protected static final String GROUPS = Pages.GROUPS.makeToken();
    protected static final String GROUP_DETAIL_PREFIX = Pages.GROUPS.makeToken("d", "");

    protected static final String SHARE_ROOM_PREFIX = Pages.WORLD.makeToken("s");
    protected static final String SHARE_GAME_PREFIX = Pages.WORLD.makeToken("game", "p", "");
    protected static final String SHARE_ITEM_PREFIX = Pages.SHOP.makeToken("l", "");

    /** Maximum number of google bot pages to log. */
    protected static final int MAX_GOOGLE_PAGES_TO_LOG = 50;

    // our dependencies
    @Inject protected GroupRepository _groupRepo;
    @Inject protected ItemLogic _itemLogic;
    @Inject protected MemberRepository _memberRepo;
    @Inject protected MsoyGameRepository _mgameRepo;
    @Inject protected MsoySceneRepository _sceneRepo;
    @Inject protected ProfileRepository _profileRepo;
    @Inject protected ServerMessages _serverMsgs;
}
