//
// $Id$

package com.threerings.msoy.world.client {

import flash.text.TextField;
import flash.text.TextFieldAutoSize;
import flash.text.TextFormat;

import flash.filters.GlowFilter;

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
    public static const MAX_WIDTH :int = 400;

    /** The maximum height of an avatar sprite. */
    public static const MAX_HEIGHT :int = 450;

    /**
     * Creates an actor sprite for the supplied occupant.
     */
    public function ActorSprite (occInfo :ActorInfo)
    {
        super(null, null);

        var labelFormat :TextFormat = new TextFormat();
        labelFormat.font = "Arial"; // there be magic here. Arial isn't
        // even available on Linux, but it works it out. The documentation
        // for TextFormat does not indicate this. Bastards.
        labelFormat.size = 12;
        labelFormat.bold = true;
        _label = new TextField();
        _label.autoSize = TextFieldAutoSize.CENTER;
        _label.defaultTextFormat = labelFormat;
        _label.filters = [ new GlowFilter(0, 1, 2, 2, 255) ];
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
        var newMedia :MediaDesc = winfo.getMedia();
        if (!newMedia.equals(_desc)) {
            setup(newMedia, winfo.getItemIdent());
        }

        _label.textColor = getStatusColor(_occInfo.status);
        _label.text = _occInfo.username.toString();
        _label.y = -1 * (_label.textHeight + 4);
        _label.width = _label.textWidth + 5; // the magic number
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
        if (_walk != null) {
            _walk.stop();
        }

        // set the orientation towards the new location
        setOrientation(destLoc.orient, false);

        _walk = new WalkAnimation(this, scene, this.loc, destLoc);
        _walk.start();
        appearanceChanged();
    }

    /**
     * @return true if we're moving.
     */
    public function isMoving () :Boolean
    {
        return (_walk != null);
    }

    /**
     * Stops the current motion of this actor.
     */
    public function stopMove () :void
    {
        if (_walk != null) {
            _walk.stop();
            _walk = null;
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
     * A callback from our walk animations.
     */
    public function walkCompleted (orient :Number) :void
    {
        _walk = null;
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

    /**
     * Called to make sure the label's horizontal position is correct.
     */
    protected function recheckLabel () :void
    {
        // note: may overflow the media area..
        _label.x = (getActualWidth() - _label.textWidth) / 2;
    }

    protected function getStatusColor (status :int) :uint
    {
        switch (status) {
        case OccupantInfo.IDLE:
            return 0xFFFFFF;

        case OccupantInfo.DISCONNECTED:
            return 0xFF0000;

        default:
            return 0x99BFFF;
        }
    }

    override protected function createBackend () :EntityBackend
    {
        return new ActorBackend();
    }

    /**
     * Update the actor's scene location.
     * Called by our backend in response to a request from usercode.
     */
    internal function setLocationFromUser (x :Number, y :Number, z: Number, orient :Number) :void
    {
        if (_ident != null && parent is RoomView) {
            (parent as RoomView).getRoomController().requestMove(
                _ident, new MsoyLocation(x, y, z, orient));
        }
    }

    /**
     * Update the actor's orientation.
     * Called by user code when it wants to change the actor's scene location.
     */
    internal function setOrientationFromUser (orient :Number) :void
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
    protected var _walk :WalkAnimation;
}
}
