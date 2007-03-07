//
// $Id$

package com.threerings.msoy.game.chiyogami.server;

import com.samskivert.util.RandomUtil;

import com.threerings.util.Name;

import com.threerings.presents.dobj.ObjectAddedEvent;
import com.threerings.presents.dobj.ObjectRemovedEvent;
import com.threerings.presents.dobj.OidListListener;

import com.threerings.crowd.data.PlaceConfig;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.parlor.game.server.GameManager;

import com.threerings.whirled.client.SceneService.SceneMoveListener;
import com.threerings.whirled.data.SceneModel;
import com.threerings.whirled.data.SceneUpdate;

import com.threerings.msoy.server.MsoyServer;

import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.MediaDesc;
import com.threerings.msoy.item.web.StaticMediaDesc;

import com.threerings.msoy.world.data.MsoyLocation;
import com.threerings.msoy.world.data.RoomObject;
import com.threerings.msoy.world.server.RoomManager;

import com.threerings.msoy.game.data.WorldGameConfig;
import com.threerings.msoy.game.server.WorldGameManagerDelegate;
import com.threerings.msoy.game.chiyogami.data.ChiyogamiObject;

import static com.threerings.msoy.Log.log;

/**
 * Manages a game of Chiyogami dance battle.
 */
public class ChiyogamiManager extends GameManager
{
    public ChiyogamiManager ()
    {
        addDelegate(_worldDelegate = new WorldGameManagerDelegate(this));
    }

    @Override
    protected PlaceObject createPlaceObject ()
    {
        return new ChiyogamiObject();
    }

    @Override
    protected void didStartup ()
    {
        _gameobj = (ChiyogamiObject) _plobj;

        // get a handle on the room we're in
        _sceneId = ((WorldGameConfig) getConfig()).startSceneId;
        _roomMgr = (RoomManager) MsoyServer.screg.getSceneManager(_sceneId);
        _roomObj = (RoomObject) _roomMgr.getPlaceObject();
        _roomObj.addListener(_roomListener);

        super.didStartup();

        startGame();
    }

    @Override
    protected void gameDidStart ()
    {
        super.gameDidStart();

        pickNewBoss();
    }

    protected void didShutdown ()
    {
        super.didShutdown();

        shutdownBoss();

        _roomObj.removeListener(_roomListener);
    }

    protected void shutdownBoss ()
    {
        if (_bossObj != null) {
            MsoyServer.screg.sceneprov.leaveOccupiedScene(_bossObj);
            MsoyServer.omgr.destroyObject(_bossObj.getOid());
            _bossObj = null;
        }
    }

    /**
     * Pick a new boss.
     */
    protected void pickNewBoss ()
    {
        shutdownBoss();
        String boss = RandomUtil.pickRandom(BOSSES);

        _bossObj = MsoyServer.omgr.registerObject(new BossObject());
        _bossObj.init(new StaticMediaDesc(
            MediaDesc.APPLICATION_SHOCKWAVE_FLASH, Item.AVATAR, "chiyogami/" + boss));
        _bossObj.setUsername(new Name("Boss"));

        // add the boss to the room
        MsoyServer.screg.sceneprov.moveTo(_bossObj, _sceneId, -1, new SceneMoveListener() {
            public void moveSucceeded (int placeId, PlaceConfig config) {
                // nada: we wait to hear the oid
            }
            public void moveSucceededWithUpdates (
                int placeId, PlaceConfig config, SceneUpdate[] updates) {
                // nada: we wait to hear the oid
            }
            public void moveSucceededWithScene (
                int placeId, PlaceConfig config, SceneModel model) {
                // nada: we wait to hear the oid
            }
            public void requestFailed (String reason) {
                log.warning("Boss failed to enter scene [scene=" + _sceneId +
                            ", reason=" + reason + "].");
                // TODO: shutdown? freakout? call the Elite Beat Agents?
            }
        });

//        _gameobj.startTransaction();
//        try {
//            _gameobj.setBossHealth(1f);
//            _gameobj.setBoss(
//        } finally {
//            _gameobj.commitTransaction();
//        }
    }

    /**
     * Called once the boss is added to the room.
     */
    protected void bossAddedToRoom ()
    {
        String error = _roomMgr.changeLocation(_bossObj, new MsoyLocation(.5, 0, .5, 0));
        if (error != null) {
            log.warning("Error moving boss [e=" + error + "].");
        }
    }

    /**
     * Listens for changes to the RoomObject in which we're hosted.
     */
    protected class RoomListener
        implements OidListListener
    {
        public void objectAdded (ObjectAddedEvent event)
        {
            if (_bossObj != null && _bossObj.getOid() == event.getOid()) {
                bossAddedToRoom();
            }
        }

        public void objectRemoved (ObjectRemovedEvent event)
        {
            // nada
        }
    } // End: class RoomListener

    /** Listens to the room we're boom-chikka-ing. */
    protected RoomListener _roomListener = new RoomListener();

    /** Our world delegate. */
    protected WorldGameManagerDelegate _worldDelegate;

    /** A casted ref to our gameobject, this hides our superclass _gameobj. */
    protected ChiyogamiObject _gameobj;

    /** The sceneId of the game. */
    protected int _sceneId;

    /** The room manager. */
    protected RoomManager _roomMgr;

    /** The room object where the game is taking place. */
    protected RoomObject _roomObj;

    /** The boss object. */
    protected BossObject _bossObj;

    /** TEMP: The filenames of current boss avatars. */
    protected static final String[] BOSSES = { "bboy" };
}
