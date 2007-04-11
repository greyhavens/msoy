//
// $Id$

package com.threerings.msoy.world.client {

import flash.display.DisplayObject;
import flash.display.Graphics;
import flash.display.Shape;
import flash.display.Sprite;

import flash.events.Event;

import flash.geom.Point;
import flash.geom.Rectangle;

import flash.utils.getTimer; // function import

import com.threerings.util.ArrayUtil;
import com.threerings.util.HashMap;
import com.threerings.util.Iterator;

import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.SetListener;

import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.flash.DisplayUtil;

import com.threerings.whirled.spot.data.Location;

import com.threerings.msoy.client.WorldContext;
import com.threerings.msoy.client.MsoyPlaceView;
import com.threerings.msoy.client.Prefs;
import com.threerings.msoy.item.web.MediaDesc;
import com.threerings.msoy.item.web.Decor;
import com.threerings.msoy.world.data.DecorData;
import com.threerings.msoy.world.data.FurniData;
import com.threerings.msoy.world.data.MsoyLocation;
import com.threerings.msoy.world.data.MsoyScene;
import com.threerings.msoy.world.data.MsoySceneModel;
import com.threerings.msoy.world.data.RoomObject;

public class AbstractRoomView extends Sprite
    implements MsoyPlaceView
{
    public static const FLOOR :int = 0;
    public static const BACK_WALL :int = 1;
    public static const CEILING :int = 2;
    public static const LEFT_WALL :int = 3;
    public static const RIGHT_WALL :int = 4;

    public function AbstractRoomView (ctx :WorldContext)
    {
        _ctx = ctx;
    }

    // from MsoyPlaceView
    public function setPlaceSize (
        unscaledWidth :Number, unscaledHeight :Number) :void
    {
        _actualWidth = unscaledWidth;
        _actualHeight = unscaledHeight;
        relayout();
        updateDrawnRoom();
    }

    /**
     * Get the room's stage bounds.
     */
    public function getGlobalBounds () :Rectangle
    {
        var r :Rectangle = getScrollBounds();
        r.topLeft = localToGlobal(r.topLeft);
        r.bottomRight = localToGlobal(r.bottomRight);
        return r;
    }

    /**
     * Called by the editor to have direct access to our sprite list..
     */
    public function getFurniSprites () :Array
    {
        return _furni.values();
    }

    /**
     * Returns the room distributed object.
     */
    public function getRoomObject () :RoomObject
    {
        return _roomObj;
    }

    /**
     * Called by MsoySprite instances when they've had their location updated.
     */
    public function locationUpdated (sprite :MsoySprite) :void
    {
        // first update the position and scale
        var loc :MsoyLocation = sprite.loc;
        positionAndScale(sprite, loc);

        // then, possibly move the child up or down, depending on z order
        if (sprite.isIncludedInLayout()) {
            adjustZOrder(sprite, loc);
        }
    }

    /**
     * Enable or disable editing. Called by the EditRoomController.
     */
    public function setEditing (editing :Boolean, spriteVisitFn :Function) :void
    {
        _editing = editing;
        _furni.forEach(spriteVisitFn);
        
        if (_bg != null) {
            spriteVisitFn(null, _bg);
        }

        if (editing) {

        } else {
            updateAllFurni();
        }
    }

    /**
     * Turn the screen coordinate into a MsoyLocation, with the orient field set to 0.
     * @param yOffset (optional) a raw pixel y adjustment. NOTE: It's only
     *                used when the click wall is FLOOR.
     *
     * @return a ClickLocation object.
     */
    public function pointToLocation (globalX :Number, globalY :Number, shiftPoint :Point = null) :ClickLocation
    {
        var yOffset :Number = 0;
        var p :Point;
        if (shiftPoint == null) {
            p = new Point(globalX, globalY);

        } else {
            yOffset = (globalY - shiftPoint.y) / this.scaleY;
            p = shiftPoint;
        }

        p = globalToLocal(p);
        var x :Number = p.x;
        var y :Number = p.y;

        var floorWidth :Number, floorInset :Number;
        var xx :Number, yy :Number, zz :Number;
        var scale :Number;
        var clickWall :int;

        // do some partitioning depending on where the y lies
        if (y < _metrics.backWallTop) {
            clickWall = ClickLocation.CEILING;
            scale = _metrics.minScale + (_metrics.backWallTop - y) / _metrics.backWallTop * (MAX_SCALE - _metrics.minScale);

        } else if (y < _metrics.backWallBottom) {
            clickWall = ClickLocation.BACK_WALL;
            scale = _metrics.minScale;

        } else {
            clickWall = ClickLocation.FLOOR;
            scale = _metrics.minScale + (y - _metrics.backWallBottom) / (_metrics.sceneHeight - _metrics.backWallBottom) *
                (MAX_SCALE - _metrics.minScale);
        }

        // see how wide the floor is at that scale
        floorWidth = (_metrics.sceneWidth * scale);
        floorInset = (_metrics.sceneWidth - floorWidth) / 2;

        if (x < floorInset || x - floorInset > floorWidth) {
            if (x < floorInset) {
                clickWall = ClickLocation.LEFT_WALL;
                xx = 0;

            } else {
                clickWall = ClickLocation.RIGHT_WALL;
                xx = MAX_COORD;
            }

            // recalculate floorWidth at the minimum scale
            if (scale != _metrics.minScale) {
                floorWidth = (_metrics.sceneWidth * _metrics.minScale);
                floorInset = (_metrics.sceneWidth - floorWidth) / 2;
            }

            switch (clickWall) {
            case ClickLocation.LEFT_WALL:
                scale = _metrics.minScale + (x / floorInset) * (MAX_SCALE - _metrics.minScale);
                break;

            case ClickLocation.RIGHT_WALL:
                scale = _metrics.minScale + ((_metrics.sceneWidth - x) / floorInset) * (MAX_SCALE - _metrics.minScale);
                break;

            default:
                throw new Error(clickWall);
            }

            // TODO: factor in horizon here
            var wallHeight :Number = (_metrics.sceneHeight * scale);
            var wallInset :Number = (_metrics.sceneHeight - wallHeight) / 2;
            yy = MAX_COORD * (1 - ((y - wallInset) / wallHeight));
            zz = MAX_COORD * ((scale - _metrics.minScale) / (MAX_SCALE - _metrics.minScale));

        } else {
            // normal case: the x coordinate is within the floor width
            // at that scale, so we're definitely not clicking on a side wall
            xx = ((x - floorInset) / floorWidth) * MAX_COORD;

            switch (clickWall) {
            case ClickLocation.CEILING:
            case ClickLocation.FLOOR:
                yy = (clickWall == ClickLocation.CEILING) ? MAX_COORD : 0;
                // if on the floor, we want take into account the yOffset
                if (clickWall == ClickLocation.FLOOR) {
                    yy = -yOffset / (scale * _metrics.sceneHeight);
                    if (yy < 0 || yy > MAX_COORD) {
                        //yy = Math.min(MAX_COORD, Math.max(0, yy));
                        clickWall = ClickLocation.NONSENSE;
                    }
                } else {
                    yy = 0;
                }
                zz = MAX_COORD * (1 - ((scale - _metrics.minScale) / _metrics.scaleRange));
                break;

            case ClickLocation.BACK_WALL:
                // y is simply how high they clicked on the wall
                yy = (_metrics.backWallBottom - y) / _metrics.backWallHeight;
                zz = 1;
                break;

            default:
                throw new Error(clickWall);
            }
        }

        return new ClickLocation(clickWall, new MsoyLocation(xx, yy, zz, 0));
    }

    /**
     * Get the y distance represented by the specified number of pixels for the given z coordinate.
     */
    public function getYDistance (z :Number, pixels :int) :Number
    {
        var scale :Number = _metrics.minScale + ((MAX_COORD - z) / MAX_COORD) * (MAX_SCALE - _metrics.minScale);
        var sheight :Number = _metrics.sceneHeight * scale;
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

        var bounds :Rectangle = getScrollBounds();
        rect.x = Math.min(bounds.x + bounds.width - rect.width,
                          Math.max(bounds.x, rect.x + xpixels));
        scrollRect = rect;
        scrollRectUpdated();
        return true;
    }

    /**
     * Get the current scroll value.
     */
    public function getScrollOffset () :Number
    {
        var r :Rectangle = scrollRect;
        return (r == null) ? 0 : r.x;
    }

    /**
     * Get the full boundaries of our scrolling area.
     * The Rectangle returned may be destructively modified.
     */
    public function getScrollBounds () :Rectangle
    {
        if (_scene == null) {
            return new Rectangle(0, 0, _actualWidth, _actualHeight);
        }

        var r :Rectangle = new Rectangle(0, 0, _scene.getWidth() * scaleX, 
            _scene.getHeight() * scaleY);
        if (_editing) {
            r.inflate(_actualWidth * 2 / 3, 0);
        }
        return r;
    }

    /**
     * Add the specified sprite to this display and have the room track it.
     */
    public function addOtherSprite (sprite :MsoySprite) :void
    {
        _otherSprites.push(sprite);
        addChild(sprite);
        sprite.setLocation(sprite.loc);
    }

    /**
     * Remove the specified sprite.
     */
    public function removeOtherSprite (sprite :MsoySprite) :void
    {
        ArrayUtil.removeAll(_otherSprites, sprite);
        removeSprite(sprite);
    }

    /**
     * Sets the background sprite. If the data value is null, simply removes the old one.
     * Note: this 
     */
    public function setBackground (decordata :DecorData) :void
    {
        if (_bg != null) {
            removeSprite(_bg); 
            _bg = null;
        }
        if (decordata != null) {
            _bg = _ctx.getMediaDirector().getDecor(decordata);
            addChild(_bg);
            setChildIndex(_bg, 0);
            _bg.setLocation(new MsoyLocation(.5, 0, 0)); // center, up front
            _bg.setEditing(_editing);
        }
    }

    /**
     * Retrieves the background sprite, if any.
     */
    public function getBackground () :DecorSprite
    {
        return _bg;
    }
    
    /**
     * Calculate the info needed to perspectivize a piece of furni.
     */
    public function getPerspInfo (sprite :MsoySprite, contentWidth :int, contentHeight :int,
                                  loc :MsoyLocation) :PerspInfo
    {
        var hotSpot :Point = sprite.getMediaHotSpot();
        var mediaScaleX :Number = Math.abs(sprite.getMediaScaleX());
        var mediaScaleY :Number = Math.abs(sprite.getMediaScaleY());

        // below, 0 refers to the right side of the source sprite
        // N refers to the left side, and H refers to the location
        // of the hotspot

        // the scale of the object is determined by the z coordinate
        var distH :Number = RoomMetrics.FOCAL + (_scene.getDepth() * loc.z);
        var dist0 :Number = (hotSpot.x * mediaScaleX);
        var distN :Number = (contentWidth - hotSpot.x) * mediaScaleX;
        if (loc.x < .5) {
            dist0 *= -1;
        } else {
            distN *= -1;
        }

        var scale0 :Number = RoomMetrics.FOCAL / (distH + dist0);
        var scaleH :Number = RoomMetrics.FOCAL / distH;
        var scaleN :Number = RoomMetrics.FOCAL / (distH + distN);

        var logicalY :Number = loc.y + ((contentHeight * mediaScaleY) / _metrics.sceneHeight);

        var p0 :Point = projectedLocation(scale0, loc.x, logicalY);
        var pH :Point = projectedLocation(scaleH, loc.x, loc.y);
        var pN :Point = projectedLocation(scaleN, loc.x, logicalY);

        var height0 :Number = contentHeight * scale0 * mediaScaleY;
        var heightN :Number = contentHeight * scaleN * mediaScaleY;

        // min/max don't account for the hotspot location
        var minX :Number = Math.min(p0.x, pN.x);
        var minY :Number = Math.min(p0.y, pN.y);
        p0.offset(-minX, -minY);
        pN.offset(-minX, -minY);
        pH.offset(-minX, -minY);

        return new PerspInfo(p0, height0, pN, heightN, pH);
    }

    // documentation inherited from interface PlaceView
    public function willEnterPlace (plobj :PlaceObject) :void
    {
        // save our scene object
        _roomObj = (plobj as RoomObject);

        rereadScene();
        updateAllFurni();
    }

    /**
     * (Re)set our scene to the one the scene director knows about.
     */
    public function rereadScene () :void
    {
        setScene(_ctx.getSceneDirector().getScene() as MsoyScene);
    }

    /**
     * Set the scene to be displayed.
     */
    public function setScene (scene :MsoyScene) :void
    {
        _scene = scene;
        _metrics.update(scene.getDecorData());
        _backdrop.setRoom(scene.getDecorData());
        updateDrawnRoom();
        relayout();
    }

    /**
     * Updates the background sprite, in case background data had changed.
     */
    public function updateBackground () :void
    {
        var data :DecorData = _scene.getDecorData();
        if (_bg != null && data != null) {
            _bg.update(data);
        }
    }

    /**
     * Updates background and furniture sprites from their data objects.
     */
    public function updateAllFurni () :void
    {
        if (shouldLoadAll()) {
            for each (var furni :FurniData in _scene.getFurni()) {
                if (!furni.media.isAudio()) {
                    updateFurni(furni);
                }
            }
        }
    }

    // documentation inherited from interface PlaceView
    public function didLeavePlace (plobj :PlaceObject) :void
    {
        removeAll(_furni);
        _roomObj = null;
        _scene = null;
    }

    /**
     * Layout everything.
     */
    protected function relayout () :void
    {
        var scale :Number = (_actualHeight / _metrics.sceneHeight);
        scaleX = scale;
        scaleY = scale;

        configureScrollRect();

        var sprite :MsoySprite;
        for each (sprite in _furni.values()) {
            locationUpdated(sprite);
        }
        for each (sprite in _otherSprites) {
            locationUpdated(sprite);
        }
    }

    /**
     * Should we load everything that we know how to?
     * This is used by a subclass to restrict loading to certain things
     * when the room is first entered.
     */
    protected function shouldLoadAll () :Boolean
    {
        return true;
    }

    /**
     * Configure the rectangle used to select a portion of the view that's showing.
     */
    protected function configureScrollRect () :void
    {
        var bounds :Rectangle = getScrollBounds();
        if (bounds.width > _actualWidth) {
            scrollRect = new Rectangle(0, 0, _actualWidth, _actualHeight);

        } else {
            scrollRect = null;
        }
    }

    protected function scrollRectUpdated () :void
    {
        if (_bg != null && _scene.getSceneType() == Decor.FIXED_IMAGE) {
            locationUpdated(_bg);
        }
    }

    /**
     * Calculate the scale and x/y position of the specified media according to its logical
     * coordinates.
     */
    protected function positionAndScale (sprite :MsoySprite, loc :MsoyLocation) :void
    {
        var info :Array = _metrics.getProjectedInfo(loc);

        var x :Number = Number(info[0]);
        var y :Number = Number(info[1]);
        sprite.setLocationScale(Number(info[2]));

        var hotSpot :Point = sprite.getLayoutHotSpot();
        if (sprite == _bg && _scene.getSceneType() == Decor.FIXED_IMAGE) {
            // adjust the background image
            x += getScrollOffset();
        }
        sprite.x = x - hotSpot.x;
        sprite.y = y - hotSpot.y;
    }

    /**
     * Determine the location of the projected coordinate.
     *
     * @param x the logical x coordinate (0 - 1)
     * @param y the logical y coordinate (0 - 1)
     */
    // TODO: deprecate, fix perspectivization, use the _metrics version
    // of these methods
    protected function projectedLocation (scale :Number, x :Number, y :Number) :Point
    {
        // x position depends on logical x and the scale
        var floorWidth :Number = (_metrics.sceneWidth * scale);
        var floorInset :Number = (_metrics.sceneWidth - floorWidth) / 2;

        return new Point(floorInset + (x * floorWidth),
            _metrics.horizonY + (_metrics.subHorizonHeight - (y * _metrics.sceneHeight)) * scale);
    }

    /**
     * Adjust the z order of the specified sprite so that it is drawn according to its logical Z
     * coordinate relative to other sprites.
     */
    protected function adjustZOrder (sprite :MsoySprite, loc :MsoyLocation) :void
    {
        if (!contains(sprite)) {
            // this can happen if we're resized during editing as we
            // try to reposition a sprite that's still in our data structures
            // but that has been removed as a child.
            return;
        }

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
     * Convenience method to get the logical z coordinate of the child at the specified index.
     */
    protected function getZOfChildAt (index :int) :Number
    {
        var disp :DisplayObject = getChildAt(index);
        if (disp is MsoySprite) {
            var spr :MsoySprite = (disp as MsoySprite);
            if (!spr.isIncludedInLayout()) {
                return NaN;
            }
            return spr.loc.z;
        }
        return Number.MAX_VALUE;
    }

    /**
     */
    protected function setActive (map :HashMap, active :Boolean) :void
    {
        for each (var sprite :MsoySprite in map.values()) {
            if (sprite != _bg) {
                sprite.setActive(active);
            }
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

    protected function addFurni (furni :FurniData) :FurniSprite
    {
        var sprite :FurniSprite = _ctx.getMediaDirector().getFurni(furni);
        addChild(sprite);
        sprite.setLocation(furni.loc);
        _furni.put(furni.id, sprite);
        return sprite;
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

    /**
     * Remove the specified sprite from the view.
     */
    protected function removeSprite (sprite :MsoySprite) :void
    {
        removeChild(sprite);
        _ctx.getMediaDirector().returnSprite(sprite);
    }

    protected function updateDrawnRoom () :void
    {
        _backdrop.drawRoom(this, _actualWidth, _actualHeight, _editing);
    }
    
    /** The msoy context. */
    protected var _ctx :WorldContext;

    /** The model of the current scene. */
    protected var _scene :MsoyScene;

    /** The transitory properties of the current scene. */
    protected var _roomObj :RoomObject;

    /** The actual screen width of this component. */
    protected var _actualWidth :Number;

    /** The actual screen height of this component. */
    protected var _actualHeight :Number;

    /** The RoomMetrics for doing our layout. */
    protected var _metrics :RoomMetrics = new RoomMetrics();

    /** Helper object that draws a room backdrop with four walls. */
    protected var _backdrop :RoomBackdrop = new RoomBackdrop();
    
    /** Our background sprite, if any. */
    protected var _bg :DecorSprite;

    /** A map of id -> Furni. */
    protected var _furni :HashMap = new HashMap();

    /** A list of other sprites (used during editing). */
    protected var _otherSprites :Array = new Array();

    /** Are we editing the scene? */
    protected var _editing :Boolean = false;

    private static const MAX_SCALE :Number = 1;
    private static const MAX_COORD :Number = 1;
}
}
