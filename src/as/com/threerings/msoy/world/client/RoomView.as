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
import com.threerings.util.ObjectMarshaller;
import com.threerings.util.ValueEvent;

import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.MessageEvent;
import com.threerings.presents.dobj.MessageListener;
import com.threerings.presents.dobj.SetListener;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.crowd.chat.data.ChatCodes;
import com.threerings.crowd.chat.data.ChatMessage;
import com.threerings.crowd.chat.data.UserMessage;

import com.threerings.crowd.chat.client.ChatDisplay;

import com.threerings.flash.MenuUtil;

import com.threerings.whirled.data.SceneUpdate;

import com.threerings.whirled.spot.data.Location;
import com.threerings.whirled.spot.data.Portal;
import com.threerings.whirled.spot.data.SpotSceneObject;
import com.threerings.whirled.spot.data.SceneLocation;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.Decor;

import com.threerings.msoy.client.ContextMenuProvider;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyController;
import com.threerings.msoy.client.PlaceBox;
import com.threerings.msoy.client.Prefs;
import com.threerings.msoy.client.TopPanel;
import com.threerings.msoy.client.WorldClient;
import com.threerings.msoy.client.WorldContext;
import com.threerings.msoy.data.ActorInfo;
import com.threerings.msoy.data.MemberObject;

import com.threerings.msoy.chat.client.ChatInfoProvider;
import com.threerings.msoy.chat.client.ChatOverlay;
import com.threerings.msoy.chat.client.ComicOverlay;
import com.threerings.msoy.chat.client.MsoyChatDirector;

import com.threerings.msoy.world.client.editor.DoorTargetEditController;
import com.threerings.msoy.world.data.AudioData;
import com.threerings.msoy.world.data.EntityControl;
import com.threerings.msoy.world.data.EffectData;
import com.threerings.msoy.world.data.FurniData;
import com.threerings.msoy.world.data.MemoryEntry;
import com.threerings.msoy.world.data.ModifyFurniUpdate;
import com.threerings.msoy.world.data.MsoyLocation;
import com.threerings.msoy.world.data.MsoyScene;
import com.threerings.msoy.world.data.MsoySceneModel;
import com.threerings.msoy.world.data.RoomCodes;
import com.threerings.msoy.world.data.RoomObject;
import com.threerings.msoy.world.data.SceneAttrsUpdate;
import com.threerings.msoy.world.data.WorldPetInfo;
import com.threerings.msoy.world.data.WorldMemberInfo;

/**
 * Displays a room or scene in the virtual world.
 */
public class RoomView extends AbstractRoomView
    implements ContextMenuProvider, SetListener, MessageListener,
               ChatDisplay, ChatInfoProvider, LoadingWatcher
{
    // the layer priority of the spinner
    public static const LAYER_ROOM_SPINNER :int = 50;

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

        chatOverlay.setSubtitlePercentage(1);
    }

    /**
     * Update the 'my' user's specified avatar's scale, non-permanently.
     * This is called via the avatarviewer, so that scale changes they make are instantly
     * viewable in the world.
     */
    public function updateAvatarScale (avatarId :int, newScale :Number) :void
    {
        var avatar :AvatarSprite = getMyAvatar();
        if (avatar != null) {
            var occInfo :WorldMemberInfo = avatar.getActorInfo() as WorldMemberInfo;
            if (occInfo.getItemIdent().equals(new ItemIdent(Item.AVATAR, avatarId))) {
                occInfo.setScale(newScale);
                avatar.setActorInfo(occInfo);
            }
        }
    }

    // from LoadingWatcher
    public function setLoading (loading :Boolean) :void
    {
        if (loading == (_loadingSpinner.parent != null)) {
            return;
        }

        var box :PlaceBox = _ctx.getTopPanel().getPlaceContainer();
        if (loading) {
            box.addOverlay(_loadingSpinner, LAYER_ROOM_SPINNER);
        } else {
            box.removeOverlay(_loadingSpinner);
        }
    }

    override public function setEditing (editing :Boolean) :void
    {
        super.setEditing(editing);

        // if we haven't yet started loading sprites other than the background, start now
        if (!_loadAllMedia) {
            _loadAllMedia = true;
            updateAllFurni();
            updateAllEffects();
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
            // trigger a portal traversal
            portalTraversed(sprite.getLocation(), false);
            // and remove the sprite
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

    /**
     * Sets whether or not to use the chat overlay.
     */
    public function setUseChatOverlay (useChatOverlay :Boolean) :void
    {
        _useChatOverlay = useChatOverlay;
        recheckChatOverlay();
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
    public function populateContextMenu (ctx :WorldContext, menuItems :Array) :void
    {
        var hit :* = _ctrl.getHitSprite(stage.mouseX, stage.mouseY, true);
        if (hit === undefined) {
            return;
        }
        var sprite :MsoySprite = (hit as MsoySprite);
        if (sprite == null) {
            if (_bg == null) {
                return;
            } else {
                sprite = _bg;
            }
        }

        var ident :ItemIdent = sprite.getItemIdent();
        if (ident != null) {
            var kind :String = Msgs.GENERAL.get(sprite.getDesc());
            if (ident.type >= 0) { // -1 is used for the default avatar, etc.
                menuItems.push(MenuUtil.createControllerMenuItem(
                                   Msgs.GENERAL.get("b.view_item", kind),
                                   MsoyController.VIEW_ITEM, ident));
            }

            if (sprite.isBlockable()) {
                var isBlocked :Boolean = sprite.isBlocked();
                menuItems.push(MenuUtil.createControllerMenuItem(
                    Msgs.GENERAL.get((isBlocked ? "b.unbleep_item" : "b.bleep_item"), kind),
                    sprite.toggleBlocked, ctx));
            }
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
            updateBackgroundAudio();
        } else {
            throw new Error();
        }

        // this will take care of anything added
        updateAllFurni();
        updateAllEffects();
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
     * A convenience function to get an array of all sprites for all pets in the room.
     */
    public function getPets () :Array /* of PetSprite */
    {
        return _actors.values().filter(function (o :*, i :int, a :Array) :Boolean {
                return (o is PetSprite);
            });
    }
    
    /**
     * Return the current location of the avatar that represents our body.
     */
    public function getMyCurrentLocation () :MsoyLocation
    {
        var avatar :AvatarSprite = getMyAvatar();
        if (avatar != null) {
            return avatar.getLocation();
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

        } else if (RoomObject.EFFECTS == name) {
            addEffect(event.getEntry() as EffectData);
        }
    }

    // from interface SetListener
    public function entryUpdated (event :EntryUpdatedEvent) :void
    {
        var name :String = event.getName();

        if (PlaceObject.OCCUPANT_INFO == name) {
            updateBody(event.getEntry() as ActorInfo, event.getOldEntry() as ActorInfo);

        } else if (SpotSceneObject.OCCUPANT_LOCS == name) {
            moveBody((event.getEntry() as SceneLocation).bodyOid);

        } else if (RoomObject.MEMORIES == name) {
            dispatchMemoryChanged(event.getEntry() as MemoryEntry);

        } else if (RoomObject.EFFECTS == name) {
            updateEffect(event.getEntry() as EffectData);
        }
    }

    // from interface SetListener
    public function entryRemoved (event :EntryRemovedEvent) :void
    {
        var name :String = event.getName();

        if (PlaceObject.OCCUPANT_INFO == name) {
            removeBody((event.getOldEntry() as ActorInfo).getBodyOid());

        } else if (RoomObject.EFFECTS == name) {
            removeEffect(event.getKey() as int);
        }
    }

    // fro interface MessageListener
    public function messageReceived (event :MessageEvent) :void
    {
        var args :Array = event.getArgs();
        switch (event.getName()) {
        case RoomCodes.SPRITE_MESSAGE:
            dispatchSpriteMessage((args[0] as ItemIdent), (args[1] as String),
                                  (args[2] as ByteArray), (args[3] as Boolean));
            break;
        }
    }

    // from ChatInfoProvider
    public function getBubblePosition (speaker :Name) :Point
    {
        var actor :ActorSprite = getActorByName(speaker);
        return (actor == null) ? null : actor.getBubblePosition();
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
    public function displayMessage (msg :ChatMessage, alreadyDisplayed :Boolean) :Boolean
    {
        if (msg.localtype == ChatCodes.PLACE_CHAT_TYPE && msg is UserMessage) {
            var umsg :UserMessage = (msg as UserMessage);
            var avatar :AvatarSprite =
                (getActorByName(umsg.getSpeakerDisplayName()) as AvatarSprite);
            if (avatar != null) {
                avatar.performAvatarSpoke();
            }

            // send it to pets as well
            var petSprites :Array = getPets();
            for each (var pet :PetSprite in petSprites) {
                pet.processChatMessage(umsg);
            }
        }

        return false; // we never display the messages ourselves
    }

    // from interface PlaceView
    override public function willEnterPlace (plobj :PlaceObject) :void
    {
        // set load-all to false, as we're going to just load the decor item first.
        _loadAllMedia = false;

        super.willEnterPlace(plobj);

        // listen for client minimization events
        _ctx.getClient().addEventListener(WorldClient.MINI_WILL_CHANGE, miniWillChange);

        _roomObj.addListener(this);

        recheckChatOverlay();

        addAllOccupants();

        // we add ourselves as a chat display so that we can trigger speak actions on avatars
        _ctx.getChatDirector().addChatDisplay(this);

        // and animate ourselves entering the room (everyone already in the (room will also have
        // seen it)
        portalTraversed(getMyCurrentLocation(), true);

        // load the background image first
        setBackground(_scene.getDecor());
        // load the decor data we have, even if it's just default values.
        _bg.setLoadedCallback(backgroundFinishedLoading);

        // start playing background audio
        _ctrl.setBackgroundMusic(_scene.getAudioData());
    }

    // from interface PlaceView
    override public function didLeavePlace (plobj :PlaceObject) :void
    {
        _roomObj.removeListener(this);

        // stop listening for avatar speak action triggers
        _ctx.getChatDirector().removeChatDisplay(this);

        // stop listening for client minimization events
        _ctx.getClient().removeEventListener(WorldClient.MINI_WILL_CHANGE, miniWillChange);

        removeAll(_effects);
        removeAllOccupants();

        super.didLeavePlace(plobj);

        recheckChatOverlay();

        setLoading(false);
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
            // remove any autoscrolling (if tick is not a registered listener this will noop)
            removeEventListener(Event.ENTER_FRAME, tick);
            _jumpScroll = false;
        }
        return canScroll;
    }

    override public function set scrollRect (r :Rectangle) :void
    {
        super.scrollRect = r;

        chatOverlay.setScrollRect(r);
    }

    /**
     * Once the background image is finished, we want to load all the rest of the sprites.
     */
    protected function backgroundFinishedLoading () :void
    {
        _loadAllMedia = true;
        updateAllFurni();
        updateAllEffects();
        addAllOccupants();
        
        // inform the "floating" door editor
        DoorTargetEditController.updateLocation();
    }

    /**
     * Called when the client is minimized or unminimized.
     */
    protected function miniWillChange (event :ValueEvent) :void
    {
        relayout();
    }

    /**
     * Re-layout any effects.
     */
    protected function updateAllEffects () :void
    {
        if (shouldLoadAll()) {
            for each (var effect :EffectData in _roomObj.effects.toArray()) {
                updateEffect(effect);
            }
        }
    }

    /**
     * Re-check whether we should be using our overlay.
     */
    protected function recheckChatOverlay () :void
    {
        if (_roomObj != null && _useChatOverlay) {
            _ctx.getChatDirector().addChatDisplay(chatOverlay);
            chatOverlay.newPlaceEntered(this);
            chatOverlay.setTarget(_ctx.getTopPanel().getPlaceContainer());

        } else {
            _ctx.getChatDirector().removeChatDisplay(chatOverlay);
            chatOverlay.setTarget(null);
        }
    }

    override protected function relayout () :void
    {
        super.relayout();

        if (_ctx.getWorldClient().isFeaturedPlaceView()) {
            var sceneWidth :int = Math.round(_scene.getWidth() * scaleX) as int;
            if (sceneWidth < _actualWidth) {
                // center a scene that is narrower than our view area.
                x = (_actualWidth - sceneWidth) / 2;
            } else {
                // center a scene that is wider than our view area.
                var rect :Rectangle = scrollRect;
                if (rect != null) {
                    var newX :Number = (_scene.getWidth() - _actualWidth / scaleX) / 2;
                    newX = Math.min(_scene.getWidth() - rect.width, Math.max(0, newX));
                    rect.x = newX;
                    scrollRect = rect;
                }
            }
        }

        var sprite :MsoySprite;
        for each (sprite in _actors.values()) {
            locationUpdated(sprite);
        }
        for each (sprite in _pendingRemovals.values()) {
            locationUpdated(sprite);
        }
        for each (sprite in _effects.values()) {
            locationUpdated(sprite);
        }
    }

    override protected function shouldLoadAll () :Boolean
    {
        return _loadAllMedia;
    }

    /**
     * Restart playing the background audio.
     */
    protected function updateBackgroundAudio () :void
    {
        var audiodata :AudioData = _scene.getAudioData();
        if (audiodata != null) {
            _ctrl.setBackgroundMusic(audiodata);
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
        var newX :Number = centerX - (_actualWidth / scaleX)/2;
        newX = Math.min(_scene.getWidth() - rect.width, Math.max(0, newX));

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
        if (occInfo is WorldMemberInfo && (occInfo as WorldMemberInfo).viewOnly) {
            // don't add viewOnly actors.
            return;
        }
        var sloc :SceneLocation = (_roomObj.occupantLocs.get(bodyOid) as SceneLocation);
        var loc :MsoyLocation = (sloc.loc as MsoyLocation);

        // see if the actor was already created, pending removal
        var actor :ActorSprite = (_pendingRemovals.remove(bodyOid) as ActorSprite);

        if (actor == null) {
            actor = _ctx.getMediaDirector().getActor(occInfo);
            actor.setChatOverlay(chatOverlay);
            _actors.put(bodyOid, actor);
            addChildAt(actor, 1);
            actor.setEntering(loc);

            // if we ever add ourselves, we follow it
            if (bodyOid == _ctx.getClient().getClientOid()) {
                setFastCentering(true);
                setCenterSprite(actor);
            }

        } else {
            // place the sprite back into the set of active sprites
            _actors.put(bodyOid, actor);
            actor.setChatOverlay(chatOverlay);
            actor.moveTo(loc, _scene);
        }

        // map the actor sprite in the entities table
        _entities.put(occInfo.getItemIdent(), actor);

        // if this actor is a pet, notify GWT that we've got a new pet in the room.
        if (actor is PetSprite) {
            (_ctx.getClient() as WorldClient).dispatchEventToGWT(PET_EVENT, 
                [true, actor.getItemIdent().itemId]);
        }
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

            // if this actor is a pet, notify GWT that we've removed a pet from the room.
            if (actor is PetSprite) {
                (_ctx.getClient() as WorldClient).dispatchEventToGWT(PET_EVENT, 
                    [false, actor.getItemIdent().itemId]);
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

    protected function updateBody (newInfo :ActorInfo, oldInfo :ActorInfo) :void
    {
        var actor :ActorSprite = (_actors.get(newInfo.getBodyOid()) as ActorSprite);
        if (actor == null) {
            if (!(newInfo is WorldMemberInfo) || !(newInfo as WorldMemberInfo).viewOnly) {
                log.warning("No actor for updated occupant? [info=" + newInfo + "].");
            }
            return;
        }
        actor.setActorInfo(newInfo);

        // update the entities table
        _entities.remove(oldInfo.getItemIdent());
        _entities.put(newInfo.getItemIdent(), actor);
    }

    protected function addEffect (effect :EffectData) :FurniSprite
    {
        var sprite :EffectSprite = new EffectSprite(_ctrl.adjustEffectData(effect));
        addChildAt(sprite, 1);
        sprite.setLocation(effect.loc);
        _effects.put(effect.id, sprite);
        return sprite;
    }

    protected function updateEffect (effect :EffectData) :void
    {
        var sprite :FurniSprite = (_effects.get(effect.id) as FurniSprite);
        if (sprite != null) {
            sprite.update(_ctrl.adjustEffectData(effect));
        } else {
            addEffect(effect);
        }
    }

    protected function removeEffect (effectId :int) :void
    {
        var sprite :EffectSprite = (_effects.remove(effectId) as EffectSprite);
        if (sprite != null) {
            removeSprite(sprite);
        }
    }

    /**
     * Called when a sprite message arrives on the room object.
     */
    protected function dispatchSpriteMessage (item :ItemIdent, name :String,
                                              arg :ByteArray, isAction :Boolean) :void
    {
        var sprite :MsoySprite = (_entities.get(item) as MsoySprite);
        if (sprite != null) {
            sprite.messageReceived(name, ObjectMarshaller.decode(arg), isAction);
        } else {
            log.info("Received sprite message for unknown sprite [item=" + item +
                     ", name=" + name + "].");
        }
    }

    /**
     * Called when a memory entry is added or updated in the room object.
     */
    protected function dispatchMemoryChanged (entry :MemoryEntry) :void
    {
        var sprite :MsoySprite = (_entities.get(entry.item) as MsoySprite);
        if (sprite != null) {
            sprite.memoryChanged(entry.key, ObjectMarshaller.decode(entry.value));
        } else {
            log.info("Received memory update for unknown sprite [item=" + entry.item +
                     ", key=" + entry.key + "].");
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
        } else {
            log.info("Received got control for unknown sprite [item=" + ident + "].");
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
            // remove the sprite from the entities table
            _entities.remove((sprite as ActorSprite).getActorInfo().getItemIdent());
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
                var bodyOid :int = _roomObj.occupants.get(ii);
                if (! _actors.containsKey(bodyOid)) {
                    addBody(bodyOid);
                }
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

    /** Event specific constants for notifying GWT */
    protected static const PET_EVENT :String = "pet";

    /** Our controller. */
    protected var _ctrl :RoomController;

    /** When we first enter the room, we only load the background (if any). */
    protected var _loadAllMedia :Boolean = false;

    /** The spinner to show when we're loading room data. */
    protected var _loadingSpinner :DisplayObject;

    /** A map of bodyOid -> ActorSprite. */
    protected var _actors :HashMap = new HashMap();

    /** Maps ItemIdent -> MsoySprite for entities (furni, avatars, pets). */
    protected var _entities :HashMap = new HashMap();

    /** Maps effect id -> EffectData for effects. */
    protected var _effects :HashMap = new HashMap();

    /** The sprite we should center on. */
    protected var _centerSprite :MsoySprite;

    /** A map of bodyOid -> ActorSprite for those that we'll remove when they stop moving. */
    protected var _pendingRemovals :HashMap = new HashMap();

    /** Should we be using our chat overlay? */
    protected var _useChatOverlay :Boolean = true;

    /** If true, the scrolling should simply jump to the right position. */
    protected var _jumpScroll :Boolean = true;

    /** True if autoscroll should be supressed for the current frame. */
    protected var _suppressAutoScroll :Boolean = false;

    /** Log this! */
    private const log :Log = Log.getLog(RoomView);

    /** The maximum number of pixels to autoscroll per frame. */
    protected static const MAX_AUTO_SCROLL :int = 15;

    [Embed(source="../../../../../../../rsrc/media/loading.swf")]
    protected static const LOADING_SPINNER :Class;
}
}
