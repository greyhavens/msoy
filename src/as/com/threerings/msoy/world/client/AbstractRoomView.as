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
import com.threerings.msoy.data.MediaDesc;
import com.threerings.msoy.world.data.FurniData;
import com.threerings.msoy.world.data.MsoyLocation;
import com.threerings.msoy.world.data.MsoyPortal;
import com.threerings.msoy.world.data.MsoyScene;
import com.threerings.msoy.world.data.RoomObject;

public class AbstractRoomView extends Canvas
    implements PlaceView
{
    public static const FLOOR :int = 0;
    public static const BACK_WALL :int = 1;
    public static const CEILING :int = 2;
    public static const LEFT_WALL :int = 3;
    public static const RIGHT_WALL :int = 4;

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
     * Enable or disable editing. Called by the EditRoomController.
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

    protected function computeMinScale () :Number
    {
        var sceneDepth :int = _scene.getDepth();
        return (sceneDepth == 0) ? 0 : (FOCAL / (FOCAL + sceneDepth));
    }
    
    /**
     * Turn the screen coordinate into a MsoyLocation, with the
     * orient field set to 0.
     *
     * @return a ClickLocation object.
     */
    public function pointToLocation (
            globalX :Number, globalY :Number) :ClickLocation
    {
        var p :Point = globalToLocal(new Point(globalX, globalY));
        var x :Number = p.x;
        var y :Number = p.y;

        var sceneWidth :int = _scene.getWidth();
        var minScale :Number = computeMinScale();
        var backWallHeight :Number = unscaledHeight * minScale;

        var horizon :Number = 1 - _scene.getHorizon();
        var horizonY :Number = unscaledHeight * horizon;
        var backWallTop :Number = horizonY - (backWallHeight * horizon);
        var backWallBottom :Number = backWallTop + backWallHeight;
        var floorWidth :Number, floorInset :Number;
        var xx :Number, yy :Number, zz :Number;
        var scale :Number;

        // do some partitioning depending on where the y lies
        if (y < backWallTop) {
            // probably the ceiling, but maybe not
            // TODO
            return new ClickLocation(ClickLocation.CEILING,
                new MsoyLocation(.5, 1, .5, 0));

        } else if (y < backWallBottom) {
            // somewhere in the realm of the back wall...
            // see how wide the floor is at that scale
            floorWidth = (sceneWidth * minScale);
            floorInset = (sceneWidth - floorWidth) / 2;
            if (x < floorInset) {
                // TODO
                return new ClickLocation(ClickLocation.LEFT_WALL,
                    new MsoyLocation(0, .5, .5));

            } else if (x - floorInset > floorWidth) {
                // TODO
                return new ClickLocation(ClickLocation.RIGHT_WALL,
                    new MsoyLocation(1, .5, .5));

            } else {
                xx = (x - floorInset) / floorWidth;
                // y is simply how high they clicked on the wall
                yy = (backWallBottom - y) / backWallHeight;
                return new ClickLocation(ClickLocation.BACK_WALL,
                    new MsoyLocation(xx, yy, 1, 0));
            }

        } else {
            // solve for scale
            scale = minScale +
                (y - backWallBottom) / (unscaledHeight - backWallBottom) *
                (MAX_SCALE - minScale);

            // see how wide the floor is at that scale
            floorWidth = (sceneWidth * scale);
            floorInset = (sceneWidth - floorWidth) / 2;
            x -= floorInset;
            if (x < 0) {
                // TODO
                return new ClickLocation(ClickLocation.LEFT_WALL,
                    new MsoyLocation(0, .5, .5));

            } else if (x > floorWidth) {
                // TODO
                return new ClickLocation(ClickLocation.RIGHT_WALL,
                    new MsoyLocation(1, .5, .5));

            } else {

                // solve for x & z
                xx = (x / floorWidth) * MAX_COORD;
                zz = MAX_COORD *
                    (1 - ((scale - minScale) / (MAX_SCALE - minScale)));

                return new ClickLocation(ClickLocation.FLOOR,
                    new MsoyLocation(xx, 0, zz, 0));
            }
        }
    }

    /**
     * Get the y distance represented by the specified number of pixels
     * for the given z coordinate.
     */
    public function getYDistance (z :Number, pixels :int) :Number
    {
        var minScale :Number = computeMinScale();
        var scale :Number = minScale +
            ((MAX_COORD - z) / MAX_COORD) * (MAX_SCALE - minScale);
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
        var minScale :Number = computeMinScale();
        // the scale of the object is determined by the z coordinate
        var scale :Number = minScale +
            ((MAX_COORD - loc.z) / MAX_COORD) * (MAX_SCALE - minScale);
        sprite.scaleX = scale;
        sprite.scaleY = scale;

        // x position depends on logical x and the scale
        var floorWidth :Number = (sceneWidth * scale);
        var floorInset :Number = (sceneWidth - floorWidth) / 2;
        sprite.x = floorInset - (scale * hotSpot.x) +
            (loc.x / MAX_COORD) * floorWidth;

        // y position depends on logical y and the scale (z)
        var horizon :Number = 1 - _scene.getHorizon();
        var horizonY :Number = unscaledHeight * horizon;

        sprite.y = (horizonY + (unscaledHeight - horizonY) * scale) -
            (scale * hotSpot.y) - 
            (loc.y / MAX_COORD) * (unscaledHeight * scale);
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
        configureBackground();
        updateAllFurniAndPortals();
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

    protected function configureBackground () :void
    {
        updateDrawnRoom();

        // set up the background image
        var bkgMedia :MediaDesc = _scene.getBackground();

        // if we had a background and now we don't or it's changed, shutdown old
        if (_bkg != null &&
                ((bkgMedia == null) || !bkgMedia.equals(_bkg.description))) {
            removeChild(_bkg);
            _bkg.shutdown();
            _bkg = null;
        }

        if (bkgMedia != null) {
            if (_bkg == null) {
                _bkg = new MsoySprite(bkgMedia);
            } else {
                removeChild(_bkg); // we'll re-add
            }
            switch (_scene.getType()) {
            case "image":
                // by adding it to the raw children, it does not participate
                // in Z order movements
                _bkg.includeInLayout = false;
                addChild(_bkg);
                _bkg.setLocation([.5, 0, 0, 0]);
                break;

            default:
                _bkg.includeInLayout = true;
                addChild(_bkg);
                _bkg.setLocation([.5, 0, 1, 0]);
                break;
            }
        }
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
        var sceneWidth :int = _scene.getWidth();
        var minScale :Number = computeMinScale();
        var backWallHeight :Number = unscaledHeight * minScale;

        var horizon :Number = 1 - _scene.getHorizon();
        var horizonY :Number = unscaledHeight * horizon;
        var backWallTop :Number = horizonY - (backWallHeight * horizon);
        var backWallBottom :Number = backWallTop + backWallHeight;

        var floorWidth :Number = (sceneWidth * minScale);
        var floorInset :Number = (sceneWidth - floorWidth) / 2;

        // rename a few things for ease of use below...
        var x1 :Number = floorInset;
        var x2 :Number = sceneWidth - floorInset;
        var y1 :Number = backWallTop;
        var y2 :Number = backWallBottom;

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

    /** The focal length of our perspective rendering. */
    // This value (488) was chosen so that the standard depth (400)
    // causes layout nearly identical to the original perspective math.
    // So, it's purely historical, but we could choose a new focal length
    // and even a new standard scene depth.
    // TODO
    private static const FOCAL :Number = 488;

    private static const MAX_SCALE :Number = 1;
    private static const MAX_COORD :Number = 1;
    private static const PHI :Number = (1 + Math.sqrt(5)) / 2;

    private static const TARGET_WIDTH :Number = 800;
    private static const TARGET_HEIGHT :Number = TARGET_WIDTH / PHI;
}
}
