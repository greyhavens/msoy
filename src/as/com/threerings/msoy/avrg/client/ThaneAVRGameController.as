// 
// $Id$

package com.threerings.msoy.avrg.client {

import com.threerings.util.HashMap;
import com.threerings.util.Iterator;
import com.threerings.util.Log;
import com.threerings.util.ResultAdapter;

import com.threerings.presents.util.SafeObjectManager;
import com.threerings.presents.util.SafeSubscriber;

import com.threerings.presents.dobj.DSet_Entry;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.ObjectAccessError;
import com.threerings.presents.dobj.SetAdapter;
import com.threerings.presents.dobj.SubscriberAdapter;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.ResultAdapter;

import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.msoy.game.data.PlayerObject;

import com.threerings.msoy.room.data.MobInfo;
import com.threerings.msoy.room.data.RoomObject;
import com.threerings.msoy.room.data.RoomPropertiesEntry;
import com.threerings.msoy.room.data.RoomPropertiesObject;

import com.threerings.msoy.bureau.client.AVRGameAgent;
import com.threerings.msoy.bureau.client.ThaneWorldService;
import com.threerings.msoy.bureau.client.Window;
import com.threerings.msoy.bureau.util.MsoyBureauContext;

import com.threerings.msoy.avrg.data.AVRGameAgentObject;
import com.threerings.msoy.avrg.data.AVRGameObject;
import com.threerings.msoy.avrg.data.PlayerLocation;
import com.threerings.msoy.avrg.data.SceneInfo;

public class ThaneAVRGameController
{
    public var log :Log = Log.getLog(this);

    /** Initializes the game controller. */
    public function init (
        ctx :MsoyBureauContext, gameObj :AVRGameObject, gameAgent :AVRGameAgent,
        gameAgentObj :AVRGameAgentObject) :void
    {
        _ctx = ctx;
        _gameObj = gameObj;
        _gameAgent = gameAgent;
        _gameAgentObj = gameAgentObj;

        // create the backend
        _backend = new ThaneAVRGameBackend(ctx, gameObj, this);

        _playerSubs = new SafeObjectManager(
            _ctx.getClient().getDObjectManager(), log, gotPlayerObject);

        // set up existing player locations
        if (_gameObj.playerLocs.size () > 0) {
            // This is an unexpected condition, but... if the game agent ever reloads the user code,
            // it may also recreate the controller?
            log.warning("Game object player locations already populated");
        }

        if (_gameAgentObj.scenes.size() > 0) {
            log.warning("Agent object scenes already populated");
            var iter :Iterator = _gameAgentObj.scenes.iterator();
            while (iter.hasNext()) {
                bindScene(iter.next() as SceneInfo);
            }
        }

        // listen for player location changes
        _gameObj.addListener(_setAdapter);
        _gameAgentObj.addListener(_setAdapter);
    }

    /** Shuts down the AVRG controller. */
    public function shutdown () :void
    {
        // flush all scene bindings
        var bindings :Array = _bindings.values();
        _bindings.clear();
        for each (var binding :SceneBinding in bindings) {
            removeBinding(binding.sceneId);
        }

        // release all player adapters
        bindings = _players.values();
        _players.clear();
        for each (var player :PlayerBinding in bindings) {
            player.netAdapter.release();
        }

        // shutdown the backend
        backend.shutdown();

        // remove listeners
        _gameObj.removeListener(_setAdapter);
        _gameAgentObj.removeListener(_setAdapter);

        // unsubscribe from all players
        _playerSubs.unsubscribeAll();

        // null our init references
        _ctx = null;
        _gameObj = null;
        _gameAgent = null;
        _gameAgentObj = null;
    }

    /** Accesses the game backend. */
    public function get backend () :ThaneAVRGameBackend
    {
        return _backend;
    }

    /** Inform the server that the agent is ready. */
    public function agentReady () :void
    {
        log.info("Reporting agent ready", "gameObj", _gameObj.which());
        _gameObj.manager.invoke("agentReady");
    }

    /** Retrieves the room for the given room id.
     *  @throws UserError if the game is not being played in this room or if the room is not yet
     *  available. */
    public function getRoom (roomId :int) :RoomObject
    {
        var binding :SceneBinding = _bindings.get(roomId);
        if (binding == null || binding.room == null) {
            throw new UserError("Room not in game [roomId=" + roomId + "]");
        }
        return binding.room;
    }

    /** Retrieves the room properties for the given room id.
     *  @throws UserError if the game is not being played in this room or if the room is not yet
     *  available. */
    public function getRoomProps (roomId :int) :RoomPropertiesObject
    {
        var binding :SceneBinding = _bindings.get(roomId);
        if (binding == null || binding.roomProps == null) {
            throw new UserError("Room not in game [roomId=" + roomId + "]");
        }
        return binding.roomProps;
    }

    /** Retrieves the client session instance for the given room id.
     *  @throws UserError if the game is not being played in this room or if the room is not yet
     *  available.
     *  @see WindowDirector */
    public function getRoomClient (roomId :int) :Client
    {
        var binding :SceneBinding = _bindings.get(roomId);
        if (binding == null || binding.window == null) {
            throw new UserError("Room not in game [roomId=" + roomId + "]");
        }
        return binding.window.getClient();
    }

    /** Retrieves the player object by the given player id (member id).
     *  @throws UserError if the player is not in the game or is not yet available. */
    public function getPlayerForUser (playerId :int) :PlayerObject
    {
        var playerObj :PlayerObject = getPlayer(playerId);
        if (playerObj == null) {
            throw new UserError("Player not in game [playerId=" + playerId + "]");
        }
        return playerObj;
    }

    /**
     * Removes the given player from the game.
     * @throws UserError if the player is not currently inthe game.
     */
    public function deactivateGame (playerId :int) :void
    {
        var binding :PlayerBinding = _players.get(playerId);
        if (binding == null) {
            throw new UserError("Deactivating player not in game [playerId=" + playerId + "]");
        }
        binding.deactivated = true;
        _transactions.addClient(_ctx.getClient());
        _gameAgentObj.agentService.leaveGame(_ctx.getClient(), playerId);
    }

    /**
     * Returns the id of the game being played.
     */
    public function getGameId () :int
    {
        return _gameAgentObj.gameId;
    }

    /**
     * Returns the array of mob ids in a room.
     * @throws UserError if the game is not being played in this room or if the room is not yet
     * available.
     */
    public function getMobIds (roomId :int) :Array
    {
        var binding :SceneBinding = _bindings.get(roomId);
        if (binding != null) {
            return binding.mobs.keys();
        }
        throw new UserError("Room not in game [roomId=" + roomId + "]");
    }

    /**
     * Retrieves the info structure for the mob in the given room and with the given id. Returns
     * null if the mob of the given id is not in the room.
     * @throws UserError if the game is not being played in this room or if the room is not yet
     * available.
     */
    public function getMobInfo (roomId :int, mobId :String) :MobInfo
    {
        var binding :SceneBinding = _bindings.get(roomId);
        if (binding == null) {
            throw new UserError("Room not in game [roomId=" + roomId + "]");
            return null;
        }
        return binding.mobs.get(mobId) as MobInfo;
    }

    /**
     * Retrieves the current set of transactions so that clients may be added. The backend should
     * always add any client that is about to be used in a service method call. The
     * <code>Transactions</code> class will start a transaction if necessary.
     */
    public function getTransactions () :Transactions
    {
        return _transactions;
    }

    /**
     * Outputs a message to the trace function within the user code.
     */
    public function outputToUserCode (msg :String, err :Error = null) :void
    {
        if (_gameAgent != null) {
            _gameAgent.outputToUserCode(msg, err);

        } else {
            log.warning("User error occurred after shutdown");
        }
    }

    /** Retrieves the player object by the given player id (member id). Returns null if the player
     *  is not in the game or is not yet available. */
    protected function getPlayer (playerId :int) :PlayerObject
    {
        var playerBinding :PlayerBinding = _players.get(playerId) as PlayerBinding;
        var playerObj :PlayerObject;
        if (playerBinding != null) {
            playerObj = _playerSubs.getObj(playerBinding.oid) as PlayerObject
        }
        return playerObj;
    }

    protected function entryAdded (event :EntryAddedEvent) :void
    {
        if (event.getName() == AVRGameObject.PLAYER_LOCS) {
            var pl :PlayerLocation = event.getEntry() as PlayerLocation;
            if (getPlayer(pl.playerId) == null) {
                // player object subscription is not yet complete, wait for that callback
                return;
            }
            playerDidJoinGame(pl);

        } else if (event.getName() == AVRGameAgentObject.SCENES) {
            bindScene(event.getEntry() as SceneInfo);

        } else if (event.getName() == PlaceObject.OCCUPANT_INFO) {
            var occInfo :OccupantInfo = event.getEntry() as OccupantInfo;
            _playerSubs.subscribe(occInfo.bodyOid);
        }
    }

    protected function entryUpdated (event :EntryUpdatedEvent) :void
    {
        if (event.getName() == AVRGameObject.PLAYER_LOCS) {
            var pl :PlayerLocation = event.getEntry() as PlayerLocation;
            if (getPlayer(pl.playerId) == null) {
                // if subscription never completed, don't dispatch a 'left' event, and wait
                // for the subscription callback for the 'enter' event.
                return;
            }
            var oldPl :PlayerLocation = event.getOldEntry() as PlayerLocation;
            var binding :PlayerBinding = PlayerBinding(_players.get(pl.playerId));
            if (binding == null) {
                log.warning(
                    "playerLocations entry updated prior to addition", "pl", pl, "oldPl", oldPl);
                return;
            }
            if (!binding.suppressRoomTransitions) {
                _backend.playerLeftRoom(oldPl.playerId, oldPl.sceneId);
                if (binding.deactivated) {
                    binding.suppressRoomTransitions = true;
                }
            }
            if (!binding.suppressRoomTransitions) {
                _backend.playerEnteredRoom(pl.playerId, pl.sceneId);
            }
        } else if (event.getName() == AVRGameAgentObject.SCENES) {
            bindScene(event.getEntry() as SceneInfo);
        }
    }

    protected function entryRemoved (event :EntryRemovedEvent) :void
    {
        var pl :PlayerLocation;
        if (event.getName() == AVRGameObject.PLAYER_LOCS) {
            pl = event.getOldEntry() as PlayerLocation;
            if (getPlayer(pl.playerId) == null) {
                // if subscription never completed, the game wasn't told the player half-way
                // joined, so don't now confuse it by mentioning they're leaving
                return;
            }

            if (!PlayerBinding(_players.get(pl.playerId)).suppressRoomTransitions) {
                _backend.playerLeftRoom(pl.playerId, pl.sceneId);
            }
        } else if (event.getName() == AVRGameAgentObject.SCENES) {
            removeBinding((event.getOldEntry() as SceneInfo).sceneId);

        } else if (event.getName() == PlaceObject.OCCUPANT_INFO) {
            var occInfo :OccupantInfo = event.getOldEntry() as OccupantInfo;
            var playerObj :PlayerObject = _playerSubs.getObj(occInfo.bodyOid) as PlayerObject;
            if (playerObj == null) {
                // TODO: downgrade to info if this is happening frequently
                log.warning("Player left game before subscription completed", "occInfo", occInfo);

            } else {
                var doRoomTransition :Boolean = true;
                var playerBinding :PlayerBinding;
                playerBinding = _players.get(playerObj.getMemberId()) as PlayerBinding;
                if (playerBinding == null) {
                    // This is very weird since we create the binding when the PlayerObject is
                    // available
                    log.warning(
                        "Player leaving game has a subscription but no binding", "playerObj",
                        playerObj.which());

                } else {
                    doRoomTransition = !playerBinding.suppressRoomTransitions;
                    playerBinding.netAdapter.release();
                }

                // If the player is still in our locations, remove from room
                pl = _gameObj.playerLocs.get(playerObj.getMemberId()) as PlayerLocation;
                if (pl != null) {
                    if (doRoomTransition) {
                        _backend.playerLeftRoom(pl.playerId, pl.sceneId);
                    }
                    _backend.playerLeftGame(pl.playerId);
                }

                _players.remove(playerObj.getMemberId());
            }

            _playerSubs.unsubscribe(occInfo.bodyOid);
        }
    }

    /** Sets up a new binding for the given scene, removing the old one if it exists. */
    protected function bindScene (scene :SceneInfo) :void
    {
        // Get rid of the old SceneBinding
        var binding :SceneBinding = _bindings.get(scene.sceneId);
        if (binding != null) {
            // this shouldn't happen since scenes should be explicitly removed well before a host 
            // change
            log.warning("Unexpected host change", "binding", binding);
            removeBinding(binding.sceneId);
        }

        // Create a binding and add it to the map
        binding = new SceneBinding();
        binding.sceneId = scene.sceneId;
        _bindings.put(scene.sceneId, binding);

        log.debug("Opening window", "scene", scene);
        var resultListener :com.threerings.util.ResultAdapter = 
            new com.threerings.util.ResultAdapter(
                function (wnd :Window) :void {
                    gotWindow(binding, wnd);
                },
                function (cause :Error) :void {
                    log.warning("Failed to open window", "scene", scene, "cause", cause);
                });

        _ctx.getWindowDirector().openWindow(scene.hostname, scene.port, resultListener);
    }

    protected function gotWindow (binding :SceneBinding, window :Window) :void
    {
        // close the window immediately if this binding has been removed
        if (wasRemoved(binding)) {
            log.warning("Window no longer needed", "binding", binding, "window", window);
            _ctx.getWindowDirector().closeWindow(window);
            return;
        }

        log.debug("Got window", "binding", binding, "window", window);

        // set the window so it can be closed later
        binding.window = window;

        // locate the room oid
        var resultListener :com.threerings.presents.client.ResultAdapter = 
            new com.threerings.presents.client.ResultAdapter(
                function (cause :String) :void {
                    log.warning(
                        "Failed to get room oid", "binding", binding, "window", window, "cause",
                        cause);
                },
                function (roomOid :int) :void {
                    gotRoomOid(binding, roomOid);
                });

        var thaneSvc :ThaneWorldService = 
            window.requireService(ThaneWorldService) as ThaneWorldService;

        thaneSvc.locateRoom(window.getClient(), binding.sceneId, resultListener);
    }

    protected function gotRoomOid (binding :SceneBinding, oid :int) :void
    {
        // if this player has been removed, forget it
        if (wasRemoved(binding)) {
            log.warning("Room oid no longer needed", "binding", binding, "roomOid", oid);
            return;
        }
        
        log.debug("Got room id", "binding", binding, "roomOid", oid);

        // subscribe to the room object
        var subscriber :SubscriberAdapter = new SubscriberAdapter(
            function (obj :RoomObject) :void {
                gotRoomObject(binding, obj);
            },
            function (oid :int, cause :ObjectAccessError) :void {
                log.warning(
                    "Failed to subscribe to room", "binding", binding, "roomOid", oid, "cause",
                    cause);
            }
        );

        binding.subscriber = new SafeSubscriber(oid, subscriber);
        binding.subscriber.subscribe(binding.window.getDObjectManager());
    }

    protected function gotRoomObject (binding :SceneBinding, roomObj :RoomObject) :void
    {
        // if this scene has been removed, unsubscribe right away
        if (wasRemoved(binding)) {
            log.warning("Room no longer needed", "binding", binding, "roomOid", roomObj.getOid());
            binding.subscriber.unsubscribe(binding.window.getDObjectManager());
            return;
        }

        log.info("Got room", "binding", binding, "roomOid", roomObj.getOid());

        binding.room = roomObj;
        binding.avatarAdapter = _backend.createAvatarAdapter(roomObj);
        binding.avatarAdapter.setTargetId(binding.sceneId);

        binding.roomListener = new SetAdapter(
            function (event :EntryAddedEvent) :void {
                roomEntryAdded(binding, event);
            },
            function (event :EntryUpdatedEvent) :void {
                roomEntryUpdated(binding, event);
            },
            function (event :EntryRemovedEvent) :void {
                roomEntryRemoved(binding, event);
            });
        binding.room.addListener(binding.roomListener);

        var occIter :Iterator = binding.room.occupantInfo.iterator();
        while (occIter.hasNext()) {
            var mob :MobInfo = occIter.next() as MobInfo;
            if (mob != null && mob.getGameId() == getGameId()) {
                binding.mobs.put(mob.getIdent(), mob);
            }
        }

        var entry :RoomPropertiesEntry;
        entry = binding.room.propertySpaces.get(_gameAgentObj.gameId) as RoomPropertiesEntry;
        if (entry != null) {
            gotRoomPropsOid(binding, entry.propsOid);
            return;
        }

        var adapter :SetAdapter = new SetAdapter(
            function (event :EntryAddedEvent) :void {
                if (event.getName() == RoomObject.PROPERTY_SPACES) {
                    entry = event.getEntry() as RoomPropertiesEntry;
                    if (entry.ownerId == _gameAgentObj.gameId) {
                        binding.room.removeListener(adapter);
                        gotRoomPropsOid(binding, entry.propsOid);
                    }
                }
            }, null, null);

        // TODO: does this need to timeout in case the room manager has caught fire?
        binding.room.addListener(adapter);
    }

    protected function gotRoomPropsOid (binding :SceneBinding, propsOid :int) :void
    {
        if (wasRemoved(binding)) {
            log.warning(
                "Room props oid no longer needed", "binding", binding, "propsOid", propsOid);
            return;
        }

        log.debug("Got room props id", "binding", binding, "propsOid", propsOid);

        // subscribe to the properties object
        var subscriber :SubscriberAdapter = new SubscriberAdapter(
            function (obj :RoomPropertiesObject) :void {
                gotRoomPropertiesObject(binding, obj);
            },
            function (oid :int, cause :ObjectAccessError) :void {
                log.warning(
                    "Failed to subscribe to room props", "binding", binding, "propsOid", propsOid,
                    "cause", cause);
            }
        );

        binding.propsSubscriber = new SafeSubscriber(propsOid, subscriber);
        binding.propsSubscriber.subscribe(binding.window.getDObjectManager());
    }

    protected function gotRoomPropertiesObject (
        binding :SceneBinding, propsObj :RoomPropertiesObject) :void
    {
        // if this scene has been removed, unsubscribe right away
        if (wasRemoved(binding)) {
            log.warning(
                "Room props no longer needed", "binding", binding, "propsOid", propsObj.getOid());
            binding.propsSubscriber.unsubscribe(binding.window.getDObjectManager());
            return;
        }

        log.info("Got room props", "binding", binding, "propsOid", propsObj.getOid());

        binding.roomProps = propsObj;

        _gameAgentObj.agentService.roomSubscriptionComplete(_ctx.getClient(), binding.sceneId);
    }

    protected function wasRemoved (binding :SceneBinding) :Boolean
    {
        return binding != _bindings.get(binding.sceneId);
    }

    /** Removes the binding of the given player. */
    protected function removeBinding (sceneId :int) :void
    {
        var binding :SceneBinding = _bindings.remove(sceneId) as SceneBinding;
        if (binding == null) {
            log.warning("SceneBinding not found to remove", "sceneId", sceneId);
            return;
        }

        log.debug("Removing binding", "binding", binding);

        _backend.roomUnloaded(sceneId);

        // Release all resources
        if (binding.room != null) {
            binding.room.removeListener(binding.roomListener);
            binding.roomListener = null;
            binding.avatarAdapter.release();
            binding.avatarAdapter = null;
            binding.subscriber.unsubscribe(binding.window.getDObjectManager());
            binding.subscriber = null;
            binding.room = null;
        }

        if (binding.roomProps != null) {
            binding.propsSubscriber.unsubscribe(binding.window.getDObjectManager());
            binding.propsSubscriber = null;
            binding.roomProps = null;
        }

        if (binding.window != null) {
            _ctx.getWindowDirector().closeWindow(binding.window);
            binding.window = null;
        }
    }

    protected function gotPlayerObject (obj :PlayerObject) :void
    {
        var playerId :int = obj.getMemberId();

        var playerBinding :PlayerBinding = new PlayerBinding();
        playerBinding.oid = obj.getOid();
        playerBinding.netAdapter = _backend.createPlayerNetAdapter(obj);
        _players.put(playerId, playerBinding);

        var pl :PlayerLocation = _gameObj.playerLocs.get(playerId) as PlayerLocation;
        if (pl == null) {
            // player location not yet updated, wait for that callback
            return;
        }
        playerDidJoinGame(pl);
    }

    protected function resolveMobInfo (name :String, entry :DSet_Entry ) :MobInfo
    {
        var mobInfo :MobInfo = null;
        if (name == PlaceObject.OCCUPANT_INFO) {
            mobInfo = entry as MobInfo;
            if (mobInfo != null) {
                if (mobInfo.getGameId() != getGameId()) {
                    mobInfo = null;
                }
            }
        }

        return mobInfo;
    }

    protected function roomEntryAdded (binding :SceneBinding, evt :EntryAddedEvent) :void
    {
        var mobInfo :MobInfo = resolveMobInfo(evt.getName(), evt.getEntry());
        if (mobInfo != null) {
            binding.mobs.put(mobInfo.getIdent(), mobInfo);
            _backend.mobSpawned(binding.sceneId, mobInfo.getIdent());
        }
    }

    protected function roomEntryUpdated (binding :SceneBinding, evt :EntryUpdatedEvent) :void
    {
        var mobInfo :MobInfo = resolveMobInfo(evt.getName(), evt.getEntry());
        if (mobInfo != null) {
            if (MobInfo(evt.getOldEntry()).getIdent() != mobInfo.getIdent()) {
                log.warning("Mob changed idents", "old", evt.getOldEntry(), "new", mobInfo);
            }
            binding.mobs.put(mobInfo.getIdent(), mobInfo);
            _backend.mobChanged(binding.sceneId, mobInfo.getIdent());
        }
    }

    protected function roomEntryRemoved (binding :SceneBinding, evt :EntryRemovedEvent) :void
    {
        var mobInfo :MobInfo = resolveMobInfo(evt.getName(), evt.getOldEntry());
        if (mobInfo != null) {
            if (!binding.mobs.remove(mobInfo.getIdent())) {
                log.warning(
                    "Removing mob not found in binding", "binding", binding, "mobId",
                    mobInfo.getIdent());
            }
            _backend.mobRemoved(binding.sceneId, mobInfo.getIdent());
        }
    }

    protected function playerDidJoinGame (pl :PlayerLocation) :void
    {
        _backend.playerJoinedGame(pl.playerId);
        _backend.playerEnteredRoom(pl.playerId, pl.sceneId);
    }

    protected var _ctx :MsoyBureauContext;
    protected var _backend :ThaneAVRGameBackend;
    protected var _gameAgent :AVRGameAgent;
    protected var _gameObj :AVRGameObject;
    protected var _gameAgentObj :AVRGameAgentObject;
    protected var _bindings :HashMap = new HashMap();
    protected var _setAdapter :SetAdapter = new SetAdapter(entryAdded, entryUpdated, entryRemoved);
    protected var _playerSubs :SafeObjectManager;
    protected var _players :HashMap = new HashMap();
    protected var _transactions :Transactions = new Transactions();
}

}

import com.threerings.util.HashMap;
import com.threerings.util.StringUtil;

import com.threerings.presents.dobj.ChangeListener;

import com.threerings.presents.util.SafeSubscriber;

import com.threerings.msoy.room.data.RoomObject;
import com.threerings.msoy.room.data.RoomPropertiesObject;

import com.threerings.msoy.bureau.client.Window;

import com.threerings.msoy.avrg.client.BackendAvatarAdapter;
import com.threerings.msoy.avrg.client.BackendNetAdapter;

/** Binds a scene id to its window, room and players. */
class SceneBinding
{
    public var sceneId :int;
    public var window :Window;
    public var subscriber :SafeSubscriber;
    public var room :RoomObject;
    public var roomListener :ChangeListener;
    public var propsSubscriber :SafeSubscriber;
    public var roomProps :RoomPropertiesObject;
    public var avatarAdapter :BackendAvatarAdapter;
    public var mobs :HashMap = new HashMap();

    // from Object
    public function toString () :String
    {
        return StringUtil.simpleToString(this);
    }
}

/** Holds the transient information about a player so that we can clean up after ourselves and
 * lookup the object id of a member. */
class PlayerBinding
{
    public var oid :int;
    public var netAdapter :BackendNetAdapter;
    public var deactivated :Boolean;
    public var suppressRoomTransitions :Boolean;
}
