package com.threerings.msoy.world.client {

import flash.display.DisplayObject;
import flash.display.Graphics;

import flash.events.Event;

import flash.geom.Point;
import flash.geom.Rectangle;

import flash.utils.getTimer; // function import

import mx.containers.Canvas;

import mx.controls.VideoDisplay;

import mx.core.UIComponent;
import mx.core.ScrollPolicy;

import mx.events.FlexEvent;
import mx.events.ResizeEvent;

import com.threerings.util.HashMap;
import com.threerings.util.Iterator;

import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.SetListener;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.crowd.chat.client.ChatDisplay;
import com.threerings.crowd.chat.data.ChatMessage;
import com.threerings.crowd.chat.data.UserMessage;

import com.threerings.whirled.spot.data.Location;
import com.threerings.whirled.spot.data.Portal;
import com.threerings.whirled.spot.data.SpotSceneObject;
import com.threerings.whirled.spot.data.SceneLocation;

import com.threerings.msoy.chat.client.ChatPopper;
import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.client.Prefs;
import com.threerings.msoy.data.MediaData;
import com.threerings.msoy.data.MsoyOccupantInfo;
import com.threerings.msoy.world.data.FurniData;
import com.threerings.msoy.world.data.MsoyLocation;
import com.threerings.msoy.world.data.MsoyPortal;
import com.threerings.msoy.world.data.MsoyScene;

public class RoomView extends AbstractRoomView
    implements SetListener, ChatDisplay
{
    public function RoomView (ctx :MsoyContext)
    {
        super(ctx);
    }

    override protected function updateComplete (evt :FlexEvent) :void
    {
        ChatPopper.setChatView(this);
        super.updateComplete(evt);
    }

    override protected function relayout () :void
    {
        super.relayout();

        var sprite :MsoySprite;
        for each (sprite in _avatars.values()) {
            locationUpdated(sprite);
        }
        for each (sprite in _pendingRemoveAvatars.values()) {
            locationUpdated(sprite);
        }
    }

    override public function setEditing (
            editing :Boolean, spriteVisitFn :Function) :void
    {
        super.setEditing(editing, spriteVisitFn);

        // we hide all avatars instead of visiting them.
        if (editing) {
            _roomObj.removeListener(this);
            removeAllOccupants();

        } else {
            _roomObj.addListener(this)
            addAllOccupants();
        }
    }

    override public function locationUpdated (sprite :MsoySprite) :void
    {
        super.locationUpdated(sprite);

        // if we moved our own sprite, possibly update the scroll position
        if (sprite is AvatarSprite) {
            var avatar :AvatarSprite = (sprite as AvatarSprite);
            if (avatar.getOid() == _ctx.getClient().getClientOid()) {
                scrollView(avatar);
            }
        }
    }

    /**
     * A callback from avatar sprites.
     */
    public function moveFinished (avatar :AvatarSprite) :void
    {
        if (null != _pendingRemoveAvatars.remove(avatar.getOid())) {
            removeSprite(avatar);
        }
    }

    public function dimAvatars (setDim :Boolean) :void
    {
        setActive(_avatars, !setDim);
        setActive(_pendingRemoveAvatars, !setDim);
    }

    public function dimPortals (setDim :Boolean) :void
    {
        setActive(_portals, !setDim);
    }

    public function dimFurni (setDim :Boolean) :void
    {
        setActive(_furni, !setDim);
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

    protected function scrollView (center :AvatarSprite) :void
    {
        var rect :Rectangle = scrollRect;
        if (rect == null) {
            // return if there's nothing to scroll
            return;
        }

        var centerX :int = center.x + center.hotSpot.x;
        var newX :Number = Math.min(_scene.getWidth() - rect.width,
            Math.max(0, centerX - rect.width/2));
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
            var avatar :AvatarSprite = getMyAvatar();
            if (avatar != null) {
                scrollView(avatar);

            } else {
                // stop scrolling
                removeEventListener(Event.ENTER_FRAME, tick);
            }
        }

        // and finally, we want ensure it can happen on the next frame if
        // our avatar doesn't move
        _suppressAutoScroll = false;
    }

    public function getMyAvatar () :AvatarSprite
    {
        var oid :int = _ctx.getClient().getClientOid();
        var avatar :AvatarSprite = (_avatars.get(oid) as AvatarSprite);
        if (avatar == null) {
            avatar = (_pendingRemoveAvatars.get(oid) as AvatarSprite);
        }
        return avatar;
    }

    /**
     * Return the current location of the avatar that represents our body.
     */
    public function getMyCurrentLocation () :MsoyLocation
    {
        return getMyAvatar().loc;
    }

    /**
     * @return true if the specified click target should trigger
     * location movements.
     */
    public function isLocationTarget (clickTarget :DisplayObject) :Boolean
    {
        return (clickTarget == this) || (clickTarget == _bkgGraphics) ||
            (_bkg != null && _bkg.contains(clickTarget)) ||
            // scan through the media and see if it was non-interactive
            isNonInteractiveTarget(clickTarget, _furni) /*||
            isNonInteractiveTarget(clickTarget, _portals) ||
            isNonInteractiveTarget(clickTarget, _avatars)*/;
    }

    protected function isNonInteractiveTarget (
            clickTarget :DisplayObject, map :HashMap) :Boolean
    {
        for each (var spr :MsoySprite in map.values()) {
            if (!spr.isInteractive() && spr.contains(clickTarget)) {
                return true;
            }
        }
        return false;
    }

    protected function addBody (bodyOid :int) :void
    {
        var occInfo :MsoyOccupantInfo =
            (_roomObj.occupantInfo.get(bodyOid) as MsoyOccupantInfo);
        var sloc :SceneLocation =
            (_roomObj.occupantLocs.get(bodyOid) as SceneLocation);
        var loc :MsoyLocation = (sloc.loc as MsoyLocation);

        // see if the avatar was already created, pending removal
        var avatar :AvatarSprite =
            (_pendingRemoveAvatars.remove(bodyOid) as AvatarSprite);

        if (avatar == null) {
            avatar = _ctx.getMediaDirector().getAvatar(occInfo);
            _avatars.put(bodyOid, avatar);
            addChild(avatar);
            avatar.setLocation(loc);
            avatar.setOrientation(loc.orient);

        } else {
            // move the sprite back to the set of active sprites
            _avatars.put(bodyOid, avatar);
            avatar.moveTo(loc, _scene.getWidth());
        }
    }

    protected function removeBody (bodyOid :int) :void
    {
        var avatar :AvatarSprite = (_avatars.remove(bodyOid) as AvatarSprite);

        if (avatar != null) {
            if (avatar.isMoving()) {
                _pendingRemoveAvatars.put(bodyOid, avatar);

            } else {
                removeSprite(avatar);
            }
        }
    }

    protected function moveBody (bodyOid :int) :void
    {
        var avatar :AvatarSprite = (_avatars.get(bodyOid) as AvatarSprite);
        var sloc :SceneLocation =
            (_roomObj.occupantLocs.get(bodyOid) as SceneLocation);
        var loc :MsoyLocation = (sloc.loc as MsoyLocation);

        avatar.moveTo(loc, _scene.getWidth());
    }

    protected function updateBody (occInfo :MsoyOccupantInfo) :void
    {
        var avatar :AvatarSprite =
            (_avatars.get(occInfo.getBodyOid()) as AvatarSprite);
        if (avatar == null) {
            Log.getLog(this).warning("No avatar for updated occupantInfo? " +
                "[occInfo=" + occInfo + "].");
            return;
        }
        avatar.setOccupantInfo(occInfo);
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
                var sprite :PortalSprite =
                    (_portals.get(portal.portalId) as PortalSprite);
                sprite.wasTraversed(entering);
                return;
            }
        }
    }

    override protected function removeSprite (sprite :MsoySprite) :void
    {
        super.removeSprite(sprite);

        if (sprite is AvatarSprite) {
            var avatar :AvatarSprite = (sprite as AvatarSprite);
            portalTraversed(avatar.loc, false);
        }
    }

    // documentation inherited from interface PlaceView
    override public function willEnterPlace (plobj :PlaceObject) :void
    {
        super.willEnterPlace(plobj);

        _roomObj.addListener(this);

        _ctx.getChatDirector().addChatDisplay(this);

        // maybe set up background music
        var music :MediaData = _scene.getMusic();
        if (music != null) {

// TODO: playing music is causing freezing right now. Revisit.
//            _music = new SoundPlayer(music);
//            _music.loop(Prefs.getMediaPosition(music.id));
        }

        addAllOccupants();

        // and animate ourselves entering the room (everyone already in the
        // (room will also have seen it)
        portalTraversed(getMyCurrentLocation(), true);
    }

    // documentation inherited from interface PlaceView
    override public function didLeavePlace (plobj :PlaceObject) :void
    {
        _roomObj.removeListener(this);

        _ctx.getChatDirector().removeChatDisplay(this);
        ChatPopper.popAllDown();

        if (_music != null) {
            Prefs.setMediaPosition(_music.getMediaId(), _music.getPosition());
            _music.stop();
            _music = null;
        }

        removeAllOccupants();

        super.didLeavePlace(plobj);
    }

    protected function addAllOccupants () :void
    {
        // add all currently present occupants
        for (var ii :int = _roomObj.occupants.size() - 1; ii >= 0; ii--) {
            addBody(_roomObj.occupants.getAt(ii));
        }
    }

    protected function removeAllOccupants () :void
    {
        removeAll(_avatars);
        removeAll(_pendingRemoveAvatars);
    }

    // documentation inherited from interface SetListener
    public function entryAdded (event :EntryAddedEvent) :void
    {
        var name :String = event.getName();

        if (PlaceObject.OCCUPANT_INFO == name) {
            addBody((event.getEntry() as MsoyOccupantInfo).getBodyOid());

        } else if (SpotSceneObject.OCCUPANT_LOCS == name) {
            var sceneLoc :SceneLocation = (event.getEntry() as SceneLocation);
            portalTraversed(sceneLoc.loc, true);
        }
    }

    // documentation inherited from interface SetListener
    public function entryUpdated (event :EntryUpdatedEvent) :void
    {
        var name :String = event.getName();

        if (PlaceObject.OCCUPANT_INFO == name) {
            updateBody(event.getEntry() as MsoyOccupantInfo);

        } else if (SpotSceneObject.OCCUPANT_LOCS == name) {
            moveBody((event.getEntry() as SceneLocation).bodyOid);
        }
    }

    // documentation inherited from interface SetListener
    public function entryRemoved (event :EntryRemovedEvent) :void
    {
        var name :String = event.getName();

        if (PlaceObject.OCCUPANT_INFO == name) {
            removeBody((event.getOldEntry() as MsoyOccupantInfo).getBodyOid());
        }
    }

    // documentation inherited from interface ChatDisplay
    public function clear () :void
    {
        ChatPopper.popAllDown();
    }

    // documentation inherited from interface ChatDisplay
    public function displayMessage (msg :ChatMessage) :void
    {
        var avatar :AvatarSprite = null;
        if (msg is UserMessage) {
            var umsg :UserMessage = (msg as UserMessage);
            var occInfo :OccupantInfo = _roomObj.getOccupantInfo(umsg.speaker);
            if (occInfo != null) {
                avatar = (_avatars.get(occInfo.bodyOid) as AvatarSprite);
            }
        }

        ChatPopper.popUp(msg, avatar);
    }

    /** The background music in the scene. */
    protected var _music :SoundPlayer;

    /** A map of bodyOid -> AvatarSprite. */
    protected var _avatars :HashMap = new HashMap();

    /** A map of bodyOid -> AvatarSprite for those that we'll remove
     * when they stop moving. */
    protected var _pendingRemoveAvatars :HashMap = new HashMap();

    /** If true, the scrolling should simply jump to the right position. */
    protected var _jumpScroll :Boolean = true;

    /** True if autoscroll should be supressed for the current frame. */
    protected var _suppressAutoScroll :Boolean = false;

    /** The maximum number of pixels to autoscroll per frame. */
    protected static const MAX_AUTO_SCROLL :int = 15;
}
}
