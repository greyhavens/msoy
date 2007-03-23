//
// $Id$

package com.threerings.msoy.world.client {

import flash.display.DisplayObject;
import flash.display.Graphics;

import flash.events.Event;

import flash.geom.Point;
import flash.geom.Rectangle;

import flash.utils.getTimer; // function import
import flash.utils.ByteArray;

import mx.binding.utils.BindingUtils;
import mx.binding.utils.ChangeWatcher;

import mx.core.Container;

import com.threerings.util.HashMap;
import com.threerings.util.Iterator;
import com.threerings.util.Name;

import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.MessageEvent;
import com.threerings.presents.dobj.MessageListener;
import com.threerings.presents.dobj.SetListener;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.crowd.chat.data.ChatMessage;
import com.threerings.crowd.chat.data.UserMessage;

import com.threerings.crowd.chat.client.ChatDisplay;

import com.threerings.flash.MenuUtil;

import com.threerings.whirled.data.SceneUpdate;

import com.threerings.whirled.spot.data.Location;
import com.threerings.whirled.spot.data.Portal;
import com.threerings.whirled.spot.data.SpotSceneObject;
import com.threerings.whirled.spot.data.SceneLocation;

import com.threerings.ezgame.util.EZObjectMarshaller;

import com.threerings.msoy.item.web.ItemIdent;
import com.threerings.msoy.item.web.MediaDesc;
import com.threerings.msoy.item.web.Decor;

import com.threerings.msoy.client.ContextMenuProvider;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyController;
import com.threerings.msoy.client.Prefs;
import com.threerings.msoy.client.WorldContext;
import com.threerings.msoy.data.ActorInfo;

import com.threerings.msoy.chat.client.ChatInfoProvider;
import com.threerings.msoy.chat.client.ChatOverlay;
import com.threerings.msoy.chat.client.ComicOverlay;

import com.threerings.msoy.world.data.DecorData;
import com.threerings.msoy.world.data.EntityControl;
import com.threerings.msoy.world.data.FurniData;
import com.threerings.msoy.world.data.MemoryEntry;
import com.threerings.msoy.world.data.ModifyFurniUpdate;
import com.threerings.msoy.world.data.MsoyLocation;
import com.threerings.msoy.world.data.MsoyScene;
import com.threerings.msoy.world.data.MsoySceneModel;
import com.threerings.msoy.world.data.RoomCodes;
import com.threerings.msoy.world.data.RoomObject;
import com.threerings.msoy.world.data.SceneAttrsUpdate;

/**
 * Displays a room or scene in the virtual world.
 */
public class RoomView extends AbstractRoomView
    implements ContextMenuProvider, SetListener, MessageListener,
               ChatDisplay, ChatInfoProvider, LoadingWatcher
{
    /** The chat overlay. */
    public var chatOverlay :ComicOverlay;

    /**
     * Create a roomview.
     */
    public function RoomView (ctx :WorldContext, ctrl :RoomController)
    {
        super(ctx);
        _ctrl = ctrl;
        chatOverlay = new ComicOverlay(ctx);

        _loadingSpinner = DisplayObject(new LOADING_SPINNER());
        FurniSprite.setLoadingWatcher(this);
    }

    /**
     * Returns the room controller.
     */
    public function getRoomController () :RoomController
    {
        return _ctrl;
    }

    override public function setScene (scene :MsoyScene) :void
    {
        super.setScene(scene);

        // set the top of the history to the back wall/floor corner
        chatOverlay.setSubtitlePercentage(
            (_metrics.sceneHeight - _metrics.backWallBottom) / _metrics.sceneHeight);
    }

    // from LoadingWatcher
    public function setLoading (loading :Boolean) :void
    {
        if (loading == (_loadingSpinner.parent != null)) {
            return;
        }

        var container :Container = _ctx.getTopPanel().getPlaceContainer();
        if (loading) {
            container.rawChildren.addChild(_loadingSpinner);
        } else {
            container.rawChildren.removeChild(_loadingSpinner);
        }
    }

    override public function setEditing (editing :Boolean, spriteVisitFn :Function) :void
    {
        super.setEditing(editing, spriteVisitFn);

        // if we haven't yet started loading sprites other than the background,
        // start now
        if (!_loadAllMedia) {
            _loadAllMedia = true;
            updateAllFurni();
        }

        // we hide all avatars instead of visiting them.
        if (editing) {
            _roomObj.removeListener(this);
            removeAllOccupants();
            setCenterSprite(null);

        } else {
            rereadScene();
            _roomObj.addListener(this)
            addAllOccupants();
        }
    }

    /**
     * A callback from actor sprites.
     */
    public function moveFinished (sprite :ActorSprite) :void
    {
//        if (_pendingRemovals.get(sprite.getOid()) != null) {
//            sprite.whirlOut(_scene);
//        }
//    }
//
//    public function whirlDone (sprite :ActorSprite) :void
//    {
        if (null != _pendingRemovals.remove(sprite.getOid())) {
            removeSprite(sprite);
        }
    }

    /**
     * Set the sprite we should be following.
     */
    public function setCenterSprite (center :MsoySprite) :void
    {
        _centerSprite = center;
        scrollView();
    }

    /**
     * Set whether we instantly jump to center, or scroll there.
     */
    public function setFastCentering (fastCentering :Boolean) :void
    {
        _jumpScroll = fastCentering;
    }

    public function dimAvatars (setDim :Boolean) :void
    {
        setActive(_actors, !setDim);
        setActive(_pendingRemovals, !setDim);
    }

    public function dimFurni (setDim :Boolean) :void
    {
        setActive(_furni, !setDim);
    }

    // from ContextMenuProvider
    public function populateContextMenu (menuItems :Array) :void
    {
        var sprite :MsoySprite = _ctrl.getHitSprite(stage.mouseX, stage.mouseY, true);
        if (sprite == null) {
            if (_bg == null) {
                return;
            } else {
                sprite = _bg;
            }
        }
        var ident :ItemIdent = sprite.getItemIdent();
        if (ident != null) {
            menuItems.push(MenuUtil.createControllerMenuItem(
                Msgs.GENERAL.get("b.view_item", Msgs.GENERAL.get(sprite.getDesc())),
                MsoyController.VIEW_ITEM, ident));
        }
    }

    /**
     * Called by our controller when a scene update is received.
     */
    public function processUpdate (update :SceneUpdate) :void
    {
        if (update is ModifyFurniUpdate) {
            for each (var furni :FurniData in (update as ModifyFurniUpdate).furniRemoved) {
                removeFurni(furni);
            }

        } else if (update is SceneAttrsUpdate) {
            rereadScene(); // re-read our scene
            updateBackground();
            return;

        } else {
            throw new Error();
        }

        // this will take care of anything added
        updateAllFurni();
    }

    /**
     * Get the actions currently published by our own avatar.
     */
    public function getMyActions () :Array
    {
        var avatar :AvatarSprite = getMyAvatar();
        return (avatar != null) ? avatar.getAvatarActions() : [];
    }

    /**
     * Get the states currently published by our own avatar.
     */
    public function getMyStates () :Array
    {
        var avatar :AvatarSprite = getMyAvatar();
        return (avatar != null) ? avatar.getAvatarStates() : [];
    }

    /**
     * A convenience function to get our personal avatar.
     */
    public function getMyAvatar () :AvatarSprite
    {
        return (getActor(_ctx.getClient().getClientOid()) as AvatarSprite);
    }

    /**
     * A convenience function to get the specified actor sprite, even if
     * it's on the way out the door.
     */
    public function getActor (bodyOid :int) :ActorSprite
    {
        var actor :ActorSprite = (_actors.get(bodyOid) as ActorSprite);
        if (actor == null) {
            actor = (_pendingRemovals.get(bodyOid) as ActorSprite);
        }
        return actor;
    }

    /**
     * A convenience function to get the specified actor sprite, even if
     * it's on the way out the door.
     */
    public function getActorByName (name :Name) :ActorSprite
    {
        var occInfo :OccupantInfo = _roomObj.getOccupantInfo(name);
        return (occInfo == null) ? null : getActor(occInfo.bodyOid);
    }

    /**
     * Return the current location of the avatar that represents our body.
     */
    public function getMyCurrentLocation () :MsoyLocation
    {
        var avatar :AvatarSprite = getMyAvatar();
        if (avatar != null) {
            return avatar.loc;
        } else {
            return new MsoyLocation(-1, -1, -1);
        }
    }

    // from interface SetListener
    public function entryAdded (event :EntryAddedEvent) :void
    {
        var name :String = event.getName();

        if (PlaceObject.OCCUPANT_INFO == name) {
            addBody((event.getEntry() as ActorInfo).getBodyOid());

        } else if (SpotSceneObject.OCCUPANT_LOCS == name) {
            var sceneLoc :SceneLocation = (event.getEntry() as SceneLocation);
            portalTraversed(sceneLoc.loc, true);

        } else if (RoomObject.MEMORIES == name) {
            dispatchMemoryChanged(event.getEntry() as MemoryEntry);

        } else if (RoomObject.CONTROLLERS == name) {
            var ctrl :EntityControl = (event.getEntry() as EntityControl);
            if (ctrl.controllerOid == _ctx.getMemberObject().getOid()) {
                dispatchGotControl(ctrl.ident);
            }
        }
    }

    // from interface SetListener
    public function entryUpdated (event :EntryUpdatedEvent) :void
    {
        var name :String = event.getName();

        if (PlaceObject.OCCUPANT_INFO == name) {
            updateBody(event.getEntry() as ActorInfo);

        } else if (SpotSceneObject.OCCUPANT_LOCS == name) {
            moveBody((event.getEntry() as SceneLocation).bodyOid);

        } else if (RoomObject.MEMORIES == name) {
            dispatchMemoryChanged(event.getEntry() as MemoryEntry);
        }
    }

    // from interface SetListener
    public function entryRemoved (event :EntryRemovedEvent) :void
    {
        var name :String = event.getName();

        if (PlaceObject.OCCUPANT_INFO == name) {
            removeBody((event.getOldEntry() as ActorInfo).getBodyOid());
        }
    }

    // fro interface MessageListener
    public function messageReceived (event :MessageEvent) :void
    {
        var args :Array = event.getArgs();
        switch (event.getName()) {
        case RoomCodes.SPRITE_MESSAGE:
            dispatchSpriteMessage((args[0] as ItemIdent), (args[1] as String), (args[2] as ByteArray), (args[3] as Boolean));
            break;
        }
    }

    // from ChatInfoProvider
    public function getSpeaker (speaker :Name) :Rectangle
    {
        var actor :ActorSprite = getActorByName(speaker);
        if (actor != null) {
            // return the global screen coords
            return actor.getStageRect();
        }
        return null;
    }

    // from ChatInfoProvider
    public function getAvoidables (speaker :Name, high :Array, low :Array) :void
    {
        // TODO: avoid any open dialog?
        // TODO: avoid the speaker's cluster?

        // iterate over occupants
        var myOid :int = _ctx.getClient().getClientOid();
        for (var ii :int = _roomObj.occupants.size() - 1; ii >= 0; ii--) {
            var actor :ActorSprite = getActor(_roomObj.occupants.get(ii));
            if (actor == null) {
                continue;
            }
            var actorInfo :ActorInfo = actor.getActorInfo();
            if ((actorInfo.bodyOid == myOid) ||
                    (speaker != null && speaker.equals(actorInfo.username))) {
                high.push(actor.getStageRect());

            } else if (low != null) {
                low.push(actor.getStageRect());
            }
        }
    }

    // from ChatDisplay
    public function clear () :void
    {
        // nada
    }

    // from ChatDisplay
    public function displayMessage (
        msg :ChatMessage, alreadyDisplayed :Boolean) :Boolean
    {
        if (msg is UserMessage) {
            var umsg :UserMessage = (msg as UserMessage);
            var avatar :AvatarSprite =
                (getActorByName(umsg.getSpeakerDisplayName()) as AvatarSprite);
            if (avatar != null) {
                avatar.performAvatarSpoke();
            }
        }

        return false; // we never display the messages ourselves
    }

    // from interface PlaceView
    override public function willEnterPlace (plobj :PlaceObject) :void
    {
        // set load-all to false, as we're going to 
        _loadAllMedia = false;

        super.willEnterPlace(plobj);

        _roomObj.addListener(this);

        recheckChatOverlay();

        addAllOccupants();

        // we add ourselves as a chat display so that we can trigger
        // speak actions on avatars
        _ctx.getChatDirector().addChatDisplay(this);

        // and animate ourselves entering the room (everyone already in the (room will also have
        // seen it)
        portalTraversed(getMyCurrentLocation(), true);

        // load the background image first
        var legacyBg :FurniData = null;
        var decordata :DecorData = _scene.getDecorData();

        if (decordata.itemId == 0) {
            // this means there is no actual decor item attached - so let's see if we can find
            // a legacy furni to load instead.
            legacyBg = _scene.getBackgroundFurniture();
        }

        if (legacyBg != null) {
            // decor item was not specified, but a legacy furni exists - let's load it
            addFurni(legacyBg).setLoadedCallback(backgroundFinishedLoading);
        } else {
            // decor item was specified, or if it wasn't, neither was a legacy background.
            // load the decor data we have, even if it's just default values.
            setBackground(decordata);
            _bg.setLoadedCallback(backgroundFinishedLoading);
        }
        
        _chatOverlayWatcher = BindingUtils.bindSetter(recheckChatOverlay,
            _ctx.worldProps, "placeViewShowsChat");
    }

    // from interface PlaceView
    override public function didLeavePlace (plobj :PlaceObject) :void
    {
        _roomObj.removeListener(this);

        // stop listening for avatar speak action triggers
        _ctx.getChatDirector().removeChatDisplay(this);

        removeAllOccupants();

        super.didLeavePlace(plobj);

        recheckChatOverlay();
        _chatOverlayWatcher.unwatch();
        _chatOverlayWatcher = null;
    }

    // from AbstractRoomView
    override public function updateAllFurni () :void
    {
        super.updateAllFurni();

        var music :FurniData = _scene.getMusic();
        if (music != null) {
            _ctrl.setBackgroundMusic(music);
        }
    }

    override public function locationUpdated (sprite :MsoySprite) :void
    {
        super.locationUpdated(sprite);

        // if we moved the _centerSprite, possibly update the scroll position
        if (sprite == _centerSprite &&
            ((sprite != _bg) || _scene.getSceneType() != Decor.FIXED_IMAGE)) {
            scrollView();
        }
    }

    override public function scrollViewBy (xpixels :int) :Boolean
    {
        var canScroll :Boolean = super.scrollViewBy(xpixels);
        if (canScroll) {
            // remove any autoscrolling (if tick is not a registered listener
            // this will safely noop)
            removeEventListener(Event.ENTER_FRAME, tick);
            _jumpScroll = false;
        }
        return canScroll;
    }

    /**
     * Once the background image is finished, we want to load all the
     * rest of the sprites.
     */
    protected function backgroundFinishedLoading () :void
    {
        _loadAllMedia = true;
        updateAllFurni();
        addAllOccupants();
    }

    override protected function relayout () :void
    {
        super.relayout();

        var sprite :MsoySprite;
        for each (sprite in _actors.values()) {
            locationUpdated(sprite);
        }
        for each (sprite in _pendingRemovals.values()) {
            locationUpdated(sprite);
        }
    }

    override protected function shouldLoadAll () :Boolean
    {
        return _loadAllMedia;
    }

    /**
     * Check the status of the chat overlay.
     * @param args nothing- makes BindingUtils happy.
     */
    protected function recheckChatOverlay (... args) :void
    {
        var shouldBeEnabled :Boolean = _ctx.worldProps.placeViewShowsChat && (_roomObj != null);

        if (chatOverlay.isActive() == shouldBeEnabled) {
            // all is well
            return;
        }

        if (shouldBeEnabled) {
            chatOverlay.newPlaceEntered(this);
            chatOverlay.setTarget(_ctx.getTopPanel().getPlaceContainer());

        } else {
            chatOverlay.setTarget(null);
        }
    }

    protected function scrollView () :void
    {
        if (_centerSprite == null) {
            return;
        }
        var rect :Rectangle = scrollRect;
        if (rect == null) {
            return; // return if there's nothing to scroll
        }

        var centerX :int = _centerSprite.x + _centerSprite.getLayoutHotSpot().x;
        var bounds :Rectangle = getScrollBounds();
        var newX :Number = centerX - rect.width/2;
        newX = Math.min(bounds.x + bounds.width - rect.width, Math.max(bounds.x, newX));

        if (_jumpScroll) {
            rect.x = newX;

        } else if (Math.abs(rect.x - newX) > MAX_AUTO_SCROLL) {
            if (newX > rect.x) {
                rect.x += MAX_AUTO_SCROLL;
            } else {
                rect.x -= MAX_AUTO_SCROLL;
            }
            addEventListener(Event.ENTER_FRAME, tick);

        } else {
            rect.x = newX;
            removeEventListener(Event.ENTER_FRAME, tick);
            _jumpScroll = true;
        }

        // assign the new scrolling rectangle
        scrollRect = rect;
        scrollRectUpdated();
        _suppressAutoScroll = true;
    }

    protected function tick (event :Event) :void
    {
        if (!_suppressAutoScroll) {
            if (_centerSprite != null) {
                scrollView();

            } else {
                // stop scrolling
                removeEventListener(Event.ENTER_FRAME, tick);
            }
        }

        // and finally, we want ensure it can happen on the next frame if
        // our avatar doesn't move
        _suppressAutoScroll = false;
    }

    protected function addBody (bodyOid :int) :void
    {
        if (!shouldLoadAll()) {
            return;
        }

        var occInfo :ActorInfo = (_roomObj.occupantInfo.get(bodyOid) as ActorInfo);
        var sloc :SceneLocation = (_roomObj.occupantLocs.get(bodyOid) as SceneLocation);
        var loc :MsoyLocation = (sloc.loc as MsoyLocation);

        // see if the actor was already created, pending removal
        var actor :ActorSprite = (_pendingRemovals.remove(bodyOid) as ActorSprite);

        if (actor == null) {
            actor = _ctx.getMediaDirector().getActor(occInfo);
            _actors.put(bodyOid, actor);
            addChild(actor);
            actor.setEntering(loc);

            // if we ever add ourselves, we follow it
            if (bodyOid == _ctx.getClient().getClientOid()) {
                setFastCentering(true);
                setCenterSprite(actor);
            }

        } else {
            // place the sprite back into the set of active sprites
            _actors.put(bodyOid, actor);
            actor.moveTo(loc, _scene);
        }

        // map the actor sprite in the entities table
        _entities.put(occInfo.getItemIdent(), actor);
    }

    protected function removeBody (bodyOid :int) :void
    {
        var actor :ActorSprite = (_actors.remove(bodyOid) as ActorSprite);
        if (actor != null) {
            if (actor.isMoving()) {
                _pendingRemovals.put(bodyOid, actor);
            } else {
                removeSprite(actor);
            }
        }
    }

    protected function moveBody (bodyOid :int) :void
    {
        var actor :ActorSprite = (_actors.get(bodyOid) as ActorSprite);
        var sloc :SceneLocation = (_roomObj.occupantLocs.get(bodyOid) as SceneLocation);
        var loc :MsoyLocation = (sloc.loc as MsoyLocation);
        actor.moveTo(loc, _scene);
    }

    protected function updateBody (occInfo :ActorInfo) :void
    {
        var actor :ActorSprite = (_actors.get(occInfo.getBodyOid()) as ActorSprite);
        if (actor == null) {
            Log.getLog(this).warning("No actor for updated occupant? [info=" + occInfo + "].");
            return;
        }
        actor.setActorInfo(occInfo);
    }

    /**
     * Called when a sprite message arrives on the room object.
     */
    protected function dispatchSpriteMessage (item :ItemIdent, name :String, arg :ByteArray, isAction :Boolean) :void
    {
        var sprite :MsoySprite = (_entities.get(item) as MsoySprite);
        if (sprite != null) {
            sprite.messageReceived(name, EZObjectMarshaller.decode(arg), isAction);
        }
    }

    /**
     * Called when a memory entry is added or updated in the room object.
     */
    protected function dispatchMemoryChanged (entry :MemoryEntry) :void
    {
        var sprite :MsoySprite = (_entities.get(entry.item) as MsoySprite);
        if (sprite != null) {
            sprite.memoryChanged(entry.key, EZObjectMarshaller.decode(entry.value));
        }
    }

    /**
     * Called when control of an entity is assigned to us.
     */
    public function dispatchGotControl (ident :ItemIdent) :void
    {
        var sprite :MsoySprite = (_entities.get(ident) as MsoySprite);
        if (sprite != null) {
            sprite.gotControl();
        }
    }

    /**
     * Called when we detect a body being added or removed.
     */
    protected function portalTraversed (loc :Location, entering :Boolean) :void
    {
        var itr :Iterator = _scene.getPortals();
        while (itr.hasNext()) {
            var portal :Portal = (itr.next() as Portal);
            if (loc.equals(portal.loc)) {
                var sprite :FurniSprite = (_furni.get(portal.portalId) as FurniSprite);
                if (sprite != null) {
                    sprite.wasTraversed(entering);
                }
                return;
            }
        }
    }

    override protected function removeSprite (sprite :MsoySprite) :void
    {
        super.removeSprite(sprite);

        if (sprite is ActorSprite) {
            // TODO: ack! This is getting triggered even when we remove
            // actors for other reasons than them leaving...
            var actor :ActorSprite = (sprite as ActorSprite);
            portalTraversed(actor.loc, false);
            // remove the sprite from the entities table
            _entities.remove(actor.getActorInfo().getItemIdent());
        }
        if (sprite == _centerSprite) {
            _centerSprite = null;
        }
    }

    protected function addAllOccupants () :void
    {
        if (shouldLoadAll()) {
            // add all currently present occupants
            for (var ii :int = _roomObj.occupants.size() - 1; ii >= 0; ii--) {
                addBody(_roomObj.occupants.get(ii));
            }
        }
    }

    protected function removeAllOccupants () :void
    {
        removeAll(_actors);
        removeAll(_pendingRemovals);
    }

    // from AbstractRoomView
    override protected function addFurni (furni :FurniData) :FurniSprite
    {
        var fsprite :FurniSprite = super.addFurni(furni);
        _entities.put(furni.getItemIdent(), fsprite);
        return fsprite;
    }

    protected function removeFurni (furni :FurniData) :void
    {
        var sprite :FurniSprite = (_furni.remove(furni.id) as FurniSprite);
        if (sprite != null) {
            removeSprite(sprite);
        }
        _entities.remove(furni.getItemIdent());
    }

    /** Our controller. */
    protected var _ctrl :RoomController;

    /** When we first enter the room, we only load the background (if any). */
    protected var _loadAllMedia :Boolean = false;

    /** Watches WorldProperties.placeViewShowsChat. */
    protected var _chatOverlayWatcher :ChangeWatcher;

    /** The spinner to show when we're loading room data. */
    protected var _loadingSpinner :DisplayObject;

    /** A map of bodyOid -> ActorSprite. */
    protected var _actors :HashMap = new HashMap();

    /** Maps ItemIdent -> MsoySprite for entities (furni, avatars, pets). */
    protected var _entities :HashMap = new HashMap();

    /** The sprite we should center on. */
    protected var _centerSprite :MsoySprite;

    /** A map of bodyOid -> ActorSprite for those that we'll remove when they stop moving. */
    protected var _pendingRemovals :HashMap = new HashMap();

    /** If true, the scrolling should simply jump to the right position. */
    protected var _jumpScroll :Boolean = true;

    /** True if autoscroll should be supressed for the current frame. */
    protected var _suppressAutoScroll :Boolean = false;

    /** The maximum number of pixels to autoscroll per frame. */
    protected static const MAX_AUTO_SCROLL :int = 15;

    [Embed(source="../../../../../../../rsrc/media/loading.swf")]
    protected static const LOADING_SPINNER :Class;
}
}
