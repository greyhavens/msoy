//
// $Id$

package com.threerings.msoy.world.client {

import flash.display.DisplayObject;

import flash.events.Event;
import flash.events.MouseEvent;

import flash.geom.Rectangle;

import flash.filters.GlowFilter;

import flash.text.TextField;
import flash.text.TextFieldAutoSize;
import flash.text.TextFormat;

import com.threerings.util.Util;

import com.threerings.flash.Animation;
import com.threerings.flash.FilterUtil;
import com.threerings.flash.MediaContainer;

import com.threerings.crowd.data.OccupantInfo;

import com.threerings.msoy.data.ActorInfo;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.all.MediaDesc;

import com.threerings.msoy.client.WorldContext;

import com.threerings.msoy.world.data.MsoyLocation;
import com.threerings.msoy.world.data.MsoyScene;
import com.threerings.msoy.world.data.WorldOccupantInfo;
import com.threerings.msoy.world.data.WorldMemberInfo;
import com.threerings.msoy.world.data.WorldPetInfo;

import com.threerings.msoy.game.data.GameSummary;

/**
 * Handles sprites for actors (things in a scene that move around).
 */
public class ActorSprite extends MsoySprite
{
    /** The maximum width of an avatar sprite. */
    public static const MAX_WIDTH :int = 600;

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
     * Add some sort of nonstandard decoration to the sprite.
     * The decoration should already be painting its full size.
     *
     * @param constraints an object containing properties that will
     *        control layout and other bits.
     *        Supported properties:
     *        toolTip <String> any tool tip text.
     */
    public function addDecoration (dec :DisplayObject, constraints :Object = null) :void
    {
        if (_decorations == null) {
            _decorations = [];
        }
        _decorations.push(dec, constraints);

        addChild(dec);
        arrangeDecorations();
    }

    /**
     * Remove a piece of nonstandard decoration.
     */
    public function removeDecoration (dec :DisplayObject) :void
    {
        if (_decorations != null) {
            var dex :int = _decorations.indexOf(dec);
            if (dex != -1) {
                _decorations.splice(dex, 2);
                removeChild(dec);
                if (_decorations.length == 0) {
                    _decorations = null;

                } else {
                    arrangeDecorations();
                }
            }
        }
    }

    /**
     * Remove all decorations.
     */
    public function removeAllDecorations () :void
    {
        if (_decorations == null) {
            return;
        }
        for (var ii :int = 0; ii < _decorations.length; ii += 2) {
            removeChild(_decorations[ii] as DisplayObject);
        }
        _decorations = null;
    }

    override public function getDesc () :String
    {
        if (_occInfo is WorldPetInfo) {
            return "m.pet";
        }
        return "m.actor";
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
    public function setActorInfo (newInfo :ActorInfo) :void
    {
        var winfo :WorldOccupantInfo = (newInfo as WorldOccupantInfo);
        var newMedia :MediaDesc = winfo.getMedia();
        if (!newMedia.equals(_desc)) {
            setup(newMedia, winfo.getItemIdent());
        }

        // take care of setting up or changing our TableIcon
        if (winfo is WorldMemberInfo) {
            var minfo :WorldMemberInfo = winfo as WorldMemberInfo;

            if (_tableIcon != null && !_tableIcon.getGameSummary().equals(minfo.pendingGame)) {
                _tableIcon.shutdown();
                _tableIcon = null;
            }
            if (_tableIcon == null && minfo.pendingGame != null) {
                _tableIcon = new TableIcon(this, minfo.pendingGame);
            }
        }

        if (_occInfo == null || (_occInfo.status != newInfo.status) ||
                !_occInfo.username.equals(newInfo.username)) {
            _label.textColor = getStatusColor(newInfo.status);
            _label.text = newInfo.username.toString();
            _label.width = _label.textWidth + 5; // the magic number
            _label.height = _label.textHeight + 4;
            _label.y = -1 * _label.height;
            recheckLabel();
            arrangeDecorations();
        }

        // note the old info...
        var oldWinfo :WorldOccupantInfo = (_occInfo as WorldOccupantInfo);

        // assign the new one
        _occInfo = newInfo;

        // finally, if the state has changed, dispatch an event
        // (we don't dispatch if the old info was null, as getting our initial
        // state isn't a "change") This is another argument for a special
        // state-changed dobj event.
        if (oldWinfo != null && !Util.equals(oldWinfo.getState(), winfo.getState())) {
            callUserCode("stateSet_v1", winfo.getState());
        }
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
        _loc.orient = orient;

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

        _walk = new WalkAnimation(this, scene, _loc, destLoc);
        _walk.start();
        appearanceChanged();
    }

//    public function whirlOut (scene :MsoyScene) :void
//    {
//        _walk = new WhirlwindAnimation(this, scene, loc);
//        _walk.start();
//    }
//
//    public function whirlDone () :void
//    {
//        _walk = null;
////        if (parent is RoomView) {
////            (parent as RoomView).whirlDone(this);
////        }
//    }

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

    override public function mouseClick (event :MouseEvent) :void
    {
        // see if it actually landed on a decoration
        var dec :DisplayObject = getDecorationAt(event.stageX, event.stageY);
        if (dec != null) {
            // deliver it there
            dec.dispatchEvent(event);

        } else {
            // otherwise, do the standard thing
            super.mouseClick(event);
        }
    }

    override public function setHovered (hovered :Boolean, stageX :int = 0, stageY :int = 0) :String
    {
        // see if we're hovering over a new decoration..
        var decorTip :String = null;
        var hoverDec :DisplayObject = hovered ? getDecorationAt(stageX, stageY) : null;
        if (hoverDec != _hoverDecoration) {
            if (_hoverDecoration != null) {
                _hoverDecoration.dispatchEvent(new MouseEvent(MouseEvent.MOUSE_OUT));
            }
            _hoverDecoration = hoverDec;
            if (_hoverDecoration != null) {
                _hoverDecoration.dispatchEvent(new MouseEvent(MouseEvent.MOUSE_OVER));
            }
        }
        if (hoverDec != null) {
            decorTip = getDecorationConstraint(hoverDec)["toolTip"];
        }

        // always call super, but hover is only true if we hit no decorations
        var superTip :String = super.setHovered((_hoverDecoration == null) && hovered);
        return (_hoverDecoration == null) ? superTip : decorTip;
    }

    override protected function setGlow (glow :Boolean) :void
    {
        if (!glow) {
            FilterUtil.removeFilter(_label, _glow);
        }

        super.setGlow(glow);

        if (glow) {
            FilterUtil.addFilter(_label, _glow);
        }
    }

    override protected function scaleUpdated () :void
    {
        super.scaleUpdated();
        recheckLabel();
        arrangeDecorations();
    }

    override protected function contentDimensionsUpdated () :void
    {
        super.contentDimensionsUpdated();
        recheckLabel();
        arrangeDecorations();
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
        _label.x = (getActualWidth() - _label.width) / 2;
    }

    /**
     * Arrange any external decorations above our name label.
     */
    protected function arrangeDecorations () :void
    {
        if (_decorations == null) {
            return;
        }

        // place the decorations over the name label, with our best
        // guess as to their size
        var ybase :Number = _label.y;
        for (var ii :int = 0; ii < _decorations.length; ii += 2) {
            var dec :DisplayObject = DisplayObject(_decorations[ii]);
            var rect :Rectangle = dec.getRect(dec);
            ybase -= (rect.height + DECORATION_PAD);
            dec.x = (getActualWidth() - rect.width) / 2 - rect.x
            dec.y = ybase - rect.y;
        }
    }

    /**
     * Return the decoration under the specified stage coordinates.
     */
    protected function getDecorationAt (stageX :int, stageY :int) :DisplayObject
    {
        if (_decorations != null) {
            for (var ii :int = 0; ii < _decorations.length; ii += 2) {
                var disp :DisplayObject = (_decorations[ii] as DisplayObject);
                if (disp.hitTestPoint(stageX, stageY, true)) {
                    return disp;
                }
            }
        }
        return null;
    }

    /**
     * Get the associated constraint object for the specified decoration.
     * Always returns non-null.
     */
    protected function getDecorationConstraint (decoration :DisplayObject) :Object
    {
        var cons :Object = null;
        if (_decorations != null) {
            for (var ii :int = 0; ii < _decorations.length; ii += 2) {
                if (_decorations[ii] == decoration) {
                    cons = _decorations[ii + 1];
                    break;
                }
            }
        }
        return (cons != null) ? cons : {};
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
        Log.getLog(this).debug("user-set orientation is currently TODO.");
    }

    /**
     * Update the actor's state.
     * Called by user code when it wants to change the actor's state.
     */
    public function setState (state :String) :void
    {
        if (_ident != null && (parent is RoomView) && validateUserData(state, null)) {
            (parent as RoomView).getRoomController().setActorState(
                _ident, _occInfo.bodyOid, state);
        }
    }

    /**
     * Get the actor's current state.
     * Called by user code.
     */
    public function getState () :String
    {
        return (_occInfo as WorldOccupantInfo).getState();
    }

    /**
     * Called when the actor changes orientation or transitions between poses.
     */
    protected function appearanceChanged () :void
    {
        callUserCode("appearanceChanged_v1", [ _loc.x, _loc.y, _loc.z ], _loc.orient, isMoving());
    }

    protected var _label :TextField;
    protected var _occInfo :ActorInfo;
    protected var _walk :Animation;

    /** A decoration used when we're in a table in a lobby. */
    protected var _tableIcon :TableIcon;

    /** Display objects to be shown above the name for this actor,
     * configured by external callers. */
    protected var _decorations :Array;

    /** The current decoration being hovered over, if any. */
    protected var _hoverDecoration :DisplayObject;

    protected static const DECORATION_PAD :int = 5;
}
}

import flash.events.MouseEvent;

import flash.filters.GlowFilter;

import com.threerings.msoy.client.MsoyController;

import com.threerings.msoy.game.data.GameSummary;

import com.threerings.msoy.world.client.ActorSprite;

import com.threerings.msoy.ui.ScalingMediaContainer;

import com.threerings.util.CommandEvent;

/**
 * A decoration used when this actor is at a table in a lobby.
 */
class TableIcon extends ScalingMediaContainer
{
    public function TableIcon (host :ActorSprite, gameSummary :GameSummary)
    {
        super(30, 30);
        _host = host;
        _gameSummary = gameSummary;
        setMedia(gameSummary.getThumbMedia().getMediaPath());

        addEventListener(MouseEvent.MOUSE_OVER, handleMouseIn);
        addEventListener(MouseEvent.MOUSE_OUT, handleMouseOut);
        addEventListener(MouseEvent.CLICK, handleMouseClick);
    }

    public function getGameSummary () :GameSummary
    {
        return _gameSummary;
    }

    override public function shutdown (completely :Boolean = true) :void
    {
        if (parent != null) {
            _host.removeDecoration(this);
        }
        super.shutdown(completely);
    }

    override protected function contentDimensionsUpdated () :void
    {
        super.contentDimensionsUpdated();

        // we wait until our size is known prior to adding ourselves
        if (parent == null) {
            _host.addDecoration(this, { toolTip: _gameSummary.name });
        }
    }

    protected function handleMouseIn (... ignored) :void
    {
        // TODO?
        this.filters = [ new GlowFilter(0x00ff00, 1, 32, 32, 2) ];
    }

    protected function handleMouseOut (... ignored) :void
    {
        this.filters = null;
    }

    protected function handleMouseClick (... ignored) :void
    {
        CommandEvent.dispatch(this, MsoyController.JOIN_GAME_LOBBY, _gameSummary.gameId);
    }

    protected var _gameSummary :GameSummary;

    protected var _host :ActorSprite;
}
