// 
// $Id$

package com.threerings.msoy.avrg.client {

import flash.events.TimerEvent;
import flash.utils.Timer;
import flash.utils.getTimer;

import com.threerings.msoy.avrg.data.AVRGameAgentObject;
import com.threerings.msoy.avrg.data.AVRGameObject;
import com.threerings.msoy.avrg.data.PlayerLocation;
import com.threerings.msoy.avrg.data.SceneInfo;
import com.threerings.msoy.bureau.client.ThaneWorldService;
import com.threerings.msoy.bureau.client.Window;
import com.threerings.msoy.bureau.util.MsoyBureauContext;
import com.threerings.msoy.room.data.RoomObject;
import com.threerings.msoy.room.data.RoomPropertiesEntry;
import com.threerings.msoy.room.data.RoomPropertiesObject;
import com.threerings.presents.client.ResultAdapter;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.ObjectAccessError;
import com.threerings.presents.dobj.SetAdapter;
import com.threerings.presents.dobj.SubscriberAdapter;
import com.threerings.presents.util.SafeSubscriber;
import com.threerings.util.HashMap;
import com.threerings.util.Iterator;
import com.threerings.util.Log;

public class ThaneAVRGameController
{
    public var log :Log = Log.getLog(this);

    /** Initializes the game controller. */
    public function init (
        ctx :MsoyBureauContext, gameObj :AVRGameObject, gameAgentObj :AVRGameAgentObject) :void
    {
        _ctx = ctx;
        _gameObj = gameObj;
        _gameAgentObj = gameAgentObj;

        // create the backend
        _backend = new ThaneAVRGameBackend();

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
        var adapter :SetAdapter = new SetAdapter(entryAdded, entryUpdated, entryRemoved);
        _gameObj.addListener(adapter);
        _gameAgentObj.addListener(adapter);
    }

    /** Shuts down the AVRG controller. */
    public function shutdown () :void
    {
        // flush all player bindings
        var bindings :Array = _bindings.values();
        _bindings.clear();
        for each (var binding :SceneBinding in bindings) {
            removeBinding(binding.sceneId);
        }

        // shutdown the backend
        backend.shutdown();

        // null our init references
        _ctx = null;
        _gameObj = null;
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
        log.info("Reporting agent ready " + _gameObj.which() + ".");
        _gameObj.manager.invoke("agentReady");
    }

    protected function entryAdded (event :EntryAddedEvent) :void
    {
        if (event.getName() == AVRGameObject.PLAYER_LOCS) {
            updatePlayer(event.getEntry() as PlayerLocation, false);

        } else if (event.getName() == AVRGameAgentObject.SCENES) {
            bindScene(event.getEntry() as SceneInfo);
        }
    }

    protected function entryUpdated (event :EntryUpdatedEvent) :void
    {
        if (event.getName() == AVRGameObject.PLAYER_LOCS) {
            updatePlayer(event.getOldEntry() as PlayerLocation, true);
            updatePlayer(event.getEntry() as PlayerLocation, false);

        } else if (event.getName() == AVRGameAgentObject.SCENES) {
            bindScene(event.getEntry() as SceneInfo);
        }
    }

    protected function entryRemoved (event :EntryRemovedEvent) :void
    {
        if (event.getName() == AVRGameObject.PLAYER_LOCS) {
            updatePlayer(event.getOldEntry() as PlayerLocation, true);

        } else if (event.getName() == AVRGameAgentObject.SCENES) {
            removeBinding((event.getOldEntry() as SceneInfo).sceneId);
        }
    }

    protected function updatePlayer (loc :PlayerLocation, remove :Boolean) :void
    {
        var binding :SceneBinding = _bindings.get(loc.sceneId);
        if (binding == null) {
            log.warning("Player updated in unbound scene: [loc=" + loc + ", remove=" + remove);
        }
        else if (binding.room == null) {
            log.warning("Player updated in unsubscribed scene: [loc=" + loc + ", binding=" + 
                binding + ", remove=" + remove);
        } else {
            // TODO: notify backend
            log.debug("Player updated [loc=" + loc + ", remove=" + remove);
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
            log.warning("Unexpected host change: " + binding);
            removeBinding(binding.sceneId);
        }

        // Create a binding and add it to the map
        binding = new SceneBinding();
        binding.sceneId = scene.sceneId;
        _bindings.put(scene.sceneId, binding);

        var info :String = "scene=" + scene;

        // Open the window to the server hosting the player's scene
        var resultListener :ResultAdapter = new ResultAdapter(
            function (cause :String) :void {
                log.warning("Failed to open window [" + info + ", cause=\"" + cause + "\"]");
            },
            function (wnd :Window) :void {
                gotWindow(binding, wnd);
            }
        );

        log.debug("Opening window ["  + info + "]");
        _ctx.getWindowDirector().openWindow(scene.hostname, scene.port, resultListener);
    }

    protected function gotWindow (binding :SceneBinding, window :Window) :void
    {
        var info :String = "binding=" + binding + ", window=" + window;

        // close the window immediately if this binding has been removed
        if (wasRemoved(binding)) {
            log.warning("Window no longer needed [" + info + "]");
            _ctx.getWindowDirector().closeWindow(window);
            return;
        }

        log.debug("Got window [" + info + "]");

        // set the window so it can be closed later
        binding.window = window;

        // locate the room oid
        var resultListener :ResultAdapter = new ResultAdapter(
            function (cause :String) :void {
                log.warning("Failed to get room oid [" + info + ", cause=\"" + cause + "\"]");
            },
            function (roomOid :int) :void {
                gotRoomOid(binding, roomOid);
            }
        );

        var thaneSvc :ThaneWorldService = 
            window.requireService(ThaneWorldService) as ThaneWorldService;

        thaneSvc.locateRoom(window.getClient(), binding.sceneId, resultListener);
    }

    protected function gotRoomOid (binding :SceneBinding, oid :int) :void
    {
        var info :String = "binding=" + binding + ", roomOid=" + oid;

        // if this player has been removed, forget it
        if (wasRemoved(binding)) {
            log.warning("Room oid no longer needed [" + info + "]");
            return;
        }
        
        log.debug("Got room id ["  + info + "]");

        // subscribe to the room object
        var subscriber :SubscriberAdapter = new SubscriberAdapter(
            function (obj :RoomObject) :void {
                gotRoomObject(binding, obj);
            },
            function (oid :int, cause :ObjectAccessError) :void {
                log.warning("Failed to subscribe to room [" + info + ", cause=\"" + cause + "\"]");
            }
        );

        binding.subscriber = new SafeSubscriber(oid, subscriber);
        binding.subscriber.subscribe(binding.window.getDObjectManager());
    }

    protected function gotRoomObject (binding :SceneBinding, roomObj :RoomObject) :void
    {
        var info :String = "binding=" + binding + ", roomOid=" + roomObj.getOid();

        // if this scene has been removed, unsubscribe right away
        if (wasRemoved(binding)) {
            log.warning("Room no longer needed [" + info + "]");
            binding.subscriber.unsubscribe(binding.window.getDObjectManager());
            return;
        }

        log.info("Got room [" + info + "]");

        binding.room = roomObj;
        
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
        var info :String = "binding=" + binding + ", propsOid=" + propsOid;
        
        if (wasRemoved(binding)) {
            log.warning("Room props oid no longer needed [" + info + "]");
            return;
        }

        log.debug("Got room props id [" + info + "]");

        // subscribe to the properties object
        var subscriber :SubscriberAdapter = new SubscriberAdapter(
            function (obj :RoomPropertiesObject) :void {
                gotRoomPropertiesObject(binding, obj);
            },
            function (oid :int, cause :ObjectAccessError) :void {
                log.warning(
                    "Failed to subscribe to room props [" + info + ", cause=\"" + cause + "\"]");
            }
        );

        binding.propsSubscriber = new SafeSubscriber(propsOid, subscriber);
        binding.propsSubscriber.subscribe(binding.window.getDObjectManager());
    }

    protected function gotRoomPropertiesObject (
        binding :SceneBinding, propsObj :RoomPropertiesObject) :void
    {
        var info :String = "binding=" + binding + ", propsOid=" + propsObj.getOid();

        // if this scene has been removed, unsubscribe right away
        if (wasRemoved(binding)) {
            log.warning("Room props no longer needed [" + info + "]");
            binding.propsSubscriber.unsubscribe(binding.window.getDObjectManager());
            return;
        }

        log.info("Got room props [" + info + "]");

        binding.roomProps = propsObj;
        _gameObj.avrgService.roomSubscriptionComplete(_ctx.getClient(), binding.sceneId);
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
            log.warning("SceneBinding not found to remove: " + sceneId);
            return;
        }

        log.debug("Removing binding: " + binding);

        // Release all resources
        if (binding.room != null) {
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

    protected var _ctx :MsoyBureauContext;
    protected var _backend :ThaneAVRGameBackend;
    protected var _gameObj :AVRGameObject;
    protected var _gameAgentObj :AVRGameAgentObject;
    protected var _bindings :HashMap = new HashMap();
}

}

import com.threerings.msoy.bureau.client.Window;
import com.threerings.msoy.room.data.RoomObject;
import com.threerings.msoy.room.data.RoomPropertiesObject;
import com.threerings.util.StringUtil;
import com.threerings.presents.util.SafeSubscriber;

/** Binds a scene id to its window, room and players. */
class SceneBinding
{
    public var sceneId :int;
    public var window :Window;
    public var subscriber :SafeSubscriber;
    public var room :RoomObject;
    public var propsSubscriber :SafeSubscriber;
    public var roomProps :RoomPropertiesObject;

    // from Object
    public function toString () :String
    {
        return StringUtil.simpleToString(this);
    }
}
