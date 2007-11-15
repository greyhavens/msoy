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
import com.threerings.util.Log;
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
import com.threerings.msoy.data.MemberObject;

import com.threerings.msoy.chat.client.ChatInfoProvider;
import com.threerings.msoy.chat.client.ChatOverlay;
import com.threerings.msoy.chat.client.ComicOverlay;
import com.threerings.msoy.chat.client.MsoyChatDirector;

import com.threerings.msoy.world.client.editor.DoorTargetEditController;

import com.threerings.msoy.world.data.ActorInfo;
import com.threerings.msoy.world.data.AudioData;
import com.threerings.msoy.world.data.EffectData;
import com.threerings.msoy.world.data.EntityControl;
import com.threerings.msoy.world.data.FurniData;
import com.threerings.msoy.world.data.MemberInfo;
import com.threerings.msoy.world.data.MemoryEntry;
import com.threerings.msoy.world.data.ModifyFurniUpdate;
import com.threerings.msoy.world.data.MsoyLocation;
import com.threerings.msoy.world.data.MsoyScene;
import com.threerings.msoy.world.data.MsoySceneModel;
import com.threerings.msoy.world.data.PetInfo;
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

        chatOverlay.setSubtitlePercentage(1);
    }

    /**
     * Update the 'my' user's specified avatar's scale, non-permanently.  This is called via the
     * avatar viewer, so that scale changes they make are instantly viewable in the world.
     */
    public function updateAvatarScale (avatarId :int, newScale :Number) :void
    {
        var avatar :MemberSprite = getMyAvatar();
        if (avatar != null) {
            var occInfo :MemberInfo = (avatar.getOccupantInfo() as MemberInfo);
            if (occInfo.getItemIdent().equals(new ItemIdent(Item.AVATAR, avatarId))) {
                occInfo.setScale(newScale);
                avatar.setOccupantInfo(occInfo);
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
            box.addOverlay(_loadingSpinner, PlaceBox.LAYER_ROOM_SPINNER);
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
     * A callback from occupant sprites.
     */
    public function moveFinished (sprite :OccupantSprite) :void
    {
//        if (_pendingRemovals.get(sprite.getOid()) != null) {
//            sprite.whirlOut(_scene);
//        }
//    }
//
//    public function whirlDone (sprite :OccupantSprite) :void
//    {
        if (sprite.getOid() == _ctx.getMemberObject().getOid()) {
            _ctx.getGameDirector().tutorialEvent("playerMoved");
        }

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
        setActive(_occupants, !setDim);
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
        var avatar :MemberSprite = getMyAvatar();
        return (avatar != null) ? avatar.getAvatarActions() : [];
    }

    /**
     * Get the states currently published by our own avatar.
     */
    public function getMyStates () :Array
    {
        var avatar :MemberSprite = getMyAvatar();
        return (avatar != null) ? avatar.getAvatarStates() : [];
    }

    /**
     * A convenience function to get our personal avatar.
     */
    public function getMyAvatar () :MemberSprite
    {
        return (getOccupant(_ctx.getClient().getClientOid()) as MemberSprite);
    }

    /**
     * A convenience function to get the specified occupant sprite, even if it's on the way out the
     * door.
     */
    public function getOccupant (bodyOid :int) :OccupantSprite
    {
        var sprite :OccupantSprite = (_occupants.get(bodyOid) as OccupantSprite);
        if (sprite == null) {
            sprite = (_pendingRemovals.get(bodyOid) as OccupantSprite);
        }
        return sprite;
    }

    /**
     * A convenience function to get the specified occupant sprite, even if it's on the way out the
     * door.
     */
    public function getOccupantByName (name :Name) :OccupantSprite
    {
        var occInfo :OccupantInfo = _roomObj.getOccupantInfo(name);
        return (occInfo == null) ? null : getOccupant(occInfo.bodyOid);
    }

    /**
     * A convenience function to get an array of all sprites for all pets in the room.
     */
    public function getPets () :Array /* of PetSprite */
    {
        return _occupants.values().filter(function (o :*, i :int, a :Array) :Boolean {
            return (o is PetSprite);
        });
    }
    
    /**
     * Return the current location of the avatar that represents our body.
     */
    public function getMyCurrentLocation () :MsoyLocation
    {
        var avatar :MemberSprite = getMyAvatar();
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
            addBody(event.getEntry() as OccupantInfo);

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
            updateBody(event.getEntry() as OccupantInfo, event.getOldEntry() as OccupantInfo);

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
            removeBody((event.getOldEntry() as OccupantInfo).getBodyOid());

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
        var sprite :OccupantSprite = getOccupantByName(speaker);
        return (sprite == null) ? null : sprite.getBubblePosition();
    }

    // from ChatInfoProvider
    public function getAvoidables (speaker :Name, high :Array, low :Array) :void
    {
        // TODO: avoid any open dialog?
        // TODO: avoid the speaker's cluster?

        // iterate over occupants
        var myOid :int = _ctx.getClient().getClientOid();
        for (var ii :int = _roomObj.occupants.size() - 1; ii >= 0; ii--) {
            var sprite :OccupantSprite = getOccupant(_roomObj.occupants.get(ii));
            if (sprite == null) {
                continue;
            }
            var occInfo :OccupantInfo = sprite.getOccupantInfo();
            if ((occInfo.bodyOid == myOid) ||
                (speaker != null && speaker.equals(occInfo.username))) {
                high.push(sprite.getStageRect());

            } else if (low != null) {
                low.push(sprite.getStageRect());
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
            if (umsg.speaker.equals(_ctx.getMemberObject().memberName)) {
                _ctx.getGameDirector().tutorialEvent("playerSpoke");
            }
            var avatar :MemberSprite =
                (getOccupantByName(umsg.getSpeakerDisplayName()) as MemberSprite);
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
        for each (sprite in _occupants.values()) {
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

    protected function addBody (occInfo :OccupantInfo) :void
    {
        if (!shouldLoadAll()) {
            return;
        }

        // TODO: handle viewonly occupants

        var bodyOid :int = occInfo.getBodyOid();
        var sloc :SceneLocation = (_roomObj.occupantLocs.get(bodyOid) as SceneLocation);
        var loc :MsoyLocation = (sloc.loc as MsoyLocation);

        // see if the occupant was already created, pending removal
        var occupant :OccupantSprite = (_pendingRemovals.remove(bodyOid) as OccupantSprite);

        if (occupant == null) {
            occupant = _ctx.getMediaDirector().getSprite(occInfo);
            occupant.setChatOverlay(chatOverlay);
            _occupants.put(bodyOid, occupant);
            addChildAt(occupant, 1);
            occupant.setEntering(loc);

            // if we ever add ourselves, we follow it
            if (bodyOid == _ctx.getClient().getClientOid()) {
                setFastCentering(true);
                setCenterSprite(occupant);
            }

        } else {
            // place the sprite back into the set of active sprites
            _occupants.put(bodyOid, occupant);
            occupant.setChatOverlay(chatOverlay);
            occupant.moveTo(loc, _scene);
        }

        // map the occupant sprite in the entities table
        if (occupant is ActorSprite) {
            var ident :ItemIdent = (occInfo as ActorInfo).getItemIdent();
            _entities.put(ident, occupant);

            // if this occupant is a pet, notify GWT that we've got a new pet in the room.
            if (occupant is PetSprite) {
                (_ctx.getClient() as WorldClient).dispatchEventToGWT(
                    PET_EVENT, [true, ident.itemId]);
            }
        }
    }

    protected function removeBody (bodyOid :int) :void
    {
        var sprite :OccupantSprite = (_occupants.remove(bodyOid) as OccupantSprite);
        if (sprite != null) {
            if (sprite.isMoving()) {
                _pendingRemovals.put(bodyOid, sprite);
            } else {
                removeSprite(sprite);
            }

            // if this occupant is a pet, notify GWT that we've removed a pet from the room.
            if (sprite is PetSprite) {
                (_ctx.getClient() as WorldClient).dispatchEventToGWT(
                    PET_EVENT, [false, sprite.getItemIdent().itemId]);
            }
        }
    }

    protected function moveBody (bodyOid :int) :void
    {
        var sprite :OccupantSprite = (_occupants.get(bodyOid) as OccupantSprite);
        var sloc :SceneLocation = (_roomObj.occupantLocs.get(bodyOid) as SceneLocation);
        sprite.moveTo(sloc.loc as MsoyLocation, _scene);
    }

    protected function updateBody (newInfo :OccupantInfo, oldInfo :OccupantInfo) :void
    {
        var sprite :OccupantSprite = (_occupants.get(newInfo.getBodyOid()) as OccupantSprite);
        if (sprite == null) {
// TODO: ?
//             if (newInfo is ActorInfo) {
//                 log.warning("No sprite for updated occupant? [info=" + newInfo + "].");
//             }
            return;
        }
        sprite.setOccupantInfo(newInfo);

        // update the entities table
        if (newInfo is ActorInfo) {
            _entities.remove((oldInfo as ActorInfo).getItemIdent());
            _entities.put((newInfo as ActorInfo).getItemIdent(), sprite);
        }
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
        if (!shouldLoadAll()) {
            return;
        }

        // add all currently present occupants
        for (var ii :int = _roomObj.occupantInfo.size() - 1; ii >= 0; ii--) {
            var occInfo :OccupantInfo = (_roomObj.occupantInfo.get(ii) as OccupantInfo);
            if (!_occupants.containsKey(occInfo.getBodyOid())) {
                addBody(occInfo);
            }
        }
    }

    protected function removeAllOccupants () :void
    {
        removeAll(_occupants);
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

    /** A map of bodyOid -> OccupantSprite. */
    protected var _occupants :HashMap = new HashMap();

    /** Maps ItemIdent -> MsoySprite for entities (furni, avatars, pets). */
    protected var _entities :HashMap = new HashMap();

    /** Maps effect id -> EffectData for effects. */
    protected var _effects :HashMap = new HashMap();

    /** The sprite we should center on. */
    protected var _centerSprite :MsoySprite;

    /** A map of bodyOid -> OccupantSprite for those that we'll remove when they stop moving. */
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
