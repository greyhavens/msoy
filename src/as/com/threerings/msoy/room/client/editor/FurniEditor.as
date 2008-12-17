//
// $Id$

package com.threerings.msoy.room.client.editor {

import flash.display.Graphics;
import flash.geom.Point;

import com.threerings.flash.GraphicsUtil;

import com.threerings.msoy.room.client.FurniSprite;
import com.threerings.msoy.room.client.RoomView;
import com.threerings.msoy.room.data.FurniData;
import com.threerings.msoy.room.data.MsoyLocation;

/**
 * Tracks the furni sprite that's currently being edited, and decorates it with draggable hotspots.
 */
public class FurniEditor extends FurniHighlight
{
    public function FurniEditor (controller :RoomEditorController)
    {
        super(controller);
    }

    /** Accessor to the controller. */
    public function get controller () :RoomEditorController
    {
        return _controller;
    }

    /** Accessor to the room view. */
    public function get roomView () :RoomView
    {
        return _controller.roomView;
    }

    /** Returns the hotspot to be used when the user immediately clicks and drags. */
    public function get defaultHotspot () :Hotspot
    {
        return _defaultHotspot;
    }

    /** Returns true if no hotspot is active (i.e., currently being dragged). */
    public function isIdle () :Boolean
    {
        return _activeHotspot == null;
    }

    /** Called by hotspots, stores a reference to the currently active hotspot. */
    public function setActive (hotspot :Hotspot) :void
    {
        if (hotspot != null) {
            // we just started a new action - make a copy of the target's data
            _originalTargetData = target.getFurniData().clone() as FurniData;
        } else {
            // the action just finished - wrap up.
            if (_originalTargetData != null) {
                _controller.updateFurni(_originalTargetData, target.getFurniData());
                _originalTargetData = null;
            }
        }

        _activeHotspot = hotspot;
    }

    /** Called by hotspots, changes the target's display scale to the specified values. */
    public function updateTargetScale (x :Number, y :Number) :void
    {
        // update furni
        target.setMediaScaleX(x);
        target.setMediaScaleY(y);

        _controller.targetSpriteUpdated();
    }

    /** Called by hotspots, changes the target's display position. */
    public function updateTargetLocation (loc :MsoyLocation) :void
    {
        target.setLocation(loc);          // change position on screen...
        target.getFurniData().loc = loc;  // ...and in the data parameters

        _controller.targetSpriteUpdated();
    }

    /** Called by hotspots, changes the target's rotation. */
    public function updateTargetRotation (rotation :Number) :void
    {
        target.setMediaRotation(rotation);
        _controller.targetSpriteUpdated();
    }

    /** Updates advanced editing state on all hotspots. */
    public function setAdvancedMode (advanced :Boolean) :void
    {
        for each (var hotspot :Hotspot in _hotspots) {
            hotspot.setAdvancedMode(advanced);
        }
        
        updateDisplay();
    }
    
    // @Override from FurniHighlight
    override public function start () :void
    {
        super.start();

        _hotspots = new Array();
        _hotspots.push(_defaultHotspot = new MovementWallHotspot(this));
        _hotspots.push(new MovementXZHotspot(this));
        _hotspots.push(new MovementYHotspot(this));

        for each (var left :Boolean in [ true, false ]) {
            for each (var top :Boolean in [ true, false]) {
                _hotspots.push(new RotatingHotspot(this, top, left));
                _hotspots.push(new ScalingHotspot(this, top, left));
            }
        }

        for each (var hotspot :Hotspot in _hotspots) {
            _border.addChild(hotspot);
            hotspot.init();
        }
    }

    // @Override from FurniHighlight
    override public function end () :void
    {
        for each (var hotspot :Hotspot in _hotspots) {
            hotspot.deinit();
            _border.removeChild(hotspot);
        }

        super.end();
    }

    // @Override from FurniHighlight
    override public function set target (sprite :FurniSprite) :void
    {
        super.target = sprite;
        _controller.updateTargetSelected();
    }

    // @Override from FurniHighlight
    override protected function clearBorder () :void
    {
        super.clearBorder();
        for each (var hotspot :Hotspot in _hotspots) {
                hotspot.updateVisible(false);
        }
    }

    // @Override from FurniHighlight
    override protected function repaintBorder () :void
    {
        // note: do not call super - this is a complete replacement.
        // it paints the border and adjusts all hotspots.

        var g :Graphics = _border.graphics;
        var w :Number = target.getActualWidth();
        var h :Number = target.getActualHeight();
        var view :RoomView = _controller.roomView;

        g.clear();

        // compute location info for the stem from the current location to the floor

        // get target location in room and stage coordinates
        var roomLocation :MsoyLocation = target.getLocation();
        var stageLocation :Point = view.localToGlobal(view.layout.locationToPoint(roomLocation));
        var targetLocation :Point = target.globalToLocal(stageLocation);

        // get stem root location by dropping the target y value, and converting back to screen
        var roomRoot :MsoyLocation = new MsoyLocation(roomLocation.x, 0, roomLocation.z, 0);
        var stageRoot :Point = view.localToGlobal(view.layout.locationToPoint(roomRoot));
        var targetRoot :Point = target.globalToLocal(stageRoot);

        // draw outer and inner outlines
        g.lineStyle(0, 0x000000, 0.5, true);
        GraphicsUtil.dashRect(g, 0, 0, w, h);
        GraphicsUtil.dashRect(g, -2, -2, w + 4, h + 4);
        g.drawRect(targetRoot.x - 1, targetRoot.y, 2, (targetLocation.y - targetRoot.y) + 2);

        // draw center lines
        g.lineStyle(0, 0xffffff, 1, true);
        GraphicsUtil.dashRect(g, -1, -1, w + 2, h + 2);
        g.moveTo(targetRoot.x, targetRoot.y);
        g.lineTo(targetLocation.x, targetLocation.y + 1);

        // reset position, so that subsequent fills don't hallucinate that a curve was
        // left open, and needs to be filled in. (you'd think that curves defined *before*
        // a call to beginFill would get ignored, but you'd be wrong.)
        g.moveTo(0, 0);

        // now update hotspot positions
        for each (var hotspot :Hotspot in _hotspots) {
            hotspot.updateVisible(_target != null);
            hotspot.updateDisplay(w, h);
        }
    }

    /** Copy of the target's original furni data, created when the user activates a hotspot. */
    protected var _originalTargetData :FurniData;

    /** The default hotspot; chosen if the player immediately clicks and drags a furni. */
    protected var _defaultHotspot :Hotspot;

    /** Reference to the currently active hotspot. */
    protected var _activeHotspot :Hotspot;

    /** Array of all Hotspot instances (initialized in the constructor). */
    protected var _hotspots :Array; // of Hotspot references
}
}
