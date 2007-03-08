//
// $Id$

package com.threerings.msoy.game.chiyogami.server;

import com.samskivert.util.Interval;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.RandomUtil;

import com.threerings.util.Name;

import com.threerings.presents.dobj.MessageEvent;
import com.threerings.presents.dobj.MessageListener;
import com.threerings.presents.dobj.ObjectAddedEvent;
import com.threerings.presents.dobj.ObjectRemovedEvent;
import com.threerings.presents.dobj.OidListListener;
import com.threerings.presents.server.InvocationException;

import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.data.PlaceConfig;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.crowd.chat.server.SpeakProvider;

import com.threerings.parlor.game.server.GameManager;

import com.threerings.whirled.client.SceneService.SceneMoveListener;
import com.threerings.whirled.data.SceneModel;
import com.threerings.whirled.data.SceneUpdate;

import com.threerings.msoy.data.MemberObject;
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

    public void setActions (BodyObject player, String[] actions)
    {
        _playerActions.put(player.getOid(), actions);

        updatePlayerAction(player);
    }

    @Override
    protected PlaceObject createPlaceObject ()
    {
        return new ChiyogamiObject();
    }

    @Override
    protected void didStartup ()
    {
        _gameObj = (ChiyogamiObject) _plobj;

        // get a handle on the room we're in
        _sceneId = ((WorldGameConfig) getConfig()).startSceneId;
        _roomMgr = (RoomManager) MsoyServer.screg.getSceneManager(_sceneId);
        _roomObj = (RoomObject) _roomMgr.getPlaceObject();
        _roomObj.addListener(_roomListener);

        super.didStartup();

        // wait 30 seconds and then start...
        new ChiInterval() {
            public void safeExpired ()
            {
                initiateRound();
            }
        }.schedule(DELAY);
    }

    protected void initiateRound ()
    {
        // right away pick music
        pickNewMusic();

        // have the boss show up in 3 seconds
        new ChiInterval() {
            public void safeExpired ()
            {
                pickNewBoss();
            }
        }.schedule(3000);

        // start the round in 30...
        new ChiInterval() {
            public void safeExpired ()
            {
                startRound();
            }
        }.schedule(DELAY);
    }

    @Override
    protected void gameWillStart ()
    {
        super.gameWillStart();

        // all player actions must be re-populated
        _playerActions.clear();
    }

    @Override
    protected void gameDidStart ()
    {
        super.gameDidStart();

        updateBossAction();
    }

    /**
     * Start the round!
     */
    protected void startRound ()
    {
        repositionAllPlayers();
        startGame();
        _roomObj.postMessage(RoomObject.PLAY_MUSIC, new Object[] { _music.getMediaPath() });
        bossSpeak("Ok... it's a dance off!");
        moveBody(_bossObj, .5, .5, 0);
    }

    protected void didShutdown ()
    {
        super.didShutdown();

        shutdownBoss();

        _roomObj.removeListener(_roomListener);
        _roomObj.postMessage(RoomObject.PLAY_MUSIC); // no arg stops music
    }

    protected void shutdownBoss ()
    {
        if (_bossObj != null) {
            bossSpeak("I'm outta here.");
            MsoyServer.screg.sceneprov.leaveOccupiedScene(_bossObj);
            MsoyServer.omgr.destroyObject(_bossObj.getOid());
            _bossObj = null;
        }
    }

    protected void pickNewMusic ()
    {
        String song = RandomUtil.pickRandom(MUSICS);
        _music  = new StaticMediaDesc(
            MediaDesc.AUDIO_MPEG, Item.AUDIO, "chiyogami/" + song);
        _roomObj.postMessage(RoomObject.LOAD_MUSIC, new Object[] { _music.getMediaPath() });
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
        _bossObj.setUsername(new Name("Boss B Boy"));

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

//        _gameObj.startTransaction();
//        try {
//            _gameObj.setBossHealth(1f);
//            _gameObj.setBoss(
//        } finally {
//            _gameObj.commitTransaction();
//        }
    }

    /**
     * Called once the boss is added to the room.
     */
    protected void bossAddedToRoom ()
    {
        bossSpeak("I'm all up in your room, screwing with your furni");

        new ChiInterval() {
            public void safeExpired ()
            {
                bossSpeak("Mind if I take over? HAhahaha!");
            }
        }.schedule(2000);

        new ChiInterval() {
            public void safeExpired ()
            {
                if (!_gameObj.isInPlay()) {
                    // move the boss randomly
                    moveBody(_bossObj, Math.random(), Math.random(), 0);

                } else {
                    cancel();
                }
            }
        }.schedule(3000, 2000);
    }

    protected void repositionAllPlayers ()
    {
        // TODO: position based on performance?

        // find all the players and arrange them in a semicircle around the boss
        int numPlayers = _gameObj.occupants.size();
        double angleIncrement = Math.PI / (numPlayers - 1);
        double angle = 0;
        for (int ii = 0; ii < numPlayers; ii++) {
            BodyObject player = (BodyObject) MsoyServer.omgr.getObject(
                _gameObj.occupants.get(ii));

            // position players in a semicircle behind the boss
            double x = .5 + .5 * Math.cos(angle);
            double z = .5 + .5 * Math.sin(angle);
            moveBody(player, x, z, 0);

            angle += angleIncrement;
        }
    }

    /**
     * Move the specified body to a fully-specified location.
     */
    protected void moveBody (BodyObject body, double x, double z, int orient)
    {
        String error = _roomMgr.changeLocation(body, new MsoyLocation(x, 0, z, orient));
        if (error != null) {
            // this shouldn't happen
            log.warning("Error moving body [e=" + error + "].");
        }
    }

    /**
     * Move the specified body to the specified location, facing center.
     */
    protected void moveBody (BodyObject body, double x, double z)
    {
        // TODO checkmath
        double angle = Math.atan2(x - .5, z - .5);
        int degrees = (int) Math.round(angle * 180 / Math.PI);
        moveBody(body, x, z, degrees);
        updatePlayerAction(body);
    }

    protected void bossSpeak (String utterance)
    {
        SpeakProvider.sendSpeak(_roomObj, _bossObj.username, null, utterance);
    }

    protected void updatePlayerAction (BodyObject player)
    {
        String[] actions = _playerActions.get(player.getOid());
        if (actions == null || actions.length == 0) {
            return;
        }

        // TODO: filtered dance actions
        // TODO: levels of dancing

        _roomObj.postMessage("avAction", player.getOid(), actions[0]);
    }

    protected void updateBossAction ()
    {
        _roomObj.postMessage("avAction", _bossObj.getOid(), _bossActions[1]);
    }

    @Override
    protected void tick (long tickStamp)
    {
        super.tick(tickStamp);

        if (!_gameObj.isInPlay()) {
            return;
        }
        int numPlayers = _gameObj.occupants.size();
        for (int ii = 0; ii < numPlayers; ii++) {
            BodyObject player = (BodyObject) MsoyServer.omgr.getObject(
                _gameObj.occupants.get(ii));
            updatePlayerAction(player);
        }
        updateBossAction();
    }

    /**
     * Listens for changes to the RoomObject in which we're hosted.
     */
    protected class RoomListener
        implements OidListListener
    {
        // from OidListListener
        public void objectAdded (ObjectAddedEvent event)
        {
            if (_bossObj != null && _bossObj.getOid() == event.getOid()) {
                bossAddedToRoom();

            } else {
                if (_gameObj.isInPlay()) {
                    repositionAllPlayers();
                }
            }
        }

        // from OidListListener
        public void objectRemoved (ObjectRemovedEvent event)
        {
            // when someone leaves the room, kick them out of the chiyogami game
            int oid = event.getOid();
            if (_gameObj.occupants.contains(oid)) {
                try {
                    MsoyServer.worldGameReg.leaveWorldGame(
                        (MemberObject) MsoyServer.omgr.getObject(oid));
                } catch (InvocationException ie) {
                    log.warning("Error removing user from chiyogami game: " + ie);
                }
            }
        }
    } // End: class RoomListener

    protected abstract class ChiInterval extends Interval
    {
        public ChiInterval ()
        {
            super(MsoyServer.omgr);
        }

        public void expired ()
        {
            if (_gameObj.isActive()) {
                safeExpired();
            }
        }

        public abstract void safeExpired ();

    } // End: class ChiInterval

    /** Listens to the room we're boom-chikka-ing. */
    protected RoomListener _roomListener = new RoomListener();

    /** Our world delegate. */
    protected WorldGameManagerDelegate _worldDelegate;

    /** A casted ref to our gameobject, this hides our superclass _gameObj. */
    protected ChiyogamiObject _gameObj;

    /** The sceneId of the game. */
    protected int _sceneId;

    protected MediaDesc _music;

    /** The room manager. */
    protected RoomManager _roomMgr;

    /** The room object where the game is taking place. */
    protected RoomObject _roomObj;

    /** The boss object. */
    protected BossObject _bossObj;

    /** A mapping of playerOid -> String[] of their actions. */
    protected HashIntMap<String[]> _playerActions = new HashIntMap<String[]>();

    protected String[] _bossActions = new String[] { "Stop", "Dance 1" };

    protected static final String[] MUSICS = {
        "18-Jay-R_MyOtherCarBeatle", "04-Jay-R_SriLankaHigh" };

    /** TEMP: The filenames of current boss avatars. */
    protected static final String[] BOSSES = { "bboy" };

    protected static final int DELAY = 10000; // 30000;
}
