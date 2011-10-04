//
// $Id$

package com.threerings.msoy.room.client {
import flash.display.BitmapData;
import flash.display.DisplayObject;
import flash.display.Loader;
import flash.display.Sprite;
import flash.events.MouseEvent;
import flash.events.ProgressEvent;
import flash.geom.Matrix;
import flash.geom.Point;
import flash.geom.Rectangle;
import flash.media.SoundTransform;

import com.threerings.util.Arrays;
import com.threerings.util.Comparators;
import com.threerings.util.ValueEvent;

import com.threerings.crowd.data.OccupantInfo;

import com.threerings.display.FilterUtil;

import com.threerings.media.MediaContainer;

import com.threerings.msoy.chat.client.ComicOverlay;
import com.threerings.msoy.client.Prefs;
import com.threerings.msoy.room.data.MsoyLocation;
import com.threerings.msoy.room.data.MsoyScene;
import com.threerings.msoy.ui.MsoyNameLabel;
import com.threerings.msoy.world.client.WorldContext;

/**
 * Displays a visualization of an occupant in a scene (could be a member, a pet, a MOB, all sorts
 * of kraaaazy stuff).
 */
public class OccupantSprite extends EntitySprite
{
    /** The maximum width of an occupant sprite. */
    public static const MAX_WIDTH :int = 600;

    /** The maximum height of an occupant sprite. */
    public static const MAX_HEIGHT :int = 450;

    /** The default move speed, in pixels per second. */
    public static const DEFAULT_MOVE_SPEED :Number = 500;

    /** The minimum move speed, in pixels per second. */
    public static const MIN_MOVE_SPEED :Number = 50;

    /** The sort order of a player's game icon amongst their decorations. */
    public static const DEC_WEIGHT_GAME :int = 100;

    /** The sort order of a player's avrg-specific display amongst their decorations. */
    public static const DEC_WEIGHT_AVRG_SPECIAL :int = 150;

    /** The sort order of a player's party icon amongst their decorations. */
    public static const DEC_WEIGHT_PARTY :int = 200;

    /** The sort order of a player's idle indicator amongst their decorations. */
    public static const DEC_WEIGHT_IDLE :int = 500;

    /**
     * Creates a sprite for the supplied occupant.
     */
    public function OccupantSprite (ctx :WorldContext, occInfo :OccupantInfo, extraInfo :Object)
    {
        super(ctx);

        // The label often jumps visibly when the actor is hovered over, a pixel up or down, and/or
        // left or right. As far as I (Ray) can figure, when the glow filter is applied it's doing
        // pixel snapping. The strange thing is that we apply our own outlining glow filter (below)
        // so it should already be snapping.  It seems that setting cacheAsBitmap makes the
        // vertical jumpiness go away, but not the horizontal jumpiness. So, it's disabled for
        // now...

        // _label.cacheAsBitmap = true;

        _sprite.setMaxContentDimensions(MAX_WIDTH, MAX_HEIGHT);

        _sprite.addEventListener(ProgressEvent.PROGRESS, handleMediaProgress);
        _sprite.addEventListener(MediaContainer.DID_SHOW_NEW_MEDIA, handleNewMedia);

        _sprite.addChild(_extras);
        _extras.addChild(_label);

        if (occInfo != null) {
            setOccupantInfo(occInfo, extraInfo);
        }

        muteChanged();
    }

    public function squelch (active :Boolean = true) :void
    {
        _sprite.soundTransform = new SoundTransform(active ? 0 : 1);
    }

    /**
     * Returns the memberId responsible for this occupant, or 0.
     */
    public function getMemberId () :int
    {
        return 0;
    }

    override public function roomScaleUpdated () :void
    {
        super.roomScaleUpdated();

        // undo the scale on _extras
        var matrix :Matrix = _sprite.transform.concatenatedMatrix;
        _extras.scaleX = 1 / matrix.a;
        _extras.scaleY = 1 / matrix.d;
        recheckLabel();
        arrangeDecorations();
    }

    /**
     * Adds some sort of nonstandard decoration to the sprite.  The decoration should already be
     * painting its full size.
     *
     * @param constraints an object containing properties that will control layout and other bits.
     *
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

        // add the new decoration and stabley sort the list
        _decorations.push(constraints);
        Arrays.stableSort(_decorations, DECORATION_SORT);

        // add it and lay it out
        _extras.addChild(dec);
        arrangeDecorations();
    }

    /**
     * Removes a piece of nonstandard decoration.
     */
    public function removeDecoration (dec :DisplayObject) :void
    {
        if (_decorations != null) {
            for (var ii :int = 0; ii < _decorations.length; ii++) {
                if (_decorations[ii].dec == dec) {
                    _decorations.splice(ii, 1);
                    _extras.removeChild(dec);
                    if (_decorations.length == 0) {
                        _decorations = null;
                    }
                    arrangeDecorations();
                    return; // no need to continue
                }
            }
        }
    }

    /**
     * Removes all decorations.
     */
    public function removeAllDecorations () :void
    {
        if (_decorations == null) {
            return;
        }
        for (var ii :int = 0; ii < _decorations.length; ii++) {
            _extras.removeChild(_decorations[ii].dec as DisplayObject);
        }
        _decorations = null;
        // this function sets _bubblePosition, even if there are no decorations
        arrangeDecorations();
    }

    /**
     * Returns the walk speed of this occupant, in pixels / second.
     */
    public function getMoveSpeed (roomScale :Number) :Number
    {
        return Math.max(MIN_MOVE_SPEED, _moveSpeed * _scale * roomScale);
    }

    /**
     * Retuns the position, in stage coordinates, where bubbles should draw up from (vertically),
     * and center on (horizontally).
     */
    public function getBubblePosition () :Point
    {
        return _sprite.localToGlobal(_bubblePosition);
    }

    /**
     * Called to set up the occupant's initial location upon entering a room.
     */
    public function setEntering (loc :MsoyLocation) :void
    {
        setLocation(loc);
        setOrientation(loc.orient);
    }

    /**
     * Returns the occupant info for this sprite.
     */
    public function getOccupantInfo () :OccupantInfo
    {
        return _occInfo;
    }

    /**
     * Returns the oid of the body that this occupant represents.
     */
    public function getOid () :int
    {
        return _occInfo.bodyOid;
    }

    /**
     * @return true if we're moving.
     */
    public function isMoving () :Boolean
    {
        return (_walk != null);
    }

    /**
     * Updates the orientation of this occupant.
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
     * Configures the overlay used to display this occupant's chat.
     */
    public function setChatOverlay (chatOverlay :ComicOverlay) :void
    {
        _chatOverlay = chatOverlay;
    }

    /**
     * Updates this occupant's info.
     */
    public function setOccupantInfo (newInfo :OccupantInfo, extraInfo :Object) :void
    {
        var oldInfo :OccupantInfo = _occInfo;
        _occInfo = newInfo;

        // potentially update our visualization
        configureDisplay(oldInfo, newInfo);

        // potentially update our name and decorations
        if (configureDecorations(oldInfo, newInfo)) {
            arrangeDecorations();
        }

        // TODO: I added this code, but can't for the life of me figure out why it's necessary
        // and it booches the remixer
        // check to see if we're blocked
//        if (_desc != null) {
//            checkBlocked();
//        }
    }

    override public function snapshot (
        bitmapData :BitmapData, matrix :Matrix, childPredicate :Function = null) :Boolean
    {
        var success :Boolean = true;

        // now snapshot everything, including the decorations and name label
        for (var ii :int = 0; ii < _sprite.numChildren; ii++) {
            var d :DisplayObject = _sprite.getChildAt(ii);
            if (d == _sprite.getMedia() && _sprite.getMedia() is Loader) {
                success = super.snapshot(bitmapData, matrix, childPredicate);

            } else {
                var m :Matrix = d.transform.matrix;
                m.concat(matrix);
                try {
                    bitmapData.draw(d, m, null, null, null, true);
                } catch (serr :SecurityError) {
                    log.info("Unable to snapshot occupant decoration", "reason", serr.message);
                }
            }
        }

        return success;
    }

    /**
     * Derived classes should update their visualization based on this call and return true if it
     * changed, false if not.
     */
    protected function configureDisplay (oldInfo :OccupantInfo, newInfo :OccupantInfo) :Boolean
    {
        return false;
    }

    /**
     * Configures our occupant-related decorations.
     *
     * @return true if decorations should be arranged as a result of this call, false otherwise.
     */
    protected function configureDecorations (oldInfo :OccupantInfo, newInfo :OccupantInfo) :Boolean
    {
        // note that we need to compare the String versions of the names, as that's the difference
        // we care about here; MemberName compares as the same if the memberId is the same...
        if (isNameChangeRequired(oldInfo, newInfo)) {
            _label.update(newInfo);
            recheckLabel();
            // the bounds of the label may have changed, re-arrange
            return true;
        }
        return false;
    }

    /**
     * Detects if an occupant info change would change the sprite's name. By default the status and
     * username are checked. Subclasses may wish to trigger a name update more frequently according
     * to their needs.
     */
    protected function isNameChangeRequired (oldInfo :OccupantInfo, newInfo :OccupantInfo) :Boolean
    {
        // see if we need to update the name label or the status
        return (oldInfo == null) || (oldInfo.status != newInfo.status) ||
            (oldInfo.username.toString() !== newInfo.username.toString());
    }

    /**
     * Effects the movement of this occupant to a new location in the scene. This just animates the
     * movement, and should be called as a result of the server informing us that we've moved.
     */
    public function moveTo (destLoc :MsoyLocation, scene :MsoyScene) :void
    {
        // if there's already a move, kill it
        if (_walk != null) {
            _walk.stopAnimation();
            dispatchEntityMoved(null);
        }

        log.info("moveTo()", "height", _sprite.height, "width", _sprite.width,
            "parent", _sprite.parent);

        // set the orientation towards the new location
        setOrientation(destLoc.orient, false);

        _walk = new WalkAnimation(this, scene, _loc, destLoc);
        _walk.startAnimation();

        dispatchEntityMoved(destLoc);
        appearanceChanged();
    }

    public function dispatchEntityMoved (destLoc :MsoyLocation) :void
    {
        if (getItemIdent() != null && _sprite.parent is RoomView) {
            (_sprite.parent as RoomView).dispatchEntityMoved(getItemIdent(),
                (destLoc != null) ? [destLoc.x, destLoc.y, destLoc.z] : null);
        }
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
     * Stops the current motion of this occupant.
     */
    public function stopMove () :void
    {
        if (_walk != null) {
            _walk.stopAnimation();
            _walk = null;
        }
    }

    /**
     * A callback from our walk animations.
     */
    public function walkCompleted (orient :Number) :void
    {
        _walk = null;

        appearanceChanged();
        dispatchEntityMoved(null);
        if (_sprite.parent is RoomView) {
            RoomView(_sprite.parent).moveFinished(this);
        }
    }

    // from EntitySprite
    override public function setScreenLocation (x :Number, y :Number, scale :Number) :void
    {
        super.setScreenLocation(x, y, scale);

        if (_chatOverlay != null) {
            // let our the chat overlay thats carrying our chat bubbles know we moved
            _chatOverlay.speakerMoved(_occInfo.username, getBubblePosition());
        }
    }

    // from EntitySprite
    override public function getDesc () :String
    {
        return "m.occupant";
    }

    /**
     * Is this occupant muted?
     */
    public function isMuted () :Boolean
    {
        return (_occInfo != null && _ctx.getMuteDirector() != null &&
             _ctx.getMuteDirector().isMuted(_occInfo.username))
    }

    /**
     * When our muteness has changed, update the block. This brutally overrides the block type
     * in the media container. Implement something more sophisticated if we need it later.
     */
    public function muteChanged ():void
    {
        _sprite.setBlocked(isMuted() ? "mute": null);
    }

    override protected function getSpecialProperty (name :String) :Object
    {
        switch (name) {
        case "name":
            return _occInfo.username.toString();

        case "move_speed":
            return _moveSpeed * _scale;

        default:
            return super.getSpecialProperty(name);
        }
    }

    // from EntitySprite
    override public function getStageRect (includeExtras :Boolean = true) :Rectangle
    {
        // Note: Ideally we could just return getRect(stage), but that seems to pay too much
        // attention to our mask.
        var r :Rectangle = super.getStageRect();

        if (includeExtras) {
            r = r.union(_label.getRect(_sprite.stage));

            if (_decorations != null) {
                for each (var obj :Object in _decorations) {
                    r = r.union(DisplayObject(obj.dec).getRect(_sprite.stage));
                }
            }
        }

        return r;
    }

    // from EntitySprite
    override public function mouseClick (event :MouseEvent) :void
    {
        // see if it actually landed on a decoration
        var decCons :Object = getDecorationAt(event.stageX, event.stageY);
        if (decCons != null) {
            // dispatch a non-bubbling copy of the click to the decoration
            var dec :DisplayObject = DisplayObject(decCons.dec);
            var p :Point = dec.globalToLocal(new Point(event.stageX, event.stageY));
            var me :MouseEvent = new MouseEvent(MouseEvent.CLICK, false, false, p.x, p.y,
                event.relatedObject, event.ctrlKey, event.altKey, event.shiftKey, event.buttonDown,
                event.delta);
            dec.dispatchEvent(me);

        } else {
            // otherwise, do the standard thing
            super.mouseClick(event);
        }
    }

    // from EntitySprite
    override public function setHovered (
        hovered :Boolean, stageX :Number = NaN, stageY :Number = NaN) :Object
    {
        // see if we're hovering over a new decoration..
        var decorTip :Object;
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
            decorTip = (hoverDec != null) ? hoverCons["toolTip"] : null;
        } else {
            decorTip = true;
        }

        // always call super, but hover is only true if we hit no decorations
        var superTip :Object = super.setHovered((_hoverDecoration == null) && hovered);
        return (_hoverDecoration == null) ? superTip : decorTip;
    }

    // from EntitySprite
    override public function toString () :String
    {
        return "OccupantSprite[" + _occInfo.username + " (oid=" + _occInfo.bodyOid + ")]";
    }

    // from EntitySprite
    override public function setHotSpot (x :Number, y :Number, height :Number) :void
    {
        super.setHotSpot(x, y, height);
        recheckLabel();
        arrangeDecorations();
    }

    // from EntitySprite
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

    // from EntitySprite
    override protected function mediaWillShutdown (event :ValueEvent) :void
    {
        if (Boolean(event.value)) {
            stopMove();
        }

        super.mediaWillShutdown(event);
    }

    // from EntitySprite
    override protected function scaleUpdated () :void
    {
        super.scaleUpdated();
        _sprite.setSpriteMediaScale(_scale, _scale);
        recheckLabel();
        arrangeDecorations();
    }

    // from EntitySprite
    override protected function mediaSizeKnown (event :ValueEvent) :void
    {
        super.mediaSizeKnown(event);

        // recheck our scale (TODO: this should perhaps be done differently, but we need to trace
        // through the twisty loading process to find out for sure.
        // scaleUpdated is specifically needed to be called to make the avatarviewer happy.
        scaleUpdated();
    }

    protected function handleMediaProgress (event :ProgressEvent) :void
    {
        var prog :Number = (event.bytesTotal == 0) ? 0 : (event.bytesLoaded / event.bytesTotal);

        // always clear the old graphics
        _sprite.graphics.clear();

        // and if we're still loading, draw a line showing progress
        if (prog < 1) {
            _sprite.graphics.lineStyle(1, 0x00FF00);
            _sprite.graphics.moveTo(0, -1);
            _sprite.graphics.lineTo(prog * 100, -1);
            _sprite.graphics.lineStyle(1, 0xFF0000);
            _sprite.graphics.lineTo(100, -1);
        }
    }

    protected function handleWillShowNewMedia () :void
    {
        // reset the move speed and the _height
        _moveSpeed = DEFAULT_MOVE_SPEED;
        _height = NaN;
    }

    /**
     * Called to make sure the label's horizontal position is correct.
     */
    protected function recheckLabel () :void
    {
        var hotSpot :Point = getMediaHotSpot();
        // note: may overflow the media area..
        _label.x = Math.abs(_sprite.getMediaScaleX() * _locScale /* * _fxScaleX*/) *
            hotSpot.x / _extras.scaleX - (_label.width/2);
        // if we have a configured _height use that in relation to the hot spot y position,
        // otherwise assume our label goes above our bounding box
        var baseY :Number = isNaN(_height) ? 0 :
            Math.abs(_sprite.getMediaScaleY() * _locScale /* * _fxScaleY*/) * (hotSpot.y - _height);
        // NOTE: At one point we thought we'd bound names to be on-screen, but names mean so
        // little in Whirled, and we've decided we don't care. Avatars with hidden names are cool,
        // and you can still click on the avatar to do whatever you need to do.
        // Also, the room's occupant list can be used to act on other avatars.
        if (Prefs.isAprilFoolsEnabled()) {
            _label.y = baseY / _extras.scaleY;
        } else {
            _label.y = (baseY - _label.height) / _extras.scaleY;
        }
    }

    /**
     * Arrange any external decorations above our name label.  Will notify the chat overlay
     * displaying this speaker's bubbles that the bubble location has moved.
     */
    protected function arrangeDecorations () :void
    {
        const FOOL :Boolean = Prefs.isAprilFoolsEnabled();

        // note: may overflow the media area..
        var hotSpot :Point = getMediaHotSpot();
        var hotX :Number = Math.abs(_sprite.getMediaScaleX() * _locScale) * hotSpot.x;

        var baseY :Number = _label.y; // we depend on recheckLabel()
        if (FOOL) {
            baseY += _label.height;
        }
        if (_decorations != null) {
            // place the decorations over the name label, with our best guess as to their size
            for (var ii :int = 0; ii < _decorations.length; ii++) {
                var dec :DisplayObject = DisplayObject(_decorations[ii].dec);
                var rect :Rectangle = _decorations[ii]["bounds"] as Rectangle;
                if (rect == null) {
                    rect = dec.getRect(dec);
                }
                dec.x = (hotX - (rect.width/2) - rect.x) / _extras.scaleX;
                if (FOOL) {
                    baseY += DECORATION_PAD;
                    dec.y = (baseY - rect.y);
                    baseY += rect.height;
                } else {
                    baseY -= (rect.height + DECORATION_PAD);
                    dec.y = (baseY - rect.y);
                }
            }
        }

        checkAndSetBubblePosition(new Point(hotX, baseY));
    }

    protected function checkAndSetBubblePosition (pos :Point) :void
    {
        var oldBubblePos :Point = _bubblePosition;
        _bubblePosition = pos;
        if (_chatOverlay != null && !_bubblePosition.equals(oldBubblePos)) {
            // notify the overlay that its bubble position for this speaker moved
            _chatOverlay.speakerMoved(_occInfo.username, getBubblePosition());
        }
    }

    /**
     * Return the decoration's constraints for the decoration under the
     * specified stage coordinates.
     */
    protected function getDecorationAt (stageX :Number, stageY :Number) :Object
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
     * Called when this occupant changes orientation or transitions between poses.
     */
    protected function appearanceChanged () :void
    {
        // nada
    }

    protected function createNameLabel () :MsoyNameLabel
    {
        // use the default implementation here.
        return new MsoyNameLabel();
    }

    /** Contains extra children (nameLabel, decorations) that should not be scaled. */
    protected var _extras :Sprite = new Sprite();

    /** A label containing the occupant's name. Note: this is not a decoration, as decorations do
     * their own seperate hit-testing and glowing, and we want the name label to be 'part' of the
     * sprite. Also, the label was not working correctly with the "general purpose" layout code for
     * decorations, which I believe to be the fault of the label (it was returning a negative X
     * coordinate for its bounding rectangle, when in fact it should have started at 0). */
    protected var _label :MsoyNameLabel = createNameLabel();

    /** Our most recent occupant information. */
    protected var _occInfo :OccupantInfo;

    /** An animation that we play when walking. */
    protected var _walk :WalkAnimation;

    /** The media scale we should use. */
    protected var _scale :Number = 1;

    /** The move speed, in pixels per second. */
    protected var _moveSpeed :Number = DEFAULT_MOVE_SPEED;

    /** The chat overlay that we notify when we change position. */
    protected var _chatOverlay :ComicOverlay;

    /** Display objects to be shown above the name for this actor,
     * configured by external callers. */
    protected var _decorations :Array;

    /** The current decoration being hovered over, if any. */
    protected var _hoverDecoration :DisplayObject;

    /** The point to center this speaker's bubbles on, in local coords. */
    protected var _bubblePosition :Point;

    /** The sort to use for decorations: reverse by weight, defaulting to 0. */
    protected static const DECORATION_SORT :Function =
        Comparators.createReverse(Comparators.createFields(["weight"], [0]));

    protected static const DECORATION_PAD :int = 5;

    [Embed(source="../../../../../../../rsrc/media/idle.swf")]
    protected static const IDLE_ICON :Class;
}
}
