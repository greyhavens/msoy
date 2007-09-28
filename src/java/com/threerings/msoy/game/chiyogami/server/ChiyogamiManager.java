//
// $Id$

package com.threerings.msoy.game.chiyogami.server;

import java.util.ArrayList;
import com.samskivert.util.Interval;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.QuickSort;
import com.samskivert.util.RandomUtil;
import com.samskivert.util.StringUtil;

import com.threerings.util.MessageBundle;
import com.threerings.presents.dobj.MessageEvent;
import com.threerings.presents.dobj.MessageListener;
import com.threerings.presents.dobj.ObjectAddedEvent;
import com.threerings.presents.dobj.ObjectRemovedEvent;
import com.threerings.presents.dobj.OidListListener;
import com.threerings.presents.server.InvocationException;

import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.crowd.chat.server.SpeakUtil;

import com.threerings.parlor.game.server.GameManager;

import com.threerings.whirled.client.SceneMoveAdapter;
import com.threerings.whirled.spot.data.SceneLocation;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyBodyObject;
import com.threerings.msoy.server.MsoyServer;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.StaticMediaDesc;

import com.threerings.msoy.world.data.EffectData;
import com.threerings.msoy.world.data.MsoyLocation;
import com.threerings.msoy.world.data.RoomCodes;
import com.threerings.msoy.world.data.RoomObject;
import com.threerings.msoy.world.server.RoomManager;

import com.threerings.msoy.game.data.AVRGameConfig;
import com.threerings.msoy.game.data.PerfRecord;
import com.threerings.msoy.game.server.WhirledGameDelegate;

import com.threerings.msoy.game.chiyogami.data.ChiyogamiObject;

import static com.threerings.msoy.Log.log;

/**
 * Manages a game of Chiyogami dance battle.
 */
public class ChiyogamiManager extends GameManager
{
    public ChiyogamiManager ()
    {
//        addDelegate(_worldDelegate = new AVRGameManagerDelegate(this));
        addDelegate(_whirledDelegate = new WhirledGameDelegate(this));
    }

    /**
     * Invoked by clients to report their avatar states.
     */
    public void setStates (BodyObject player, String[] states)
    {
        setAvatarStates(player, states);

        // update the player's state, just in case
        updatePlayerState(player);
    }

    /**
     * Invoked by clients to report the boss' states, which we
     * otherwise do not know!
     */
    public void setBossStates (BodyObject player, String[] states)
    {
        // TODO: intercept hacking, perhaps record all the states that
        // each player submits and go with the consensus..
        //
        // TODO: Wait! No! We should just have the script contain the boss states/actions.
        // Yes? No?

        System.err.println("Player reported boss states: " + StringUtil.toString(states));

        // for now, just believe the last person
        setAvatarStates(_bossObj, states);
    }

//    /**
//     * Invoked by clients to submit tags for consideration in picking music.
//     */
//    public void submitTags (BodyObject player, String tags)
//    {
//        if (tags != null) {
//            _playerTags.put(player.getOid(), tags);
//        }
//    }

    /**
     * Invoked by clients to report their performance at their minigame.
     */
    public void reportPerf (BodyObject player, float score, float style)
    {
//        System.err.println(player.who() + " reported [score=" + score + ", " +
//            "style=" + style + "].");

        PlayerRec perf = _playerPerfs.get(player.getOid());
        if (perf == null) {
            log.warning("Received performance report from non-player [where=" + where() +
                        ", who=" + player.who() + "].");
            return;
        }

        long now = System.currentTimeMillis();
        perf.recordPerformance(now, score, style);

        // and go ahead and set the player's instant dancing to this
        // last score, for now
        updatePlayerState(player, score);

        // possibly play an effect for the player
        if (score >= .8 || style >= .8) {
            _roomMgr.addTransientEffect(player.getOid(),
                new StaticMediaDesc(MediaDesc.APPLICATION_SHOCKWAVE_FLASH, Item.FURNITURE,
                    "chiyogami/BonusHarp"));
        }

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
                bossSpeak(_script.deathBlowProse);
                updateState(_bossObj, null);
                // we'll notice that he's dead next tick
            }
        }
    }

    /**
     * Record the states for an avatar, as sent by a player.
     */
    protected void setAvatarStates (BodyObject body, String[] states)
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
        _playerStates.put(body.getOid(), states);
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
        _sceneId = ((AVRGameConfig) getConfig()).startSceneId;
        _roomMgr = (RoomManager) MsoyServer.screg.getSceneManager(_sceneId);
        _roomObj = (RoomObject) _roomMgr.getPlaceObject();
        _roomObj.addListener(_roomListener);

        super.didStartup();

        startGame();
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
        awardFlow();

        super.didShutdown();

        _endBattle.cancel();
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
                pickNewScript();
            }
            break;

        case ChiyogamiObject.PRE_BATTLE:
            switch (_phaseCounter) {
            case 1:
                bossSpeak(_script.trashProse);
                break;

            default:
                // move the boss randomly around the room
                moveBodyTo(_bossObj, Math.random(), Math.random());
                break;

            case 3:
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
                bossSpeak(_bossWon ? _script.victoryProse : _script.defeatProse);
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

    /**
     * We can move to the pre-battle phase once we've picked all the assets
     * for the upcoming battle.
     */
    protected void checkTransitionToPreBattle ()
    {
        if (!_gameObj.isActive() || (_bossObj == null) ||
                !_roomObj.occupants.contains(_bossObj.getOid())) {
            return;
        }
        setPhase(ChiyogamiObject.PRE_BATTLE);
    }

    /**
     * Transition to the BATTLE phase.
     */
    protected void startBattle ()
    {
        setPhase(ChiyogamiObject.BATTLE);

        // all player states must be re-populated, but keep the boss states
        String[] bossStates = _playerStates.get(_bossObj.getOid());
        _playerStates.clear();
        _playerStates.put(_bossObj.getOid(), bossStates);
//        int bossOid = _bossObj.getOid();
//        for (Interator itr = _playerStates.keys(); itr.hasNext(); ) {
//            if (bossOid != itr.next()) {
//                itr.remove();
//            }
//        }

        // create blank perf records for every player, and randomly assign them a side (L|R)
        _playerPerfs.clear();
        for (int ii = _gameObj.occupants.size() - 1; ii >= 0; ii--) {
            int oid = _gameObj.occupants.get(ii);
            _playerPerfs.put(oid, new PlayerRec(oid));
        }

        // start the music playing
        _roomObj.postMessage(RoomObject.PLAY_MUSIC, new Object[] { _script.music.getMediaPath() });

        // get the boss ready
        bossSpeak(_script.startProse);
        moveBody(_bossObj, .5, .5, 0);
        repositionAllPlayers(System.currentTimeMillis());
        updateBossState();

        // add the backgrounds specified in the script
        for (Script.EffectSpec spec : _script.effects) {
            _effects.add(_roomMgr.addEffect(spec.media, spec.location, spec.layer));
        }

        _playerTags.clear(); // clear tags until next time

        // and set up an interval to put the kibosh on things if it takes too long
        _endBattle.schedule(_script.battleTime * 1000L);
    }

    /**
     * Transition to the POST_BATTLE phase.
     */
    protected void endBattle ()
    {
        _endBattle.cancel();

        awardFlow();
        clearPlayerStates();
        _roomObj.postMessage(RoomObject.PLAY_MUSIC); // no arg stops music
        removeAllEffects();
        setPhase(ChiyogamiObject.POST_BATTLE);
    }

    /**
     * Award flow to all the players.
     */
    protected void awardFlow ()
    {
// TODO: revampulate!
//         MediaDesc effectMedia = new StaticMediaDesc(
//             MediaDesc.APPLICATION_SHOCKWAVE_FLASH, Item.FURNITURE, "chiyogami/FlowGain");
//         for (PlayerRec rec : _playerPerfs.values()) {
//             // their score / style averages will be between 0 and 1,
//             // we award them the max of those as a percentage of their possible flow..
//             int flowGained = _whirledDelegate.tracker.awardFlowPercentage(rec.oid,
//                 Math.max(rec.getAverageScore(), rec.getAverageStyle()));
//             if (flowGained > 0) {
//                 _roomMgr.addTransientEffect(rec.oid, effectMedia, EffectData.MODE_XLATE,
//                     MessageBundle.tcompose("m.flow_gain", String.valueOf(flowGained)));
//             }
//         }
    }

    /**
     * Called when the music ends (or from our _endBattle interval.
     */
    protected void musicDidEnd ()
    {
        if (_gameObj.bossHealth == 0) {
            // the boss just died anyway, don't worry about it
            return;
        }

        // otherwise..
        _bossWon = true;
        endBattle();
    }

//    /**
//     * Get all the unique tags that users have submitted.
//     */
//    protected String[] getAllUserTags ()
//    {
//        HashSet<String> set = new HashSet<String>();
//        for (String s : _playerTags.values()) {
//            for (String tag : s.split("\\s")) {
//                set.add(tag);
//            }
//        }
//
//        return set.toArray(new String[set.size()]);
//    }

    protected void pickNewScript ()
    {
        // clean up old stuff
        shutdownBoss();
        _bossWon = false;

        // TODO: really pick from the catalog
        _script = RandomUtil.pickRandom(_scripts);

        // tell everyone to load the music
        _roomObj.postMessage(RoomObject.LOAD_MUSIC, new Object[] { _script.music.getMediaPath() });

        _bossObj = MsoyServer.omgr.registerObject(new BossObject());
        _bossObj.init(_script.boss, _script.bossName);

        // add the boss to the room
        MsoyServer.screg.moveTo(_bossObj, _sceneId, -1, new SceneMoveAdapter() {
            public void requestFailed (String reason) {
                log.warning("Boss failed to enter scene [where=" + where() +
                            ", scene=" + _sceneId + ", reason=" + reason + "].");
                // TODO: shutdown? freakout? call the Elite Beat Agents?
            }
        });
    }

//    /**
//     * Initiate picking new music for the next battle.
//     */
//    protected void pickNewMusic ()
//    {
//        _music = null;
//        _musicPicked = false;
//        pickNewMusic(getAllUserTags());
//    }
//
//    /**
//     * Pick new music according to the specified tags, or randomly
//     * if nothing matches the tags.
//     */
//    protected void pickNewMusic (final String[] tags)
//    {
//        MsoyServer.itemMan.getRandomCatalogItem(Item.AUDIO, tags, new ResultListener<Item>() {
//            public void requestFailed (Exception cause) {
//                log.log(Level.WARNING, "Failed to pick new music", cause);
//            }
//
//            public void requestCompleted (Item music) {
//                if (music == null && tags != null && tags.length > 0) {
//                    // none of the tags worked, try again without them
//                    pickNewMusic(null);
//
//                } else {
//                    musicPicked((Audio) music);
//                }
//            }
//        });
//    }
//
//    /**
//     * Called when the music for the battle has finally been picked.
//     */
//    protected void musicPicked (Audio music)
//    {
//        _music = music;
//        _musicPicked = true; // because _music can legally be null, currently
//        if (_roomObj.isActive() && _music != null) {
//            _roomObj.postMessage(RoomObject.LOAD_MUSIC,
//                new Object[] { _music.audioMedia.getMediaPath() });
//        }
//        checkTransitionToPreBattle();
//    }

//    /**
//     * Pick a new boss.
//     */
//    protected void pickNewBoss ()
//    {
//        shutdownBoss();
//        _bossWon = false;
//        pickNewBoss(getAllUserTags());
//    }

//    /**
//     * Pick new boss according to the specified tags, or randomly
//     * if nothing matches the tags.
//     */
//    protected void pickNewBoss (final String[] tags)
//    {
//        MsoyServer.itemMan.getRandomCatalogItem(Item.AVATAR, tags, new ResultListener<Item>() {
//            public void requestFailed (Exception cause) {
//                log.log(Level.WARNING, "Failed to pick new boss", cause);
//            }
//
//            public void requestCompleted (Item boss) {
//                if (boss == null && tags != null && tags.length > 0) {
//                    // none of the tags worked, try again without them
//                    pickNewBoss(null);
//
//                } else {
//                    bossPicked((Avatar) boss);
//                }
//            }
//        });
//    }

//    /**
//     * Called when the boss for a battle is finally available.
//     */
//    protected void bossPicked (Avatar boss)
//    {
//        _bossObj = MsoyServer.omgr.registerObject(new BossObject());
//
//       if (boss == null) {
//           // TODO: remove this old stuff
//            String hardBoss = RandomUtil.pickRandom(BOSSES);
//            _bossObj.init(new StaticMediaDesc(
//                MediaDesc.APPLICATION_SHOCKWAVE_FLASH, Item.AVATAR, "chiyogami/" + hardBoss));
//            _bossObj.setUsername(new Name("Downrock"));
//
//        } else {
//            _bossObj.init(boss);
//        }
//
//        // add the boss to the room
//        MsoyServer.screg.moveTo(_bossObj, _sceneId, -1, new SceneMoveListener() {
//            public void moveSucceeded (int placeId, PlaceConfig config) {
//                // nada: we wait to hear the oid in RoomListener
//            }
//            public void moveSucceededWithUpdates (
//                int placeId, PlaceConfig config, SceneUpdate[] updates) {
//                // nada: we wait to hear the oid in RoomListener
//            }
//            public void moveSucceededWithScene (
//                int placeId, PlaceConfig config, SceneModel model) {
//                // nada: we wait to hear the oid in RoomListener
//            }
//            public void requestFailed (String reason) {
//                log.warning("Boss failed to enter scene [scene=" + _sceneId +
//                            ", reason=" + reason + "].");
//                // TODO: shutdown? freakout? call the Elite Beat Agents?
//            }
//        });
//    }

    /**
     * Called once the boss is added to the room.
     */
    protected void bossAddedToRoom ()
    {
        bossSpeak(_script.enterProse);

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
     * Shutdown and clear any boss currently in the game.
     */
    protected void shutdownBoss ()
    {
        if (_bossObj != null) {
            bossSpeak(_script.exitProse);
            MsoyServer.screg.leaveOccupiedScene(_bossObj);
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

    /**
     * Called during the BATTLE phase to reposition players according to their performance.
     */
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
        ArrayList<PlayerRec>[] sides = new ArrayList[2];
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
            log.warning("Error moving body [where=" + where() + ", e=" + error + "].");
        }
    }

    /**
     * Move the specified body to the specified location, facing [ .5, .5, 0 ].
     */
    protected void moveBody (BodyObject body, double x, double z)
    {
        moveBody(body, x, z, radiansToOrient(Math.atan2(.5 - z, .5 - x)));
    }

    /**
     * Move the body to the specified position, facing in the direction
     * that this new location lies from their previous location.
     */
    protected void moveBodyTo (BodyObject body, double x, double z)
    {
        SceneLocation sloc = (SceneLocation) _roomObj.occupantLocs.get(body.getOid());
        if (sloc != null) {
            MsoyLocation loc = (MsoyLocation) sloc.loc;
            moveBody(body, x, z, radiansToOrient(Math.atan2(z - loc.z, x - loc.x)));

        } else {
            // what? fallback
            moveBody(body, x, z);
        }
    }

    /**
     * Turn an angle in radians into a whirled orientation value, which is degrees with
     * 0 facing forward.
     */
    protected int radiansToOrient (double radians)
    {
        return (360 + 90 + (int) Math.round(radians * 180 / Math.PI)) % 360;
    }

    /**
     * Send an info message to the room.
     */
    protected void roomSpeak (String message)
    {
        // TODO: translations
        SpeakUtil.sendInfo(_roomObj, null, message);
    }

    protected void bossSpeak (String utterance)
    {
        if (utterance != null) {
            // TODO: translations
            SpeakUtil.sendSpeak(_roomObj, _bossObj.username, null, utterance);
        }
    }

    protected void updateBossState ()
    {
        updatePlayerState(_bossObj, .5f); // TODO: boss scoring ? ? ?
    }

    protected void updatePlayerState (BodyObject player)
    {
        updatePlayerState(player, System.currentTimeMillis());
    }

    protected void updatePlayerState (BodyObject player, long now)
    {
        PlayerRec perf = _playerPerfs.get(player.getOid());
        if (perf == null) {
            log.warning("Missing perf record for player [where=" + where () +
                        ", who=" + player.who() + "].");
        } else {
            updatePlayerState(player, perf.calculateScore(now));
        }
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
                int bossOid = _bossObj.getOid();
                long now = System.currentTimeMillis();
                int numPlayers = _gameObj.occupants.size();
                for (int ii = 0; ii < numPlayers; ii++) {
                    int oid = _gameObj.occupants.get(ii);
                    if (oid == bossOid) {
                        continue;
                    }
                    BodyObject player = (BodyObject)
                        MsoyServer.omgr.getObject(_gameObj.occupants.get(ii));
                    if (player != null) {
                        updatePlayerState(player, now);
                    }
                }
                // then, update the boss
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
        implements OidListListener, MessageListener
    {
        // from OidListListener
        public void objectAdded (ObjectAddedEvent event)
        {
            if (_bossObj != null && _bossObj.getOid() == event.getOid()) {
                bossAddedToRoom();
            }
        }

        // from OidListListener
        public void objectRemoved (ObjectRemovedEvent event)
        {
            // when someone leaves the room, kick them out of the chiyogami game
            int oid = event.getOid();
            if (_gameObj.occupants.contains(oid)) {
//                try {
//                    MsoyServer.worldGameReg.leaveAVRGame(
//                        (MemberObject) MsoyServer.omgr.getObject(oid));
//                } catch (InvocationException ie) {
//                    log.warning("Error removing user from chiyogami game [where=" + where() +
//                                ", error=" + ie + "].");
//                }
            }
        }

        // from MessageListener
        public void messageReceived (MessageEvent event)
        {
            if (RoomObject.MUSIC_ENDED.equals(event.getName())) {
                String url = (String) event.getArgs()[0];
                if (_script != null && url.equals(_script.music.getMediaPath())) {
                    musicDidEnd();
                }
            }
        }

    } // End: class RoomListener

    /**
     * TEMP?
     * A Script for a battle.
     *
     * TODO: eventually we will pick a datapack from the catalog, and construct
     * this in-memory data structure from that.
     */
    protected static class Script
    {
        /** Specifies effect details. */
        protected static class EffectSpec
        {
            public MediaDesc media;
            public byte layer;
            public MsoyLocation location;

            public EffectSpec (MediaDesc media, byte layer)
            {
                this(media, layer, null);
            }

            public EffectSpec (MediaDesc media, byte layer, MsoyLocation location)
            {
                this.media = media;
                this.layer = layer;
                this.location = location;
            }
        }

        //public Avatar boss;
        public MediaDesc boss;
        public String bossName;

        //public Audio music;
        public MediaDesc music;

        /** The maximum length of the battle, in seconds. */
        public int battleTime;

        /** Possible minigames to play. */
        public ArrayList<MediaDesc> games = new ArrayList<MediaDesc>();

        /** The effects to add during the dance. */
        public ArrayList<EffectSpec> effects = new ArrayList<EffectSpec>();

        //public ArrayList<String> bossStates = new ArrayList<String>();

        /** Prose spoken upon entry. */
        public String enterProse;

        /** Prose spoken during the trash-talking phase. */
        public String trashProse;

        /** Prose spoken at the start of the battle. */
        public String startProse;

        /** Prose to speak upon receiving the 'death blow'. */
        public String deathBlowProse;

        /** Prose to have the boss speak if he wins. */
        public String victoryProse;

        /** Prose to have the boss speak if he loses. */
        public String defeatProse;

        /** Prose spoken upon exit. */
        public String exitProse;

    } // End: static class Script

    /** TEMP. */
    protected static ArrayList<Script> _scripts = new ArrayList<Script>();

    static {
        // create a few starter scripts

        // Sample: Downrock
        Script script = new Script();
        script.boss = new StaticMediaDesc(MediaDesc.APPLICATION_SHOCKWAVE_FLASH, Item.AVATAR,
                    "chiyogami/bboy");
        script.bossName = "Downrock";
        script.music = new StaticMediaDesc(MediaDesc.AUDIO_MPEG, Item.AUDIO,
                    "chiyogami/04-Jay-R_SriLankaHigh");
        script.battleTime = 4*60 + 28;
        script.games.add(new StaticMediaDesc(MediaDesc.APPLICATION_SHOCKWAVE_FLASH,
                    Item.GAME, "chiyogami/Match3"));
        script.games.add(new StaticMediaDesc(MediaDesc.APPLICATION_SHOCKWAVE_FLASH,
                    Item.GAME, "chiyogami/KeyJam"));
        script.effects.add(new Script.EffectSpec(
            new StaticMediaDesc(MediaDesc.APPLICATION_SHOCKWAVE_FLASH, Item.FURNITURE,
            "chiyogami/FX_arrow"), RoomCodes.BACKGROUND_EFFECT_LAYER,
            /*new MsoyLocation(.5, 0, 0, 0)*/ null));
        script.effects.add(new Script.EffectSpec(
            new StaticMediaDesc(MediaDesc.APPLICATION_SHOCKWAVE_FLASH, Item.FURNITURE,
            "chiyogami/FallBalls"), RoomCodes.FOREGROUND_EFFECT_LAYER,
            /*new MsoyLocation(.5, 0, 0, 0)*/ null));
        script.enterProse = "I'm all up in your room, screwing with your furni";
        script.trashProse = "Mind if I take over? Hahahaha!";
        script.startProse = "Ok... it's a dance off!";
        script.deathBlowProse = "Oh! My liver! My spleen!";
        script.victoryProse = "Ah ha! You were no match for me! I'm so great!";
        script.defeatProse = "I think I sprained my pinky, I've got to go...";
        script.exitProse = "I'm outta here.";
        _scripts.add(script);

        // sample: Hula Girl
        script = new Script();
        script.boss = new StaticMediaDesc(MediaDesc.APPLICATION_SHOCKWAVE_FLASH, Item.AVATAR,
                    "chiyogami/HulaGirl");
        script.bossName = "Pretty Little Pyro";
        script.music = new StaticMediaDesc(MediaDesc.AUDIO_MPEG, Item.AUDIO,
                    "chiyogami/18-Jay-R_MyOtherCarBeatle");
        script.battleTime = 4*60 + 52;
        script.games.add(new StaticMediaDesc(MediaDesc.APPLICATION_SHOCKWAVE_FLASH,
                    Item.GAME, "chiyogami/Match3"));
        script.games.add(new StaticMediaDesc(MediaDesc.APPLICATION_SHOCKWAVE_FLASH,
                    Item.GAME, "chiyogami/KeyJam"));
        script.effects.add(new Script.EffectSpec(
            new StaticMediaDesc(MediaDesc.APPLICATION_SHOCKWAVE_FLASH, Item.FURNITURE,
            "chiyogami/FX_arrow"), RoomCodes.BACKGROUND_EFFECT_LAYER,
            /*new MsoyLocation(.5, 0, 0, 0)*/ null));
        script.effects.add(new Script.EffectSpec(
            new StaticMediaDesc(MediaDesc.APPLICATION_SHOCKWAVE_FLASH, Item.FURNITURE,
            "chiyogami/FallBalls"), RoomCodes.FOREGROUND_EFFECT_LAYER,
            /*new MsoyLocation(.5, 0, 0, 0)*/ null));
        script.enterProse = "Hello there..";
        script.trashProse = "Cute place, but I'd better burn it down anyway.";
        script.startProse = "Oh? You'll have to outdance me to stop me!";
        script.deathBlowProse = "I feel my internal flame a'flickering...";
        script.victoryProse = "Oh, I'm going to love redecorating.";
        script.defeatProse = "That wasn't fair! You cheated! You... must have...";
        script.exitProse = "Aloha!";
        _scripts.add(script);
    }

    /** Listens to the room we're boom-chikka-ing. */
    protected RoomListener _roomListener = new RoomListener();

    /** Handles Whirled game services. */
    protected WhirledGameDelegate _whirledDelegate;

    /** A casted ref to our gameobject, this hides our superclass _gameObj. */
    protected ChiyogamiObject _gameObj;

    /** The sceneId of the game. */
    protected int _sceneId;

    /** The room manager. */
    protected RoomManager _roomMgr;

    /** The room object where the game is taking place. */
    protected RoomObject _roomObj;

    /** Counts the number of ticks we've received in the current phase. */
    protected int _phaseCounter = 0;

    /** Set after the BATTLE phase. */
    protected boolean _bossWon;

    /** The script we've chosen for a battle. */
    protected Script _script;

    /** The currently displayed effects. */
    protected ArrayList<EffectData> _effects = new ArrayList<EffectData>();

    /** The boss object. */
    protected BossObject _bossObj;

    /** An interval that will end our battle phase at the appropriate time. */
    protected Interval _endBattle = new Interval(MsoyServer.omgr) {
        public void expired () {
            musicDidEnd();
        }
    };

    /** playerOid -> String[] of their states. */
    protected HashIntMap<String[]> _playerStates = new HashIntMap<String[]>();

    /** playerOid -> PlayerRec. */
    protected HashIntMap<PlayerRec> _playerPerfs = new HashIntMap<PlayerRec>();

    /** playerOid -> submitted tags. */
    protected HashIntMap<String> _playerTags = new HashIntMap<String>();
}
