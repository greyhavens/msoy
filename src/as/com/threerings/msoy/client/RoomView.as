package com.threerings.msoy.client {

import flash.display.DisplayObject;

import flash.events.Event;

import mx.containers.Canvas;

import mx.core.IUIComponent;

import mx.core.ScrollPolicy;

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

import com.threerings.msoy.data.MediaData;
import com.threerings.msoy.data.MsoyOccupantInfo;
import com.threerings.msoy.world.data.MsoyLocation;
import com.threerings.msoy.world.data.MsoyPortal;
import com.threerings.msoy.world.data.MsoyScene;

import com.threerings.msoy.ui.Avatar;
import com.threerings.msoy.ui.PortalMedia;
import com.threerings.msoy.ui.ScreenMedia;

public class RoomView extends Canvas
    implements PlaceView, SetListener, ChatDisplay
{
    public function RoomView (ctx :MsoyContext)
    {
        _ctx = ctx;

        verticalScrollPolicy = ScrollPolicy.OFF;
        horizontalScrollPolicy = ScrollPolicy.OFF;

        width = 800;
        height = width / PHI;

        // draw the box, slightly
        graphics.clear();
        graphics.lineStyle(2);

        var swidth :Number = (width * MIN_SCALE);
        var sheight :Number = (height * MIN_SCALE);
        var xoffset :Number = (width - swidth) / 2;
        var yoffset :Number = (height - sheight) / 2;

        // draw the lines defining the walls
        graphics.moveTo(0, 0);
        graphics.lineTo(xoffset, yoffset);
        graphics.lineTo(width - xoffset, yoffset);

        graphics.moveTo(width, 0);
        graphics.lineTo(width - xoffset, yoffset);
        graphics.lineTo(width - xoffset, height - yoffset);

        graphics.moveTo(width, height);
        graphics.lineTo(width - xoffset, height - yoffset);
        graphics.lineTo(xoffset, height - yoffset);

        graphics.moveTo(0, height);
        graphics.lineTo(xoffset, height - yoffset);
        graphics.lineTo(xoffset, yoffset);
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
        y = height - y;

        var sheight :Number = (height * MIN_SCALE);
        var yoffset :Number = (height - sheight) / 2;
        if (y > yoffset) {
            return null;
        }

        // then, solve for scale given the current y
        var scale :Number = 1 + (y * -2) / height;

        // see if x is legal
        var swidth :Number = (width * scale);
        var xoffset :Number = (width - swidth) / 2;
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
     * Called by ScreenMedia instances when they've had their location
     * updated.
     */
    public function locationUpdated (sm :ScreenMedia) :void
    {
        // first update the position and scale
        var loc :MsoyLocation = sm.loc;
        positionAndScale(sm, loc);

        // then, possibly move the child up or down, depending on z order
        var dex :int = getChildIndex(sm);
        var newdex :int = dex;
        while (newdex > 0) {
            if (getZOfChildAt(newdex - 1) >= loc.z) {
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
            setChildIndex(sm, newdex);
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

    /**
     * Calculate the scale and x/y position of the specified media
     * according to its logical coordinates.
     */
    protected function positionAndScale (
            sm :ScreenMedia, loc :MsoyLocation) :void
    {
        // the scale of the object is determined by the z coordinate
        var scale :Number = MIN_SCALE +
            ((MAX_COORD - loc.z) / MAX_COORD) * (MAX_SCALE - MIN_SCALE);
        sm.scaleX = scale;
        sm.scaleY = scale;

        // x position depends on logical x and the scale
        var swidth :Number = (width * scale);
        var xoffset :Number = (width - swidth) / 2;
        sm.x = (scale * sm.contentWidth)/-2 + xoffset +
            (loc.x / MAX_COORD) * swidth;

        // y position depends on logical y and the scale (z)
        var sheight :Number = (height * scale);
        var yoffset :Number = (height - sheight) / 2;
        sm.y = height - yoffset - (scale * sm.contentHeight);
        // TODO: incorporate y coord
    }

    /**
     * Convenience method to get the logical z coordinate of the child
     * at the specified index.
     */
    protected function getZOfChildAt (index :int) :int
    {
        var disp :DisplayObject = getChildAt(index);
        if (disp is ScreenMedia) {
            return (disp as ScreenMedia).loc.z;
        }
        return int.MAX_VALUE;
    }

    /**
     */
    protected function setActive (map :HashMap, active :Boolean) :void
    {
        for each (var media :ScreenMedia in map.values()) {
            media.setActive(active);
        }
    }

    /**
     * Return the current location of the avatar that represents our body.
     */
    public function getMyCurrentLocation () :MsoyLocation
    {
        var oid :int = _ctx.getClient().getClientOid();
        var avatar :Avatar = (_avatars.get(oid) as Avatar);
        return avatar.loc;
    }

    protected function addBody (bodyOid :int) :void
    {
        var occInfo :MsoyOccupantInfo =
            (_sceneObj.occupantInfo.get(bodyOid) as MsoyOccupantInfo);
        var sloc :SceneLocation =
            (_sceneObj.occupantLocs.get(bodyOid) as SceneLocation);
        var loc :MsoyLocation = (sloc.loc as MsoyLocation);

        var avatar :Avatar = new Avatar(occInfo, loc);
        _avatars.put(bodyOid, avatar);
        addChild(avatar);
        avatar.setLocation(loc);
    }

    protected function removeBody (bodyOid :int) :void
    {
        var avatar :Avatar = (_avatars.remove(bodyOid) as Avatar);
        if (avatar != null) {
            removeChild(avatar);
        }
    }

    protected function moveBody (bodyOid :int) :void
    {
        var avatar :Avatar = (_avatars.get(bodyOid) as Avatar);
        var sloc :SceneLocation =
            (_sceneObj.occupantLocs.get(bodyOid) as SceneLocation);
        var loc :MsoyLocation = (sloc.loc as MsoyLocation);

        avatar.moveTo(loc);
    }

    protected function addPortal (portal :MsoyPortal) :void
    {
        var pm :PortalMedia = new PortalMedia(portal);
        var loc :MsoyLocation = (portal.loc as MsoyLocation);
        addChild(pm);
        pm.setLocation(loc);

        _portals.put(portal.portalId, pm);
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
                var pm :PortalMedia =
                    (_portals.get(portal.portalId) as PortalMedia);
                pm.wasTraversed(entering);
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

        // set up the background image
        var bkg :ScreenMedia = new ScreenMedia(_scene.getBackground());
        addChild(bkg);
        bkg.setLocation([50, 0, 100, 0]);
//        bkg.x = 0;
//        bkg.y = 0;
//        addChild(bkg);

        // set up any portals
        var itr :Iterator = _scene.getPortals();
        while (itr.hasNext()) {
            var portal :MsoyPortal = (itr.next() as MsoyPortal);
            addPortal(portal);
        }

        // set up any furniture
        // TODO

        // add all currently present occupants
        for (var ii :int = _sceneObj.occupants.size() - 1; ii >= 0; ii--) {
            addBody(_sceneObj.occupants.getAt(ii));
        }

        // and animate ourselves entering the room (everyone already in the
        // (room will also have seen it)
        portalTraversed(getMyCurrentLocation(), true);
        trace("Set up all bodies");
    }

    // documentation inherited from interface PlaceView
    public function didLeavePlace (plobj :PlaceObject) :void
    {
        _ctx.getChatDirector().removeChatDisplay(this);

        _sceneObj.removeListener(this);
        _sceneObj = null;

        _scene = null;

        // TODO: clean up avatars, remove them, etc.
    }

    // documentation inherited from interface SetListener
    public function entryAdded (event :EntryAddedEvent) :void
    {
        var name :String = event.getName();

        if (PlaceObject.OCCUPANT_INFO == name) {
            trace("addBody");
            addBody((event.getEntry() as MsoyOccupantInfo).getBodyOid());

        } else if (SpotSceneObject.OCCUPANT_LOCS == name) {
            var sceneLoc :SceneLocation = (event.getEntry() as SceneLocation);
            trace("someone added at " + sceneLoc.loc);
            portalTraversed(sceneLoc.loc, true);
        }
    }

    // documentation inherited from interface SetListener
    public function entryUpdated (event :EntryUpdatedEvent) :void
    {
        var name :String = event.getName();

        if (SpotSceneObject.OCCUPANT_LOCS == name) {
            trace("locationUpdated");
            moveBody((event.getEntry() as SceneLocation).bodyOid);
        }
    }

    // documentation inherited from interface SetListener
    public function entryRemoved (event :EntryRemovedEvent) :void
    {
        var name :String = event.getName();

        if (PlaceObject.OCCUPANT_INFO == name) {
            trace("removeBody");
            removeBody((event.getOldEntry() as MsoyOccupantInfo).getBodyOid());

        } else if (SpotSceneObject.OCCUPANT_LOCS == name) {
            var sceneLoc :SceneLocation =
                (event.getOldEntry() as SceneLocation);
            trace("someone removed at " + sceneLoc.loc);
            portalTraversed(sceneLoc.loc, false);
        }
    }

/*
    override public function validateSize (recursive :Boolean = false) :void
    {
        super.validateSize(recursive);

        trace("Rando z");

        // sort our children by z order
        for (var ii :int = numChildren; ii > 1; ii--) {
            var dex :int = int(Math.random() * ii);
            swapChildrenAt(ii - 1, dex);
        }
    }
    */

    // documentation inherited from interface ChatDisplay
    public function clear () :void
    {
        // TODO
    }

    // documentation inherited from interface ChatDisplay
    public function displayMessage (msg :ChatMessage) :void
    {
        if (!(msg is UserMessage)) {
            return;
        }

        var umsg :UserMessage = (msg as UserMessage);
        var occInfo :OccupantInfo = _sceneObj.getOccupantInfo(umsg.speaker);
        if (occInfo != null) {
            var avatar :Avatar = (_avatars.get(occInfo.bodyOid) as Avatar);
            avatar.speak(umsg);
        }
    }

    protected var _ctx :MsoyContext;

    /** The model of the current scene. */
    protected var _scene :MsoyScene;

    /** The transitory properties of the current scene. */
    protected var _sceneObj :SpotSceneObject;

    /** A map of bodyOid -> Avatar. */
    protected var _avatars :HashMap = new HashMap();

    /** A map of portalId -> Portal. */
    protected var _portals :HashMap = new HashMap();

    /** A map of id -> Furni. */
    protected var _furni :HashMap = new HashMap();

    private static const MIN_SCALE :Number = 0.55;
    private static const MAX_SCALE :Number = 1;
    private static const MAX_COORD :Number = 100;
    private static const PHI :Number = (1 + Math.sqrt(5)) / 2;
}
}
