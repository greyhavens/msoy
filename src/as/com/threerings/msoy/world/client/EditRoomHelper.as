package com.threerings.msoy.world.client {

import flash.events.MouseEvent;
import flash.events.KeyboardEvent;

import flash.display.Graphics;

import flash.geom.Point;

import flash.ui.Keyboard;

import com.threerings.msoy.world.data.MsoyLocation;

public class EditRoomHelper
{
    public function EditRoomHelper (roomView :RoomView)
    {
        _roomView = roomView;

        _roomView.setEditing(true, enableEditingVisitor);
    }

    public function endEditing () :void
    {
        _roomView.setEditing(false, disableEditingVisitor);
    }

    protected function enableEditingVisitor (key :Object, value :Object) :void
    {
        var sprite :MsoySprite = (value as MsoySprite);
        sprite.setEditing(true);

        addEditingListeners(sprite);
    }

    protected function disableEditingVisitor (key :Object, value :Object) :void
    {
        var sprite :MsoySprite = (value as MsoySprite);

        sprite.removeEventListener(MouseEvent.CLICK, spriteClicked);
        sprite.removeEventListener(MouseEvent.MOUSE_OVER, spriteRollOver);
        sprite.removeEventListener(MouseEvent.MOUSE_OUT, spriteRollOut);

        sprite.setEditing(false);
    }

    protected function addEditingListeners (sprite :MsoySprite) :void
    {
        sprite.addEventListener(MouseEvent.CLICK, spriteClicked);
        sprite.addEventListener(MouseEvent.MOUSE_OVER, spriteRollOver);
        sprite.addEventListener(MouseEvent.MOUSE_OUT, spriteRollOut);
    }

    protected function spriteClicked (event :MouseEvent) :void
    {
        if (_editSprite != null) {
            return;
        }
        // Stop event prop, otherwise the roomView registers the click too
        // causing spritePositioned() to be called.
        event.stopPropagation();

        // determine the edit sprite
        _editSprite = (event.currentTarget as MsoySprite);

        var hs :Point = _editSprite.hotSpot;
        _xoffset = event.localX - hs.x;
        _yoffset = event.localY - hs.y;

        // TODO: determine whether we're going to alter position, scaleX,
        // scaleY, both scales, or what
        _editSprite.removeEventListener(MouseEvent.CLICK, spriteClicked);
        _editSprite.removeEventListener(MouseEvent.MOUSE_OVER, spriteRollOver);
        _editSprite.removeEventListener(MouseEvent.MOUSE_OUT, spriteRollOut);

        // if (editingPosition)
        _roomView.addEventListener(MouseEvent.MOUSE_MOVE, spritePositioning);
        _roomView.addEventListener(MouseEvent.CLICK, spritePositioned);
    }

    protected function spriteRollOver (event :MouseEvent) :void
    {
        var sprite :MsoySprite = (event.currentTarget as MsoySprite);

        var w :Number = sprite.contentWidth;
        var h :Number = sprite.contentHeight;
        var g :Graphics = sprite.graphics;
        g.clear();
        g.lineStyle(3, 0xFFFFFF);
        g.moveTo(0, 15);
        g.lineTo(0, 0);
        g.lineTo(15, 0);

        g.moveTo(w - 15, 0);
        g.lineTo(w, 0);
        g.lineTo(w, 15);

        g.moveTo(w, h - 15);
        g.lineTo(w, h);
        g.lineTo(w - 15, h);

        g.moveTo(15, h);
        g.lineTo(0, h);
        g.lineTo(0, h - 15);
    }

    protected function spriteRollOut (event :MouseEvent) :void
    {
        var sprite :MsoySprite = (event.currentTarget as MsoySprite);
        sprite.graphics.clear();
    }

    /**
     * Listens for MOUSE_MOVE when positioning a sprite.
     */
    protected function spritePositioning (event :MouseEvent) :void
    {
        var newLoc :MsoyLocation = _roomView.pointToLocation(
            event.stageX, event.stageY);
        if (newLoc != null) {
            newLoc.y = _editSprite.loc.y;
            _editSprite.setLocation(newLoc);
        }
    }

    /**
     * Listens for CLICK when positioning a sprite.
     */
    protected function spritePositioned (event :MouseEvent) :void
    {
        trace("Sprite positioned");
        _roomView.removeEventListener(MouseEvent.MOUSE_MOVE, spritePositioning);
        _roomView.removeEventListener(MouseEvent.CLICK, spritePositioned);

        addEditingListeners(_editSprite);
        _editSprite = null;
    }

    /** The room view. */
    protected var _roomView :RoomView;

    /** The offset from the clicked point to the object's hotspot. */
    protected var _xoffset :Number;
    protected var _yoffset :Number;

    protected var _editSprite :MsoySprite;
}
}
