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
import mx.core.mx_internal;

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
import com.threerings.whirled.spot.data.SceneLocation;

import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.client.Prefs;
import com.threerings.msoy.data.MediaData;
import com.threerings.msoy.world.data.FurniData;
import com.threerings.msoy.world.data.MsoyLocation;
import com.threerings.msoy.world.data.MsoyPortal;
import com.threerings.msoy.world.data.MsoyScene;
import com.threerings.msoy.world.data.RoomObject;

public class AbstractRoomView extends Canvas
    implements PlaceView
{
    public function AbstractRoomView (ctx :MsoyContext)
    {
        _ctx = ctx;
        clipContent = false; // needed because we scroll
        includeInLayout = false;

        verticalScrollPolicy = ScrollPolicy.OFF;
        horizontalScrollPolicy = ScrollPolicy.OFF;

        width = TARGET_WIDTH;
        height = TARGET_HEIGHT;

        addEventListener(FlexEvent.UPDATE_COMPLETE, updateComplete);
        addEventListener(ResizeEvent.RESIZE, didResize);
    }

    protected function updateComplete (evt :FlexEvent) :void
    {
        removeEventListener(FlexEvent.UPDATE_COMPLETE, updateComplete);

        relayout();
    }

/*
    override protected function updateDisplayList (
        unscaledWidth :Number, unscaledHeight :Number) :void
    {
        super.updateDisplayList(unscaledWidth, unscaledHeight);
        var scale :Number = int(100 * unscaledHeight / TARGET_HEIGHT) / 100;

        scaleX = scale;
        scaleY = scale;
        setActualSize(unscaledWidth / scale, TARGET_HEIGHT);
    }
*/

    override public function setActualSize (w :Number, h :Number) :void
    {
        var scale :Number = int(100 * h / TARGET_HEIGHT) / 100;

        scaleX = scale;
        scaleY = scale;
        super.setActualSize(w, TARGET_HEIGHT);
    }

/*
    override mx_internal function adjustSizesForScaleChanges () :void
    {

    }
*/

/*
    override protected function measure () :void
    {
    }
*/

    protected function didResize (event :ResizeEvent) :void
    {
        relayout();
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
    }

    /**
     * Layout everything.
     */
    protected function relayout () :void
    {
        configureScrollRect();

        if (_bkg != null) {
            locationUpdated(_bkg);
        }

        var sprite :MsoySprite;
        for each (sprite in _portals.values()) {
            locationUpdated(sprite);
        }
        for each (sprite in _furni.values()) {
            locationUpdated(sprite);
        }
    }

    /**
     * Enable or disable editing. Called by the EditRoomHelper.
     */
    public function setEditing (
            editing :Boolean, spriteVisitFn :Function) :void
    {
        _editing = editing;
        _portals.forEach(spriteVisitFn);
        _furni.forEach(spriteVisitFn);

        if (!editing) {
            updateAllFurniAndPortals();
        }
    }

    /**
     * Configure the rectangle used to select a portion of the view
     * that's showing.
     */
    protected function configureScrollRect () :void
    {
        if (_scene.getWidth() > width) {
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
    public function pointToLocation (
            globalX :Number, globalY :Number) :MsoyLocation
    {
        var p :Point = globalToLocal(new Point(globalX, globalY));
        var x :Number = p.x;
        var y :Number = p.y;

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
     * Get the y distance represented by the specified number of pixels
     * for the given z coordinate.
     */
    public function getYDistance (z :Number, pixels :int) :Number
    {
        var scale :Number = MIN_SCALE +
            ((MAX_COORD - z) / MAX_COORD) * (MAX_SCALE - MIN_SCALE);
        var sheight :Number = (unscaledHeight * scale);
        return (pixels / sheight);
    }

    /**
     * Scroll the view by the specified number of pixels.
     *
     * @return true if the view is scrollable.
     */
    public function scrollViewBy (xpixels :int) :Boolean
    {
        var rect :Rectangle = scrollRect;
        if (rect == null) {
            return false;
        }

        rect.x = Math.min(_scene.getWidth() - rect.width,
            Math.max(0, rect.x + xpixels));
        scrollRect = rect;
        return true;
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
     * Shutdown all the sprites in the specified map.
     */
    protected function removeAll (map :HashMap) :void
    {
        for each (var sprite :MsoySprite in map.values()) {
            removeSprite(sprite);
        }
        map.clear();
    }

    protected function addFurni (furni :FurniData) :void
    {
        var sprite :FurniSprite = _ctx.getMediaDirector().getFurni(furni);
        addChild(sprite);
        sprite.setLocation(furni.loc);

        _furni.put(furni.id, sprite);
    }

    protected function updateFurni (furni :FurniData) :void
    {
        var sprite :FurniSprite = (_furni.get(furni.id) as FurniSprite);
        if (sprite != null) {
            sprite.update(furni);
        } else {
            addFurni(furni);
        }
    }

    protected function addPortal (portal :MsoyPortal) :void
    {
        var sprite :PortalSprite = _ctx.getMediaDirector().getPortal(portal);
        var loc :MsoyLocation = (portal.loc as MsoyLocation);
        addChild(sprite);
        sprite.setLocation(loc);

        _portals.put(portal.portalId, sprite);
    }

    protected function updatePortal (portal :MsoyPortal) :void
    {
        var sprite :PortalSprite =
            (_portals.get(portal.portalId) as PortalSprite);
        if (sprite != null) {
            sprite.update(portal);
        } else {
            addPortal(portal);
        }
    }

    protected function removeSprite (sprite :MsoySprite) :void
    {
        removeChild(sprite);
        _ctx.getMediaDirector().returnSprite(sprite);
    }

    // documentation inherited from interface PlaceView
    public function willEnterPlace (plobj :PlaceObject) :void
    {
        // save our scene object
        _roomObj = (plobj as RoomObject);

        // get the specifics on the current scene from the scene director
        _scene = (_ctx.getSceneDirector().getScene() as MsoyScene);
        configureScrollRect();

        updateDrawnRoom();

        // set up the background image
        var bkgMedia :MediaData = _scene.getBackground();
        if (bkgMedia != null) {
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
    }

    public function updateAllFurniAndPortals () :void
    {
        var itr :Iterator = _scene.getPortals();
        while (itr.hasNext()) {
            var portal :MsoyPortal = (itr.next() as MsoyPortal);
            updatePortal(portal);
        }

        // set up any furniture
        for each (var furni :FurniData in _scene.getFurni()) {
            updateFurni(furni);
        }
    }

    // documentation inherited from interface PlaceView
    public function didLeavePlace (plobj :PlaceObject) :void
    {
        if (_bkg != null) {
            removeSprite(_bkg);
            _bkg = null;
        }

        removeAll(_portals);
        removeAll(_furni);

        _roomObj = null;
        _scene = null;
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

    /** The msoy context. */
    protected var _ctx :MsoyContext;

    /** The model of the current scene. */
    protected var _scene :MsoyScene;

    /** The transitory properties of the current scene. */
    protected var _roomObj :RoomObject;

    /** The background image. */
    protected var _bkg :MsoySprite;

    /** A hand-drawn background to look like a room. */
    protected var _bkgGraphics :UIComponent;

    /** A map of portalId -> Portal. */
    protected var _portals :HashMap = new HashMap();

    /** A map of id -> Furni. */
    protected var _furni :HashMap = new HashMap();

    /** Are we editing the scene? */
    protected var _editing :Boolean = false;

    private static const MIN_SCALE :Number = 0.55;
    private static const MAX_SCALE :Number = 1;
    private static const MAX_COORD :Number = 1;
    private static const PHI :Number = (1 + Math.sqrt(5)) / 2;

    private static const TARGET_WIDTH :Number = 800;
    private static const TARGET_HEIGHT :Number = TARGET_WIDTH / PHI;
}
}
