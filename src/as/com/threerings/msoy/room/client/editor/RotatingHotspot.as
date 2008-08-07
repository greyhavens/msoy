//
// $Id$

package com.threerings.msoy.room.client.editor {

import flash.display.DisplayObject;
import flash.events.MouseEvent;
import flash.geom.Point;

import com.threerings.flash.MathUtil;
import com.threerings.flash.Vector2;
import com.threerings.util.Log;

import com.threerings.msoy.client.Msgs;

import com.threerings.msoy.room.client.ClickLocation;
import com.threerings.msoy.room.client.MsoySprite;
import com.threerings.msoy.room.client.RoomMetrics;

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
        _originalRotation = _editor.target.getMediaRotation();
        _anchorAngle = Vector2.fromPoints(
            _editor.target.getMediaCentroid(), _editor.target.globalToLocal(_anchor)).angle;
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
        _originalRotation = 0;
        _anchorAngle = 0;
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

    override protected function getToolTip () :String
    {
        return Msgs.EDITING.get("i.rotation");
    }

    /** Moves the furni over to the new location. */
    protected function updateTargetRotation (sx :Number, sy :Number) :void
    {
        var target :MsoySprite = _editor.target;
        if (target == null) {
            return;
        }

        var mouseAngle :Number = Vector2.fromPoints(
            target.getMediaCentroid(), target.globalToLocal(new Point(sx, sy))).angle;
        var delta :Number = MathUtil.toDegrees(mouseAngle - _anchorAngle);

        _editor.updateTargetRotation(MathUtil.normalizeDegrees(_originalRotation + delta));
    }

    /** Specifies which corner of the furni we occupy. */
    protected var _corner :Point;
    
    /** Sprite rotation at the beginning of modifications. Only valid during action. */
    protected var _originalRotation :Number;

    /** Angle to the original mouse anchor (in trig radians). */
    protected var _anchorAngle :Number;
    
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
