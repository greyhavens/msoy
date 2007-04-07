//
// $Id$

package com.threerings.msoy.game.chiyogami.server;

import java.util.ArrayList;

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
import com.threerings.msoy.data.MsoyBodyObject;
import com.threerings.msoy.server.MsoyServer;

import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.MediaDesc;
import com.threerings.msoy.item.web.StaticMediaDesc;

import com.threerings.msoy.world.data.MsoyLocation;
import com.threerings.msoy.world.data.RoomCodes;
import com.threerings.msoy.world.data.RoomObject;
import com.threerings.msoy.world.data.WorldOccupantInfo;
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

    /**
     * Invoked by clients to report their avatar states.
     */
    public void setStates (BodyObject player, String[] states)
    {
        // possibly filter down to just dance actions (and the default state)
        ArrayList<String> list = new ArrayList<String>(states.length);
        for (String state : states) {
            if (state == null || state.toLowerCase().startsWith("dance")) {
                list.add(state);
            }
        }
        int size = list.size();
        // if a non-empty (and non-identical) subset of the states
        // are dancing states, select out just those
        if (size != 0 && size != states.length) {
            states = new String[size];
            list.toArray(states);
        }

        // stash the states
        _playerStates.put(player.getOid(), states);

        // update the player's state, just in case
        updatePlayerState(player);
    }

    /**
     * Invoked by clients to report their performance at their minigame.
     */
    public void reportPerf (BodyObject player, float score, float style)
    {
//        System.err.println(player.who() + " reported [score=" + score + ", " +
//            "style=" + style + "].");

        PlayerPerfRecord perf = _playerPerfs.get(player.getOid());
        if (perf == null) {
            log.warning("Received performance report from non-player [who=" + player.who() + "].");
            return;
        }

        long now = System.currentTimeMillis();
        perf.recordPerformance(now, score, style);

        // and go ahead and set the player's instant dancing to this
        // last score, for now
        updatePlayerState(player, score);

        // and we want to report this performance instantly to clients
        // so that they can react?
        // TODO

        // affect the health of the boss
        float health = _gameObj.bossHealth;
        // but only if the boss is not already dead!
        if (health > 0) {
            // TODO: scoring stuff
            //
            health = Math.max(0, health - (score / 10f));
            _gameObj.setBossHealth(health);
            if (health == 0) {
                // the boss is dead!
                bossSpeak("Oh! My liver! My spleen!");
                updateState(_bossObj, null);

                // then wait 2 seconds and end the round.
                new ChiInterval() {
                    public void safeExpired () {
                        endRound();
                    }
                }.schedule(2000);
            }
        }
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
        _playerStates.clear();

        // create blank perf records for every player
        _playerPerfs.clear();
        for (int ii = _gameObj.occupants.size() - 1; ii >= 0; ii--) {
            _playerPerfs.put(_gameObj.occupants.get(ii), new PlayerPerfRecord());
        }
    }

    @Override
    protected void gameDidStart ()
    {
        super.gameDidStart();

        updateBossState();
    }

    @Override
    protected void gameDidEnd ()
    {
        super.gameDidEnd();

        // TEMP
        shutdown();
    }

    /**
     * Start the round!
     */
    protected void startRound ()
    {
        startGame();
        _roomObj.postMessage(RoomObject.PLAY_MUSIC, new Object[] { _music.getMediaPath() });
        bossSpeak("Ok... it's a dance off!");

        moveBody(_bossObj, .5, .5);
        repositionAllPlayers(System.currentTimeMillis());
    }

    protected void endRound ()
    {
        bossSpeak("I think I sprained my pinky, I've got to go...");

        shutdownBoss();
        endGame();
    }

    protected void didShutdown ()
    {
        super.didShutdown();

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

        // set the health to NaN to indicate that it's irrelevant
        _gameObj.startTransaction();
        try {
            _gameObj.setBossOid(0);
            _gameObj.setBossHealth(Float.NaN);
        } finally {
            _gameObj.commitTransaction();
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
        _bossObj.setUsername(new Name("Downrock"));

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
    }

    /**
     * Called once the boss is added to the room.
     */
    protected void bossAddedToRoom ()
    {
        bossSpeak("I'm all up in your room, screwing with your furni");

        // set the new boss' health to 1
        _gameObj.startTransaction();
        try {
            _gameObj.setBossOid(_bossObj.getOid());
            _gameObj.setBossHealth(1f);

        } finally {
            _gameObj.commitTransaction();
        }

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
                    moveBody(_bossObj, Math.random(), Math.random());

                } else {
                    cancel();
                }
            }
        }.schedule(3000, 2000);
    }

    protected void repositionAllPlayers (long now)
    {
        int numPlayers = _gameObj.occupants.size();

        // TODO: reconsider?
        //
        // arrange all the players in a circular area around the boss depending
        // on their performance. Alternate placing players on the left or right
        // sides

        for (int ii = 0; ii < numPlayers; ii++) {
            int oid = _gameObj.occupants.get(ii);
            BodyObject player = (BodyObject) MsoyServer.omgr.getObject(oid);
            PlayerPerfRecord perf = _playerPerfs.get(oid);
            float score = perf.getScore(now);

            // score will range from 0 -> 1, arrange them that-a-way
            double angle;
            if (ii % 2 == 0) {
                // arrange them on the left
                angle = Math.PI * score + Math.PI/2;

            } else {
                // arrange them on the right
                angle = Math.PI * (1 - score) - Math.PI/2;
            }

            // position players in a semicircle behind the boss
            double x = .5 + .5 * Math.cos(angle);
            double z = .5 + .5 * Math.sin(angle);
//            System.err.println("Score: " + score + ", position=" +
//                x + ", " + z);
            moveBody(player, x, z);
        }

//        double angleIncrement = Math.PI / (numPlayers - 1);
//        double angle = 0;
//        for (int ii = 0; ii < numPlayers; ii++) {
//            BodyObject player = (BodyObject) MsoyServer.omgr.getObject(
//                _gameObj.occupants.get(ii));
//
//            // position players in a semicircle behind the boss
//            double x = .5 + .5 * Math.cos(angle);
//            double z = 1 * Math.sin(angle);
//            moveBody(player, x, z);
//
//            angle += angleIncrement;
//        }
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
     * Move the specified body to the specified location, facing [ .5, 0, 0 ].
     */
    protected void moveBody (BodyObject body, double x, double z)
    {
        double angle = Math.atan2(.5 - x, z - .5);
        int degrees = (360 + (int) Math.round(angle * 180 / Math.PI)) % 360;
        moveBody(body, x, z, degrees);
    }

    protected void bossSpeak (String utterance)
    {
        SpeakProvider.sendSpeak(_roomObj, _bossObj.username, null, utterance);
    }

    protected void updatePlayerState (BodyObject player)
    {
        updatePlayerState(player, System.currentTimeMillis());
    }

    protected void updatePlayerState (BodyObject player, long now)
    {
        PlayerPerfRecord perf = _playerPerfs.get(player.getOid());
        updatePlayerState(player, perf.getScore(now));
    }

    protected void updatePlayerState (BodyObject player, float score)
    {
        String[] states = _playerStates.get(player.getOid());
        if (states == null || states.length == 0) {
            // nothing to do
            return;
        }

        int danceStates = states.length - 1;
        String state;
        if (score == 0 || danceStates == 0) {
            // only if they have a completely-zero score do they not dance
            state = states[0];

        } else {
            // pick a state corresponding to their performance
            state = states[1 + Math.min(danceStates - 1, (int) Math.floor(score * danceStates))];
        }

        updateState(player, state);
    }

    protected void updateBossState ()
    {
        updateState(_bossObj, _bossStates[RandomUtil.getInt(2) + 1]);
    }

//    protected void updateAction (int oid, String action)
//    {
//        WorldOccupantInfo winfo = (WorldOccupantInfo) _roomObj.occupantInfo.get(oid);
//
//        _roomObj.postMessage(RoomCodes.SPRITE_MESSAGE, winfo.getItemIdent(),
//            action, null, true);
//    }

    /**
     * Update the state of the specified player.
     */
    protected void updateState (BodyObject body, String state)
    {
        _roomMgr.setState((MsoyBodyObject) body, state);
    }

    @Override
    protected void tick (long tickStamp)
    {
        super.tick(tickStamp);

        if (!_gameObj.isInPlay()) {
            return;
        }

        _gameObj.startTransaction();
        try {
            _roomObj.startTransaction();
            try {
                long now = System.currentTimeMillis();
                int numPlayers = _gameObj.occupants.size();
                for (int ii = 0; ii < numPlayers; ii++) {
                    BodyObject player = (BodyObject) MsoyServer.omgr.getObject(
                        _gameObj.occupants.get(ii));
                    updatePlayerState(player, now);
                }
                if (_gameObj.bossHealth > 0) {
                    updateBossState();
                }

                repositionAllPlayers(now);

            } finally {
                _roomObj.commitTransaction();
            }
        } finally {
            _gameObj.commitTransaction();
        }
    }

    /**
     * Tracks performance for each player.
     */
    protected static class PlayerPerfRecord
    {
        public void recordPerformance (long now, float score, float style)
        {
            int index = _count % BUCKETS;
            _scores[index] = score;
            _styles[index] = style;
            _stamps[index] = now;

            // track totals
            _count++;
            _totalScore += score;
            _totalStyle += style;
        }

        public float getScore (long now)
        {
            return getAccumulated(now, _scores);
        }

        public float getStyle (long now)
        {
            return getAccumulated(now, _styles);
        }

        /**
         * Get the decaying score/style value for this player, disregarding
         * scores that are too old.
         */
        protected float getAccumulated (long now, float[] values)
        {
            float accum = 0;
            float total = 0;
            long oldest = now - MAX_TIME;
            for (int ii = 1; ii <= BUCKETS; ii++) {
                int index = _count - ii;
                if (index < 0) {
                    break;
                }
                index = index % BUCKETS;
                if (_stamps[index] < oldest) {
                    break;
                }

                // the most recent score has a weight of 1, the one before
                // a weight of .5, the one before of .25...
                float frac = 1f / ii;
                accum += values[index] * frac;
                total += frac;
            }

            return (total > 0) ? (accum / total) : accum;
        }

        /** The number of scores recorded. */
        protected int _count;

        protected float _totalScore;
        protected float _totalStyle;

        // number of previous scores to count
        protected static final int BUCKETS = 10;
        // the maximum time a score will last
        protected static final int MAX_TIME = 30000;

        protected float[] _scores = new float[BUCKETS];
        protected float[] _styles = new float[BUCKETS];
        protected long[] _stamps = new long[BUCKETS];

    } // End: static class PlayerPerfReccord

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

//            } else {
//                if (_gameObj.isInPlay()) {
//                    repositionAllPlayers();
//                }
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

    /** A mapping of playerOid -> String[] of their states. */
    protected HashIntMap<String[]> _playerStates = new HashIntMap<String[]>();

    /** A mapping of playerOid -> PlayerPerfRecord. */
    protected HashIntMap<PlayerPerfRecord> _playerPerfs = new HashIntMap<PlayerPerfRecord>();

    protected String[] _bossStates = new String[] { null, "Dance 1", "Dance 2" };

    protected static final String[] MUSICS = {
        "18-Jay-R_MyOtherCarBeatle", "04-Jay-R_SriLankaHigh" };

    /** TEMP: The filenames of current boss avatars. */
    protected static final String[] BOSSES = { "bboy" };

    protected static final int DELAY = 10000; // 30000;
}
