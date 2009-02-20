//
// $Id$

package com.threerings.msoy.room.client {

import flash.display.BlendMode;
import flash.display.DisplayObject;
import flash.display.DisplayObjectContainer;
import flash.display.Loader;

import flash.events.MouseEvent;

import flash.filters.GlowFilter;

import flash.geom.Point;
import flash.geom.Rectangle;

import com.threerings.util.CommandEvent;
import com.threerings.util.StringUtil;
import com.threerings.util.ValueEvent;

import com.threerings.flash.FilterUtil;
import com.threerings.flash.media.VideoPlayer;

import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.client.MsoyController;

import com.threerings.msoy.ui.DataPackMediaContainer;
import com.threerings.msoy.ui.MsoyVideoDisplay;

import com.threerings.msoy.item.data.all.ItemIdent;

import com.threerings.msoy.world.client.WorldContext;

import com.threerings.msoy.room.data.MsoyLocation;
import com.threerings.msoy.room.data.RoomCodes;

/**
 * A base sprite that concerns itself with the mundane details of loading and communication with
 * the loaded media content.
 */
public class MsoySprite extends DataPackMediaContainer
    implements RoomElement
{
    /** The type of a ValueEvent that is dispatched when the location is updated, but ONLY if the
     * parent is not a RoomView. */
    public static const LOCATION_UPDATED :String = "locationUpdated";

    /** Hover colors. */
    public static const AVATAR_HOVER :uint = 0x99BFFF;// light blue
    public static const PET_HOVER :uint = 0x999999; // light gray
    public static const PORTAL_HOVER :uint = 0x7BFFB0; // happy green
    public static const GAME_HOVER :uint = 0xFFFFFF;  // white
    public static const OTHER_HOVER :uint = 0xFF993F; // orange

    /**
     * Construct a MsoySprite.
     */
    public function MsoySprite (ctx :WorldContext)
    {
        _ctx = ctx;
    }

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
    public function isImportant () :Boolean
    {
        return false;
    }

    // from RoomElement
    public function setScreenLocation (x :Number, y :Number, scale :Number) :void
    {
        this.x = x;
        this.y = y;

        if (!useLocationScale()) {
            scale = 1;
        }
        if (scale != _locScale) {
            _locScale = scale;
            scaleUpdated();
        }
    }

//    public function setEffectScales (xscale :Number, yscale :Number) :void
//    {
//        _fxScaleX = xscale;
//        _fxScaleY = yscale;
//        scaleUpdated();
//    }

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
        return getContentWidth() * _locScale /* * _fxScaleX*/;
    }

    /**
     * Get the screen height of this sprite, taking into account both vertical scales.
     */
    public function getActualHeight () :Number
    {
        return getContentHeight() * _locScale /* * _fxScaleY*/;
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
        return (parent is RoomView) ? RoomView(parent).getRoomBounds() : null;
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
        return new Point(Math.abs(p.x * getMediaScaleX() * _locScale /* * _fxScaleX*/),
                         Math.abs(p.y * getMediaScaleY() * _locScale /* * _fxScaleY*/));
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
        var xscale :Number = _locScale * getMediaScaleX() /* * _fxScaleX*/;
        if (xscale < 0) {
            anchor.x = -anchor.x;
        }

        var yscale :Number = _locScale * getMediaScaleY() /* * _fxScaleY*/;
        if (yscale < 0) {
            anchor.y = -anchor.y;
        }

        return anchor;
    }

    /**
     * Turn on or off the glow surrounding this sprite.
     *
     * @return a String, null, or true. A String or null is returned if the tooltip should
     * be changed, true is returned if nothing has changed since the last hover.
     */
    public function setHovered (
        hovered :Boolean, stageX :Number = NaN, stageY :Number = NaN) :Object
    {
        if (hovered == (_glow == null)) {
            setGlow(hovered);
            return hovered ? getToolTipText() : null;
        }
        return true;
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
     * Called to inform this sprite that the mouse has been clicked on it. We don't use the normal
     * Flash click handling because it doesn't do the right thing with transparent pixels, so
     * RoomController does its own custom hist testing and calls this method manually.
     */
    public function mouseClick (event :MouseEvent) :void
    {
        postClickAction();
    }

    /**
     * Receives a chat message from the room, and forwards it over to user land.
     */
    public function processChatMessage (
        fromEntityIdent :String, fromEntityName :String, msg :String) :void
    {
        if (hasUserCode("receivedChat_v2")) {
            callUserCode("receivedChat_v2", fromEntityIdent, msg);
        } else {
            // only pets have this old receivedChat_v1
            callUserCode("receivedChat_v1", fromEntityName, msg);
        }
    }

    /**
     * Called when an action or message to received for this sprite.
     */
    public function messageReceived (name :String, arg :Object, isAction :Boolean) :void
    {
        callUserCode("messageReceived_v1", name, arg, isAction);
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
     * Called when an entity has been added to this sprite's room.
     */
    public function entityEntered (entityId :String) :void
    {
        callUserCode("entityEntered_v1", entityId);
    }

    /**
     * Called when an entity has been removed from this sprite's room.
     */
    public function entityLeft (entityId :String) :void
    {
        callUserCode("entityLeft_v1", entityId);
    }

    /**
     * Called when an entity has moved within the room.
     */
    public function entityMoved (entityId :String, destination :Array) :void
    {
        if (hasUserCode("entityMoved_v2")) {
            // make a copy of the array so that usercode can't fuck with it
            callUserCode("entityMoved_v2", entityId,
                (destination == null) ? null : destination.concat());
        } else if (destination == null) {
            // v1 only fired this event upon arrival of the destination
            callUserCode("entityMoved_v1", entityId);
        }
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

    /** @inheritDoc */
    // from MediaContainer
    final override public function getMediaScaleX () :Number
    {
        // use a fixed 3/4 scale for blocked media
        return isBlocked() ? 0.75 : getSpriteMediaScaleX();
    }

    /** @inheritDoc */
    // from MediaContainer
    final override public function getMediaScaleY () :Number
    {
        // use a fixed 3/4 scale for blocked media
        return isBlocked() ? 0.75 : getSpriteMediaScaleY();
    }

    /**
     * Provides the horizontal scale of the sprite when not blocked. When the media is blocked, it
     * has a fixed scale. Callers should always access the scale using <code>getMediaScaleX</code>.
     */
    protected function getSpriteMediaScaleX () :Number
    {
        return 1.0;
    }

    /**
     * Provides the vertical scale of the sprite when not blocked. When the media is blocked, it
     * has a fixed scale. Callers should always access the scale using <code>getMediaScaleY</code>.
     */
    protected function getSpriteMediaScaleY () :Number
    {
        return 1.0;
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
        if (_backend != null) {
            _backend.init(_ctx, loader.contentLoaderInfo);
            _backend.setSprite(this);
        }

        return loader;
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
        if (parent is RoomView) {
            (parent as RoomView).locationUpdated(this);

        } else {
            dispatchEvent(new ValueEvent(LOCATION_UPDATED, null));
        }
    }

    protected function scaleUpdated () :void
    {
        if (_media != null && !(_media is Perspectivizer)) {
            var scalex :Number = _locScale * getMediaScaleX() /* * _fxScaleX*/;
            var scaley :Number = _locScale * getMediaScaleY() /* * _fxScaleY*/;

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
        if (_media != null) {
            _media.rotation = getMediaRotation();
        }
        updateMediaPosition();
    }

    /**
     * Should be called when the media scale or size changes to ensure that the media is positioned
     * correctly.
     */
    protected function updateMediaPosition () :void
    {
        if (_media != null) {
            // if scale is negative, the image is flipped and we need to move the origin
            var xscale :Number = _locScale * getMediaScaleX() /* * _fxScaleX*/;
            var yscale :Number = _locScale * getMediaScaleY() /* * _fxScaleY*/;
            _media.x = (xscale >= 0) ? 0 : Math.abs(Math.min(_w, getMaxContentWidth()) * xscale);
            _media.y = (yscale >= 0) ? 0 : Math.abs(Math.min(_h, getMaxContentHeight()) * yscale);
        }

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
        if (_media == null || _media.rotation == 0) {
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
        var ctrl :RoomController = getController(true);
        if (ctrl != null) {
            ctrl.requestControl(_ident);
        }
    }

    /**
     * This sprite is sending a message to all clients. Called by our backend in response to a
     * request from usercode.
     */
    public function sendMessage (name :String, arg :Object, isAction :Boolean) :void
    {
        var ctrl :RoomController = getController(true);
        if (ctrl != null && validateUserData(name, arg)) {
            ctrl.sendSpriteMessage(_ident, name, arg, isAction);
        }
    }

    /**
     * This sprite is sending a signal to all entities on all clients. Called by our backend
     * in response to a request from usercode.
     */
    public function sendSignal (name :String, arg :Object) :void
    {
        var ctrl :RoomController = getController();
        if (ctrl != null && validateUserData(name, arg)) {
            ctrl.sendSpriteSignal(_ident, name, arg);
        }
    }

    /**
     * Retrieve the instanceId for this item. Called by our backend in response to a request from
     * usercode.
     */
    internal function getInstanceId () :int
    {
        var ctrl :RoomController = getController();
        return (ctrl != null) ? ctrl.getEntityInstanceId() : 0; // 0 == not connected, not instance
    }

    /**
     * Retrieve the display name for an instanceId/bodyOid.
     * Called by our backend in response to a request from usercode.
     */
    internal function getViewerName (instanceId :int) :String
    {
        var ctrl :RoomController = getController();
        return (ctrl != null) ? ctrl.getViewerName(instanceId) : null;
    }

    /**
     * Return all memories in an associative hash.
     * Called by our backend in response to a request from usercode.
     */
    internal function getMemories () :Object
    {
        var ctrl :RoomController = getController(true);
        return (ctrl != null) ? ctrl.getMemories(_ident) : {};
    }

    /**
     * Locate the value bound to a particular key in the item's memory. Called by our backend in
     * response to a request from usercode.
     */
    internal function lookupMemory (key :String) :Object
    {
        var ctrl :RoomController = getController(true);
        return (ctrl != null) ? ctrl.lookupMemory(_ident, key) : null;
    }

    /**
     * Update a memory datum. Called by our backend in response to a request from usercode.
     */
    internal function updateMemory (key :String, value: Object, callback :Function) :void
    {
        var ctrl :RoomController = getController(true);
        if (ctrl != null) {
            // the controller will validate the key/value when it encodes them
            ctrl.updateMemory(_ident, key, value, callback);
        }
    }

    protected function getSpecialProperty (name :String) :Object
    {
        switch (name) {
        case "hotspot":
            var hotspot :Point = getMediaHotSpot();
            return [ hotspot.x, hotspot.y ];

        case "location_logical":
            var loc :MsoyLocation = getLocation();
            return [ loc.x, loc.y, loc.z ];

        case "location_pixel":
            var ploc :MsoyLocation = getLocation();
            var bounds :Array = getRoomBounds();
            bounds[0] *= ploc.x;
            bounds[1] *= ploc.y;
            bounds[2] *= ploc.z;
            return bounds;

        case "dimensions":
            return [ getContentWidth(), getContentHeight() ];

        case "orientation":
            return getLocation().orient;

        case "type":
            return RoomController.ENTITY_TYPES[_ident.type]; // will return null if unknown type

        default:
            return null;
        }
    }

    /**
     * Fetch a property using this sprite's property provider.
     */
    public function lookupEntityProperty (key :String) :Object
    {
        // see if it's one of our special standard properties
        if (key != null && StringUtil.startsWith(key, "std:")) {
            return getSpecialProperty(key.substr(4));
        }

        return callUserCode("lookupEntityProperty_v1", key);
    }

    /**
     * Requests the item this sprite represents be deleted from its owner's inventory
     * and removed from the room.
     */
    public function selfDestruct () :void
    {
        var ctrl :RoomController = getController(true);
        if (ctrl != null) {
            ctrl.deleteItem(_ident);
        }
    }

    public function getEntityIds (type :String) :Array
    {
        var ctrl :RoomController = getController();

        return (ctrl == null) ? null : ctrl.getEntityIds(type);
    }

    public function getEntityProperty (entityId :String, key :String) :Object
    {
        // If the entityId is ME, take the shortcut
        if (entityId == null) {
            return lookupEntityProperty(key);
        }

        var ctrl :RoomController = getController();

        return (ctrl == null) ? null :
            ctrl.getEntityProperty(ItemIdent.fromString(entityId), key);
    }

    /**
     * Returns true if this client has management privileges in the current room.
     * Called by our backend in response to a request from usercode.
     */
    internal function canManageRoom (memberId :int) :Boolean
    {
        var ctrl :RoomController = getController();
        return (ctrl != null) && ctrl.canManageRoom(memberId);
    }

    /**
     * Convenience function to get the room controller, or null if we're disconnected.
     */
    protected function getController (requireIdent :Boolean = false) :RoomController
    {
        if (requireIdent && _ident == null) {
            return null;
        }
        var room :RoomView = parent as RoomView;
        return (room != null) ? room.getRoomController() : null;
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
     */
    protected function validateUserData (name :Object, arg :Object) :Boolean
    {
        if (name != null && (!(name is String) || String(name).length > 64)) {
            return false;
        }
        // NOTE: the size of the arg is validated elsewhere

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

    override protected function createVideoUI (player :VideoPlayer) :DisplayObject
    {
        // here, we assume that the ItemIdent is configured prior to the MediaDesc. Should be true.
        return new MsoyVideoDisplay(player, (_ident == null) ? null : handleViewItem);
    }

    /**
     * Handle the "comment" button on the video player.
     */
    protected function handleViewItem () :void
    {
        CommandEvent.dispatch(this, MsoyController.VIEW_ITEM, _ident);
    }

    /** The giver of life. */
    protected var _ctx :WorldContext;

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

//    protected var _fxScaleX :Number = 1;
//    protected var _fxScaleY :Number = 1;

    /** The 'location' scale of the media: the scaling that is the result of emulating perspective
     * while we move around the room. */
    protected var _locScale :Number = 1;

    /** Are we being edited? */
    protected var _editing :Boolean;

    /** Our control backend, communicates with usercode. */
    protected var _backend :EntityBackend;
}
}
