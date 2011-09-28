//
// $Id$

package com.threerings.msoy.room.client {

import flash.display.Sprite;
import flash.events.Event;
import flash.geom.Point;
import flash.geom.Rectangle;
import flash.utils.ByteArray;

import mx.managers.PopUpManager;

import com.threerings.util.Arrays;
import com.threerings.util.Comparators;
import com.threerings.util.ImmutableProxyObject;
import com.threerings.util.Map;
import com.threerings.util.Name;
import com.threerings.util.Predicates;
import com.threerings.util.ValueEvent;

import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.MessageEvent;
import com.threerings.presents.dobj.MessageListener;
import com.threerings.presents.dobj.SetListener;

import com.threerings.crowd.chat.client.ChatDisplay;
import com.threerings.crowd.chat.client.ChatSnooper;
import com.threerings.crowd.chat.data.ChatMessage;
import com.threerings.crowd.chat.data.UserMessage;
import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.media.AudioPlayer;
import com.threerings.media.MediaPlayerCodes;

import com.threerings.ui.MenuUtil;

import com.threerings.whirled.data.SceneUpdate;
import com.threerings.whirled.spot.data.SceneLocation;
import com.threerings.whirled.spot.data.SpotSceneObject;

import com.threerings.msoy.avrg.client.AVRGameBackend;
import com.threerings.msoy.chat.client.ChatInfoProvider;
import com.threerings.msoy.chat.client.ComicOverlay;
import com.threerings.msoy.chat.data.MsoyChatChannel;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyClient;
import com.threerings.msoy.client.MsoyController;
import com.threerings.msoy.client.PlaceLoadingDisplay;
import com.threerings.msoy.client.Prefs;
import com.threerings.msoy.client.TopPanel;
import com.threerings.msoy.client.UberClient;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.item.data.all.Audio;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.all.Item_UsedAs;
import com.threerings.msoy.room.data.ControllableEntity;
import com.threerings.msoy.room.data.Deejay;
import com.threerings.msoy.room.data.EntityControl;
import com.threerings.msoy.room.data.EntityMemories;
import com.threerings.msoy.room.data.FurniUpdate_Remove;
import com.threerings.msoy.room.data.MemberInfo;
import com.threerings.msoy.room.data.MemoryChangedListener;
import com.threerings.msoy.room.data.MobInfo;
import com.threerings.msoy.room.data.MsoyLocation;
import com.threerings.msoy.room.data.MsoyScene;
import com.threerings.msoy.room.data.RoomCodes;
import com.threerings.msoy.room.data.RoomObject;
import com.threerings.msoy.room.data.SceneAttrsUpdate;
import com.threerings.msoy.room.data.SceneOwnershipUpdate;
import com.threerings.msoy.world.client.WorldClient;
import com.threerings.msoy.world.client.WorldContext;

/**
 * Extends the base roomview with the ability to view a RoomObject, view chat, and edit.
 */
public class RoomObjectView extends RoomView
    implements AttributeChangeListener, SetListener, MessageListener,
               ChatSnooper, ChatDisplay, ChatInfoProvider,
               MemoryChangedListener
{
    /**
     * Create a roomview.
     */
    public function RoomObjectView (ctx :WorldContext, ctrl :RoomObjectController)
    {
        super(ctx, ctrl);
        _octrl = ctrl;
    }

    public function getRoomObjectController () :RoomObjectController
    {
        return _octrl;
    }

    /**
     * Have we finished loading all the furni/decor in this room?
     * Note that adding new furniture to the room may cause this to return false.
     */
    public function loadingDone () :Boolean
    {
        return (_loadingWatcher != null) && _loadingWatcher.isFinished();
    }

    // from MsoyPlaceView, via RoomView
    override public function setPlaceSize (unscaledWidth :Number, unscaledHeight :Number) :void
    {
        super.setPlaceSize(unscaledWidth, unscaledHeight);
        updateEditingOverlay();
    }

    // from MsoyPlaceView, via RoomView
    override public function getBackgroundColor () :uint
    {
        if (_scene != null) {
            return _scene.getBackgroundColor();
        }
        return 0x000000;
    }

    // from Zoomable, via RoomView
    override public function getZoom () :String
    {
        if (_zoom == null) {
            _zoom = Prefs.getRoomZoom();
        }
        return _ctx.getMsoyClient().isChromeless() ? FIT_WIDTH : super.getZoom();
    }

    // from Zoomable, via RoomView
    override public function setZoom (zoom :String) :void
    {
        Prefs.setRoomZoom(zoom);
        super.setZoom(zoom);
    }

    /**
     * (Re)set our scene to the one the scene director knows about.
     */
    public function rereadScene () :void
    {
        setScene(_ctx.getSceneDirector().getScene() as MsoyScene);
    }

    /**
     * Called by the editor to have direct access to our sprite list..
     */
    public function getFurniSprites () :Map
    {
        return _furni;
    }

    override public function setScene (scene :MsoyScene) :void
    {
        super.setScene(scene);
        updateEditingOverlay();

        // let the place box know that the frame background color may have changed (if using the
        // room background as frame background)
        _ctx.getTopPanel().getPlaceContainer().updateFrameBackgroundColor();
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
                avatar.setOccupantInfo(occInfo, _roomObj);
            }
        }
    }

    /**
     * Enable or disable editing.
     */
    public function setEditing (editing :Boolean) :void
    {
        _editing = editing;

        // update all sprites
        _furni.forEach(function (key :*, sprite :EntitySprite) :void {
            sprite.setEditing(_editing);
        });
        if (_bg != null) {
            _bg.setEditing(_editing);
        }

        showBackdropOverlay(_editing);
        updateEditingOverlay();
        if (!_editing) {
            // definitely update the furni
            updateAllFurni();
        }

        // if we haven't yet started loading sprites other than the background, start now
        if (!_loadAllMedia) {
            _loadAllMedia = true;
            updateAllFurni();
        }
    }

    /**
     * Refreshes the overlay used to draw the room edges in editing mode.
     */
    protected function updateEditingOverlay () :void
    {
        // if the overlay exists, then we should update it
        if (_backdropOverlay != null) {
            _backdrop.drawRoom(
                _backdropOverlay.graphics, _actualWidth, _actualHeight, true, false, 0.4);
            _layout.updateScreenLocation(_backdropOverlay);
        }
    }

    /**
     * Called by our controller when a scene update is received.
     */
    public function processUpdate (update :SceneUpdate) :void
    {
        if (update is FurniUpdate_Remove) {
            removeFurni((update as FurniUpdate_Remove).data);

        } else if (update is SceneAttrsUpdate) {
            rereadScene(); // re-read our scene
            updateBackground();

        } else if (update is SceneOwnershipUpdate) {
            rereadScene();
        }

        // this will take care of anything added
        updateAllFurni();
    }

    /**
     * A convenience function to get our personal avatar.
     */
    override public function getMyAvatar () :MemberSprite
    {
        return (getOccupant(_ctx.getMemberObject().getOid()) as MemberSprite);
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
        if (_roomObj == null) {
            return null;
        }

        var occInfo :OccupantInfo = _roomObj.getOccupantInfo(name);
        return (occInfo == null) ? null : getOccupant(occInfo.bodyOid);
    }

    public function getOccupantByMemberId (memberId :int) :OccupantSprite
    {
        if (_roomObj == null) {
            return null;
        }

        var memInfo :MemberInfo = _roomObj.getMemberInfo(memberId);
        return (memInfo == null) ? null : getOccupant(memInfo.bodyOid);
    }

    /**
     * A convenience function to get an array of all sprites for all pets in the room.
     */
    public function getPets () :Array /* of PetSprite */
    {
        return _occupants.values().filter(Predicates.createIs(PetSprite));
    }

    // from interface AttributeChangedListener
    public function attributeChanged (event :AttributeChangedEvent) :void
    {
        switch (event.getName()) {
        case RoomObject.PLAY_COUNT:
            // when the play count changes, it's time for us to re-check the audio
            updateBackgroundAudio();
            break;

        case RoomObject.TRACK:
            updateTrackOverlay();
            break;

        case RoomObject.DJS:
            updateCrownHolder();
            break;

        case RoomObject.CURRENT_DJ:
            updateCurrentDj();
            break;
        }
    }

    // from interface SetListener
    public function entryAdded (event :EntryAddedEvent) :void
    {
        switch (event.getName()) {
        case PlaceObject.OCCUPANT_INFO:
            addBody(event.getEntry() as OccupantInfo);
            break;

        case SpotSceneObject.OCCUPANT_LOCS:
            var sceneLoc :SceneLocation = (event.getEntry() as SceneLocation);
            portalTraversed(sceneLoc.loc, true);
            break;

        case RoomObject.MEMORIES:
            var entry :EntityMemories = event.getEntry() as EntityMemories;
            entry.memories.forEach(function (key :String, value :ByteArray) :void {
                dispatchMemoryChanged(entry.ident, key, value);
            });
            break;

        case RoomObject.CONTROLLERS:
            var ctrl :EntityControl = (event.getEntry() as EntityControl);
            if (ctrl.controllerOid == _ctx.getMemberObject().getOid()) {
                if (ctrl.controlled is ControllableEntity) {
                    _ctrl.dispatchEntityGotControl(
                        (ctrl.controlled as ControllableEntity).getItemIdent());

                } else {
                    log.warning("Did not expect to receive control of unknown type",
                        "controlled", ctrl.controlled, "controllerOid", ctrl.controllerOid);
                }
            }
            break;

        case RoomObject.PLAYLIST:
            var audio :Audio = event.getEntry() as Audio;
            if (audio.ownerId == _ctx.getMyId()) {
                _ctx.getMsoyClient().itemUsageChangedToGWT(
                    Item.AUDIO, audio.itemId, audio.used, audio.location);
            }
            break;

        case RoomObject.DJS:
            updateCrownHolder();
            break;
        }
    }

    // from interface SetListener
    public function entryUpdated (event :EntryUpdatedEvent) :void
    {
        switch (event.getName()) {
        case PlaceObject.OCCUPANT_INFO:
            updateBody(event.getEntry() as OccupantInfo, event.getOldEntry() as OccupantInfo);
            break;

        case SpotSceneObject.OCCUPANT_LOCS:
            moveBody((event.getEntry() as SceneLocation).bodyOid);
            break;

        case RoomObject.MEMORIES:
            // TODO: this presently should not happen, but we cope and treat it like an add
            var entry :EntityMemories = event.getEntry() as EntityMemories;
            entry.memories.forEach(function (key :String, value :ByteArray) :void {
                dispatchMemoryChanged(entry.ident, key, value);
            });
            break;

        case RoomObject.DJS:
            updateCrownHolder();
            break;
        }
    }

    // from interface SetListener
    public function entryRemoved (event :EntryRemovedEvent) :void
    {
        switch (event.getName()) {
        case PlaceObject.OCCUPANT_INFO:
            removeBody((event.getOldEntry() as OccupantInfo).getBodyOid());
            break;

        case RoomObject.PLAYLIST:
            var audio :Audio = event.getOldEntry() as Audio;
            if (audio.ownerId == _ctx.getMyId()) {
                // always assume that if we're present to see a remove, it's unused
                _ctx.getMsoyClient().itemUsageChangedToGWT(
                    Item.AUDIO, audio.itemId, Item_UsedAs.NOTHING, 0);
            }
            break;

        case RoomObject.DJS:
            updateCrownHolder();
            break;
        }
    }

    // from interface MemoryChangedListener
    public function memoryChanged (ident :ItemIdent, key :String, value :ByteArray) :void
    {
        dispatchMemoryChanged(ident, key, value);
    }

    // from interface MessageListener
    public function messageReceived (event :MessageEvent) :void
    {
        var args :Array = event.getArgs();
        switch (event.getName()) {
        case RoomCodes.SPRITE_MESSAGE:
            dispatchSpriteMessage((args[0] as ItemIdent), (args[1] as String),
                                  (args[2] as ByteArray), (args[3] as Boolean));
            break;
        case RoomCodes.SPRITE_SIGNAL:
            dispatchSpriteSignal((args[0] as String), (args[1] as ByteArray));
            break;
        }
    }

    // from ChatInfoProvider
    public function getBubblePosition (speaker :Name) :Point
    {
        var sprite :OccupantSprite = getOccupantByName(speaker);
        return (sprite == null) ? null : sprite.getBubblePosition();
    }

    // from ChatDisplay
    public function clear () :void
    {
        // nada
    }

    // from ChatDisplay
    public function displayMessage (msg :ChatMessage) :void
    {
        // we don't do this in snoopChat in case the message was filtered into nothing
        var avatar :MemberSprite = getSpeaker(msg) as MemberSprite;
        if (avatar != null) {
            avatar.performAvatarSpoke();
        }
    }

    // from ChatSnooper
    public function snoopChat (msg :ChatMessage) :void
    {
        var speaker :OccupantSprite = getSpeaker(msg);
        if (speaker != null) {
            // send it to all entities
            var ident :String = speaker.getItemIdent().toString();
            var name :String = speaker.getOccupantInfo().username.toString();
            for each (var entity :EntitySprite in _entities.values()) {
                entity.processChatMessage(ident, name, msg.message);
            }

            // TODO: ugh
            // What should happen is that we should do many operations straight on the backend
            // (not on the sprite)
            // then, all the backends can be iterated.
            // Then, when an avrg is running, it just adds its own entity backend to the list
            // and it's iterated and called just like everything else
            // and we won't forget to add methods to avrg.
            var avrg :AVRGameBackend = _ctx.getGameDirector().getAVRGameBackend();
            if (avrg != null) {
                avrg.processChatMessage(ident, msg.message);
            }
        }
    }

    // from RoomView
    override public function willEnterPlace (plobj :PlaceObject) :void
    {
        // set load-all to false, as we're going to just load the decor item first.
        _loadAllMedia = false;
        _loadingWatcher = new PlaceLoadingDisplay(_ctx.getTopPanel().getPlaceContainer());
        FurniSprite.setLoadingWatcher(_loadingWatcher);

        // save our scene object
        _roomObj = (plobj as RoomObject);

        rereadScene();
        updateAllFurni();

        // listen for client minimization events
        _ctx.getClient().addEventListener(MsoyClient.MINI_WILL_CHANGE, miniWillChange);

        _roomObj.addListener(this);

        var player :AudioPlayer = _ctx.getWorldController().getMusicPlayer();
        player.addEventListener(MediaPlayerCodes.STATE, handleMusicStateChanged);
        player.addEventListener(MediaPlayerCodes.METADATA, handleMusicMetadata);

        addAllOccupants();

        // we add ourselves as a chat display so that we can trigger speak actions on avatars
        _ctx.getChatDirector().addChatSnooper(this);
        _ctx.getChatDirector().addChatDisplay(this);
        _ctx.getUIState().setInRoom(true);

        // let the chat overlay know about us so we can be queried for chatter locations
        var comicOverlay :ComicOverlay = _ctx.getTopPanel().getPlaceChatOverlay();
        if (comicOverlay != null) {
            comicOverlay.willEnterPlace(this);
        }

        // and animate ourselves entering the room (everyone already in the room will also have
        // seen it)
        portalTraversed(getMyCurrentLocation(), true);

        // load the background image first
        setBackground(_scene.getDecor());
        // load the decor data we have, even if it's just default values.
        _bg.setLoadedCallback(backgroundFinishedLoading);

        var localOccupant :MemberInfo;
        localOccupant = _roomObj.occupantInfo.get(_ctx.getMemberObject().getOid()) as MemberInfo;
        if (localOccupant != null && localOccupant.isStatic()) {
            _ctx.displayInfo(MsoyCodes.GENERAL_MSGS, "m.static_avatar");
        }

        updateTrackOverlay();
    }

    // from RoomView
    override public function didLeavePlace (plobj :PlaceObject) :void
    {
        _roomObj.removeListener(this);

        _ctx.getUIState().setInRoom(false);
        // stop listening for avatar speak action triggers
        _ctx.getChatDirector().removeChatDisplay(this);
        _ctx.getChatDirector().removeChatSnooper(this);

        // tell the comic overlay to forget about us
        var comicOverlay :ComicOverlay = _ctx.getTopPanel().getPlaceChatOverlay();
        if (comicOverlay != null) {
            comicOverlay.didLeavePlace(this);
        }

        // stop listening for client minimization events
        _ctx.getClient().removeEventListener(MsoyClient.MINI_WILL_CHANGE, miniWillChange);

        removeAllOccupants();

        var player :AudioPlayer = _ctx.getWorldController().getMusicPlayer();
        player.removeEventListener(MediaPlayerCodes.STATE, handleMusicStateChanged);
        player.removeEventListener(MediaPlayerCodes.METADATA, handleMusicMetadata);
        _ctx.getWorldController().handlePlayMusic(null);

        super.didLeavePlace(plobj);

        _roomObj = null;

        _loadingWatcher = null;
        FurniSprite.setLoadingWatcher(null);

        // in case we were auto-scrolling, remove the event listener..
        removeEventListener(Event.ENTER_FRAME, tick);

        if (_trackOverlay != null) {
            _trackOverlay.parent.removeChild(_trackOverlay);
            _trackOverlay = null;
        }
        if (_crown != null) {
            _crown.shutdown();
            _crown = null;
        }
    }

    // from RoomView
    override public function set scrollRect (r :Rectangle) :void
    {
        super.scrollRect = r;
        var overlay :ComicOverlay = _ctx.getTopPanel().getPlaceChatOverlay();
        if (overlay != null) {
            overlay.setScrollRect(r);
        }
    }

    // documentation inherited
    override protected function populateSpriteContextMenu (
        sprite :EntitySprite, menuItems :Array) :void
    {
        var ident :ItemIdent = sprite.getItemIdent();
        if (ident != null) {
            var kind :String = Msgs.GENERAL.get(sprite.getDesc());
            if (ident.type > Item.NOT_A_TYPE) { // -1 is used for the default avatar, etc.
                menuItems.push(MenuUtil.createCommandContextMenuItem(
                    Msgs.GENERAL.get("b.view_item", kind), MsoyController.VIEW_ITEM, ident));
                if (!_ctx.getMemberObject().isPermaguest()) {
                    menuItems.push(MenuUtil.createCommandContextMenuItem(
                        Msgs.GENERAL.get("b.flag_item", kind), MsoyController.FLAG_ITEM, ident));
                }
            }

            if (sprite.viz.isBleepable()) {
                var isBleeped :Boolean = sprite.viz.isBleeped();
                menuItems.push(MenuUtil.createCommandContextMenuItem(
                    Msgs.GENERAL.get((isBleeped ? "b.unbleep_item" : "b.bleep_item"), kind),
                    sprite.viz.toggleBleeped, _ctx));
            }
        }

        super.populateSpriteContextMenu(sprite, menuItems);
    }

    /**
     * Return the sprite of the speaker of the specified message.
     */
    protected function getSpeaker (msg :ChatMessage) :OccupantSprite
    {
        if (msg is UserMessage && MsoyChatChannel.typeIsForRoom(msg.localtype, _scene.getId())) {
            return getOccupantByName(UserMessage(msg).getSpeakerDisplayName());
        }
        return null;
    }

    /** Return an array of the MOB sprites associated with the identified game. */
    public function getMobs (gameId :int) :Array
    {
        var result :Array = new Array();
        for each (var occInfo :OccupantInfo in _roomObj.occupantInfo.toArray()) {
            if (occInfo is MobInfo && MobInfo(occInfo).getGameId() == gameId) {
                var sprite :MobSprite = (_occupants.get(occInfo.getBodyOid()) as MobSprite);
                if (sprite) {
                    result.push(sprite);
                }
            }
        }
        return result;
    }

    /** Return a uniquely identified MOB associated with the given game, or null. */
    public function getMob (gameId :int, mobId :String) :MobSprite
    {
        for each (var occInfo :OccupantInfo in _roomObj.occupantInfo.toArray()) {
            if (occInfo is MobInfo && MobInfo(occInfo).getGameId() == gameId &&
                MobInfo(occInfo).getIdent() == mobId) {
                var sprite :MobSprite = (_occupants.get(occInfo.getBodyOid()) as MobSprite);
                if (sprite) {
                    return sprite;
                }
            }
        }
        return null;
    }

    /** Signals that an AVRG has started (gameId != 0) or ended (gameId == 0). */
    public function avrGameAvailable (gameId :int) :void
    {
        for each (var occInfo :OccupantInfo in _roomObj.occupantInfo.toArray()) {
            if (occInfo is MobInfo) {
                if (gameId == 0) {
                    removeBody(occInfo.getBodyOid());

                } else if (MobInfo(occInfo).getGameId() == gameId) {
                    addBody(occInfo);
                }
            }
        }
    }

    override protected function backgroundFinishedLoading () :void
    {
        super.backgroundFinishedLoading();

        // play any music..
        updateBackgroundAudio();

        _octrl.backgroundFinishedLoading();
    }

    /**
     * Called when the client is minimized or unminimized.
     */
    protected function miniWillChange (event :ValueEvent) :void
    {
        relayout();
    }

    override protected function relayout () :void
    {
        super.relayout();

        if (UberClient.isFeaturedPlaceView()) {
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
    }

    /**
     * Restart playing the background audio.
     */
    protected function updateBackgroundAudio () :void
    {
        var audio :Audio = _roomObj.inDjMode() ?
            _roomObj.track.audio :
            _roomObj.playlist.get(new ItemIdent(Item.AUDIO, _roomObj.currentSongId)) as Audio;
        const dispatchStopped :Boolean = (_musicPlayCount >= 0);
        const dispatchStarted :Boolean = (audio != null);
        var avrg :AVRGameBackend = _ctx.getGameDirector().getAVRGameBackend();
        var entity :EntitySprite;
        if (dispatchStopped) {
            for each (entity in _entities.values()) {
                entity.processMusicStartStop(false);
            }
            if (avrg != null) {
                avrg.processMusicStartStop(false);
            }
        }

        // play the new music
        _ctx.getWorldController().handlePlayMusic(audio);
        _musicPlayCount = _roomObj.playCount;

        if (dispatchStarted) {
            for each (entity in _entities.values()) {
                entity.processMusicStartStop(true);
            }
            if (avrg != null) {
                avrg.processMusicStartStop(true);
            }
        }
    }

    protected function updateTrackOverlay () :void
    {
        if (_roomObj.inDjMode()) {
            if (_trackOverlay == null) {
                _trackOverlay = new TrackOverlay(_ctx, _roomObj);

                // Why doesn't this work?
                // _ctx.getTopPanel().getPlaceContainer().addOverlay(
                //     _trackOverlay, 9999999);

                // Auto positioning doesn't work with addPopUp
                // PopUpManager.addPopUp(_trackOverlay, _ctx.getTopPanel());

                // Just slap it on the top panel and let Flex its positioning magic
                // TODO(bruno): Make this a proper overlay
                var top :TopPanel = _ctx.getTopPanel();
                _trackOverlay.setStyle("bottom", top.getControlBarHeight() - 2);
                top.addChild(_trackOverlay);
            }

        } else if (_trackOverlay != null) {
            _trackOverlay.parent.removeChild(_trackOverlay);
            _trackOverlay = null;
        }
    }

    // Revalidate who has the crown for being the best DJ
    protected function updateCrownHolder () :void
    {
        var best :Deejay = (_roomObj == null) ? null :
            Arrays.max(_roomObj.djs.toArray(), function (a :Deejay, b :Deejay) :int {
                return Comparators.compareInts(a.lastRating, b.lastRating);
            });

        // Possibly oust the old king
        if (_crown != null && (best == null || best.memberId != _crown.avatar.getMemberId())) {
            _crown.shutdown();
            _crown = null;
        }

        if (best == null || best.lastRating == int.MIN_VALUE) {
            return; // Don't crown players who haven't been rated
        }

        // Possibly crown the new king
        if (_crown == null) {
            var newHolder :MemberSprite = MemberSprite(getOccupantByMemberId(best.memberId));
            if (newHolder != null) {
                _crown = new CrownDecoration(newHolder);
            }
        }

        if (_crown != null) {
            _crown.setRating(best.lastRating);
        }
    }

    protected function updateCurrentDj () :void
    {
        if (_spotlight != null) {
            _spotlight.shutdown();
            _spotlight = null;
        }

        if (_roomObj.currentDj != 0) {
            var avatar :OccupantSprite = getOccupantByMemberId(_roomObj.currentDj);
            if (avatar != null) {
                _spotlight = new Spotlight(this, avatar);
            }
        }
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
            occupant = _ctx.getMediaDirector().getSprite(occInfo, _roomObj);
            if (occupant == null) {
                return; // we have no visualization for this kind of occupant, no problem
            }

            var overlay :ComicOverlay = _ctx.getTopPanel().getPlaceChatOverlay();
            if (overlay != null) {
                occupant.setChatOverlay(overlay as ComicOverlay);
            }
            _occupants.put(bodyOid, occupant);
            addSprite(occupant);
            dispatchEntityEntered(occupant.getItemIdent());
            occupant.setEntering(loc);
            occupant.roomScaleUpdated();

            // if we ever add ourselves, we follow it
            if (bodyOid == _ctx.getMemberObject().getOid()) {
                setFastCentering(true);
                setCenterSprite(occupant);
            }

        } else {
            // update the sprite
            spriteWillUpdate(occupant);
            occupant.setOccupantInfo(occInfo, _roomObj);
            spriteDidUpdate(occupant);

            // place the sprite back into the set of active sprites
            _occupants.put(bodyOid, occupant);
            overlay = _ctx.getTopPanel().getPlaceChatOverlay();
            if (overlay != null) {
                occupant.setChatOverlay(overlay);
            }
            occupant.moveTo(loc, _scene);
        }

        // if this occupant is a pet, notify GWT that we've got a new pet in the room.
        if (occupant is PetSprite) {
            var ident :ItemIdent = occupant.getItemIdent();
            (_ctx.getClient() as WorldClient).itemUsageChangedToGWT(
                Item.PET, ident.itemId, Item_UsedAs.PET, _scene.getId());
        }

        // If this occupant belongs to another player that isn't our friend, squelch it
        var memberId :int = occupant.getMemberId();
        if (memberId > 0 && memberId != _ctx.getMyId() &&
                !_ctx.getMemberObject().isOnlineFriend(memberId)) {
            occupant.squelch();
        }
    }

    protected function removeBody (bodyOid :int) :void
    {
        var sprite :OccupantSprite = (_occupants.remove(bodyOid) as OccupantSprite);
        if (sprite == null) {
            return;
        }
        if (sprite.isMoving()) {
            _pendingRemovals.put(bodyOid, sprite);
        } else {
            dispatchEntityLeft(sprite.getItemIdent());
            removeSprite(sprite);
        }

        // if this occupant is a pet, notify GWT that we've removed a pet from the room.
        if (sprite is PetSprite) {
            (_ctx.getClient() as WorldClient).itemUsageChangedToGWT(
                Item.PET, sprite.getItemIdent().itemId, Item_UsedAs.NOTHING, 0);
        }
    }

    protected function moveBody (bodyOid :int) :void
    {
        var sprite :OccupantSprite = (_occupants.get(bodyOid) as OccupantSprite);
        if (sprite == null) {
            // It's possible to get an occupant update while we're still loading the room
            // and haven't yet set up the occupant's sprite. Ignore.
            return;
        }
        var sloc :SceneLocation = (_roomObj.occupantLocs.get(bodyOid) as SceneLocation);
        sprite.moveTo(sloc.loc as MsoyLocation, _scene);
    }

    protected function updateBody (newInfo :OccupantInfo, oldInfo :OccupantInfo) :void
    {
        var sprite :OccupantSprite = (_occupants.get(newInfo.getBodyOid()) as OccupantSprite);
        if (sprite == null) {
            // It's possible to get an occupant update while we're still loading the room
            // and haven't yet set up the occupant's sprite. Ignore.
            return;
        }
        spriteWillUpdate(sprite);
        sprite.setOccupantInfo(newInfo, _roomObj);
        spriteDidUpdate(sprite);
    }

    override protected function addAllOccupants () :void
    {
        if (!shouldLoadAll()) {
            return;
        }

        // add all currently present occupants
        for each (var occInfo :OccupantInfo in _roomObj.occupantInfo.toArray()) {
            if (!_occupants.containsKey(occInfo.getBodyOid())) {
                addBody(occInfo);
            }
        }

        updateCurrentDj();
        updateCrownHolder();
    }

    override protected function removeSprite (sprite :EntitySprite) :void
    {
        super.removeSprite(sprite);

        if (sprite is MobSprite) {
            MobSprite(sprite).removed();
        }
    }

    /**
     * Called after we've told the music player about our music and the state changes.
     */
    protected function handleMusicStateChanged (event :ValueEvent) :void
    {
        if (event.value == MediaPlayerCodes.STATE_STOPPED) {
            _roomObj.roomService.songEnded(_musicPlayCount);
        }
    }

    /**
     * Dispatch music metadata to all entites.
     */
    protected function handleMusicMetadata (event :ValueEvent) :void
    {
        var metadata :Object = new ImmutableProxyObject(event.value);
        for each (var entity :EntitySprite in _entities.values()) {
            entity.processMusicId3(metadata);
        }
        var avrg :AVRGameBackend = _ctx.getGameDirector().getAVRGameBackend();
        if (avrg != null) {
            avrg.processMusicId3(metadata);
        }
    }

    override public function getMusicId3 () :Object
    {
        var metadata :Object = _ctx.getWorldController().getMusicPlayer().getMetadata();
        // if non-null, wrap it up to prevent the users from fucking it up
        return (metadata == null) ? null : new ImmutableProxyObject(metadata);
    }

    override public function getMusicOwner () :int
    {
        var audio :Audio =
            _roomObj.playlist.get(new ItemIdent(Item.AUDIO, _roomObj.currentSongId)) as Audio;
        return (audio == null) ? 0 : audio.ownerId;
    }

    /** _ctrl, casted as a RoomObjectController. */
    protected var _octrl :RoomObjectController;

    protected var _trackOverlay :TrackOverlay;
    protected var _crown :CrownDecoration;

    /** The transitory properties of the current scene. */
    protected var _roomObj :RoomObject;

    /** Monitors and displays loading progress for furni/decor. */
    protected var _loadingWatcher :PlaceLoadingDisplay;

    /** The id of the current music being played from the room's playlist. */
    protected var _musicPlayCount :int = -1; // -1 means nothing is playing

    /** The currently shown spotlight effect, if any. */
    protected var _spotlight :Spotlight;
}
}
