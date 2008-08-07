//
// $Id$

package com.threerings.msoy.room.client.editor {

import flash.display.DisplayObject;
import flash.events.MouseEvent;
import flash.geom.Point;

import com.threerings.flash.MathUtil;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.room.client.ClickLocation;
import com.threerings.msoy.room.client.FurniSprite;
import com.threerings.msoy.room.client.RoomMetrics;
import com.threerings.msoy.room.data.MsoyLocation;

/**
 * Hotspot that moves the target object on the XY plane at current depth. 
 */
public class SpriteDragHotspot extends Hotspot
{
    public function SpriteDragHotspot (editor :FurniEditor)
    {
        super(editor);
    }

    // @Override from Hotspot
    override protected function initializeDisplay () :void
    {
        // do not call super - this hotspot doesn't display anything :)
    }
    
    // @Override from Hotspot
    override protected function updateAction (event :MouseEvent) :void
    {
        super.updateAction(event);
        updateTargetLocation(event.stageX, event.stageY);
    }

    /** Moves the furni over to the new location. */
    protected function updateTargetLocation (sx :Number, sy :Number) :void
    {
        sx -= (_anchor.x - _originalHotspot.x);
        sy -= (_anchor.y - _originalHotspot.y);

        var fz :Number = _editor.target.getLocation().z;
        var loc :MsoyLocation = _editor.roomView.layout.pointToLocationAtDepth(sx, sy, fz);
        if (loc != null) {
            if (! _advancedMode) {
                loc.x = MathUtil.clamp(loc.x, 0, 1);
                loc.y = MathUtil.clamp(loc.y, 0, 1);
            }
            
            _editor.updateTargetLocation(loc);
        }
    }
}
}
