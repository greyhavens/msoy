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
import com.threerings.util.Log;

import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.SetListener;

import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.flash.DisplayUtil;

import com.threerings.whirled.spot.data.Location;

import com.threerings.msoy.chat.client.ChatOverlay;
import com.threerings.msoy.client.MsoyPlaceView;
import com.threerings.msoy.client.Prefs;
import com.threerings.msoy.item.data.all.Decor;
import com.threerings.msoy.item.data.all.MediaDesc;

import com.threerings.msoy.world.client.layout.RoomLayout;
import com.threerings.msoy.world.client.layout.RoomLayoutFactory;
import com.threerings.msoy.world.data.FurniData;
import com.threerings.msoy.world.data.MsoyLocation;
import com.threerings.msoy.world.data.MsoyScene;
import com.threerings.msoy.world.data.MsoySceneModel;
import com.threerings.msoy.world.data.RoomObject;

public class AbstractRoomView extends Sprite
    implements MsoyPlaceView
{
    public function AbstractRoomView (ctx :WorldContext)
    {
        _ctx = ctx;
        _layout = RoomLayoutFactory.createLayout(null, this);
    }

    /**
     * Returns the layout object responsible for room layout.
     */
    public function get layout () :RoomLayout
    {
        return _layout;
    }

    // from MsoyPlaceView
    public function setPlaceSize (unscaledWidth :Number, unscaledHeight :Number) :void
    {
        _actualWidth = unscaledWidth;
        _actualHeight = unscaledHeight;
        relayout();
        updateEditingOverlay();
    }

    // from MsoyPlaceView
    public function setIsShowing (showing :Boolean) :void
    {
        _showing = showing;
    }

    // from MsoyPlaceView
    public function padVertical () :Boolean
    {
        return true;
    }

    // from MsoyPlaceView
    public function getChatOverlay () :ChatOverlay
    {
        return null;
    }

    // from MsoyPlaceView
    public function setUseChatOverlay (useOverlay :Boolean) :void
    {
        // nada
    }

    /**
     * Are we actually showing?
     */
    public function isShowing () :Boolean
    {
        return _showing;
    }

    /**
     * Called by the editor to have direct access to our sprite list..
     */
    public function getFurniSprites () :HashMap
    {
        return _furni;
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
        _layout.updateScreenLocation(sprite, sprite.getLayoutHotSpot());

        if (sprite == _bg && _scene.getSceneType() == Decor.FIXED_IMAGE) {
            sprite.x += getScrollOffset();
        }
    }

    /**
     * Enable or disable editing. Called by the EditRoomController.
     */
    public function setEditing (editing :Boolean) :void
    {
        _editing = editing;

        // update all sprites
        _furni.forEach(function (key :*, sprite :MsoySprite) :void {
                sprite.setEditing(_editing);
            });
        if (_bg != null) {
            _bg.setEditing(_editing);
        }

        if (_editing) {

            // see if we need to recreate the overlay
            if (_backdropOverlay == null) {
                _backdropOverlay = new BackdropOverlay();
                addChild(_backdropOverlay);
                updateEditingOverlay();
            }

        } else {
            // destroy the overlay if it's still around
            if (_backdropOverlay != null) {
                removeChild(_backdropOverlay);
                _backdropOverlay = null;
            }

            // definitely update the furni
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
        rect.x = Math.min(_scene.getWidth() - bounds.width, Math.max(0, rect.x + xpixels));
        scrollRect = rect;
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
     * Get the full boundaries of our scrolling area in scaled (decor pixel) dimensions.
     * The Rectangle returned may be destructively modified.
     */
    public function getScrollBounds () :Rectangle
    {
        var r :Rectangle = new Rectangle(0, 0, _actualWidth / scaleX, _actualHeight / scaleY);
        if (_scene != null) {
            r.width = Math.min(_scene.getWidth(), r.width);
            r.height = Math.min(_scene.getHeight(), r.height);
        }
        return r;
    }

    /**
     * Get the full boundaries of our scrolling area in unscaled (stage pixel) dimensions.
     * The Rectangle returned may be destructively modified.
     */
    public function getScrollSize () :Rectangle
    {
        var x :int = _actualWidth;
        var y :int = _actualHeight;
        if (_scene != null) {
            x = Math.min(_scene.getWidth() * scaleX, x);
            y = Math.min(_scene.getHeight() * scaleY, y);
        }
        var bottomRight :Point = this.localToGlobal(new Point(x, y));
        var topLeft :Point = this.localToGlobal(new Point(0, 0));
        return new Rectangle(
            topLeft.x, topLeft.y, bottomRight.x - topLeft.x, bottomRight.y - topLeft.y);
    }

    /**
     * Add the specified sprite to this display and have the room track it.
     */
    public function addOtherSprite (sprite :MsoySprite) :void
    {
        _otherSprites.push(sprite);
        addChildAt(sprite, 1);
        relayoutSprite(sprite);
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
     */
    public function setBackground (decor :Decor) :void
    {
        if (_bg != null) {
            removeSprite(_bg);
            _bg = null;
        }
        if (decor != null) {
            _bg = _ctx.getMediaDirector().getDecor(decor);
            addChildAt(_bg, 0);
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
        updateLayout(scene.getDecor());
        _backdrop.update(scene.getDecor());
        updateEditingOverlay();
        relayout();
    }

    /**
     * Updates the layout object, creating a new one if necessary.
     */
    protected function updateLayout (decor :Decor) :void
    {
        if (! (RoomLayoutFactory.isDecorSupported(_layout, decor))) {
            _layout = RoomLayoutFactory.createLayout(decor, this);
        }
        
        _layout.update(decor);
    }
    
    /**
     * Updates the background sprite, in case background data had changed.
     */
    public function updateBackground () :void
    {
        var decor :Decor = _scene.getDecor();
        if (_bg != null && decor != null) {
            _bg.updateFromDecor(decor);
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
        setBackground(null);
        _roomObj = null;
        _scene = null;
    }
    
    override public function set scrollRect (r :Rectangle) :void
    {
        super.scrollRect = r;

        if (_bg != null && _scene.getSceneType() == Decor.FIXED_IMAGE) {
            locationUpdated(_bg);
        }
    }

    /**
     * Layout everything.
     */
    protected function relayout () :void
    {
        var scale :Number = computeScale();
        scaleY = scale;
        scaleX = scale;
        if (!_ctx.getMsoyClient().isFeaturedPlaceView()) {
            y = (_actualHeight - _layout.metrics.sceneHeight * scale) / 2;
        }

        configureScrollRect();

        relayoutSprites(_furni.values());
        relayoutSprites(_otherSprites);
    }

    /**
     * Called from relayout(), relayout the specified sprites.
     */
    protected function relayoutSprites (sprites :Array) :void
    {
        for each (var sprite :MsoySprite in sprites) {
            relayoutSprite(sprite);
        }
    }

    /**
     * Do anything necessary to (re)layout a sprite.
     */
    protected function relayoutSprite (sprite :MsoySprite) :void
    {
        locationUpdated(sprite);
        sprite.roomScaleUpdated();
    }

    /**
     * Returns the scale at which to render our room view.
     */
    protected function computeScale () :Number
    {
        var maxScale :Number = _actualHeight / _layout.metrics.sceneHeight;
        if (isNaN(_fullSizeActualWidth) || !_ctx.getTopPanel().isMinimized()) {
            _fullSizeActualWidth = _actualWidth;
        }
        var minScale :Number = _fullSizeActualWidth / _layout.metrics.sceneWidth;
        if (maxScale > minScale && !_ctx.getMsoyClient().isFeaturedPlaceView()) {
            _ctx.getTopPanel().getControlBar().enableZoomControl(true);
            return minScale + (maxScale - minScale) * Prefs.getZoom();
        } else {
            if (!_ctx.getMsoyClient().isFeaturedPlaceView()) {
                _ctx.getTopPanel().getControlBar().enableZoomControl(false);
            }
            return maxScale;
        }
    }

    /**
     * Should we load everything that we know how to?  This is used by a subclass to restrict
     * loading to certain things when the room is first entered.
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
        if (_scene != null && _actualWidth >= _scene.getWidth()) {
            scrollRect = null;
        } else {
            scrollRect = getScrollBounds();
        }
    }

    /**
     * Sets all sprites in the supplied map to active or non-active.
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
        addChildAt(sprite, 1);
        sprite.setLocation(furni.loc);
        sprite.roomScaleUpdated();
        sprite.setEditing(_editing);
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

    /** For logging. */
    protected const log :Log = Log.getLog(this);

    /** The msoy context. */
    protected var _ctx :WorldContext;

    /** Are we actually showing? */
    protected var _showing :Boolean = true;

    /** The model of the current scene. */
    protected var _scene :MsoyScene;

    /** The transitory properties of the current scene. */
    protected var _roomObj :RoomObject;

    /** The actual screen width of this component. */
    protected var _actualWidth :Number;

    /** The actual width we had last time we weren't minimized */
    protected var _fullSizeActualWidth :Number;

    /** The actual screen height of this component. */
    protected var _actualHeight :Number;

    /** Object responsible for our spatial layout. */
    protected var _layout :RoomLayout;

    /** Helper object that draws a room backdrop with four walls. */
    protected var _backdrop :RoomBackdrop = new RoomBackdrop();

    /** Transparent bitmap on which we can draw the room backdrop.*/
    protected var _backdropOverlay :BackdropOverlay;

    /** Our background sprite, if any. */
    protected var _bg :DecorSprite;

    /** A map of id -> Furni. */
    protected var _furni :HashMap = new HashMap();

    /** A list of other sprites (used during editing). */
    protected var _otherSprites :Array = new Array();

    /** Are we editing the scene? */
    protected var _editing :Boolean = false;
}
}

import flash.display.Shape;

import com.threerings.msoy.world.client.RoomElement;
import com.threerings.msoy.world.data.MsoyLocation;
import com.threerings.msoy.world.data.RoomCodes;

/**
 * This shape is used to draw room edges on top of everything during editing.
 */
internal class BackdropOverlay extends Shape
    implements RoomElement
{
    // documentation inherited from interface RoomElement
    public function getLayoutType () :int
    {
        return RoomCodes.LAYOUT_NORMAL;
    }

    // documentation inherited from interface RoomElement
    public function getRoomLayer () :int
    {
        return RoomCodes.FOREGROUND_EFFECT_LAYER;
    }

    // documentation inherited from interface RoomElement
    public function getLocation () :MsoyLocation
    {
        return DEFAULT_LOCATION;
    }        

    // documentation inherited from interface RoomElement
    public function setLocation (newLoc :Object) :void
    {
        // no op - this object is not placed inside the room
    }

    // documentation inherited from interface RoomElement
    public function setScreenLocation (x :Number, y :Number, scale :Number) :void
    {
        // no op - this object cannot be moved, it's always displayed directly on top of the room
    }

    protected static const DEFAULT_LOCATION :MsoyLocation = new MsoyLocation(0, 0, 0, 0);
}
