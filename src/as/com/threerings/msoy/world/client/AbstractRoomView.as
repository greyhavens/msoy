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
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.Decor;
import com.threerings.msoy.world.data.DecorData;
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
        _layout = new RoomLayout(this);
    }

    /**
     * Returns the layout object responsible for room layout.
     */
    public function get layout () :RoomLayout
    {
        return _layout;
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
        _layout.updateScreenLocation(sprite, sprite.getLayoutHotSpot());
        
        if (sprite == _bg && _scene.getSceneType() == Decor.FIXED_IMAGE) {
            sprite.x += getScrollOffset();
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
        addChildAt(sprite, 1);
        locationUpdated(sprite);
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
            addChildAt(_bg, 0);
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
        _layout.update(scene.getDecorData());
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
        var scale :Number = (_actualHeight / _layout.metrics.sceneHeight);
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

    /** Object responsible for our spatial layout. */
    protected var _layout :RoomLayout;

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

}
}
