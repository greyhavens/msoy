//
// $Id$

package com.threerings.msoy.world.client.editor {

import flash.display.DisplayObject;
import flash.display.Graphics;
import flash.display.Shape;
import flash.events.MouseEvent;
import flash.geom.Point;
import flash.geom.Rectangle;

import com.threerings.msoy.world.client.FurniSprite;
import com.threerings.msoy.world.client.MsoySprite;
import com.threerings.msoy.world.client.RoomView;
import com.threerings.msoy.world.data.MsoyLocation;


/**
 * Tracks the furni sprite that's currently being edited, and decorates it with draggable hotspots.
 */
public class FurniEditor extends FurniHighlight
{
    public function FurniEditor (roomCtrl :RoomEditorController)
    {
        super(roomCtrl);
    }

    // @Override from FurniHighlight
    override public function start () :void
    {
        super.start();

        _resizeHotspot = new ScalingHotspot(this);
        _border.addChild(_resizeHotspot);
        _resizeHotspot.init();
    }

    // @Override from FurniHighlight
    override public function end () :void
    {
        _resizeHotspot.deinit();
        _border.removeChild(_resizeHotspot);
        _resizeHotspot = null;
        super.end();
    }

    /** Accessor to the room view. */
    public function get roomView () :RoomView
    {
        return _roomCtrl.roomView;
    }

    /** Returns true if no hotspot is active (i.e., currently being dragged). */
    public function isIdle () :Boolean
    {
        return _activeHotspot == null;
    }

    /** Called by hotspots, stores a reference to the currently active hotspot. */
    public function setActive (hotspot :Hotspot) :void
    {
        _activeHotspot = hotspot;
    }

    /** Called by hotspots, changes the target's display scale to the specified values. */
    public function updateTargetScale (x :Number, y :Number) :void
    {
        // update furni
        target.setMediaScaleX(x);
        target.setMediaScaleY(y);
        
        _roomCtrl.targetSpriteUpdated();
    }

    // @Override from FurniHighlight
    override protected function clearBorder () :void
    {
        super.clearBorder();
        _resizeHotspot.visible = false;
    }

    // @Override from FurniHighlight
    override protected function repaintBorder () :void
    {
        // note: do not call super - this is a complete replacement.
        // it paints the border and adjusts all hotspots.
        
        var g :Graphics = _border.graphics;
        var w :Number = target.getActualWidth();
        var h :Number = target.getActualHeight();
        var view :RoomView = _roomCtrl.roomView;

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
        g.drawRect(0, 0, w, h);
        g.drawRect(-2, -2, w + 4, h + 4);
        g.drawRect(targetRoot.x - 1, targetRoot.y, 2, (targetLocation.y - targetRoot.y) + 2);

        // draw center lines
        g.lineStyle(0, 0xffffff, 1, true);
        g.drawRect(-1, -1, w + 2, h + 2);
        g.moveTo(targetRoot.x, targetRoot.y);
        g.lineTo(targetLocation.x, targetLocation.y + 1);

        // reset position, so that subsequent fills don't hallucinate that a curve was
        // left open, and needs to be filled in. (you'd think that curves defined *before*
        // a call to beginFill would get ignored, but you'd be wrong.)
        g.moveTo(0, 0);

        // now update hotspot positions
        _resizeHotspot.visible = (_target != null);
        _resizeHotspot.x = w;
        _resizeHotspot.y = 0;
    }
    
    /** Hotspot for resizing the target sprite. */
    protected var _resizeHotspot :Hotspot;

    /** Reference to the currently active hotspot. */
    protected var _activeHotspot :Hotspot;
}
}
