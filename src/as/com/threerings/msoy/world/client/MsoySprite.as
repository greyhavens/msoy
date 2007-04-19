package com.threerings.msoy.world.client {

import flash.display.BlendMode;
import flash.display.DisplayObject;
import flash.display.DisplayObjectContainer;
import flash.display.Loader;
import flash.display.LoaderInfo;
import flash.display.Shape;
import flash.display.Sprite;

import flash.display.Bitmap;
import flash.display.BitmapData;

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

import flash.geom.Point;
import flash.geom.Rectangle;

import flash.net.LocalConnection;
import flash.net.NetConnection;
import flash.net.NetStream;

import flash.system.ApplicationDomain;
import flash.system.LoaderContext;
import flash.system.SecurityDomain;

import flash.net.URLRequest;

import com.threerings.util.Util;

import com.threerings.flash.FilterUtil;
import com.threerings.flash.MediaContainer;
import com.threerings.flash.VideoDisplayer;

import com.threerings.ezgame.util.EZObjectMarshaller;

import com.threerings.msoy.client.Prefs;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.StaticMediaDesc;

import com.threerings.msoy.world.data.MemoryEntry;
import com.threerings.msoy.world.data.MsoyLocation;
import com.threerings.msoy.world.data.RoomObject;


/**
 * A base sprite that concerns itself with the mundane details of
 * loading and communication with the loaded media content.
 */
public class MsoySprite extends MediaContainer
    implements RoomElement
{
    /**
     * Constructor.
     */
    public function MsoySprite (desc :MediaDesc, ident :ItemIdent)
    {
        super(null);
        setup(desc, ident);
    }

    // from RoomElement
    public function isIncludedInLayout () :Boolean
    {
        return true;
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
        this.x = x
        this.y = y
        if (scale != _locScale) {
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
     * Return the item ident used to identify this sprite.
     */
    public function getItemIdent () :ItemIdent
    {
        return _ident;
    }

    /**
     * Get the screen width of this sprite, taking into account both
     * horizontal scales.
     */
    public function getActualWidth () :Number
    {
        return getContentWidth() * _locScale * _fxScaleX;
    }

    /**
     * Get the screen height of this sprite, taking into account both
     * vertical scales.
     */
    public function getActualHeight () :Number
    {
        return getContentHeight() * _locScale * _fxScaleY;
    }

    /**
     * Get the rectangle bounding this sprite on the stage.
     */
    public function getStageRect () :Rectangle
    {
        var botRight :Point = new Point(getActualWidth(), getActualHeight());
        var r :Rectangle = new Rectangle();
        r.topLeft = localToGlobal(new Point(0, 0));
        r.bottomRight = localToGlobal(botRight);
        return r;
    }

    public function hasAction () :Boolean
    {
        return false;
    }

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
     * Get the basic hotspot that is the registration point on the media.
     * This point is not scaled.
     */
    public function getMediaHotSpot () :Point
    {
        // the hotspot is null until set-up.
        return (_hotSpot != null) ? _hotSpot : new Point(0, 0);
    }

    /**
     * Get the hotspot to use for layout purposes. This point is
     * adjusted for scale and any perspectivization.
     */
    public function getLayoutHotSpot () :Point
    {
        var p :Point = getMediaHotSpot();
        return new Point(
            Math.abs(p.x * getMediaScaleX() * _locScale * _fxScaleX),
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

//    /**
//     * Get the media descriptor.
//     */
//    public function getMediaDesc () :MediaDesc
//    {
//        return _desc;
//    }

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

        } else {
            FilterUtil.removeFilter(_media, _glow);
            _glow = null;
        }
    }

    /**
     * Callback function.
     */
    public function mouseClick (event :MouseEvent) :void
    {
        // by default, we post some CommandEvent
        postClickAction();
    }

    /**
     * Called when an action or message to received for this sprite.
     */
    public function messageReceived (name :String, arg :Object, isAction :Boolean) :void
    {
        callUserCode("messageReceived_v1", name, arg, isAction);

        // TODO: remove someday
        // TEMP: dispatch a backwards compatible event to older style
        // entities. This older method was deprecated 2007-03-12, so hopefully
        // we don't have to keep this around too long.
        if (isAction) {
            callUserCode("eventTriggered_v1", name, arg);
        }
    }

    /**
     * Called when a datum in the this sprite's item's memory changes.
     */
    public function memoryChanged (key :String, value: Object) :void
    {
        callUserCode("memoryChanged_v1", key, value);
    }

    /**
     * Called when this client is assigned control of this entity.
     */
    public function gotControl () :void
    {
        callUserCode("gotControl_v1");
    }

    /**
     * This method is NOT used in normal mouseOver calculations.
     * Normal mouseOver stuff seems to be completely broken for transparent
     * images: the transparent portion is a 'hit'. I've (Ray) tried
     * just about everything to fix this, more than once.
     */
    override public function hitTestPoint (
        x :Number, y :Number, shapeFlag :Boolean = false) :Boolean
    {
        // if we're holding a bitmap, do something smarter than the
        // flash built-in and actually check for *GASP* transparent pixels!
        try {
            if (shapeFlag && _media is Loader && _desc.isImage() &&
                    // the childAllowsParent check here causes a security
                    // violation if it doesn't. WHAT THE FUCK. So we also
                    // do the isImage() check above in addition to
                    // try to head-off security errors at the pass
                    Loader(_media).contentLoaderInfo.childAllowsParent &&
                    (Loader(_media).content is Bitmap)) {
                var b :Bitmap = Bitmap(Loader(_media).content);
                var p :Point = b.globalToLocal(new Point(x, y));
                return b.bitmapData.hitTest(new Point(0, 0), 0, p);
            }
        } catch (err :Error) {
            // nada
        }
        return super.hitTestPoint(x, y, shapeFlag);
    }

    /**
     * Unload the media we're displaying, clean up any resources.
     *
     * @param completely if true, we're going away and should stop
     * everything. Otherwise, we're just loading up new media.
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

    public function isBlockable () :Boolean
    {
        return !(_desc is StaticMediaDesc);
    }

    public function isBlocked () :Boolean
    {
        return Prefs.isMediaBlocked(_desc.getMediaId());
    }

    public function toggleBlocked () :void
    {
        var nowBlocked :Boolean = !isBlocked();
        Prefs.setMediaBlocked(_desc.getMediaId(), nowBlocked);
        setIsBlocked(nowBlocked);
    }

    protected function setup (desc :MediaDesc, ident :ItemIdent) :void
    {
        if (Util.equals(desc, _desc)) {
            return;
        }

        _desc = desc;
        _ident = ident;

        setIsBlocked(Prefs.isMediaBlocked(_desc.getMediaId()));
//        scaleUpdated();
//        configureMouseProperties();
    }

    protected function setIsBlocked (blocked :Boolean) :void
    {
        var desc :MediaDesc;
        if (blocked) {
            desc = new StaticMediaDesc(MediaDesc.IMAGE_JPEG, Item.FURNITURE, "blocked");
        } else {
            desc = _desc;
        }
        setMedia(desc.getMediaPath());
        scaleUpdated();
        configureMouseProperties();
    }

    override protected function setupSwfOrImage (url :String) :void
    {
        super.setupSwfOrImage(url);

        _backend = createBackend();
        _backend.init(Loader(_media));
        _backend.setSprite(this);
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
        mouseChildren = active && !_editing && !hasAction() && capturesMouse();
        mouseEnabled = active;
    }

    /**
     * An internal convenience method to recompute our screen position when our size, location, or
     * anything like that has been updated.
     */
    protected function locationUpdated () :void
    {
        if (parent is AbstractRoomView) {
            (parent as AbstractRoomView).locationUpdated(this);
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

    /**
     * Should be called when the media scale or size changes to ensure that the media is positioned
     * correctly.
     */
    protected function updateMediaPosition () :void
    {
        // if scale is negative, the image is flipped and we need to move
        // the origin
        var xscale :Number = _locScale * getMediaScaleX() * _fxScaleX;
        var yscale :Number = _locScale * getMediaScaleY() * _fxScaleY;
        _media.x = (xscale >= 0) ? 0 : Math.abs(Math.min(_w, getMaxContentWidth()) * xscale);
        _media.y = (yscale >= 0) ? 0 : Math.abs(Math.min(_h, getMaxContentHeight()) * yscale);

        // we may need to be repositioned
        locationUpdated();
    }

    /**
     * A callback called when there is a status event from using the local connection.
     */
    protected static function onLocalConnStatus (event :StatusEvent) :void
    {
        // This method exists because if we don't eat status-error messages then they end up
        // bubbling up somewhere else.

        if (event.level != "status") {
//            Log.getLog(MsoySprite).debug("Unable to communicate with media " +
//                "[event=" + event + "].");
        }
    }

    override protected function contentDimensionsUpdated () :void
    {
        super.contentDimensionsUpdated();

        // update the hotspot
        if (_hotSpot == null) {
            _hotSpot = new Point(_w / 2, _h);
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
     * Create the 'back end' that will be used to proxy communication with any usercode we're
     * hosting.
     */
    protected function createBackend () :EntityBackend
    {
        return new EntityBackend();
    }

    /**
     * Request control of this entity. Called by our backend in response to a request from
     * usercode.  If this succeeds, a <code>gotControl</code> notification will be dispatched when
     * we hear back from the server.
     */
    internal function requestControl () :void
    {
        if (_ident != null && parent is RoomView) {
            (parent as RoomView).getRoomController().requestControl(_ident);
        }
    }

    /**
     * This sprite is sending a message to all clients. Called by our backend in response to a
     * request from usercode.
     */
    internal function sendMessage (name :String, arg :Object, isAction :Boolean) :void
    {
        if (_ident != null && (parent is RoomView) && validateUserData(name, arg)) {
            (parent as RoomView).getRoomController().sendSpriteMessage(_ident, name, arg, isAction);
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
     * Locate the value bound to a particular key in the item's memory. Called by our backend in
     * response to a request from usercode.
     */
    internal function lookupMemory (key :String) :Object
    {
        if (_ident != null && parent is RoomView) {
            var mkey :MemoryEntry = new MemoryEntry(_ident, key, null),
                roomObj :RoomObject = (parent as RoomView).getRoomObject(),
                entry :MemoryEntry = roomObj.memories.get(mkey) as MemoryEntry;
            if (entry != null) {
                return EZObjectMarshaller.decode(entry.value);
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
            locationUpdated();
        }
    }

    /**
     * Validate that the user message is kosher prior to sending it.
     * This method is used to validate all states/actions/messages.
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

    /** The current logical coordinate of this media. */
    public const _loc :MsoyLocation = new MsoyLocation();

    /** Our Media descriptor. */
    protected var _desc :MediaDesc;

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

    /** The 'location' scale of the media: the scaling that is the result of
     * emulating perspective while we move around the room. */
    protected var _locScale :Number = 1;

    /** Are we being edited? */
    protected var _editing :Boolean;

    /** Our control backend, communicates with usercode. */
    protected var _backend :EntityBackend;

    /** Hover colors. */
    protected static const AVATAR_HOVER :uint = 0x99BFFF;// light blue
    protected static const PET_HOVER :uint = 0x999999;// light gray
    protected static const PORTAL_HOVER :uint = 0xe04040; // reddish
    protected static const GAME_HOVER :uint = 0xFFFFFF;  // white
    protected static const OTHER_HOVER :uint = 0x000000; // black
}
}
