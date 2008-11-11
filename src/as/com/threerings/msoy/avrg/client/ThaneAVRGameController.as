// 
// $Id$

package com.threerings.msoy.avrg.client {

import flash.utils.ByteArray;

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
import com.threerings.presents.dobj.MessageAdapter;
import com.threerings.presents.dobj.MessageEvent;
import com.threerings.presents.dobj.ObjectAccessError;
import com.threerings.presents.dobj.SetAdapter;
import com.threerings.presents.dobj.SubscriberAdapter;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.ResultAdapter;

import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.data.PlaceObject;

import com.whirled.bureau.client.GameAgentController;

import com.threerings.msoy.game.data.PlayerObject;

import com.threerings.msoy.room.data.MemberInfo;
import com.threerings.msoy.room.data.MobInfo;
import com.threerings.msoy.room.data.RoomCodes;
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
    implements GameAgentController
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

    /** @inheritDoc */
    // from AgentController
    public function shutdown () :void
    {
        // flush all scene bindings
        for each (var binding :SceneBinding in _bindings.values()) {
            removeBinding(binding.sceneId);
        }

        // release all player adapters
        for each (var player :PlayerBinding in _players.values()) {
            player.netAdapter.release();
        }
        _players.clear();

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

    /** @inheritDoc */
    // from AgentController
    public function agentReady () :void
    {
        log.info("Reporting agent ready", "gameObj", _gameObj.which());
        _gameObj.manager.invoke("agentReady");
    }

    /** @inheritDoc */
    // from AgentController
    public function agentFailed () :void
    {
        log.info("Reporting agent failed", "gameObj", _gameObj.which());
        _gameObj.manager.invoke("agentFailed");
    }

    /** @inheritDoc */
    // from AgentController
    public function getConnectListener () :Function
    {
        return backend.getConnectListener();
    }

    /** @inheritDoc */
    // from AgentController
    public function isConnected () :Boolean
    {
        return backend.isConnected();
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

    /** Retrieves the player object by the given player id (member id). Returns null if the player
     *  is not in the game or is not yet available. */
    public function getPlayer (playerId :int) :PlayerObject
    {
        var pbind :PlayerBinding = _players.get(playerId) as PlayerBinding;
        if (pbind != null) {
            return pbind.pobj;
        }
        return null;
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

    protected function entryAdded (event :EntryAddedEvent) :void
    {
        if (event.getName() == AVRGameObject.PLAYER_LOCS) {
            updatePlayer(PlayerLocation(event.getEntry()).playerId, "location added");

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
            updatePlayer(PlayerLocation(event.getEntry()).playerId, "location updated");

        } else if (event.getName() == AVRGameAgentObject.SCENES) {
            bindScene(event.getEntry() as SceneInfo);
        }
    }

    protected function entryRemoved (event :EntryRemovedEvent) :void
    {
        var pl :PlayerLocation;
        if (event.getName() == AVRGameObject.PLAYER_LOCS) {
            playerDidLeaveGame(PlayerLocation(event.getOldEntry()).playerId, "location removed");

        } else if (event.getName() == AVRGameAgentObject.SCENES) {
            removeBinding((event.getOldEntry() as SceneInfo).sceneId);

        } else if (event.getName() == PlaceObject.OCCUPANT_INFO) {
            var occInfo :OccupantInfo = OccupantInfo(event.getOldEntry());
            var playerObj :PlayerObject = PlayerObject(_playerSubs.getObj(occInfo.bodyOid));
            if (playerObj == null) {
                // easy come, easy go
                log.info("Player left game before subscription completed", "occInfo", occInfo);
                return;
            }
            playerDidLeaveGame(playerObj.getMemberId(), "game occupant removed");
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
            return;
        }

        log.info("Got room", "binding", binding, "roomOid", roomObj.getOid());

        binding.room = roomObj;
        binding.avatarAdapter = _backend.createAvatarAdapter(roomObj);
        binding.avatarAdapter.setTargetId(binding.sceneId);

        binding.roomChangeListener = new SetAdapter(
            function (event :EntryAddedEvent) :void {
                roomEntryAdded(binding, event);
            },
            function (event :EntryUpdatedEvent) :void {
                roomEntryUpdated(binding, event);
            },
            function (event :EntryRemovedEvent) :void {
                roomEntryRemoved(binding, event);
            });
        binding.room.addListener(binding.roomChangeListener);

        binding.roomMessageListener = new MessageAdapter(
            function (event :MessageEvent) :void {
                if (event.getName() == RoomCodes.SPRITE_SIGNAL) {
                    var args :Array = event.getArgs();
                    _backend.signalReceived(
                        binding.sceneId, args[0] as String, args[1] as ByteArray);
                }
            });
        binding.room.addListener(binding.roomMessageListener);

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
        if (binding.subscriber != null) {
            binding.subscriber.unsubscribe(binding.window.getDObjectManager());
            binding.subscriber = null;
        }

        if (binding.room != null) {
            binding.room.removeListener(binding.roomChangeListener);
            binding.room.removeListener(binding.roomMessageListener);
            binding.roomChangeListener = null;
            binding.roomMessageListener = null;
            binding.avatarAdapter.release();
            binding.avatarAdapter = null;
            binding.room = null;
        }

        if (binding.propsSubscriber != null) {
            binding.propsSubscriber.unsubscribe(binding.window.getDObjectManager());
            binding.propsSubscriber = null;
        }

        if (binding.roomProps != null) {
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

        // If the player has already left the game, fuhgedabbadit
        if (_gameObj.occupantInfo.get(obj.getOid()) == null) {
            log.info("Subscription completed after player left game", "playerId", playerId);
            return;
        }

        // Set up the player binding
        var playerBinding :PlayerBinding = new PlayerBinding(obj);
        playerBinding.netAdapter = _backend.createPlayerNetAdapter(obj);
        _players.put(playerId, playerBinding);

        // Dispatch the joining of the game and entering of the room if possible
        updatePlayer(playerId, "player object available");
    }

    protected function resolveMobInfo (entry :DSet_Entry ) :MobInfo
    {
        var mobInfo :MobInfo = null;
        mobInfo = entry as MobInfo;
        if (mobInfo != null) {
            if (mobInfo.getGameId() != getGameId()) {
                mobInfo = null;
            }
        }

        return mobInfo;
    }

    protected function roomEntryAdded (binding :SceneBinding, evt :EntryAddedEvent) :void
    {
        if (evt.getName() == PlaceObject.OCCUPANT_INFO) {
            var mobInfo :MobInfo = resolveMobInfo(evt.getEntry());
            if (mobInfo != null) {
                binding.mobs.put(mobInfo.getIdent(), mobInfo);
                _backend.mobSpawned(binding.sceneId, mobInfo.getIdent());

            } else if (evt.getEntry() is MemberInfo) {
                updatePlayer(MemberInfo(evt.getEntry()).getMemberId(), "room occupant added");
            }
        }
    }

    protected function roomEntryUpdated (binding :SceneBinding, evt :EntryUpdatedEvent) :void
    {
        if (evt.getName() == PlaceObject.OCCUPANT_INFO) {
            var mobInfo :MobInfo = resolveMobInfo(evt.getEntry());
            if (mobInfo != null) {
                if (MobInfo(evt.getOldEntry()).getIdent() != mobInfo.getIdent()) {
                    log.warning("Mob changed idents", "old", evt.getOldEntry(), "new", mobInfo);
                }
                binding.mobs.put(mobInfo.getIdent(), mobInfo);
                _backend.mobChanged(binding.sceneId, mobInfo.getIdent());
            }
        }
    }

    protected function roomEntryRemoved (binding :SceneBinding, evt :EntryRemovedEvent) :void
    {
        if (evt.getName() == PlaceObject.OCCUPANT_INFO) {
            var mobInfo :MobInfo = resolveMobInfo(evt.getOldEntry());
            if (mobInfo != null) {
                if (!binding.mobs.remove(mobInfo.getIdent())) {
                    log.warning(
                        "Removing mob not found in binding", "binding", binding, "mobId",
                        mobInfo.getIdent());
                }
                _backend.mobRemoved(binding.sceneId, mobInfo.getIdent());

            } else if (evt.getOldEntry() is MemberInfo) {
                updatePlayer(
                    MemberInfo(evt.getOldEntry()).getMemberId(), "room occupant removed", true);
            }
        }
    }

    /**
     * Takes care of telling the backend that a player has joined the game, left a room or entered
     * a room or any combination of the above, guaranteeing the correct event ordering and
     * preconditions.
     *
     * <p>In order to join the game, the player must:<ul>
     * <li>Not have previously joined the game</li>
     * <li>Have a player binding (which by definition has a player object)</li></ul>
     * If all conditions are met, the backend is informed via <code>playerJoinedGame</code></p>
     *
     * <p>In order to enter a room, the player must:<ul>
     * <li>Have previously joined the game in this update or a previous one</li>
     * <li>Have most recently left a room in this update or a previous one</li>
     * <li>Have a <code>PlayerLocation</code> with a non-zero <code>sceneId</code></li>
     * <li>Not have a pending deactivation</li>
     * <li>Be an occupant of the room (the server guarantees that the room is non-null)</li></ul>
     * If all conditions are met, the backend is informed via <code>playerEnteredRoom</code></p>
     *
     * <p>In order to leave a room, the player must:<ul>
     * <li>Have previously joined the game in this update or a previous one</li>
     * <li>Have previously entered a room</li>
     * <li>Have undergone some other transition that causes the leaving paramter to be true</li>
     * </ul>If all conditions are met, the backend is informed via <code>playerLeftRoom</code></p>
     */
    protected function updatePlayer (playerId :int, why :String, leaving :Boolean = false) :void
    {
        log.debug("Updating player", "why", why, "playerId", playerId);

        // Check the binding exists
        var pbind :PlayerBinding = PlayerBinding(_players.get(playerId));
        if (pbind == null) {
            log.debug("Skipping player update: no binding", "playerId", playerId);
            return;
        }

        // Join the game if not already. 
        if (!pbind.joined) {
            _backend.playerJoinedGame(playerId);
            pbind.joined = true;
        }

        // Calculate the new scene id
        var newSceneId :int = 0;
        var ploc :PlayerLocation = PlayerLocation(_gameObj.playerLocs.get(playerId));
        if (ploc != null && !leaving) {
            newSceneId = ploc.sceneId;
        }

        // Don't dispatch entry to the same room again
        if (newSceneId == pbind.currentSceneId) {
            log.debug(
                "Skipping room transition: scenes equal", "playerId", playerId,
                "sceneId", newSceneId);
            return;
        }

        // Leave the last room entered
        if (pbind.currentSceneId != 0) {
            _backend.playerLeftRoom(playerId, pbind.currentSceneId);
            pbind.currentSceneId = 0;
        }

        // No need to enter scene zero, wait for the next update if any
        if (newSceneId == 0) {
            log.debug(
                "Skipping room entry: new scene is zero", "playerId", playerId);
            return;
        }

        // If the player was deactivated by our agent, don't enter any new rooms
        if (pbind.deactivated) {
            log.debug(
                "Skipping room entry: player deactivated", "playerId", playerId);
            return;
        }

        // Check the player is an occupant (Dion's bug)
        if (SceneBinding(_bindings.get(newSceneId)).room.getMemberInfo(playerId) == null) {
            log.debug(
                "Skipping room entry: player not in room", "playerId", playerId);
            return;
        }

        // Enter the new room
        _backend.playerEnteredRoom(playerId, newSceneId);
        pbind.currentSceneId = newSceneId;
    }

    /**
     * Finalizes the withdrawal of a player from the game, ensuring the backend is informed of
     * any pending room exits as well as the player quitting. 
     */
    protected function playerDidLeaveGame (playerId :int, why :String) :void
    {
        log.debug("Player leaving", "why", why, "playerId", playerId);

        var pbind :PlayerBinding = PlayerBinding(_players.get(playerId));

        // Check the player hasn't already left
        if (pbind == null) {
            return;
        }

        // Make sure we dispatch any pending room exits
        if (pbind.joined && pbind.currentSceneId != 0) {
            _backend.playerLeftRoom(playerId, pbind.currentSceneId);
            pbind.currentSceneId = 0;
        }

        // Now dispatch the game leaving event
        if (pbind.joined) {
            _backend.playerLeftGame(playerId);
            pbind.joined = false;
        }

        // Tear down the binding
        _players.remove(playerId);
        _playerSubs.unsubscribe(pbind.pobj.getOid());
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
import com.threerings.presents.dobj.MessageListener;

import com.threerings.presents.util.SafeSubscriber;

import com.threerings.msoy.game.data.PlayerObject;

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
    public var roomChangeListener :ChangeListener;
    public var roomMessageListener :MessageListener;
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
    public function PlayerBinding (pobj :PlayerObject)
    {
        this.pobj = pobj;
    }

    /** The player object, never null. */
    public var pobj :PlayerObject;
    
    /** For dispatching the property changes. */
    public var netAdapter :BackendNetAdapter;

    /** True if we've called <code>playerJoinedGame</code>.
     * @see com.threerings.msoy.avrg.client.ThaneAVRGameBackend#playerJoinedGame() */
    public var joined :Boolean;

    /** The last scene we passed to <code>playerEnteredRoom</code>, or zero if we've called
     * <code>playerLeftRoom</code> instead.
     * @see com.threerings.msoy.avrg.client.ThaneAVRGameBackend#playerEnteredRoom()
     * @see com.threerings.msoy.avrg.client.ThaneAVRGameBackend#playerLeftRoom() */
    public var currentSceneId :int;

    /** True if our backend has called <code>deactivateGame()</code>.
     * @see com.threerings.msoy.avrg.client.ThaneAVRGameController.deactivateGame() */
    public var deactivated :Boolean;
}
