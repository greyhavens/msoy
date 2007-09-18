//
// $Id$

package com.threerings.msoy.world.client {

import flash.display.DisplayObject;

import flash.events.Event;
import flash.events.MouseEvent;

import flash.geom.Point;
import flash.geom.Rectangle;

import flash.filters.GlowFilter;

import flash.text.TextField;
import flash.text.TextFieldAutoSize;
import flash.text.TextFormat;

import com.threerings.util.Util;
import com.threerings.util.ValueEvent;

import com.threerings.flash.Animation;
import com.threerings.flash.FilterUtil;
import com.threerings.flash.MediaContainer;
import com.threerings.flash.TextFieldUtil;

import com.threerings.crowd.data.OccupantInfo;

import com.threerings.msoy.data.ActorInfo;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.all.MediaDesc;

import com.threerings.msoy.client.WorldContext;

import com.threerings.msoy.world.data.EffectData;
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

    /** The default move speed, in pixels per second. */
    public static const DEFAULT_MOVE_SPEED :Number = 500;

    /** The minimum move speed, in pixels per second. */
    public static const MIN_MOVE_SPEED :Number = 50;

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
        // The label often jumps visibly when the actor is hovered over, a pixel
        // up or down, and/or left or right. As far as I (Ray) can figure, when the glow
        // filter is applied it's doing pixel snapping. The strange thing is that we apply
        // our own outlining glow filter (below) so it should already be snapping.
        // It seems that setting cacheAsBitmap makes the vertical jumpiness go away, but
        // not the horizontal jumpiness. So, I've disabled it for now...
        //_label.cacheAsBitmap = true;
        _label.selectable = false;
        _label.autoSize = TextFieldAutoSize.CENTER;
        _label.defaultTextFormat = labelFormat;
        _label.filters = [ new GlowFilter(0, 1, 2, 2, 255) ];
        addChild(_label);
        
        if (occInfo != null) {
            setActorInfo(occInfo);
        }
    }

    /**
     * Add a special effect to this actor.
     */
    public function addTransientEffect (effect :EffectData) :void
    {
        var sprite :EffectSprite = new EffectSprite(effect);
        sprite.addEventListener(MsoySprite.LOCATION_UPDATED, handleEffectUpdated);
        sprite.addEventListener(EffectSprite.EFFECT_FINISHED, handleEffectFinished);
        sprite.x = getActualWidth()/2;
        addChild(sprite);
    }

    /**
     * Add some sort of nonstandard decoration to the sprite.  The decoration should already be
     * painting its full size.
     *
     * @param constraints an object containing properties that will control layout and other bits.
     * Supported properties:
     *   toolTip <String> any tool tip text.
     *   weight <Number> a sort order, higher numbers will be closer to the name. (0 if missing)
     *   bounds <Rectangle> hand-specify the bounds (useful for SWFs)
     */
    public function addDecoration (dec :DisplayObject, constraints :Object = null) :void
    {
        if (_decorations == null) {
            _decorations = [];
        }
        // provide a default constraints if none
        if (constraints == null) {
            constraints = {};
        }
        // store the decoration inside the constraints object
        constraints["dec"] = dec;
        _decorations.push(constraints);

        // TODO: is there no stable sort available to us?
        // For now, I'll just sort, but I don't like it!
        _decorations.sort(decorationSort);

        addChild(dec);
        arrangeDecorations();
    }

    /**
     * Remove a piece of nonstandard decoration.
     */
    public function removeDecoration (dec :DisplayObject) :void
    {
        if (_decorations != null) {
            for (var ii :int = 0; ii < _decorations.length; ii++) {
                if (_decorations[ii].dec == dec) {
                    _decorations.splice(ii, 1);
                    removeChild(dec);
                    if (_decorations.length == 0) {
                        _decorations = null;

                    } else {
                        arrangeDecorations();
                    }

                    return; // no need to continue
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
        for (var ii :int = 0; ii < _decorations.length; ii++) {
            removeChild(_decorations[ii].dec as DisplayObject);
        }
        _decorations = null;
    }

    /**
     * Return the walk speed of this actor, in pixels / second.
     */
    public function getMoveSpeed () :Number
    {
        return Math.max(MIN_MOVE_SPEED, _moveSpeed * _scale);
    }

    override public function getDesc () :String
    {
        if (_occInfo is WorldPetInfo) {
            return "m.pet";
        }
        return "m.actor";
    }

    // from MsoySprite
    override public function getStageRect (includeExtras :Boolean = true) :Rectangle
    {
        // Note: Ideally we could just return getRect(stage), but that seems to pay too
        // much attention to our mask. 
        var r :Rectangle = super.getStageRect();

        if (includeExtras) {
            // But it seems to work for these other bits...
            // Add the bounds of the name label.
            r = r.union(_label.getRect(this.stage));

            // Add the bounds of any decorations.
            if (_decorations != null) {
                for each (var obj :Object in _decorations) {
                    r = r.union(DisplayObject(obj.dec).getRect(this.stage));
                }
            }
        }

        return r;
    }

    /**
     * Get the information needed by the ChatOverlay in order to draw
     * chat bubbles around this actor.
     *
     * @return [ bounds, mouthPoint, tail termination distance ], all in
     * screen coordinates.
     */
    public function getChatInfo () :Array
    {
        var w :int = getContentWidth();
        var h :int = getContentHeight();

        var mouthSpot :Point;
        if (_mouthSpot == null) {
            mouthSpot = new Point(w/2, h/4);

        } else {
            mouthSpot = new Point(Math.max(0, Math.min(_mouthSpot.x, w)),
                Math.max(0, Math.min(_mouthSpot.y, h)));
        }

        var tailDist :Number;
        if (isNaN(_tailDistance)) {
            tailDist = Math.max(w/4, h/4);
        } else {
            tailDist = _tailDistance;
        }

        return [ getStageRect(false), localToGlobal(mouthSpot), tailDist ];
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
        var triggerAppearanceChanged :Boolean = false;
        var oldScale :Number = _scale;
        _scale = winfo.getScale();
        var newMedia :MediaDesc = winfo.getMedia();
        if (!newMedia.equals(_desc)) {
            setup(newMedia, winfo.getItemIdent());

        } else if (oldScale != _scale) {
            scaleUpdated();
        }

        // take care of setting up or changing our TableIcon
        if (winfo is WorldMemberInfo) {
            var minfo :WorldMemberInfo = winfo as WorldMemberInfo;

            if (_tableIcon != null && !_tableIcon.getGameSummary().equals(minfo.game)) {
                _tableIcon.shutdown();
                _tableIcon = null;
            }
            if (_tableIcon == null && minfo.game != null) {
                _tableIcon = new TableIcon(this, minfo.game);
            }
        }

        // See if we need to update the name label or the status.
        // Note that we need to compare the String versions of the names, because that's
        // the difference we care about here. MemberNames compare as the same if the memberId
        // is the same...
        var newName :String = newInfo.username.toString();
        if (_occInfo == null || (_occInfo.status != newInfo.status) ||
                (_occInfo.username.toString() !== newName)) {
            _label.textColor = getStatusColor(newInfo.status);
            _label.text = newName;
            _label.width = _label.textWidth + TextFieldUtil.WIDTH_PAD;
            _label.height = _label.textHeight + TextFieldUtil.HEIGHT_PAD;
            recheckLabel();

            // if our idle status has changed...
            if ((newInfo.status == OccupantInfo.IDLE) == (_idleIcon == null)) {
                triggerAppearanceChanged = true;
                if (_idleIcon == null) {
                    _idleIcon = (new IDLE_ICON() as DisplayObject);
                    addDecoration(_idleIcon, {
                        weight: Number.MAX_VALUE / 2,
                        bounds: new Rectangle(0, 0, 50, 80)
                    });

                } else {
                    removeDecoration(_idleIcon);
                    _idleIcon = null;
                }

            } else {
                // the bounds of the label may have changed, re-arrange
                // (if we added or removed the idle icon, it was already done...)
                arrangeDecorations();
            }
        }

        // note the old info...
        var oldWinfo :WorldOccupantInfo = (_occInfo as WorldOccupantInfo);

        // assign the new one
        _occInfo = newInfo;

        // finally, if the state has changed, dispatch an event (we don't dispatch if the old info
        // was null, as getting our initial state isn't a "change") This is another argument for a
        // special state-changed dobj event.
        if (oldWinfo != null && !Util.equals(oldWinfo.getState(), winfo.getState())) {
            callUserCode("stateSet_v1", winfo.getState());
        }

        if (triggerAppearanceChanged) {
            appearanceChanged();
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
            _walk.stopAnimation();
        }

        // set the orientation towards the new location
        setOrientation(destLoc.orient, false);

        _walk = new WalkAnimation(this, scene, _loc, destLoc);
        _walk.startAnimation();
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
     * @return true if we're idle.
     */
    public function isIdle () :Boolean
    {
        return (_occInfo.status == OccupantInfo.IDLE);
    }

    /**
     * Stops the current motion of this actor.
     */
    public function stopMove () :void
    {
        if (_walk != null) {
            _walk.stopAnimation();
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
        var decCons :Object = getDecorationAt(event.stageX, event.stageY);
        if (decCons != null) {
            // deliver it there
            DisplayObject(decCons.dec).dispatchEvent(event);

        } else {
            // otherwise, do the standard thing
            super.mouseClick(event);
        }
    }

    override public function setHovered (hovered :Boolean, stageX :int = 0, stageY :int = 0) :String
    {
        // see if we're hovering over a new decoration..
        var decorTip :String = null;
        var hoverCons :Object = hovered ? getDecorationAt(stageX, stageY) : null;
        var hoverDec :DisplayObject = (hoverCons == null) ? null : DisplayObject(hoverCons.dec);
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
            decorTip = hoverCons["toolTip"];
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

        // recheck our scale (TODO: this should perhaps be done differently, but we need to trace
        // through the twisty loading process to find out for sure.
        // scaleUpdated is specifically needed to be called to make the avatarviewer happy.
        scaleUpdated();
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

    override public function setHotSpot (x :Number, y :Number, height :Number) :void
    {
        super.setHotSpot(x, y, height);
        recheckLabel();
        arrangeDecorations();
    }

    override protected function updateLoadingProgress (soFar :Number, total :Number) :void
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
        var hotSpot :Point = getMediaHotSpot();
        // note: may overflow the media area..
        _label.x = Math.abs(getMediaScaleX() * _locScale * _fxScaleX) * hotSpot.x -
            (_label.width/2);
        // if we have a configured _height use that in relation to the hot spot y position,
        // otherwise assume our label goes above our bounding box
        var baseY :Number = isNaN(_height) ? 0 :
            Math.abs(getMediaScaleY() * _locScale * _fxScaleY) * (hotSpot.y - _height);
        _label.y = baseY - _label.height;
    }

    /**
     * Handles an update to an effect's size/hotspot/etc.
     */
    protected function handleEffectUpdated (event :ValueEvent) :void
    {
        var effectSprite :EffectSprite = (event.target as EffectSprite);
        var p :Point = effectSprite.getLayoutHotSpot();
        effectSprite.x = getActualWidth()/2 - p.x;
        effectSprite.y = getActualHeight()/2 - p.y;
    }

    /**
     * Handle an effect that has finished playing.
     */
    protected function handleEffectFinished (event :ValueEvent) :void
    {
        var effectSprite :EffectSprite = (event.target as EffectSprite);
        removeChild(effectSprite);
    }

    /**
     * Arrange any external decorations above our name label.
     */
    protected function arrangeDecorations () :void
    {
        if (_decorations == null) {
            return;
        }

        var hotSpot :Point = getMediaHotSpot();
        // note: may overflow the media area..
        var hotX :Number = Math.abs(getMediaScaleX() * _locScale * _fxScaleX) * hotSpot.x;
        var baseY :Number = _label.y; // we depend on recheckLabel()

        // place the decorations over the name label, with our best guess as to their size
        for (var ii :int = 0; ii < _decorations.length; ii++) {
            var dec :DisplayObject = DisplayObject(_decorations[ii].dec);
            var rect :Rectangle = _decorations[ii]["bounds"] as Rectangle;
            if (rect == null) {
                rect = dec.getRect(dec);
            }
            baseY -= (rect.height + DECORATION_PAD);
            dec.x = hotX - (rect.width/2) - rect.x;
            dec.y = baseY - rect.y;
        }
    }

    /**
     * Return the decoration's constraints for the decoration under the
     * specified stage coordinates.
     */
    protected function getDecorationAt (stageX :int, stageY :int) :Object
    {
        if (_decorations != null) {
            for (var ii :int = 0; ii < _decorations.length; ii++) {
                var disp :DisplayObject = (_decorations[ii].dec as DisplayObject);
                if (disp.hitTestPoint(stageX, stageY, true)) {
                    return _decorations[ii];
                }
            }
        }
        return null;
    }

    /**
     * Sort function for decoration constraints...
     */
    protected function decorationSort (cons1 :Object, cons2 :Object) :int
    {
        var w1 :Number = ("weight" in cons1) ? (cons1["weight"] as Number) : 0;
        var w2 :Number = ("weight" in cons2) ? (cons2["weight"] as Number) : 0;

        // higher weights have a higher priority
        if (w1 > w2) {
            return -1;

        } else if (w1 < w2) {
            return 1;

        } else {
            return 0;
        }
    }

    protected function getStatusColor (status :int) :uint
    {
        switch (status) {
        case OccupantInfo.IDLE:
            return 0x777777;

        case OccupantInfo.DISCONNECTED:
            return 0xFF0000;

        default:
            return 0x99BFFF;
        }
    }

    override protected function willShowNewMedia () :void
    {
        super.willShowNewMedia();

        // reset the move speed and the _height
        _moveSpeed = DEFAULT_MOVE_SPEED;
        _height = NaN;
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
     * Update the actor's mouthspot.
     */
    internal function setMouthSpot (x :Number, y :Number, tailTerminationDist :Number) :void
    {
        if (isNaN(x) || isNaN(y)) {
            _mouthSpot = null;
        } else {
            // just set it here, we'll bound it when it's used...
            _mouthSpot = new Point(x, y);
        }

        _tailDistance = tailTerminationDist; // ok to be NaN
    }

    internal function setMoveSpeedFromUser (speed :Number) :void
    {
        if (!isNaN(speed)) {
            // don't worry, it'll be bounded by the minimum at the appropriate place
            _moveSpeed = speed;
        }
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

    override public function getMediaScaleX () :Number
    {
        return _scale;
    }

    override public function getMediaScaleY () :Number
    {
        return _scale;
    }

    /**
     * Called when the actor changes orientation or transitions between poses.
     */
    protected function appearanceChanged () :void
    {
        var locArray :Array = [ _loc.x, _loc.y, _loc.z ];
        if (hasUserCode("appearanceChanged_v2")) {
            callUserCode("appearanceChanged_v2", locArray, _loc.orient, isMoving(), isIdle());
        } else {
            callUserCode("appearanceChanged_v1", locArray, _loc.orient, isMoving());
        }
    }

    /** A label containing the actor's name.
     * Note that this is not a decoration, as decorations do their own seperate hit-testing
     * and glowing, and we want the name label to be 'part' of the sprite. Also, the label
     * was not working correctly with the "general purpose" layout code for decorations,
     * which I believe to be the fault of the label (it was returning a negative X coordinate
     * for its bounding rectangle, when in fact it should have started at 0). */
    protected var _label :TextField;

    protected var _occInfo :ActorInfo;
    protected var _walk :WalkAnimation;

    /** The media scale we should use. */
    protected var _scale :Number = 1;

    /** The actor's mouthspot, or null to use the calculated one. */
    protected var _mouthSpot :Point = null;

    /** The tail termination distance, or NaN to use the calculated one. */
    protected var _tailDistance :Number = NaN;

    /** The move speed, in pixels per second. */
    protected var _moveSpeed :Number = DEFAULT_MOVE_SPEED;

    /** A decoration used when we're in a table in a lobby. */
    protected var _tableIcon :TableIcon;

    /** A decoration added when we've idled out. */
    protected var _idleIcon :DisplayObject;

    /** Display objects to be shown above the name for this actor,
     * configured by external callers. */
    protected var _decorations :Array;

    /** The current decoration being hovered over, if any. */
    protected var _hoverDecoration :DisplayObject;

    protected static const DECORATION_PAD :int = 5;

    [Embed(source="../../../../../../../rsrc/media/idle.swf")]
    protected static const IDLE_ICON :Class;
}
}

import flash.events.MouseEvent;

import flash.filters.GlowFilter;

import com.threerings.msoy.client.MsoyController;

import com.threerings.msoy.game.data.GameSummary;

import com.threerings.msoy.world.client.ActorSprite;
import com.threerings.msoy.world.client.MsoySprite;

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
        setMediaDesc(gameSummary.getThumbMedia());

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
        this.filters = [ new GlowFilter(MsoySprite.GAME_HOVER, 1, 32, 32, 2) ];
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
