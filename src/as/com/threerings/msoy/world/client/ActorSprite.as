//
// $Id$

package com.threerings.msoy.world.client {

import mx.controls.Label;

import com.threerings.crowd.data.OccupantInfo;

import com.threerings.msoy.data.ActorInfo;
import com.threerings.msoy.item.web.ItemIdent;
import com.threerings.msoy.item.web.MediaDesc;

import com.threerings.msoy.client.MsoyContext;

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

        _label = new Label();
        _label.includeInLayout = false;
        _label.setStyle("textAlign", "center");
        _label.setStyle("fontWeight", "bold");
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

        _label.setStyle("color", getStatusColor(_occInfo.status));
        _label.text = _occInfo.username.toString();
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

        _move = new SceneMove(this, scene, this.loc, destLoc);
        _move.play();
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

    override public function moveCompleted (orient :Number) :void
    {
        super.moveCompleted(orient);

        _move = null;
        if (parent is RoomView) {
            (parent as RoomView).moveFinished(this);
        }
        appearanceChanged();
    }

    /**
     * Called to make sure the label's width and position are correct.
     */
    protected function recheckLabel () :void
    {
        // make it the right size
        _label.width = _w * _locScale;

        // this can't be done until the text is set and the label is
        // part of the hierarchy. We just recheck it often...
        _label.y = -1 * _label.textHeight;
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
     * Called when the actor changes orientation or transitions between poses.
     */
    protected function appearanceChanged () :void
    {
        callUserCode("appearanceChanged_v1", isMoving(), loc.orient);
    }

    protected var _label :Label;
    protected var _occInfo :ActorInfo;
    protected var _move :SceneMove;
}
}
