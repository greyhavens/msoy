//
// $Id$

package com.threerings.msoy.world.client {

import flash.display.BitmapData;
import flash.display.BlendMode;
import flash.display.DisplayObject;
import flash.display.DisplayObjectContainer;
import flash.display.Loader;
import flash.display.LoaderInfo;
import flash.display.Shape;
import flash.display.Sprite;

import flash.errors.IOError;

import flash.events.Event;
import flash.events.EventDispatcher;
import flash.events.IEventDispatcher;
import flash.events.IOErrorEvent;
import flash.events.MouseEvent;
import flash.events.NetStatusEvent;
import flash.events.ProgressEvent;
import flash.events.SecurityErrorEvent;
import flash.events.StatusEvent;
import flash.events.TextEvent;

import flash.filters.GlowFilter;

import flash.geom.Matrix;
import flash.geom.Point;
import flash.geom.Rectangle;

import flash.net.LocalConnection;
import flash.net.NetConnection;
import flash.net.NetStream;
import flash.net.URLRequest;

import flash.system.ApplicationDomain;
import flash.system.LoaderContext;
import flash.system.SecurityDomain;

import com.threerings.util.CommandEvent;
import com.threerings.util.ObjectMarshaller;
import com.threerings.util.ValueEvent;

import com.threerings.flash.FilterUtil;
import com.threerings.flash.VideoDisplayer;

import com.threerings.msoy.client.MsoyContext;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.StaticMediaDesc;

import com.threerings.msoy.world.data.EntityMemoryEntry;
import com.threerings.msoy.world.data.MsoyLocation;
import com.threerings.msoy.world.data.RoomCodes;
import com.threerings.msoy.world.data.RoomPropertyEntry;
import com.threerings.msoy.world.data.RoomObject;

/**
 * A base sprite that concerns itself with the mundane details of loading and communication with
 * the loaded media content.
 */
public class MsoySprite extends DataPackMediaContainer
    implements RoomElement
{
    /** The type of a ValueEvent that is dispatched when the location is updated, but ONLY if the
     * parent is not an AbstractRoomView. */
    public static const LOCATION_UPDATED :String = "locationUpdated";

    /** Hover colors. */
    public static const AVATAR_HOVER :uint = 0x99BFFF;// light blue
    public static const PET_HOVER :uint = 0x999999;// light gray
    public static const PORTAL_HOVER :uint = 0x7BFFB0; // happy green
    public static const GAME_HOVER :uint = 0xFFFFFF;  // white
    public static const OTHER_HOVER :uint = 0x000000; // black

    // from ContextMenuProvider, via MsoyMediaContainer
    override public function populateContextMenu (ctx :MsoyContext, items :Array) :void
    {
        // put the kibosh on super's big ideas
    }

    /**
     * Called by the containing room when it changes scale.
     */
    public function roomScaleUpdated () :void
    {
        // nada
    }

    /**
     * Snapshot this sprite into the specified BitmapData.
     *
     * @return true on success.
     */
    public function snapshot (bitmapData :BitmapData, matrix :Matrix) :Boolean
    {
        if (_media is Loader) {
            try {
                var m :Matrix = _media.transform.matrix;
                m.concat(matrix);

                // Since we draw just the media, bypassing the mask,
                // we need to clip to the mask coordinates
                var r :Rectangle = getMaskRectangle();
                if (r != null) {
                    r.topLeft = m.transformPoint(r.topLeft);
                    r.bottomRight = m.transformPoint(r.bottomRight);
                }

                // TODO: note that we are not drawing any decorations
                // associated with this sprite. If we want those, we'll
                // have to do some ballache to get them.
                // (Actually, there's a good case for making all decorations
                // on a different sub-object that undoes parent scale to
                // keep scale consistent.
                Object(Loader(_media).content).snapshot(bitmapData, m, r);
                //trace("== Snapshot: inside stub");
                return true;

            } catch (serr :SecurityError) {
                // not a critical error
                log.info("Unable to snapshot MsoySprite: " + serr);
                return false;

            } catch (rerr :ReferenceError) {
                // fall through; log nothing
            }
        }

        // do the snapshot outselves
        try {
            bitmapData.draw(this, matrix, null, null, null, true);
            //trace("== Snapshot: MsoySprite");
            return true;

        } catch (serr :SecurityError) {
            // not a critical error
            log.info("Unable to snapshot MsoySprite: " + serr);
        }

        return false;
    }

    // from RoomElement
    public function getLayoutType () :int
    {
        return RoomCodes.LAYOUT_NORMAL;
    }

    // from RoomElement
    public function getRoomLayer () :int
    {
        return RoomCodes.FURNITURE_LAYER;
    }

    // from RoomElement
    public function setLocation (newLoc :Object) :void
    {
        _loc.set(newLoc);
        locationUpdated();
    }

    // from RoomElement
    public function getLocation () :MsoyLocation
    {
        return _loc;
    }

    // from RoomElement
    public function setScreenLocation (x :Number, y :Number, scale :Number) :void
    {
        this.x = x;
        this.y = y;
        if (useLocationScale() && scale != _locScale) {
            _locScale = scale;
            scaleUpdated();
        }
    }

    public function setEffectScales (xscale :Number, yscale :Number) :void
    {
        _fxScaleX = xscale;
        _fxScaleY = yscale;
        scaleUpdated();
    }

    /**
     * Get a translatable message briefly describing this type of item.
     */
    public function getDesc () :String
    {
        // should return something like m.furni, m.avatar...
        throw new Error("abstract");
    }

    /**
     * Return the item ident from which this sprite was based or null if it is not an item-based
     * sprite.
     */
    public function getItemIdent () :ItemIdent
    {
        return _ident;
    }

    /**
     * Get the screen width of this sprite, taking into account both horizontal scales.
     */
    public function getActualWidth () :Number
    {
        return getContentWidth() * _locScale * _fxScaleX;
    }

    /**
     * Get the screen height of this sprite, taking into account both vertical scales.
     */
    public function getActualHeight () :Number
    {
        return getContentHeight() * _locScale * _fxScaleY;
    }

    /**
     * Get the stage-coordinate rectangle of the bounds of this sprite.
     *
     * @param includeExtras unless false, include any non-media content like decorations.
     */
    public function getStageRect (includeExtras :Boolean = true) :Rectangle
    {
        var botRight :Point = new Point(getActualWidth(), getActualHeight());
        var r :Rectangle = new Rectangle();
        r.topLeft = localToGlobal(new Point(0, 0));
        r.bottomRight = localToGlobal(botRight);
        return r;
    }

    /**
     * Returns the room bounds. Called by user code.
     */
    public function getRoomBounds () :Array
    {
        if (!(parent is RoomView)) {
            return null;
        }
        var metrics :RoomMetrics = RoomView(parent).layout.metrics;
        return [ metrics.sceneWidth, metrics.sceneHeight, metrics.sceneDepth];
    }

    /**
     * Does this sprite have action? In other words, do we want to glow it
     * for the user when they hover over it?
     */
    public function hasAction () :Boolean
    {
        return false;
    }

    /**
     * Does this sprite capture the mouse? If the sprite has action
     * then it should really capture the mouse, otherwise either is fine.
     */
    public function capturesMouse () :Boolean
    {
        return hasAction();
    }

    public function setEditing (editing :Boolean) :void
    {
        _editing = editing;
        configureMouseProperties();
    }

    /**
     * Return the tooltip text for this sprite, or null if none.
     */
    public function getToolTipText () :String
    {
        return null;
    }

    /**
     * Get the basic hotspot that is the registration point on the media.  This point is not
     * scaled.
     */
    public function getMediaHotSpot () :Point
    {
        // the hotspot is null until set-up.
        return (_hotSpot != null) ? _hotSpot : new Point(0, 0);
    }

    /**
     * Get the hotspot to use for layout purposes. This point is adjusted for scale and any
     * perspectivization.
     */
    public function getLayoutHotSpot () :Point
    {
        var p :Point = getMediaHotSpot();
        return new Point(Math.abs(p.x * getMediaScaleX() * _locScale * _fxScaleX),
                         Math.abs(p.y * getMediaScaleY() * _locScale * _fxScaleY));
    }

    public function setActive (active :Boolean) :void
    {
        alpha = active ? 1.0 : 0.4;
        blendMode = active ? BlendMode.NORMAL : BlendMode.LAYER;
        configureMouseProperties();
    }

    // TODO: don't rely on our blendmode.. ?
    public function isActive () :Boolean
    {
        return (blendMode == BlendMode.NORMAL);
    }

    /**
     * During editing, set the X scale of this sprite.
     */
    public function setMediaScaleX (scaleX :Number) :void
    {
        throw new Error("Cannot set scale of abstract MsoySprite");
    }

    /**
     * During editing, set the Y scale of this sprite.
     */
    public function setMediaScaleY (scaleY :Number) :void
    {
        throw new Error("Cannot set scale of abstract MsoySprite");
    }

    /**
     * During editing, set the rotation of this sprite.
     */
    public function setMediaRotation (rotation :Number) :void
    {
        throw new Error("Cannot set rotation of abstract MsoySprite");
    }

    /**
     * Returns current sprite rotation angle in degrees
     * (compatible with {@link DisplayObject#rotation}).
     */
    public function getMediaRotation () :Number
    {
        return 0;
    }

    /**
     * Returns the center point of the media object, after scaling.
     */
    public function getMediaCentroid () :Point
    {
        var anchor :Point = new Point(getActualWidth() / 2, getActualHeight() / 2);

        // if the furni is mirrored along one of the axes, undo that for anchor calculation
        var xscale :Number = _locScale * getMediaScaleX() * _fxScaleX;
        if (xscale < 0) {
            anchor.x = -anchor.x;
        }

        var yscale :Number = _locScale * getMediaScaleY() * _fxScaleY;
        if (yscale < 0) {
            anchor.y = -anchor.y;
        }

        return anchor;
    }

    /**
     * Turn on or off the glow surrounding this sprite.
     */
    public function setHovered (hovered :Boolean, stageX :int = 0, stageY :int = 0) :String
    {
        if (hovered == (_glow == null)) {
            setGlow(hovered);
        }
        return hovered ? getToolTipText() : null;
    }

    protected function setGlow (glow :Boolean) :void
    {
        if (glow) {
            _glow = new GlowFilter(getHoverColor(), 1, 32, 32);
            FilterUtil.addFilter(_media, _glow);
            if (_media.mask != null) {
                FilterUtil.addFilter(_media.mask, _glow);
            }

        } else {
            FilterUtil.removeFilter(_media, _glow);
            if (_media.mask != null) {
                FilterUtil.removeFilter(_media.mask, _glow);
            }
            _glow = null;
        }
    }

    /**
     * Callback function.
     */
    public function mouseClick (event :MouseEvent) :void
    {
        if ((parent as RoomView).getRoomController().isEditMode()) {
            CommandEvent.dispatch(this, RoomController.EDIT_CLICKED, this);
        } else {
            postClickAction();
        }
    }

    /**
     * Called when an action or message to received for this sprite.
     */
    public function messageReceived (name :String, arg :Object, isAction :Boolean) :void
    {
        callUserCode("messageReceived_v1", name, arg, isAction);

        // TODO: remove someday
        // TEMP: dispatch a backwards compatible event to older style entities. This older method
        // was deprecated 2007-03-12, so hopefully we don't have to keep this around too long.
        // Commented out 2008-02-03. We should be good.
        //if (isAction) {
        //    callUserCode("eventTriggered_v1", name, arg);
        //}
    }

    /**
     * Called when an action or message to received for this sprite.
     */
    public function signalReceived (name :String, arg :Object) :void
    {
        callUserCode("signalReceived_v1", name, arg);
    }

    /**
     * Called when a datum in the this sprite's item's memory changes.
     */
    public function memoryChanged (key :String, value: Object) :void
    {
        callUserCode("memoryChanged_v1", key, value);
    }

    /**
     * Called when a property in this sprite's room's shared space changes.
     */
    public function roomPropertyChanged (key :String, value: Object) :void
    {
        callUserCode("roomPropertyChanged_v1", key, value);
    }

    /**
     * Called when this client is assigned control of this entity.
     */
    public function gotControl () :void
    {
        callUserCode("gotControl_v1");
    }

    /**
     * Unload the media we're displaying, clean up any resources.
     *
     * @param completely if true, we're going away and should stop everything. Otherwise, we're
     * just loading up new media.
     */
    override public function shutdown (completely :Boolean = true) :void
    {
        // clean up our backend
        if (_backend != null) {
            _backend.shutdown();
            _backend = null;
        }

        setHovered(false);

        super.shutdown(completely);

        _hotSpot = null;
    }

    /**
     * Configures this sprite with the item it represents.
     */
    protected function setItemIdent (ident :ItemIdent) :void
    {
        _ident = ident;
    }

    override protected function didShowNewMedia () :void
    {
        super.didShowNewMedia();

        scaleUpdated();
        rotationUpdated();
        configureMouseProperties();
    }

    override protected function initLoader () :Loader
    {
        var loader :Loader = super.initLoader();

        _backend = createBackend();
        _backend.init(loader);
        _backend.setSprite(this);

        return loader;
    }

    override protected function getMaskRectangle () :Rectangle
    {
        if (_desc != null && _desc.mimeType == MediaDesc.VIDEO_YOUTUBE) {
            // do not mask!
            return null;
        }

        return super.getMaskRectangle();
    }

    /**
     * Post a command event when we're clicked.
     */
    protected function postClickAction () :void
    {
        // nada
    }

    protected function configureMouseProperties () :void
    {
        var active :Boolean = isActive();
        // If we want to capture mouse events for this sprite up in the whirled layer, then
        // we cannot allow the click to go down into the usercode, because when we're running
        // with security boundaries then the click won't come back out.
        // TODO: have a way for entities to temporarily capture mouse events? Maybe only
        // your own personal avatar, for things like an art-vatar, or an avatar that plays back
        // mouse motions...
        mouseChildren = active && !_editing && !hasAction() && capturesMouse();
        mouseEnabled = active && !_editing;
    }

    /**
     * An internal convenience method to recompute our screen position when our size, location, or
     * anything like that has been updated.
     */
    protected function locationUpdated () :void
    {
        if (parent is AbstractRoomView) {
            (parent as AbstractRoomView).locationUpdated(this);

        } else {
            dispatchEvent(new ValueEvent(LOCATION_UPDATED, null));
        }
    }

    protected function scaleUpdated () :void
    {
        if (!(_media is Perspectivizer)) {
            var scalex :Number = _locScale * getMediaScaleX() * _fxScaleX;
            var scaley :Number = _locScale * getMediaScaleY() * _fxScaleY;

            _media.scaleX = scalex;
            _media.scaleY = scaley;

            if (_media.mask != null && (!(_media is DisplayObjectContainer) ||
                                        !DisplayObjectContainer(_media).contains(_media.mask))) {
                _media.mask.scaleX = Math.abs(scalex);
                _media.mask.scaleY = Math.abs(scaley);
            }
        }

        updateMediaPosition();
    }

    protected function rotationUpdated () :void
    {
        _media.rotation = getMediaRotation();
        updateMediaPosition();
    }

    /**
     * Should be called when the media scale or size changes to ensure that the media is positioned
     * correctly.
     */
    protected function updateMediaPosition () :void
    {
        // if scale is negative, the image is flipped and we need to move the origin
        var xscale :Number = _locScale * getMediaScaleX() * _fxScaleX;
        var yscale :Number = _locScale * getMediaScaleY() * _fxScaleY;
        _media.x = (xscale >= 0) ? 0 : Math.abs(Math.min(_w, getMaxContentWidth()) * xscale);
        _media.y = (yscale >= 0) ? 0 : Math.abs(Math.min(_h, getMaxContentHeight()) * yscale);

        updateMediaAfterRotation();
        
        // we may need to be repositioned
        locationUpdated();
    }

    /**
     * Called when media scale, rotation or location changes, adjusts the media position
     * to look like the sprite is rotated around its center.
     */
    protected function updateMediaAfterRotation () :void
    {
        if (_media.rotation == 0) {
            return; // nothing to adjust
        }

        // rotation anchor as a vector from the origin in the upper left
        var anchor :Point = getMediaCentroid();
        if (anchor.x == 0 && anchor.y == 0) {
            return; // if the anchor is already in the upper left, we don't need to shift anything
        }
       
        // convert from Flash's whacked "degrees clockwise" to standard radians counter-clockwise,
        // and rotate the anchor vector by the given angle (caution: y+ points down, not up!)
        var theta :Number = _media.rotation * Math.PI / -180;
        var cos :Number = Math.cos(theta);
        var sin :Number = Math.sin(theta);
        var newanchor :Point = new Point(        
            cos * anchor.x + sin * anchor.y, - sin * anchor.x + cos * anchor.y);

        // finally, shift the media over so that the new anchor overlaps the old one
        var delta :Point = anchor.subtract(newanchor);
        _media.x += delta.x;
        _media.y += delta.y;
        
    }
    
    override protected function contentDimensionsUpdated () :void
    {
        super.contentDimensionsUpdated();

        // update the hotspot
        if (_hotSpot == null) {
            _hotSpot = new Point(Math.min(_w, getMaxContentWidth())/2,
                                 Math.min(_h, getMaxContentHeight()));
        }

        // we'll want to call locationUpdated() now, but it's done for us as a result of calling
        // updateMediaPosition(), below.

        // even if we don't have strange (negative) scaling, we should do this because it ends up
        // calling locationUpdated().
        updateMediaPosition();
    }

    public function getHoverColor () :uint
    {
        return 0; // black by default
    }

    /**
     * Should we scale ourselves based on our location?
     */
    protected function useLocationScale () :Boolean
    {
        return true;
    }

    /**
     * Create the 'back end' that will be used to proxy communication with any usercode we're
     * hosting.
     */
    protected function createBackend () :EntityBackend
    {
        return new EntityBackend();
    }

    /**
     * Request control of this entity. Called by our backend in response to a request from
     * usercode. If this succeeds, a <code>gotControl</code> notification will be dispatched when
     * we hear back from the server.
     */
    public function requestControl () :void
    {
        if (_ident != null && parent is RoomView) {
            (parent as RoomView).getRoomController().requestControl(_ident);
        }
    }

    /**
     * This sprite is sending a message to all clients. Called by our backend in response to a
     * request from usercode.
     */
    public function sendMessage (name :String, arg :Object, isAction :Boolean) :void
    {
        if (_ident != null && (parent is RoomView) && validateUserData(name, arg)) {
            (parent as RoomView).getRoomController().sendSpriteMessage(_ident, name, arg, isAction);
        }
    }

    /**
     * This sprite is sending a signal to all entities on all clients. Called by our backend
     * in response to a request from usercode.
     */
    public function sendSignal (name :String, arg :Object) :void
    {
        if ((parent is RoomView) && validateUserData(name, arg)) {
            (parent as RoomView).getRoomController().sendSpriteSignal(name, arg);
        }
    }

    /**
     * Retrieve the instanceId for this item. Called by our backend in response to a request from
     * usercode.
     */
    internal function getInstanceId () :int
    {
        if (parent is RoomView) {
            return (parent as RoomView).getRoomController().getEntityInstanceId();
        }
        return 0; // not connected, not an instance
    }

    /**
     * Retrieve the display name for an instanceId/bodyOid.
     * Called by our backend in response to a request from usercode.
     */
    internal function getViewerName (instanceId :int) :String
    {
        if (parent is RoomView) {
            return (parent as RoomView).getRoomController().getViewerName(instanceId);
        }
        return null;
    }

    /**
     * Return all memories in an associative hash.
     * Called by our backend in response to a request from usercode.
     */
    internal function getMemories () :Object
    {
        var mems :Object = {};
        if (_ident != null && parent is RoomView) {
            var roomObj :RoomObject = (parent as RoomView).getRoomObject();
            for each (var entry :EntityMemoryEntry in roomObj.memories.toArray()) {
                // filter out memories with null as the value, those will not be persisted
                if (entry.value != null && entry.item.equals(_ident)) {
                    mems[entry.key] = ObjectMarshaller.decode(entry.value);
                }
            }
        }
        return mems;
    }

    /**
     * Locate the value bound to a particular key in the item's memory. Called by our backend in
     * response to a request from usercode.
     */
    internal function lookupMemory (key :String) :Object
    {
        if (_ident != null && parent is RoomView) {
            var mkey :EntityMemoryEntry = new EntityMemoryEntry(_ident, key, null),
                roomObj :RoomObject = (parent as RoomView).getRoomObject(),
                entry :EntityMemoryEntry = roomObj.memories.get(mkey) as EntityMemoryEntry;
            if (entry != null) {
                return ObjectMarshaller.decode(entry.value);
            }
        }
        return null;
    }

    /**
     * Update a memory datum. Called by our backend in response to a request from usercode.
     */
    internal function updateMemory (key :String, value: Object) :Boolean
    {
        if (_ident != null && parent is RoomView) {
            return (parent as RoomView).getRoomController().updateMemory(_ident, key, value);
        } else {
            return false;
        }
    }

    /**
     * Return all room properties in an associative hash.
     */
    internal function getRoomProperties () :Object
    {
        var props :Object = {};
        if (_ident != null && parent is RoomView) {
            var roomObj :RoomObject = (parent as RoomView).getRoomObject();
            for each (var entry :RoomPropertyEntry in roomObj.roomProperties.toArray()) {
                // filter out memories with null as the value, those will not be persisted
                if (entry.value != null) {
                    props[entry.key] = ObjectMarshaller.decode(entry.value);
                }
            }
        }
        return props;
    }

    /**
     * Locate the value bound to a particular key in the room's properties.
     */
    internal function getRoomProperty (key :String) :Object
    {
        if (_ident != null && parent is RoomView) {
            var roomObj :RoomObject = (parent as RoomView).getRoomObject(),
                entry :RoomPropertyEntry = roomObj.roomProperties.get(key) as RoomPropertyEntry;
            if (entry != null) {
                return ObjectMarshaller.decode(entry.value);
            }
        }
        return null;
    }

    /**
     * Update a room property.
     */
    internal function setRoomProperty (key :String, value: Object) :Boolean
    {
        if (_ident != null && parent is RoomView) {
            return (parent as RoomView).getRoomController().setRoomProperty(_ident, key, value);
        } else {
            return false;
        }
    }

    /**
     * Returns true if this client has edit privileges in the current room. Called by our backend
     * in response to a request from usercode.
     */
    internal function canEditRoom () :Boolean
    {
        return (parent as RoomView).getRoomController().canEditRoom();
    }

    /**
     * Update the sprite's hotspot. Called by our backend in response to a request from usercode.
     * Should be *internal* but needs to be overridable. Fucking flash!
     */
    public function setHotSpot (x :Number, y :Number, height :Number) :void
    {
        var updated :Boolean = false;
        if (!isNaN(x) && !isNaN(y) && (_hotSpot == null || x != _hotSpot.x || y != _hotSpot.y)) {
            _hotSpot = new Point(x, y);
            updated = true;
        }

        if (height != _height) {
            _height = height;
            updated = true;
        }

        if (updated && !_editing) {
            rotationUpdated();
            locationUpdated();
        }
    }

    /**
     * Validate that the user message is kosher prior to sending it.  This method is used to
     * validate all states/actions/messages.
     *
     * Note: name is taken as an Object, some methods accept an array from users and we verify
     * Stringliness too.
     *
     * TODO: memory too? (keys and values)
     */
    protected function validateUserData (name :Object, arg :Object) :Boolean
    {
        if (name != null && (!(name is String) || String(name).length > 64)) {
            return false;
        }
        // TODO: validate the size of the arg

        // looks OK!
        return true;
    }

    /**
     * Convenience method to call usercode safely.
     */
    protected function callUserCode (name :String, ... args) :*
    {
        if (_backend != null) {
            args.unshift(name);
            return _backend.callUserCode.apply(_backend, args);
        }
        return undefined;
    }

    /**
     * Has the usercode published a function with the specified name?
     */
    protected function hasUserCode (name :String) :Boolean
    {
        return (_backend != null) && _backend.hasUserCode(name);
    }

    /** The current logical coordinate of this media. */
    protected const _loc :MsoyLocation = new MsoyLocation();

    /** Identifies the item we are visualizing. All furniture will have an ident, but only our
     * avatar sprite will know its ident (and only we can update our avatar's memory, etc.).  */
    protected var _ident :ItemIdent;

    protected var _glow :GlowFilter;

    /** The media hotspot, which should be used to position it. */
    protected var _hotSpot :Point = null;

    /** The natural "height" of our visualization. If NaN then the height of the bounding box is
     * assumed, but an Entity can configure its height when configuring its hotspot. */
    protected var _height :Number = NaN;

    protected var _fxScaleX :Number = 1;
    protected var _fxScaleY :Number = 1;

    /** The 'location' scale of the media: the scaling that is the result of emulating perspective
     * while we move around the room. */
    protected var _locScale :Number = 1;

    /** Are we being edited? */
    protected var _editing :Boolean;

    /** Our control backend, communicates with usercode. */
    protected var _backend :EntityBackend;
}
}
