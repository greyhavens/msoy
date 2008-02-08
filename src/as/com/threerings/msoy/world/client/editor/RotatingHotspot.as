//
// $Id$

package com.threerings.msoy.world.client.editor {

import flash.display.DisplayObject;
import flash.events.MouseEvent;
import flash.geom.Point;

import com.threerings.flash.MathUtil;
import com.threerings.util.Log;

import com.threerings.msoy.client.Msgs;

import com.threerings.msoy.world.client.ClickLocation;
import com.threerings.msoy.world.client.RoomMetrics;
import com.threerings.msoy.world.client.FurniSprite;

/**
 * Hotspot that rotates the object.
 */
public class RotatingHotspot extends Hotspot
{
    public function RotatingHotspot (editor :FurniEditor, top :Boolean, left :Boolean)
    {
        super(editor, false);
        _corner = new Point(left ? 0 : 1, top ? 0 : 1);
    }

    // @Override from Hotspot
    override public function updateDisplay (targetWidth :Number, targetHeight :Number) :void
    {
        super.updateDisplay(targetWidth, targetHeight);

        this.x = _corner.x * (targetWidth + this.width) - this.width / 2;
        this.y = _corner.y * (targetHeight + this.height) - this.height / 2;
    }

    // @Override from Hotspot
    override protected function startAction (event :MouseEvent) :void
    {
        super.startAction(event);
        _originalScale = new Point(_editor.target.getMediaScaleX(), _editor.target.getMediaScaleY());
    }

    // @Override from Hotspot
    override protected function updateAction (event :MouseEvent) :void
    {
        super.updateAction(event);
        updateTargetRotation(event.stageX, event.stageY);
    }

    // @Override from Hotspot
    override protected function endAction (event :MouseEvent) :void
    {
        super.endAction(event);
        _originalScale = null;
    }

    // @Override from Hotspot
    override protected function initializeDisplay () :void
    {
        _displayStandard = new HOTSPOT() as DisplayObject;

        var name :String = "HOTSPOT_OVER_" +
            (_corner.y == 0 ? "TOP" : "BOTTOM") + (_corner.x == 0 ? "LEFT" : "RIGHT");

        var c :Class = RotatingHotspot[name] as Class;
        _displayMouseOver = new c() as DisplayObject;
    }

    /** Moves the furni over to the new location. */
    protected function updateTargetRotation (sx :Number, sy :Number) :void
    {
        // testing only! this version just moves the furni around, doesn't actually rotate
        
        sx -= (_anchor.x - _originalHotspot.x);
        sy -= (_anchor.y - _originalHotspot.y);

        var cloc :ClickLocation = _editor.roomView.layout.pointToFurniLocation(
            sx, sy, _editor.target.getLocation(), RoomMetrics.N_UP, false);

        if (! _advancedMode) {
            cloc.loc.y = MathUtil.clamp(cloc.loc.y, 0, 1);
        }
        
        _editor.updateTargetLocation(cloc.loc);
    }

    override protected function getToolTip () :String
    {
        return Msgs.EDITING.get("i.rotation");
    }

    /** Specifies which corner of the furni we occupy. */
    protected var _corner :Point;
    
    /** Sprite scale at the beginning of modifications. Only valid during action. */
    protected var _originalScale :Point;

    /** Bitmap used for hotspot with no scale locking. */
    protected var _displayUnlocked :DisplayObject;

    // Bitmaps galore!
    [Embed(source="../../../../../../../../rsrc/media/skins/button/roomeditor/hotspot_rotate.png")]
    public static const HOTSPOT :Class;
    [Embed(source="../../../../../../../../rsrc/media/skins/button/roomeditor/hotspot_rotate_over_tl.png")]
    public static const HOTSPOT_OVER_TOPLEFT :Class;
    [Embed(source="../../../../../../../../rsrc/media/skins/button/roomeditor/hotspot_rotate_over_tr.png")]
    public static const HOTSPOT_OVER_TOPRIGHT :Class;
    [Embed(source="../../../../../../../../rsrc/media/skins/button/roomeditor/hotspot_rotate_over_bl.png")]
    public static const HOTSPOT_OVER_BOTTOMLEFT :Class;
    [Embed(source="../../../../../../../../rsrc/media/skins/button/roomeditor/hotspot_rotate_over_br.png")]
    public static const HOTSPOT_OVER_BOTTOMRIGHT :Class;
}
}
