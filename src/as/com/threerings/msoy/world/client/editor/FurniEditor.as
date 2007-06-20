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
 * Component responsible for tracking and highlighting targets of mouse hovers and editing actions.
 */
public class FurniEditor extends FurniHighlight
{
    public function FurniEditor (controller :RoomEditorController)
    {
        super(controller);
    }

    public function mouseMove (sx :Number, sy :Number) :void
    {
        detectHotspot(sx, sy);
    }
    
    override protected function repaintBorder () :void
    {
        // note: do not call super - this is a complete replacement.
        
        var g :Graphics = _border.graphics;
        var w :Number = target.getActualWidth();
        var h :Number = target.getActualHeight();
        var view :RoomView = _controller.roomView;
        
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
        
        // draw sizing knobs
        g.lineStyle(0, 0x000000, 0.5, true);
        g.beginFill(0xffffff, 1.0);
        forAllGrabSpots(function (pixelx :Number, pixely :Number) :void {
                trace("x: " + pixelx + ", y: " + pixely);
                g.drawRect(pixelx - CORNER_SIZE, pixely - CORNER_SIZE,
                           2 * CORNER_SIZE, 2 * CORNER_SIZE);
                           });
        g.endFill();

    }
    
    /** Detects whether mouse lands on one of the hotspots. */
    protected function detectHotspot (sx :Number, sy :Number) :void
    {
        _currentCorner.x = _currentCorner.y = H_NONE;
        _currentArea = H_AREA_NONE;
        
        if (this.target == null) {
            return; // nothing to do
        }

        // convert to sprite location
        var m :Point = this.target.globalToLocal(new Point(sx, sy));

        // find a corner click location. x and y will be assigned values in {-1, 0, 1}
        // corresponding to the click location relative to the border (<+1, 0> is the right
        // border, <+1, +1> is the upper right corner, etc). the special case of <0, 0> means
        // the click did not hit a border hotspot.

        var w :Number = target.getActualWidth();
        var h :Number = target.getActualHeight();

        const e :int = HOTSPOT_TOLERANCE;
        for (var x :int = -1; x <= 1; x++) {
            for (var y :int = -1; y <= 1; y++) {
                var pixelx :Number = (x + 1) * w / 2;
                var pixely :Number = (y + 1) * h / 2;
                if (m.x >= pixelx - e && m.x <= pixelx + e &&
                    m.y >= pixely - e && m.y <= pixely + e)
                {
                    _currentCorner.x =  x;
                    _currentCorner.y = -y; // invert, because pixel origin is in upper right
                }
            }
        }

        // find if the mouse also landed in one of the area hotspots.
        if (m.x >= e && m.x <= w - e && m.y >= e && m.y <= h - e) {
            _currentArea = (m.y <= h * TOP_MULTIPLIER) ? H_AREA_TOP : H_AREA_BOTTOM;
        }

        trace("corner: " + _currentCorner + ", area: " + _currentArea);
    }

    /**
     * Helper iterator, takes a function of the form: function (x :Number, y :Number) :void { },
     * and iterates it over all border grab spots, passing in x and y pixel values of each spot.
     * If no target is selected, no iteration is performed.
     */
    protected function forAllGrabSpots (fn :Function) :void
    {
        if (target != null) {
            var w :Number = target.getActualWidth();
            var h :Number = target.getActualHeight();
            
            for (var x :int = -1; x <= 1; x++) {
                for (var y :int = -1; y <= 1; y++) {
                    if (! (x == 0 && y == 0)) {  // skip the invalid center location
                        var pixelx :Number = (x + 1) * w / 2;
                        var pixely :Number = (y + 1) * h / 2;
                        fn(pixelx, pixely);
                    }
                }
            }
        }
    }

    /** Corner grabbable radius. */
    protected static const CORNER_SIZE :int = 3;
    
    /**
     * Epsilon value expressing hotspot tolerance. Mouse clicks will count as landing on the
     * hotspot if they happen at hotspot location +/- epsilon.
     */
    protected static const HOTSPOT_TOLERANCE :int = 5;

    /**
     * Top area multiplier expressing how much of the top of the sprite contains the
     * y-movement hotspot. */
    protected static const TOP_MULTIPLIER :Number = 1 / 3;

    // Constants that describe corner hotspots being hit during mouse movement.
    protected static const H_NONE  :int = 0;
    protected static const H_LEFT  :int = -1;
    protected static const H_RIGHT :int = 1;
    protected static const H_BOTTOM :int = -1;
    protected static const H_TOP    :int = 1;
    // Constants that describe area hotspots being hit during mouse movement.
    protected static const H_AREA_BOTTOM :int = -1;
    protected static const H_AREA_NONE   :int = 0;
    protected static const H_AREA_TOP    :int = 1;
    
    /**
     * Stores currently selected corner hotspot location, in relative coordinates
     * (x in {H_LEFT, H_NONE, H_RIGHT} and y in {H_TOP, H_NONE, H_BOTTOM}).
     * Special location of <H_NONE, H_NONE> means none of the corners was selected.
     */
    protected var _currentCorner :Point = new Point(H_NONE, H_NONE);

    /**
     * Stores currently selected area hotspot location, as one of the H_AREA_* constants.
     */
    protected var _currentArea :int = H_AREA_NONE;
}
}
