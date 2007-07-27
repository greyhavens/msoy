//
// $Id$

package com.threerings.msoy.game.server;

import java.util.HashMap;

import java.util.logging.Level;

import com.samskivert.util.HashIntMap;
import com.samskivert.util.IntTuple;
import com.samskivert.util.ResultListener;
import com.samskivert.util.ResultListenerList;

import com.threerings.util.Name;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationCodes;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationManager;

import com.threerings.parlor.game.data.GameConfig;
import com.threerings.parlor.game.data.GameObject;
import com.threerings.parlor.game.server.GameManager;

import com.threerings.whirled.data.ScenePlace;

import com.threerings.ezgame.data.GameDefinition;
import com.threerings.ezgame.data.TableMatchConfig;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.server.MsoyServer;

import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;

import com.threerings.msoy.world.data.MemoryEntry;

import com.threerings.msoy.game.data.AVRGameObject;
import com.threerings.msoy.game.data.WorldGameConfig;
import com.threerings.msoy.game.xml.MsoyGameParser;

import static com.threerings.msoy.Log.*;

/**
 * Manages AVRG games.
 */
public class WorldGameRegistry
    implements WorldGameProvider
{
    /**
     * Initializes the registry.
     */
    public void init (InvocationManager invmgr)
    {
        invmgr.registerDispatcher(new WorldGameDispatcher(this), MsoyCodes.WORLD_GROUP);
    }

    // from WorldGameProvider
    public void joinWorldGame (ClientObject caller, int gameId,
                               final InvocationService.InvocationListener listener)
        throws InvocationException
    {
        // see what we've got..
        final MemberObject member = (MemberObject)caller;
        if (member.getSceneId() == 0) {
            log.warning("User joining world game, but not in scene?");
            throw new InvocationException(InvocationCodes.E_INTERNAL_ERROR);
        }

        final IntTuple gameKey = new IntTuple(gameId, member.getSceneId());
        Integer gameOid = _games.get(gameKey);
        if (gameOid != null) {
            // if we know the game oid, join immediately
            joinWorldGame(member, gameOid);
            return;
        }

        // create the result listener to join the game
        ResultListener<Integer> rlistener = new ResultListener<Integer>() {
            public void requestCompleted (Integer result) {
                if (!member.isActive()) {
                    return; // he bailed
                }
                try {
                    joinWorldGame(member, result);
                } catch (InvocationException e) {
                    requestFailed(e);
                }
            }
            public void requestFailed (Exception cause) {
                listener.requestFailed(InvocationCodes.INTERNAL_ERROR);
            }
        };

        ResultListenerList<Integer> list = _loading.get(gameKey);
        if (list != null) {
            // if we're already resolving this game, add this listener
            // to the list of those interested in the outcome
            list.add(rlistener);
            return;
        }

        list = new ResultListenerList<Integer>();
        list.add(rlistener);
        _loading.put(gameKey, list);

        // retrieve the game item
        MsoyServer.itemMan.getItem(new ItemIdent(Item.GAME, gameId), new ResultListener<Item>() {
            public void requestCompleted (Item item) {
                try {
                    final Game game = (Game) item;
                    final GameDefinition gdef = new MsoyGameParser().parseGame(game);

                    WorldGameConfig config = new WorldGameConfig();
                    config.init(game, gdef);
                    config.startSceneId = gameKey.right;
                    if (config.getMatchType() == GameConfig.PARTY) {
                        config.players = new Name[0];
                    } else {
                        config.players = new Name[((TableMatchConfig)gdef.match).maxSeats];
                    }

                    // TODO: fix Chiyogami stuff... game.config will never be non-xml anymore
                    if (game.config != null &&
                        game.config.contains("<toggle ident=\"chiyogami\" start=\"true\"/>")) {
                        String prefix = "com.threerings.msoy.game.chiyogami.";
                        gdef.controller = prefix + "client.ChiyogamiController";
                        gdef.manager = prefix + "server.ChiyogamiManager";
                    }

                    MsoyServer.plreg.createPlace(config);

                } catch (Exception e) {
                    log.log(Level.WARNING, "Exception configuring world game", e);
                    requestFailed(e);
                }
            }
            public void requestFailed (Exception cause) {
                _loading.remove(gameKey).requestFailed(cause);
            }
        });
    }

    // from WorldGameProvider
    public void leaveWorldGame (ClientObject caller, InvocationService.InvocationListener arg1)
        throws InvocationException
    {
        leaveWorldGame((MemberObject)caller);
    }

    /**
     * Removes the user from the world game he currently occupies (if any).
     */
    public void leaveWorldGame (MemberObject member)
        throws InvocationException
    {
        // nothing to do if they aren't in a game
        if (member.worldGameOid == 0) {
            return;
        }

        int gameOid = member.worldGameOid;

        clearWorldGame(member);

        // remove them from the occupant list
        GameObject gobj = getGameObject(member, gameOid);
        if (gobj != null && gobj.isActive()) {
            int memberOid = member.getOid();
            gobj.startTransaction();
            try {
                gobj.removeFromOccupantInfo(memberOid);
                gobj.removeFromOccupants(memberOid);
            } finally {
                gobj.commitTransaction();
            }
        }
    }

    // from WorldGameProvider
    public void updateMemory (ClientObject caller, MemoryEntry entry)
    {
        // get their game object
        MemberObject member = (MemberObject)caller;
        GameObject gameObj = (GameObject)MsoyServer.omgr.getObject(member.worldGameOid);
        if (!(gameObj instanceof AVRGameObject)) {
            log.warning("Received memory update request from user not in world game [who=" +
                member.who() + "].");
            return;
        }

        GameManager manager = _managers.get(gameObj.getOid());
        WorldGameConfig config = (WorldGameConfig) manager.getConfig();

        // make sure the entry refers to the game
        entry.item = new ItemIdent(Item.GAME, config.getGameId());

        // TODO: verify that the memory does not exceed legal size

        // mark it as modified and update the game object; we'll save it when we unload the game
        AVRGameObject wgobj = (AVRGameObject) gameObj;
        entry.modified = true;
        if (wgobj.memories.contains(entry)) {
            wgobj.updateMemories(entry);
        } else {
            wgobj.addToMemories(entry);
        }
    }

    /**
     * Called by WorldGameManagerDelegates instances after they're all ready to go.
     */
    public void gameStartup (GameManager manager, int gameOid)
    {
        WorldGameConfig config = (WorldGameConfig) manager.getConfig();

        // record the oid and manager for the game
        IntTuple gameKey = new IntTuple(config.getGameId(), config.startSceneId);

        _games.put(gameKey, gameOid);
        _managers.put(gameOid, manager);

        // remove the list of listeners and notify each of them
        _loading.remove(gameKey).requestCompleted(gameOid);
    }

    /**
     * Called by WorldGameManagerDelegate instances when they start to shut down and destroy their
     * dobject.
     */
    public void gameShutdown (GameManager manager, int gameOid)
    {
        // try removing all players left in the game
        GameObject game = (GameObject) manager.getPlaceObject();
        for (int ii = game.occupants.size() - 1; ii >= 0; ii--) {
            MemberObject member = (MemberObject) MsoyServer.omgr.getObject(game.occupants.get(ii));
            if (member != null) {
                clearWorldGame(member);
            }
        }

        WorldGameConfig config = (WorldGameConfig) manager.getConfig();

        // destroy our record of that game
        IntTuple gameKey = new IntTuple(config.getGameId(), config.startSceneId);

        _games.remove(gameKey);
        _managers.remove(gameOid);
        _loading.remove(gameKey); // just in case
    }

    /**
     * Adds the user to a world game.
     */
    protected void joinWorldGame (MemberObject member, int gameOid)
        throws InvocationException
    {
        // make sure they're not already in the game
        if (member.worldGameOid == gameOid) {
            return;
        }

        // make sure the game object exists
        GameObject gobj = getGameObject(member, gameOid);

        // TODO: verify that there's room for them to join?

        // leave the current game, if any
        leaveWorldGame(member);

        // add to the occupant list
        int memberOid = member.getOid();
        GameManager gmgr = _managers.get(gameOid);
        WorldGameConfig wgcfg = (WorldGameConfig) gmgr.getConfig();

        gobj.startTransaction();
        try {
            gmgr.buildOccupantInfo(member);
            gobj.addToOccupants(memberOid);
        } finally {
            gobj.commitTransaction();
        }

        // set their game field
        member.startTransaction();
        try {
            member.setWorldGameCfg(wgcfg);
            member.setWorldGameOid(gameOid);
        } finally {
            member.commitTransaction();
        }
    }

    /**
     * Remove this member from any world games.
     */
    protected void clearWorldGame (MemberObject member)
    {
        member.startTransaction();
        try {
            member.setWorldGameOid(0);
            member.setWorldGameCfg(null);
        } finally {
            member.commitTransaction();
        }
    }

    /**
     * Retrieves the world game object, throwing an exception if it does not exist.
     */
    protected GameObject getGameObject (MemberObject member, int gameOid)
        throws InvocationException
    {
        GameObject gobj = (GameObject) MsoyServer.omgr.getObject(gameOid);
        if (gobj == null) {
            log.warning("Missing world game object [who=" + member.who() +
                ", oid=" + gameOid + "].");
            throw new InvocationException(InvocationCodes.INTERNAL_ERROR);
        }
        return gobj;
    }

    /** Maps [gameId, sceneId] -> world game oid. */
    protected HashMap<IntTuple, Integer> _games = new HashMap<IntTuple, Integer>();

    /** Maps game oids -> game managers. */
    protected HashIntMap<GameManager> _managers = new HashIntMap<GameManager>();

    /** Maps [gameId, sceneId] -> listeners waiting for a world game to load. */
    protected HashMap<IntTuple, ResultListenerList<Integer>> _loading =
        new HashMap<IntTuple, ResultListenerList<Integer>>();
}
