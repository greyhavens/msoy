//
// $Id$

package com.threerings.msoy.world.client {

import flash.text.TextField;
import flash.text.TextFieldAutoSize;
import flash.text.TextFormat;

import com.threerings.crowd.data.OccupantInfo;

import com.threerings.msoy.data.ActorInfo;
import com.threerings.msoy.item.web.ItemIdent;
import com.threerings.msoy.item.web.MediaDesc;

import com.threerings.msoy.client.WorldContext;

import com.threerings.msoy.world.data.MsoyLocation;
import com.threerings.msoy.world.data.MsoyScene;
import com.threerings.msoy.world.data.WorldOccupantInfo;

/**
 * Handles sprites for actors (things in a scene that move around).
 */
public class ActorSprite extends MsoySprite
{
    /** The maximum width of an avatar sprite. */
    public static const MAX_WIDTH :int = 300;

    /** The maximum height of an avatar sprite. */
    public static const MAX_HEIGHT :int = 400;

    /**
     * Creates an actor sprite for the supplied occupant.
     */
    public function ActorSprite (occInfo :ActorInfo)
    {
        super(null, null);

        var labelFormat :TextFormat = new TextFormat();
        labelFormat.size = 10;
        labelFormat.bold = true;
        _label = new TextField();
        _label.autoSize = TextFieldAutoSize.CENTER;
        _label.defaultTextFormat = labelFormat;
        addChild(_label);
        
        if (occInfo != null) {
            setActorInfo(occInfo);
        }
    }

    /**
     * Called to set up the actor's initial location upon entering a room.
     */
    public function setEntering (loc :MsoyLocation) :void
    {
        setLocation(loc);
        setOrientation(loc.orient);
    }

    /**
     * Updates this actor's occupant info.
     */
    public function setActorInfo (occInfo :ActorInfo) :void
    {
        var winfo :WorldOccupantInfo = (occInfo as WorldOccupantInfo);
        _occInfo = occInfo;
        if (!winfo.getMedia().equals(_desc)) {
            setup(winfo.getMedia(), winfo.getItemIdent());
        }

        _label.textColor = getStatusColor(_occInfo.status);
        _label.text = _occInfo.username.toString();
        _label.y = -1 * _label.textHeight;
        _label.width = _label.textWidth;
        recheckLabel();
    }

    /**
     * Returns the occupant info for this actor.
     */
    public function getActorInfo () :ActorInfo
    {
        return _occInfo;
    }

    /**
     * Returns the oid of the body that this actor represents.
     */
    public function getOid () :int
    {
        return _occInfo.bodyOid;
    }

    /**
     * Updates the orientation of this actor.
     */
    public function setOrientation (orient :int, report :Boolean = true) :void
    {
        loc.orient = orient;

        // unless instructed otherwise, report that our appearance changed
        if (report) {
            appearanceChanged();
        }
    }

    /**
     * Effects the movement of this actor to a new location in the scene. This just animates the
     * movement, and should be called as a result of the server informing us that we've moved.
     */
    public function moveTo (destLoc :MsoyLocation, scene :MsoyScene) :void
    {
        // if there's already a move, kill it
        if (_move != null) {
            _move.cancel();
        }

        // set the orientation towards the new location
        setOrientation(destLoc.orient, false);

        _move = new SceneMover(this, scene, this.loc, destLoc);
        _move.start();
        appearanceChanged();
    }

    /**
     * @return true if we're moving.
     */
    public function isMoving () :Boolean
    {
        return (_move != null);
    }

    /**
     * Stops the current motion of this actor.
     */
    public function stopMove () :void
    {
        if (_move != null) {
            _move.cancel();
            _move = null;
        }
    }

    override public function getMaxContentWidth () :int
    {
        return MAX_WIDTH;
    }

    override public function getMaxContentHeight () :int
    {
        return MAX_HEIGHT;
    }

    override protected function scaleUpdated () :void
    {
        super.scaleUpdated();
        recheckLabel();
    }

    override protected function contentDimensionsUpdated () :void
    {
        super.contentDimensionsUpdated();
        recheckLabel();
    }

    override public function shutdown (completely :Boolean = true) :void
    {
        if (completely) {
            stopMove();
        }

        super.shutdown(completely);
    }

    /**
     * A callback from our scene mover.
     */
    public function moveCompleted (orient :Number) :void
    {
        _move = null;
        if (parent is RoomView) {
            (parent as RoomView).moveFinished(this);
        }
        appearanceChanged();
    }

    override public function toString () :String
    {
        return "ActorSprite[" + _occInfo.username + " (oid=" + _occInfo.bodyOid + ")]";
    }

    override protected function updateLoadingProgress (
            soFar :Number, total :Number) :void
    {
        var prog :Number = (total == 0) ? 0 : (soFar / total);

        // always clear the old graphics
        graphics.clear();

        // and if we're still loading, draw a line showing progress
        if (prog < 1) {
            graphics.lineStyle(1, 0x00FF00);
            graphics.moveTo(0, -1);
            graphics.lineTo(prog * 100, -1);
            graphics.lineStyle(1, 0xFF0000);
            graphics.lineTo(100, -1);
        }
    }

    override protected function populateControlProperties (o :Object) :void
    {
        super.populateControlProperties(o);

        o["setLocation_v1"] = setLocation_v1;
        o["setOrientation_v1"] = setOrientation_v1;
    }

    /**
     * Called to make sure the label's horizontal position is correct.
     */
    protected function recheckLabel () :void
    {
        // note: may overflow the media area..
        _label.x = ((_w * _locScale) - _label.textWidth) / 2;
    }

    protected function getStatusColor (status :int) :uint
    {
        switch (status) {
        case OccupantInfo.IDLE:
            return 0xFFFF00;

        case OccupantInfo.DISCONNECTED:
            return 0xFF0000;

        default:
            return 0x00FF00;
        }
    }

    /**
     * Called by user code when it wants to change the actor's scene location.
     */
    protected function setLocation_v1 (x :Number, y :Number, z: Number, orient :Number) :void
    {
        if (_ident != null && parent is RoomView) {
            (parent as RoomView).getRoomController().requestMove(
                _ident, new MsoyLocation(x, y, z, orient));
        }
    }

    /**
     * Called by user code when it wants to change the actor's scene orientation.
     */
    protected function setOrientation_v1 (orient :Number) :void
    {
        // TODO
    }

    /**
     * Called when the actor changes orientation or transitions between poses.
     */
    protected function appearanceChanged () :void
    {
        callUserCode("appearanceChanged_v1", [ loc.x, loc.y, loc.z ], loc.orient, isMoving());
    }

    protected var _label :TextField;
    protected var _occInfo :ActorInfo;
    protected var _move :SceneMover;
}
}
