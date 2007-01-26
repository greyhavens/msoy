package com.threerings.msoy.world.client {

import flash.display.BlendMode;
import flash.display.DisplayObject;
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

import flash.media.Video;

import flash.net.LocalConnection;
import flash.net.NetConnection;
import flash.net.NetStream;

import flash.system.ApplicationDomain;
import flash.system.LoaderContext;
import flash.system.SecurityDomain;

import flash.net.URLRequest;

import flash.utils.getTimer; // function import

//import mx.controls.VideoDisplay;

//import mx.events.VideoEvent;

import com.threerings.util.Util;
import com.threerings.util.MediaContainer;

import com.threerings.ezgame.util.EZObjectMarshaller;

import com.threerings.msoy.client.Prefs;

import com.threerings.msoy.item.web.ItemIdent;
import com.threerings.msoy.item.web.MediaDesc;

import com.threerings.msoy.world.data.MemoryEntry;
import com.threerings.msoy.world.data.MsoyLocation;
import com.threerings.msoy.world.data.RoomObject;


/**
 * A base sprite that concerns itself with the mundane details of
 * loading and communication with the loaded media content.
 */
public class MsoySprite extends MediaContainer
{
    /** The 'instance id', which is our ClientObject's oid. */
    public static var instanceId :int;

    /** The current logical coordinate of this media. */
    public const loc :MsoyLocation = new MsoyLocation();

    /**
     * Constructor.
     */
    public function MsoySprite (desc :MediaDesc, ident :ItemIdent)
    {
        super(null);
        setup(desc, ident);
    }

    /**
     * Is this sprite included in standard layout in the RoomView?
     */
    public function isIncludedInLayout () :Boolean
    {
        return true;
    }

    /**
     * Set the scale of the media as affected by our location in the room.
     */ 
    public function setLocationScale (scale :Number) :void
    {
        if (scale != _locScale) {
            _locScale = scale;
            scaleUpdated();
        }
    }

    /**
     * Get the screen width of this sprite, taking into account both
     * horizontal scales.
     */
    public function getActualWidth () :Number
    {
        return getContentWidth() * _locScale;
    }

    /**
     * Get the screen height of this sprite, taking into account both
     * vertical scales.
     */
    public function getActualHeight () :Number
    {
        return getContentHeight() * _locScale;
    }

    public function isInteractive () :Boolean
    {
        return _desc.isInteractive();
    }

    public function hasAction () :Boolean
    {
        return false;
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
        // TODO: figure out where we're going to store hotspot info
        var p :Point = null; // TODO _desc.hotSpot;
        if (p == null) {
            // if there's no hotspot, it defaults to along the bottom
            p = new Point(_w/2, _h);

        } else {
            // return a clone of the hotspot in the descriptor
            p = p.clone();
        }
        return p;
    }

    /**
     * Get the hotspot to use for layout purposes. This point is 
     * adjusted for scale and any perspectivization.
     */
    public function getLayoutHotSpot () :Point
    {
        var p :Point = getMediaHotSpot();
        p.x = Math.abs(p.x * getMediaScaleX() * _locScale);
        p.y = Math.abs(p.y * getMediaScaleY() * _locScale);
        return p;
    }

    /**
     * Update the location (but not the orientation).
     *
     * @param newLoc may be an MsoyLocation or an Array
     */
    public function setLocation (newLoc :Object) :void
    {
        if (newLoc is MsoyLocation) {
            var mloc :MsoyLocation = (newLoc as MsoyLocation);
            loc.x = mloc.x;
            loc.y = mloc.y;
            loc.z = mloc.z;

        } else {
            var aloc :Array = (newLoc as Array);
            loc.x = aloc[0];
            loc.y = aloc[1];
            loc.z = aloc[2];
        }

        locationUpdated();
    }

    public function setActive (active :Boolean) :void
    {
        alpha = active ? 1.0 : 0.4;
        blendMode = active ? BlendMode.NORMAL : BlendMode.LAYER;
        if (active) {
            configureMouseProperties();
        } else {
            mouseEnabled = false;
            mouseChildren = false;
        }
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

    public function getDesc () :MediaDesc
    {
        return _desc;
    }

    protected function getGlowFilterIndex () :int
    {
        var ourFilters :Array = filters; // must make a copy
        if (ourFilters != null) {
        }
        return -1;
    }

    /**
     * Turn on or off the glow surrounding this sprite.
     */
    public function setGlow (doGlow :Boolean) :void
    {
        var glowIndex :int = -1;
        var ourFilters :Array = filters;
        if (ourFilters != null) {
            for (var ii :int = 0; ii < ourFilters.length; ii++) {
                if (ourFilters[ii] is GlowFilter) {
                    glowIndex = ii;
                    break;
                }
            }
        }

        // if things are already in the proper state, do nothing
        if (doGlow == (glowIndex != -1)) {
            return;
        }

        // otherwise, enable or disable the glow
        if (doGlow) {
            if (ourFilters == null) {
                ourFilters = [];
            }
            // TODO: we used to use a flex GlowEffect to make the glow
            // "grow-in" by adjusting the blur from 0 to 200 over the
            // course of 200ms. We could easily write our own,
            // but mostly I'm trying to de-flex this class, so maybe
            // we could just have the roomview take care of applying
            // and removing the glow filter...
            var glow :GlowFilter = new GlowFilter(
                getHoverColor(), 1, 20, 20);
            ourFilters.push(glow);
            filters = ourFilters;

        } else {
            ourFilters.splice(glowIndex, 1);
            filters = ourFilters;
        }
    }

    /**
     * Callback function.
     */
    public function mouseClick (event :MouseEvent) :void
    {
        // nada
    }

    /**
     * Called when a trigger event is received for this sprite.
     */
    public function eventTriggered (event: String, arg :Object) :void
    {
        callUserCode("eventTriggered_v1", event, arg);
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
            if (_media is Loader && _desc.isImage() &&
                    // the childAllowsParent check here causes a security
                    // violation if it doesn't. WHAT THE FUCK. So we also
                    // do the isImage() check above in addition to
                    // try to head-off security errors at the pass
                    Loader(_media).contentLoaderInfo.childAllowsParent &&
                    (Loader(_media).content is Bitmap)) {
                var b :Bitmap = Bitmap(Loader(_media).content);
                var p :Point = b.globalToLocal(new Point(x, y));
                return b.bitmapData.hitTest(new Point(0, 0), 0xFF, p);
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
//        if (_media is VideoDisplay) {
//            var vid :VideoDisplay = (_media as VideoDisplay);
//            Prefs.setMediaPosition(
//                MediaDesc.hashToString(_desc.hash), vid.playheadTime);
//        }

        // clean up
        if (_media is Loader) {
            Loader(_media).contentLoaderInfo.sharedEvents.removeEventListener(
                "controlConnect", handleUserCodeQuery);
        }

        super.shutdown(completely);
    }

    protected function setup (desc :MediaDesc, ident :ItemIdent) :void
    {
        if (Util.equals(desc, _desc)) {
            return;
        }

        _desc = desc;
        _ident = ident;

        setMedia(desc.getMediaPath());
        scaleUpdated();
        configureMouseProperties();
    }

    override protected function setupSwfOrImage (url :String) :void
    {
//        if (_desc.mimeType == MediaDesc.APPLICATION_SHOCKWAVE_FLASH) {
//            // create a unique id for the media
//            _id = String(getTimer()) + int(Math.random() * int.MAX_VALUE);
//
//            // TODO
//            url += "?oid=" + _id;
//        }
        super.setupSwfOrImage(url);

        // then, grab a reference to the shared event dispatcher
        Loader(_media).contentLoaderInfo.sharedEvents.addEventListener(
            "controlConnect", handleUserCodeQuery);
    }

    protected function configureMouseProperties () :void
    {
        // TODO: Oh god, we have got to figure this shit out.
        // It seems that things behave differently depending
        // on whether the loaded content is an image or
        // a SWF. Perhaps we need to check that and do the right
        // thing in either case.
        // Things may be broken in the currently checked-in state.

        if (_editing) {
            mouseEnabled = true;
            mouseChildren = false;

        } else {
            if (isInteractive()) {
                mouseEnabled = false; //true;
                mouseChildren = true;

            } else {
            /*
                mouseEnabled = false;
                mouseChildren = false;
                */
                mouseEnabled = true;
                mouseChildren = true;
            }
        }
    }

    /**
     * An internal convenience method to recompute our screen
     * position when our size, location, or anything like that has
     * been updated.
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
            _media.scaleX = _locScale * getMediaScaleX();
            _media.scaleY = _locScale * getMediaScaleY();
        }

        updateMediaPosition();
    }

    /**
     * Should be called when the media scale or size changes to ensure
     * that the media is positioned correctly.
     */
    protected function updateMediaPosition () :void
    {
        // if scale is negative, the image is flipped and we need to move
        // the origin
        var xscale :Number = getMediaScaleX();
        var yscale :Number = getMediaScaleY();
        _media.x = (xscale >= 0) ? 0 : Math.abs(_w * xscale);
        _media.y = (yscale >= 0) ? 0 : Math.abs(_h * yscale);

        // we may need to be repositioned
        locationUpdated();
    }

    /**
     * A callback called when there is a status event from using
     * the local connection.
     */
    protected static function onLocalConnStatus (event :StatusEvent) :void
    {
        // This method exists because if we don't eat status-error messages
        // then they end up bubbling up somewhere else.

        if (event.level != "status") {
//            Log.getLog(MsoySprite).debug("Unable to communicate with media " +
//                "[event=" + event + "].");
        }
    }

//    override protected function loadVideoReady (event :VideoEvent) :void
//    {
//        var vid :VideoDisplay = (event.currentTarget as VideoDisplay);
//
//        // TODO: this seems broken, check it
//        // set the position of the media to the specified timestamp
//        vid.playheadTime = Prefs.getMediaPosition(
//            MediaDesc.hashToString(_desc.hash));
//
//        super.loadVideoReady(event);
//    }

    override protected function contentDimensionsUpdated () :void
    {
        super.contentDimensionsUpdated();

        // even if we don't have strange (negative) scaling, we should do this
        // because it ends up calling locationUpdated().
        updateMediaPosition();
    }

    protected function getHoverColor () :uint
    {
        return 0x40e0e0;
    }

    /**
     * Handle a query from our usercode content.
     */
    protected function handleUserCodeQuery (evt :Object) :void
    {
        // copy down the user functions
        setUserProperties(evt.userProps);
        // pass back ours
        var hostProps :Object = new Object();
        populateControlProperties(hostProps);
        evt.hostProps = hostProps;
    }

    protected function setUserProperties (o :Object) :void
    {
//        // prototype for backwards compatability:
//        var oldFunc :Function = (o["avatarChanged_v1"] as Function);
//        if (oldFunc != null) {
//            // make a new function that adapts to the old one
//            o["avatarChanged_v2"] =
//                function (moving :Boolean, orient :Number, newParam :String)
//                :void {
//                    oldFunc(moving, orient);
//                };
//        }

        // then, simply save the properties
        _props = o;
    }

    /**
     * Populate the properties we pass back to user-code.
     */
    protected function populateControlProperties (o :Object) :void
    {
        o["requestControl_v1"] = requestControl_v1;
        o["triggerEvent_v1"] = triggerEvent_v1;
        o["lookupMemory_v1"] = lookupMemory_v1;
        o["updateMemory_v1"] = updateMemory_v1;
        o["getInstanceId_v1"] = getInstanceId_v1;
    }

    /**
     * Explicitly requests control for this entity by this client. If this succeeds, a
     * <code>gotControl_v1</code> notification will be dispatched when we hear back from the
     * server.
     */
    protected function requestControl_v1 () :void
    {
        if (_ident != null && parent is RoomView) {
            (parent as RoomView).getRoomController().requestControl(_ident);
        }
    }

    /**
     * Called by {@link MsoyControl} to trigger an event on this sprite in all clients.
     */
    protected function triggerEvent_v1 (event :String, arg :Object = null) :void
    {
        if (_ident != null && parent is RoomView) {
            (parent as RoomView).getRoomController().triggerEvent(_ident, event, arg);
        }
    }

    /**
     * Called by {@link MsoyControl} to retrieve the instanceId for this item.
     */
    protected function getInstanceId_v1 () :int
    {
        return instanceId;
    }

    /**
     * Called by {@link MsoyControl} to locate the value bound to a particular key in the item's
     * memory.
     */
    protected function lookupMemory_v1 (key :String) :Object
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
     * Called by {@link MsoyControl} to update a memory datum.
     */
    protected function updateMemory_v1 (key :String, value: Object) :Boolean
    {
        if (_ident != null && parent is RoomView) {
            return (parent as RoomView).getRoomController().updateMemory(_ident, key, value);
        } else {
            return false;
        }
    }

    /**
     * Call an exposed function in usercode.
     */
    protected function callUserCode (name :String, ... args) :*
    {
        if (_props != null) {
            try {
                var func :Function = (_props[name] as Function);
                if (func != null) {
                    return func.apply(null, args);
                }

            } catch (err :Error) {
                log.warning("Error in user-code: " + err);
                log.logStackTrace(err);
            }
        }
        return undefined;
    }

    /** Our Media descriptor. */
    protected var _desc :MediaDesc;

    /** Identifies the item we are visualizing. All furniture will have an ident, but only our
     * avatar sprite will know its ident (and only we can update our avatar's memory, etc.).  */
    protected var _ident :ItemIdent;

    /** The 'location' scale of the media: the scaling that is the result of
     * emulating perspective while we move around the room. */
    protected var _locScale :Number = 1;

    /** Are we being edited? */
    protected var _editing :Boolean;

    /** Properties populated by *Control usercode. */
    protected var _props :Object;
}
}
