//
// $Id$

package com.threerings.msoy.game.chiyogami.server;

import java.util.ArrayList;
import java.util.HashSet;

import java.util.logging.Level;

import com.samskivert.util.Interval;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.QuickSort;
import com.samskivert.util.RandomUtil;
import com.samskivert.util.ResultListener;

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

import com.threerings.msoy.item.data.all.Audio;
import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.StaticMediaDesc;

import com.threerings.msoy.world.data.EffectData;
import com.threerings.msoy.world.data.MsoyLocation;
import com.threerings.msoy.world.data.RoomCodes;
import com.threerings.msoy.world.data.RoomObject;
import com.threerings.msoy.world.data.WorldOccupantInfo;
import com.threerings.msoy.world.server.RoomManager;

import com.threerings.msoy.game.data.WorldGameConfig;
import com.threerings.msoy.game.data.PerfRecord;

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
     * Invoked by clients to submit tags for consideration in picking music.
     */
    public void submitTags (BodyObject player, String tags)
    {
        if (tags != null) {
            _playerTags.put(player.getOid(), tags);
        }
    }

    /**
     * Invoked by clients to report their performance at their minigame.
     */
    public void reportPerf (BodyObject player, float score, float style)
    {
//        System.err.println(player.who() + " reported [score=" + score + ", " +
//            "style=" + style + "].");

        PlayerRec perf = _playerPerfs.get(player.getOid());
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
                // we'll notice that he's dead next tick
            }
        }
    }

    @Override
    protected PlaceObject createPlaceObject ()
    {
        return new ChiyogamiObject();
    }

    @Override
    protected boolean shouldCreateSpeakService ()
    {
        return false;
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
    }

    @Override
    protected void gameWillStart ()
    {
        super.gameWillStart();
    }

    @Override
    protected void gameDidStart ()
    {
        super.gameDidStart();
    }

    @Override
    protected void gameDidEnd ()
    {
        super.gameDidEnd();

        // TEMP
        shutdown();
    }

    protected void didShutdown ()
    {
        super.didShutdown();

        removeAllEffects();
        clearPlayerStates();
        shutdownBoss();
        _roomObj.removeListener(_roomListener);
        _roomObj.postMessage(RoomObject.PLAY_MUSIC); // no arg stops music
    }

    protected void removeAllEffects ()
    {
        _roomObj.startTransaction();
        try {
            for (EffectData effect : _effects) {
                _roomObj.removeFromEffects(effect.getKey());
            }
        } finally {
            _roomObj.commitTransaction();
        }
        _effects.clear();
    }

    @Override
    protected void bodyLeft (int bodyOid)
    {
        super.bodyLeft(bodyOid);

        BodyObject player = (BodyObject) MsoyServer.omgr.getObject(bodyOid);
        if (player != null && player.isActive()) {
            // make them stop dancing. I don't care what state they were in before.
            updateState(player, null);
        }
    }

    @Override
    protected void tick (long tickStamp)
    {
        super.tick(tickStamp);

        _phaseCounter++;
        switch (_gameObj.phase) {
        case ChiyogamiObject.WAITING:
            // wait, wait, then at #3 pick the boss and music
            // move to pre-battle once both are ready
            if (_phaseCounter == 3) {
                pickNewBoss();
                pickNewMusic();
            }
            break;

        case ChiyogamiObject.PRE_BATTLE:
            switch (_phaseCounter) {
            case 1:
                bossSpeak("Mind if I take over? HAhahaha!");
                break;

            default:
                // move the boss randomly around the room
                moveBody(_bossObj, Math.random(), Math.random());
                break;

            case 4:
                // let's start the battle
                startBattle();
                break;
            }
            break;

        case ChiyogamiObject.BATTLE:
            updatePlayerPerformances();
            if (_gameObj.bossHealth == 0) {
                endBattle();
            }
            break;

        case ChiyogamiObject.POST_BATTLE:
            switch (_phaseCounter) {
            case 1:
                bossSpeak("I think I sprained my pinky, I've got to go...");
                break;

            case 2:
                shutdownBoss();
                break;

            case 3:
                roomSpeak("The next round will begin in a few moments...");
                setPhase(ChiyogamiObject.WAITING);
                break;
            }
        }
    }

    /**
     * Change the phase of the game.
     */
    protected void setPhase (byte phase)
    {
        _gameObj.setPhase(phase);
        _phaseCounter = 0;
    }

    protected void startBattle ()
    {
        setPhase(ChiyogamiObject.BATTLE);

        // all player actions must be re-populated
        _playerStates.clear();

        // create blank perf records for every player, and randomly assign them a side (L|R)
        _playerPerfs.clear();
        for (int ii = _gameObj.occupants.size() - 1; ii >= 0; ii--) {
            int oid = _gameObj.occupants.get(ii);
            _playerPerfs.put(oid, new PlayerRec(oid));
        }

        // start the music playing
        if (_music != null) {
            _roomObj.postMessage(RoomObject.PLAY_MUSIC,
                new Object[] { _music.audioMedia.getMediaPath() });
        }

        // get the boss ready
        bossSpeak("Ok... it's a dance off!");
        moveBody(_bossObj, .5, .5);
        repositionAllPlayers(System.currentTimeMillis());
        updateBossState();

        // TEMP background effect
        _effects.add(_roomMgr.addEffect(
            new StaticMediaDesc(MediaDesc.APPLICATION_SHOCKWAVE_FLASH, Item.FURNITURE,
                "chiyogami/FX_arrow"),
            new MsoyLocation(.5, 0, 0, 0), RoomCodes.BACKGROUND_EFFECT_LAYER));
        // TEMP foreground effect
        _effects.add(_roomMgr.addEffect(
            new StaticMediaDesc(MediaDesc.APPLICATION_SHOCKWAVE_FLASH, Item.FURNITURE,
                "chiyogami/FallBalls"),
            new MsoyLocation(.5, 0, 0, 0), RoomCodes.FOREGROUND_EFFECT_LAYER));


        _playerTags.clear(); // clear tags until next time
    }

    protected void endBattle ()
    {
        clearPlayerStates(); // TODO: final animation?
        removeAllEffects();
        setPhase(ChiyogamiObject.POST_BATTLE);
    }

    /**
     * Get all the unique tags that users have submitted.
     */
    protected String[] getAllUserTags ()
    {
        HashSet<String> set = new HashSet<String>();
        for (String s : _playerTags.values()) {
            for (String tag : s.split("\\s")) {
                set.add(tag);
            }
        }

        return set.toArray(new String[set.size()]);
    }

    /**
     * Initiate picking new music for the next battle.
     */
    protected void pickNewMusic ()
    {
        _music = null;
        _musicPicked = false;
        pickNewMusic(getAllUserTags());
    }

    /**
     * Pick new music according to the specified tags, or randomly
     * if nothing matches the tags.
     */
    protected void pickNewMusic (final String[] tags)
    {
        MsoyServer.itemMan.getRandomCatalogItem(Item.AUDIO, tags, new ResultListener<Item>() {
            public void requestFailed (Exception cause) {
                log.log(Level.WARNING, "Failed to pick new music", cause);
            }

            public void requestCompleted (Item music) {
                if (music == null && tags != null && tags.length > 0) {
                    // none of the tags worked, try again without them
                    pickNewMusic(null);

                } else {
                    musicPicked((Audio) music);
                }
            }
        });
    }

    /**
     * Called when the music for the battle has finally been picked.
     */
    protected void musicPicked (Audio music)
    {
        _music = music;
        _musicPicked = true; // because _music can legally be null, currently
        if (_roomObj.isActive() && _music != null) {
            _roomObj.postMessage(RoomObject.LOAD_MUSIC,
                new Object[] { _music.audioMedia.getMediaPath() });
        }
        checkTransitionToPreBattle();
    }

    /**
     * Pick a new boss.
     */
    protected void pickNewBoss ()
    {
        shutdownBoss();

        pickNewBoss(getAllUserTags());
    }

    /**
     * Pick new boss according to the specified tags, or randomly
     * if nothing matches the tags.
     */
    protected void pickNewBoss (final String[] tags)
    {
        MsoyServer.itemMan.getRandomCatalogItem(Item.AVATAR, tags, new ResultListener<Item>() {
            public void requestFailed (Exception cause) {
                log.log(Level.WARNING, "Failed to pick new boss", cause);
            }

            public void requestCompleted (Item boss) {
                if (boss == null && tags != null && tags.length > 0) {
                    // none of the tags worked, try again without them
                    pickNewBoss(null);

                } else {
                    bossPicked((Avatar) boss);
                }
            }
        });
    }

    /**
     * Called when the boss for a battle is finally available.
     */
    protected void bossPicked (Avatar boss)
    {
        _bossObj = MsoyServer.omgr.registerObject(new BossObject());

       if (boss == null) {
           // TODO: remove this old stuff
            String hardBoss = RandomUtil.pickRandom(BOSSES);
            _bossObj.init(new StaticMediaDesc(
                MediaDesc.APPLICATION_SHOCKWAVE_FLASH, Item.AVATAR, "chiyogami/" + boss));
            _bossObj.setUsername(new Name("Downrock"));

        } else {
            _bossObj.init(boss);
        }

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

        checkTransitionToPreBattle();
    }

    /**
     * We can move to the pre-battle phase once we've picked all the assets
     * for the upcoming battle.
     */
    protected void checkTransitionToPreBattle ()
    {
        if (!_gameObj.isActive() || !_musicPicked || (_bossObj == null) ||
                !_roomObj.occupants.contains(_bossObj.getOid())) {
            return;
        }
        setPhase(ChiyogamiObject.PRE_BATTLE);
    }

    /**
     * Shutdown and clear any boss currently in the game.
     */
    protected void shutdownBoss ()
    {
        if (_bossObj != null) {
            bossSpeak("I'm outta here.");
            MsoyServer.screg.sceneprov.leaveOccupiedScene(_bossObj);
            MsoyServer.omgr.destroyObject(_bossObj.getOid());
            _bossObj = null;
        }

        // set the health to NaN to indicate that it's irrelevant
        if (_gameObj.isActive()) {
            _gameObj.startTransaction();
            try {
                _gameObj.setBossOid(0);
                _gameObj.setBossHealth(Float.NaN);
            } finally {
                _gameObj.commitTransaction();
            }
        }
    }

    protected void repositionAllPlayers (long now)
    {
        // create a list containing only the present players
        ArrayList<PlayerRec> list = new ArrayList<PlayerRec>(_playerPerfs.size());
        for (PlayerRec rec : _playerPerfs.values()) {
            if (_gameObj.occupants.contains(rec.oid)) {
                list.add(rec);
            }
        }

        // sort the list completely
        QuickSort.sort(list);

        @SuppressWarnings("unchecked")
        ArrayList<PlayerRec>[] sides = (ArrayList<PlayerRec>[]) new ArrayList[2];
        sides[0] = new ArrayList<PlayerRec>(list.size());
        sides[1] = new ArrayList<PlayerRec>(list.size());

        // as long as there are two folks left in the big list...
        while (list.size() >= 2) {
            PlayerRec first = list.remove(0);
            PlayerRec second = list.remove(0);

            if (first.lastSide == second.lastSide) { // both may also be -1
                // they're the same, so just assign and only one will have to
                // to change sides
                first.lastSide = 0;
                second.lastSide = 1;

            } else if (first.lastSide == -1) {
                first.lastSide = 1 - second.lastSide;

            } else if (second.lastSide == -1) {
                second.lastSide = 1 - first.lastSide;
            }

            // add them to their respective sides
            sides[first.lastSide].add(first);
            sides[second.lastSide].add(second);
        }

        // if there's one more...
        if (!list.isEmpty()) {
            PlayerRec rec = list.remove(0);

            if (rec.lastSide == -1) {
                // if there are no others, just put it in 0
                if (sides[0].isEmpty()) {
                    rec.lastSide = 0;

                } else {
                    // it's the lowest scoring guy, so assign him to the side with
                    // the higher bottom score
                    rec.lastSide = (sides[0].get(sides[0].size() - 1).calcScore >
                        sides[1].get(sides[1].size() - 1).calcScore) ? 0 : 1;
                }
            }

            // add it to its side
            sides[rec.lastSide].add(rec);
        }

        // ok, now simply lay everyone out on their respective sides such that performance
        // is relative
        for (int side = 0; side < 2; side++) {
            int count = sides[side].size();
            // we're going to spread them out evenly in the range

            // figure out this user's backness as a rating between 0 - 1
            float portion = 1f / (count + 1);
            float perc = 0;

            for (PlayerRec rec : sides[side]) {
                perc += portion;
                double angle;
                if (side == 0) {
                    angle = (1 - perc) * Math.PI + Math.PI/2; 

                } else {
                    angle = Math.PI * perc - Math.PI/2;
                }
                // position players in a semicircle behind the boss
                double x = .5 + .5 * Math.cos(angle);
                double z = .5 + .5 * Math.sin(angle);
                //System.err.println("On the " + ((side == 0) ? "left" : "right") +
                //    " someone's at " + perc + " from the front: " + x + ", " + z);
                BodyObject player = (BodyObject) MsoyServer.omgr.getObject(rec.oid);
                moveBody(player, x, z);
            }
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
     * Move the specified body to the specified location, facing [ .5, 0, 0 ].
     */
    protected void moveBody (BodyObject body, double x, double z)
    {
        double angle = Math.atan2(.5 - x, z - .5);
        int degrees = (360 + (int) Math.round(angle * 180 / Math.PI)) % 360;
        moveBody(body, x, z, degrees);
    }

    protected void roomSpeak (String message)
    {
        // TODO: translations
        SpeakProvider.sendInfo(_roomObj, null, message);
    }

    protected void bossSpeak (String utterance)
    {
        // TODO: translations
        SpeakProvider.sendSpeak(_roomObj, _bossObj.username, null, utterance);
    }

    protected void updatePlayerState (BodyObject player)
    {
        updatePlayerState(player, System.currentTimeMillis());
    }

    protected void updatePlayerState (BodyObject player, long now)
    {
        PlayerRec perf = _playerPerfs.get(player.getOid());
        updatePlayerState(player, perf.calculateScore(now));
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

    protected void clearPlayerStates ()
    {
        int numPlayers = _gameObj.occupants.size();
        for (int ii = 0; ii < numPlayers; ii++) {
            BodyObject player = (BodyObject) MsoyServer.omgr.getObject(_gameObj.occupants.get(ii));
            updateState(player, null);
        }
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

    protected void updatePlayerPerformances ()
    {
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
    protected static class PlayerRec extends PerfRecord
    {
        /** The oid of the player. */
        public int oid;

        /** The player's last-used side, or -1 if not yet assigned. */
        public int lastSide = -1;

        public PlayerRec (int oid)
        {
            this.oid = oid;
        }

    } // End: static class PlayerRec

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

//    protected abstract class ChiInterval extends Interval
//    {
//        public ChiInterval ()
//        {
//            super(MsoyServer.omgr);
//        }
//
//        public void expired ()
//        {
//            if (_gameObj.isActive()) {
//                safeExpired();
//            }
//        }
//
//        public abstract void safeExpired ();
//
//    } // End: class ChiInterval

    /** Listens to the room we're boom-chikka-ing. */
    protected RoomListener _roomListener = new RoomListener();

    /** Our world delegate. */
    protected WorldGameManagerDelegate _worldDelegate;

    /** A casted ref to our gameobject, this hides our superclass _gameObj. */
    protected ChiyogamiObject _gameObj;

    /** The sceneId of the game. */
    protected int _sceneId;

    protected Audio _music;

    protected boolean _musicPicked;

    /** The room manager. */
    protected RoomManager _roomMgr;

    /** The room object where the game is taking place. */
    protected RoomObject _roomObj;

    /** Counts the number of ticks we've received in the current phase. */
    protected int _phaseCounter = 0;

    /** The currently displayed effects. */
    protected ArrayList<EffectData> _effects = new ArrayList<EffectData>();

    /** The boss object. */
    protected BossObject _bossObj;

    /** playerOid -> String[] of their states. */
    protected HashIntMap<String[]> _playerStates = new HashIntMap<String[]>();

    /** playerOid -> PlayerRec. */
    protected HashIntMap<PlayerRec> _playerPerfs = new HashIntMap<PlayerRec>();

    /** playerOid -> submitted tags. */
    protected HashIntMap<String> _playerTags = new HashIntMap<String>();

    protected String[] _bossStates = new String[] { null, "Dance 1", "Dance 2" };

    /** TEMP: The filenames of current boss avatars. */
    protected static final String[] BOSSES = { "bboy" };

    protected static final int DELAY = 10000; // 30000;
}
