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

import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.client.Prefs;
import com.threerings.msoy.data.MediaData;
import com.threerings.msoy.data.MsoyOccupantInfo;
import com.threerings.msoy.world.data.FurniData;
import com.threerings.msoy.world.data.MsoyLocation;
import com.threerings.msoy.world.data.MsoyPortal;
import com.threerings.msoy.world.data.MsoyScene;

import com.threerings.msoy.world.chat.client.ChatPopper;

public class RoomView extends Canvas
    implements PlaceView, SetListener, ChatDisplay
{
    public function RoomView (ctx :MsoyContext)
    {
        _ctx = ctx;
        clipContent = false; // needed because we scroll

        verticalScrollPolicy = ScrollPolicy.OFF;
        horizontalScrollPolicy = ScrollPolicy.OFF;

        width = 800;
        height = width / PHI;

        addEventListener(FlexEvent.UPDATE_COMPLETE, updateComplete);
    }

    protected function updateComplete (evt :FlexEvent) :void
    {
        removeEventListener(FlexEvent.UPDATE_COMPLETE, updateComplete);
        ChatPopper.setChatView(this);

        relayout();
    }

    /**
     * Layout everything.
     */
    protected function relayout () :void
    {
        configureScrollRect();

        var sprite :MsoySprite;
        for each (sprite in _avatars.values()) {
            locationUpdated(sprite);
        }
        for each (sprite in _portals.values()) {
            locationUpdated(sprite);
        }
        for each (sprite in _furni.values()) {
            locationUpdated(sprite);
        }
        if (_bkg != null) {
            locationUpdated(_bkg);
        }
    }

    /**
     * Configure the rectangle used to select a portion of the view
     * that's showing.
     */
    protected function configureScrollRect () :void
    {
        if (_scene.getWidth() > unscaledWidth) {
            scrollRect = new Rectangle(0, 0, unscaledWidth, unscaledHeight);
        } else {
            scrollRect = null;
        }
    }
    
    /**
     * Turn the screen coordinate into a MsoyLocation, with the
     * orient field set to 0.
     *
     * @return null if the coordinates could not be turned into a location.
     */
    public function pointToLocation (x :Number, y :Number) :MsoyLocation
    {
        // flip y
        y = unscaledHeight - y;

        var sceneWidth :Number = _scene.getWidth();

        var sheight :Number = (unscaledHeight * MIN_SCALE);
        var yoffset :Number = (unscaledHeight - sheight) / 2;
        if (y > yoffset) {
            return null;
        }

        // then, solve for scale given the current y
        var scale :Number = 1 + (y * -2) / unscaledHeight;

        // see if x is legal
        var swidth :Number = (sceneWidth * scale);
        var xoffset :Number = (sceneWidth - swidth) / 2;
        x -= xoffset;
        if (x < 0 || x > swidth) {
            return null;
        }

        // solve for x
        var xx :Number = (x / swidth) * MAX_COORD;

        // solve for z
        var zz :Number =
            MAX_COORD * (1 - ((scale - MIN_SCALE) / (MAX_SCALE - MIN_SCALE)));

        return new MsoyLocation(xx, 0, zz, 0);
    }

    /**
     * Called by MsoySprite instances when they've had their location
     * updated.
     */
    public function locationUpdated (sprite :MsoySprite) :void
    {
        // first update the position and scale
        var loc :MsoyLocation = sprite.loc;
        positionAndScale(sprite, loc);

        // then, possibly move the child up or down, depending on z order
        if (sprite.includeInLayout) {
            adjustZOrder(sprite, loc);
        }

        // if we moved our own sprite, possibly update the scroll position
        if (sprite is AvatarSprite) {
            var avatar :AvatarSprite = (sprite as AvatarSprite);
            if (avatar.getOid() == _ctx.getClient().getClientOid()) {
                scrollView(avatar);
            }
        }
    }

    public function dimAvatars (setDim :Boolean) :void
    {
        setActive(_avatars, !setDim);
    }

    public function dimPortals (setDim :Boolean) :void
    {
        setActive(_portals, !setDim);
    }

    public function dimFurni (setDim :Boolean) :void
    {
        setActive(_furni, !setDim);
    }

    public function scrollViewBy (xpixels :int) :void
    {
        var rect :Rectangle = scrollRect;
        if (rect == null) {
            return;
        }

        rect.x = Math.min(_scene.getWidth() - rect.width,
            Math.max(0, rect.x + xpixels));
        scrollRect = rect;
        _jumpScroll = false;

        // remove any autoscrolling (if tick is not a registered listener
        // this will safely noop)
        removeEventListener(Event.ENTER_FRAME, tick);
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

    /**
     * Calculate the scale and x/y position of the specified media
     * according to its logical coordinates.
     */
    protected function positionAndScale (
            sprite :MsoySprite, loc :MsoyLocation) :void
    {
        var sceneWidth :Number = _scene.getWidth();
        var hotSpot :Point = sprite.hotSpot;
        // the scale of the object is determined by the z coordinate
        var scale :Number = MIN_SCALE +
            ((MAX_COORD - loc.z) / MAX_COORD) * (MAX_SCALE - MIN_SCALE);
        sprite.scaleX = scale;
        sprite.scaleY = scale;

        // x position depends on logical x and the scale
        var swidth :Number = (sceneWidth * scale);
        var xoffset :Number = (sceneWidth - swidth) / 2;
        sprite.x = xoffset - (scale * hotSpot.x) +
            (loc.x / MAX_COORD) * swidth;

        // y position depends on logical y and the scale (z)
        var sheight :Number = (unscaledHeight * scale);
        var yoffset :Number = (unscaledHeight - sheight) / 2;
        sprite.y = unscaledHeight - yoffset - (scale * hotSpot.y) -
            (loc.y / MAX_COORD) * sheight;
    }

    /**
     * Adjust the z order of the specified sprite so that it is drawn
     * according to its logical Z coordinate relative to other sprites.
     */
    protected function adjustZOrder (
             sprite :MsoySprite, loc :MsoyLocation) :void
    {
        var dex :int = getChildIndex(sprite);
        var newdex :int = dex;
        var z :Number;
        while (newdex > 0) {
            z = getZOfChildAt(newdex - 1);
            if (isNaN(z) || z >= loc.z) {
                break;
            }
            newdex--;
        }

        if (newdex == dex) {
            while (newdex < numChildren - 1) {
                if (getZOfChildAt(newdex + 1) <= loc.z) {
                    break;
                }
                newdex++;
            }
        }

        if (newdex != dex) {
            setChildIndex(sprite, newdex);
        }
    }

    /**
     * Convenience method to get the logical z coordinate of the child
     * at the specified index.
     */
    protected function getZOfChildAt (index :int) :Number
    {
        var disp :DisplayObject = getChildAt(index);
        if ((disp is UIComponent) && !(disp as UIComponent).includeInLayout) {
            return NaN;
        }
        if (disp is MsoySprite) {
            return (disp as MsoySprite).loc.z;
        }
        return Number.MAX_VALUE;
    }

    /**
     */
    protected function setActive (map :HashMap, active :Boolean) :void
    {
        for each (var sprite :MsoySprite in map.values()) {
            sprite.setActive(active);
        }
    }

    /**
     * Shutdown all the media in the specified map.
     */
    protected function shutdown (map :HashMap) :void
    {
        for each (var sprite :MsoySprite in map.values()) {
            sprite.shutdown();
        }
    }

    public function getMyAvatar () :AvatarSprite
    {
        var oid :int = _ctx.getClient().getClientOid();
        return (_avatars.get(oid) as AvatarSprite);
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
            (_sceneObj.occupantInfo.get(bodyOid) as MsoyOccupantInfo);
        var sloc :SceneLocation =
            (_sceneObj.occupantLocs.get(bodyOid) as SceneLocation);
        var loc :MsoyLocation = (sloc.loc as MsoyLocation);

        var avatar :AvatarSprite = new AvatarSprite(occInfo, loc);
        _avatars.put(bodyOid, avatar);
        addChild(avatar);
        avatar.setLocation(loc);
    }

    protected function removeBody (bodyOid :int) :void
    {
        var avatar :AvatarSprite = (_avatars.remove(bodyOid) as AvatarSprite);
        if (avatar != null) {
            removeChild(avatar);
            avatar.shutdown();
        }
    }

    protected function moveBody (bodyOid :int) :void
    {
        var avatar :AvatarSprite = (_avatars.get(bodyOid) as AvatarSprite);
        var sloc :SceneLocation =
            (_sceneObj.occupantLocs.get(bodyOid) as SceneLocation);
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

    protected function addPortal (portal :MsoyPortal) :void
    {
        var sprite :PortalSprite = new PortalSprite(portal);
        var loc :MsoyLocation = (portal.loc as MsoyLocation);
        addChild(sprite);
        sprite.setLocation(loc);

        _portals.put(portal.portalId, sprite);
    }

    protected function addFurni (furni :FurniData) :void
    {
        var sprite :FurniSprite = new FurniSprite(furni);
        addChild(sprite);
        sprite.setLocation(furni.loc);

        _furni.put(furni.id, sprite);
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

    // documentation inherited from interface PlaceView
    public function willEnterPlace (plobj :PlaceObject) :void
    {
        _ctx.getChatDirector().addChatDisplay(this);

        // listen to updates
        _sceneObj = (plobj as SpotSceneObject);
        _sceneObj.addListener(this);

        // get the specifics on the current scene from the scene director
        _scene = (_ctx.getSceneDirector().getScene() as MsoyScene);
        configureScrollRect();
        var music :MediaData = _scene.getMusic();
        if (music != null) {
            trace("Now looping background music");
            _music = new SoundPlayer(music);
            _music.loop(Prefs.getMediaPosition(music.id));
        }

        updateDrawnRoom();

        // set up the background image
        _bkg = new MsoySprite(_scene.getBackground());
        switch (_scene.getType()) {
        case "image":
            // by adding it to the raw children, it does not participate
            // in Z order movements
            _bkg.includeInLayout = false;
            addChild(_bkg);
            _bkg.setLocation([.5, 0, 0, 0]);
            break;

        default:
            addChild(_bkg);
            _bkg.setLocation([.5, 0, 1, 0]);
            break;
        }

        // set up any portals
        var itr :Iterator = _scene.getPortals();
        while (itr.hasNext()) {
            var portal :MsoyPortal = (itr.next() as MsoyPortal);
            addPortal(portal);
        }

        // set up any furniture
        for each (var furni :FurniData in _scene.getFurni()) {
            addFurni(furni);
        }

        // add all currently present occupants
        for (var ii :int = _sceneObj.occupants.size() - 1; ii >= 0; ii--) {
            addBody(_sceneObj.occupants.getAt(ii));
        }

        // and animate ourselves entering the room (everyone already in the
        // (room will also have seen it)
        portalTraversed(getMyCurrentLocation(), true);
    }

    protected function updateDrawnRoom () :void
    {
        if (_bkgGraphics != null) {
            removeChild(_bkgGraphics);
            _bkgGraphics = null;
        }

        if (_scene.getType() == "image") {
            return; // nothing to draw
        }

        _bkgGraphics = new UIComponent();
        _bkgGraphics.includeInLayout = false;
        addChild(_bkgGraphics);

        var g :Graphics = _bkgGraphics.graphics;

        g.clear();
        var sceneWidth :Number = _scene.getWidth();

        var swidth :Number = (sceneWidth * MIN_SCALE);
        var sheight :Number = (height * MIN_SCALE);
        var xoffset :Number = (sceneWidth - swidth) / 2;
        var yoffset :Number = (height - sheight) / 2;

        // calculate the coordinates of the back wall corners
        var x1 :Number = xoffset;
        var y1 :Number = yoffset;
        var x2 :Number = sceneWidth - xoffset;
        var y2 :Number = height - yoffset;

        // fill in the floor
        g.beginFill(0x333333);
        g.moveTo(0, height);
        g.lineTo(x1, y2);
        g.lineTo(x2, y2);
        g.lineTo(sceneWidth, height);
        g.lineTo(0, height);
        g.endFill();

        // fill in the three walls
        g.beginFill(0x666666);
        g.moveTo(0, 0);
        g.lineTo(x1, y1);
        g.lineTo(x2, y1);
        g.lineTo(sceneWidth, 0);
        g.lineTo(sceneWidth, height);
        g.lineTo(x2, y2);
        g.lineTo(x1, y2);
        g.lineTo(0, height);
        g.lineTo(0, 0);
        g.endFill();

        // fill in the ceiling
        g.beginFill(0x999999);
        g.moveTo(0, 0);
        g.lineTo(x1, y1);
        g.lineTo(x2, y1);
        g.lineTo(sceneWidth, 0);
        g.lineTo(0, 0);
        g.endFill();

        // draw the lines defining the walls
        g.lineStyle(2);
        g.moveTo(0, 0);
        g.lineTo(x1, y1);
        g.lineTo(x2, y1);

        g.moveTo(sceneWidth, 0);
        g.lineTo(x2, y1);
        g.lineTo(x2, y2);

        g.moveTo(sceneWidth, height);
        g.lineTo(x2, y2);
        g.lineTo(x1, y2);

        g.moveTo(0, height);
        g.lineTo(x1, y2);
        g.lineTo(x1, y1);
    }

    // documentation inherited from interface PlaceView
    public function didLeavePlace (plobj :PlaceObject) :void
    {
        _ctx.getChatDirector().removeChatDisplay(this);
        ChatPopper.popAllDown();

        _sceneObj.removeListener(this);
        _sceneObj = null;

        if (_music != null) {
            Prefs.setMediaPosition(_music.getMediaId(), _music.getPosition());
            _music.stop();
            _music = null;
        }
        if (_bkg != null) {
            _bkg.shutdown();
            _bkg = null;
        }

        shutdown(_avatars);
        shutdown(_portals);
        shutdown(_furni);

        _scene = null;

        // TODO: clean up avatars, remove them, etc.
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

        } else if (SpotSceneObject.OCCUPANT_LOCS == name) {
            var sceneLoc :SceneLocation =
                (event.getOldEntry() as SceneLocation);
            portalTraversed(sceneLoc.loc, false);
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
            var occInfo :OccupantInfo = _sceneObj.getOccupantInfo(umsg.speaker);
            if (occInfo != null) {
                avatar = (_avatars.get(occInfo.bodyOid) as AvatarSprite);
            }
        }

        ChatPopper.popUp(msg, avatar);
    }

    /** The msoy context. */
    protected var _ctx :MsoyContext;

    /** The model of the current scene. */
    protected var _scene :MsoyScene;

    /** The transitory properties of the current scene. */
    protected var _sceneObj :SpotSceneObject;

    /** The background image. */
    protected var _bkg :MsoySprite;

    /** The background music in the scene. */
    protected var _music :SoundPlayer;

    /** A hand-drawn background to look like a room. */
    protected var _bkgGraphics :UIComponent;

    /** A map of bodyOid -> AvatarSprite. */
    protected var _avatars :HashMap = new HashMap();

    /** A map of portalId -> Portal. */
    protected var _portals :HashMap = new HashMap();

    /** A map of id -> Furni. */
    protected var _furni :HashMap = new HashMap();

    /** If true, the scrolling should simply jump to the right position. */
    protected var _jumpScroll :Boolean = true;

    /** True if autoscroll should be supressed for the current frame. */
    protected var _suppressAutoScroll :Boolean = false;

    /** The maximum number of pixels to autoscroll per frame. */
    protected static const MAX_AUTO_SCROLL :int = 15;

    private static const MIN_SCALE :Number = 0.55;
    private static const MAX_SCALE :Number = 1;
    private static const MAX_COORD :Number = 1;
    private static const PHI :Number = (1 + Math.sqrt(5)) / 2;
}
}
