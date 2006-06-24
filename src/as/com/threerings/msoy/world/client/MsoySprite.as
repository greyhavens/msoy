package com.threerings.msoy.world.client {

import flash.display.BlendMode;
import flash.display.DisplayObject;
import flash.display.DisplayObjectContainer;
import flash.display.Loader;
import flash.display.LoaderInfo;
import flash.display.Shape;

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

import flash.geom.Point;

import flash.media.Video;

import flash.net.LocalConnection;
import flash.net.NetConnection;
import flash.net.NetStream;

import flash.system.ApplicationDomain;
import flash.system.LoaderContext;
import flash.system.SecurityDomain;

import flash.net.URLRequest;

import flash.utils.Timer;

import mx.core.Container;

import mx.containers.Box;
import mx.controls.VideoDisplay;

import mx.effects.Glow;

import mx.events.EffectEvent;
import mx.events.VideoEvent;

import com.threerings.media.image.ImageUtil;

import com.threerings.msoy.client.Prefs;
import com.threerings.msoy.data.MediaData;

import com.threerings.msoy.world.client.RoomView;
import com.threerings.msoy.world.data.MsoyLocation;

import com.threerings.util.HashMap;

/**
 * A wrapper class for all media that will be placed on the screen.
 * Subject to change.
 */
/**
 NOTES:
 Any piece of media seen in msoy will likely have two interaction modes:
 1) Interaction based on the object it represents.
    If the media represents a user avatar, other users can view user info,
    send tells, etc. If the media represents furniture then clicking may
    interact with that furniture (play a game, etc).
 2) Interaction with the media itself.
   - tag it / edit tags
   - jump to it in the catalog
   - buy it without jumping to the catalog

 Perhaps we standardize on something like straight-clicks are for mode 1,
 right-clicking is for mode 2. Or we just have a standard popup menu
 that contains all possible commands..

 */
public class MsoySprite extends Box
{
    /** The current logical coordinate of this media. */
    public const loc :MsoyLocation = new MsoyLocation();

    /**
     * Constructor.
     */
    public function MsoySprite (desc :MediaData)
    {
        _desc = desc;
        _id = int(Math.random() * int.MAX_VALUE);

        // configure the media
        var url :String = desc.URL;
        if (url.toLowerCase().lastIndexOf(".flv") ==
                url.length - ".flv".length) {
            setupVideo(url);

        } else {
            setupOther(url);
        }

        // set up the scale
        _media.scaleX = getMediaScaleX();
        _media.scaleY = getMediaScaleY();

        // set up mouse listeners
        if (isInteractive()) {
            addEventListener(MouseEvent.MOUSE_OVER, mouseOver);
            addEventListener(MouseEvent.MOUSE_OUT, mouseOut);
            addEventListener(MouseEvent.CLICK, mouseClick);
/*
            loader.addEventListener(MouseEvent.CLICK, mouseClickCap, true);
            loader.addEventListener(MouseEvent.CLICK, mouseClickCap);
            addEventListener(MouseEvent.CLICK, mouseClickCap);
            */
        } else {
            mouseEnabled = false;
            mouseChildren = false;
        }
    }

    /**
     * Configure this sprite to show a video.
     */
    protected function setupVideo (url :String) :void
    {
        var vid :VideoDisplay = new VideoDisplay();
        _media = vid;
        addChild(vid);
        vid.addEventListener(ProgressEvent.PROGRESS, loadVideoProgress);
        vid.addEventListener(VideoEvent.READY, loadVideoReady);
        vid.addEventListener(VideoEvent.REWIND, videoDidRewind);

        // start it loading
        vid.source = url;

        /*
        var timer :Timer = new Timer(1000);
        timer.addEventListener(TimerEvent.TIMER,
            function (evt :Event) :void {
                trace("Video: (" + vid.bytesLoaded + " / " +
                    vid.bytesTotal + " bytes) " + vid.playheadTime +
                    ": " + vid.state + "  (" + width + ", " + height +
                    ") (" + vid.width + ", " + vid.height + ")");
                updateContentDimensions(
                    vid.videoWidth, vid.videoHeight);
            });
        timer.start();
        */
    }

    /**
     * Configure this sprite to show an image or flash movie.
     */
    protected function setupOther (url :String) :void
    {
        if (_desc.isAVM1) {
            // TODO
            url += "?oid=" + _id;
        }

        // create our loader and set up some event listeners
        var loader :Loader = new Loader();
        _media = loader;
        loader.contentLoaderInfo.addEventListener(
            Event.COMPLETE, loadingComplete);
        loader.contentLoaderInfo.addEventListener(
            IOErrorEvent.IO_ERROR, loadError);
        loader.contentLoaderInfo.addEventListener(
            ProgressEvent.PROGRESS, loadProgress);

        // grab hold of the EventDispatcher we'll use for comm
        _dispatch = loader.contentLoaderInfo.sharedEvents;

        // create a mask to prevent the media from drawing out of bounds
        if (maxContentWidth < int.MAX_VALUE  &&
                maxContentHeight < int.MAX_VALUE) {
            var mask :Shape = new Shape();
            mask.graphics.beginFill(0xFFFFFF);
            mask.graphics.drawRect(0, 0, maxContentWidth, maxContentHeight);
            mask.graphics.endFill();
            // the mask must be added to the display list (which is wacky)
            rawChildren.addChild(mask);
            loader.mask = mask;
        }

        // start it loading, add it as a child
        loader.load(new URLRequest(url), getContext(url));
        rawChildren.addChild(loader);
    }

    /**
     * Stop displaying media, clean up any resources.
     */
    public function shutdown () :void
    {
        try {
            if (_media is Loader) {
                var loader :Loader = (_media as Loader);
                loader.unload();
                loader.close();

            } else {
                var vid :VideoDisplay = (_media as VideoDisplay);
                vid.removeEventListener(VideoEvent.REWIND, videoDidRewind);
                vid.pause();
                Prefs.setMediaPosition(_desc.id, vid.playheadTime);
                trace("saving media pos: " + vid.playheadTime);
                vid.stop();
                vid.close();
            }
        } catch (ioe :IOError) {
            trace("error shutdown " + ioe);
        }

        // additional clearing: needed?
        _media = null;
    }

    public function get hotSpot () :Point
    {
        var p :Point = _desc.hotSpot;
        if (p == null) {
            p = new Point(contentWidth/2, contentHeight);

        } else {
            // scale the hotspot associated with the media
            p = new Point(Math.abs(p.x * getMediaScaleX()),
                Math.abs(p.y * getMediaScaleY()));
        }
        return p;
    }

    public function get contentWidth () :int
    {
        return Math.min(_w, maxContentWidth);
    }

    public function get contentHeight () :int
    {
        return Math.min(_h, maxContentHeight);
    }

    /**
     * Get the maximum allowable width for our content.
     */
    public function get maxContentWidth () :int
    {
        return int.MAX_VALUE;
    }

    /**
     * Get the maximum allowable height for our content.
     */
    public function get maxContentHeight () :int
    {
        return int.MAX_VALUE;
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

    /**
     * An internal convenience method to recompute our screen
     * position when our size, location, or anything like that has
     * been updated.
     */
    protected function locationUpdated () :void
    {
        if (parent is RoomView) {
            (parent as RoomView).locationUpdated(this);
        }
    }

    /** A callback from the move. */
    public function moveCompleted (orient :Number) :void
    {
        // nada
    }

    public function setActive (active :Boolean) :void
    {
        alpha = active ? 1.0 : 0.4;
        blendMode = active ? BlendMode.NORMAL : BlendMode.LAYER;
        mouseEnabled = active && isInteractive();
        mouseChildren = active && isInteractive();
    }

    /**
     * Get the X scaling factor to use on the actual media, independent
     * of the scaling done to simulate depth.
     */
    public function getMediaScaleX () :Number
    {
        return 1;
    }

    /**
     * Get the Y scaling factor to use on the actual media, independent
     * of the scaling done to simulate depth.
     */
    public function getMediaScaleY () :Number
    {
        return 1;
    }

/* Commented out: this doesn't fucking work, this method should be called
 * parentWillChange. For now, just require folks to add the component first
 * and then set the location.

    override public function parentChanged (p :DisplayObjectContainer) :void
    {
        super.parentChanged(p);
        if (p is RoomView) {
            (p as RoomView).locationUpdated(this);
        }
    }
 */

    public function get description () :MediaData
    {
        return _desc;
    }

    /**
     * Accessor: media property.
     */
    // Probably this should be removed
/*
    public function get media () :DisplayObject
    {
        // untested
        // TODO: needed? remove?
        for (var ii :int = rawChildren.numChildren - 1; ii >= 0; ii--) {
            var disp :DisplayObject = rawChildren.getChildAt(ii);
            if (disp is Loader) {
                return (disp as Loader).content;

            } else if (!(disp is Shape)) {
                return disp;
            }
        }
        return null; // never found!
    }
    */

    /**
     * Send a message to the client swf that we're representing.
     */
    protected function sendMessage (type :String, msg :String) :void
    {
        trace("sending [" + type + "=" + msg + "]");

        if (_desc.isAVM1) {
            if (_oldDispatch == null) {
                _oldDispatch = new LocalConnection();
                _oldDispatch.allowDomain("*");
                _oldDispatch.addEventListener(
                    StatusEvent.STATUS, onLocalConnStatus);
            }
            //trace("dispatching on \"_msoy" + _id + "\".");
            try {
                _oldDispatch.send("_msoy" + _id, type, msg);
            } catch (e :Error) {
                // nada
            }

        } else {
            // the new way
            // simply post an event across the security boundary
            _dispatch.dispatchEvent(new TextEvent(type, false, false, msg));
        }
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
            Log.getLog(MsoySprite).debug("Unable to communicate with media " +
                "[event=" + event + "].");
        }
    }

    protected function getContext (url :String) :LoaderContext
    {
        var loadCtx :LoaderContext = (_loadCtx.get(url) as LoaderContext);
        if (loadCtx == null) {
//            trace("Creating new loadctx for " + url);
            loadCtx = new LoaderContext(
                false,
                //new ApplicationDomain(ApplicationDomain.currentDomain),
                ApplicationDomain.currentDomain,
                null
                );
//            _loadCtx.put(url, loadCtx);
        }
        return loadCtx;
    }

    /**
     * Remove our listeners from the LoaderInfo object.
     */
    protected function removeListeners (info :LoaderInfo) :void
    {
        info.removeEventListener(Event.COMPLETE, loadingComplete);
        info.removeEventListener(IOErrorEvent.IO_ERROR, loadError);
        info.removeEventListener(ProgressEvent.PROGRESS, loadProgress);
    }

    /**
     * Callback function.
     */
    protected function loadError (event :IOErrorEvent) :void
    {
        var info :LoaderInfo = (event.target as LoaderInfo);
        removeListeners(info);

        // remove all children
        for (var ii :int = rawChildren.numChildren - 1; ii >= 0; ii--) {
            rawChildren.removeChildAt(ii);
        }

        // create a 'broken media' image and use that instead
        var w :int = _desc.width;
        var h :int = _desc.height;
        if (w == -1) {
            w = 100;
        }
        if (h == -1) {
            h = 100;
        }
        rawChildren.addChild(ImageUtil.createErrorImage(w, h));
    }

    protected function loadProgress (event :ProgressEvent) :void
    {
        updateLoadingProgress(event.bytesLoaded, event.bytesTotal);
        var info :LoaderInfo = (event.target as LoaderInfo);
        try {
            updateContentDimensions(info.width, info.height);
        } catch (err :Error) {
            // an error is thrown trying to access these props before they're
            // ready
        }
    }

    protected function loadVideoProgress (event :ProgressEvent) :void
    {
        var vid :VideoDisplay = (event.currentTarget as VideoDisplay);
        updateContentDimensions(vid.videoWidth, vid.videoHeight);

        updateLoadingProgress(vid.bytesLoaded, vid.bytesTotal);
    }

    protected function loadVideoReady (event :VideoEvent) :void
    {
        var vid :VideoDisplay = (event.currentTarget as VideoDisplay);
        updateContentDimensions(vid.videoWidth, vid.videoHeight);
        updateLoadingProgress(1, 1);

        // TODO: this seems broken, check it
        // set the position of the media to the specified timestamp
        vid.playheadTime = Prefs.getMediaPosition(_desc.id);
        trace("restored playhead time: " + Prefs.getMediaPosition(_desc.id));

        // remove the two listeners
        vid.removeEventListener(ProgressEvent.PROGRESS, loadVideoProgress);
        vid.removeEventListener(VideoEvent.READY, loadVideoReady);
    }

    /**
     * Callback function.
     */
    protected function loadingComplete (event :Event) :void
    {
        var info :LoaderInfo = (event.target as LoaderInfo);
        removeListeners(info);

        updateContentDimensions(info.width, info.height);
        updateLoadingProgress(1, 1);

/*** TODO: decide whether to keep this. I'm not sure we gain anything 
 by trying to remove the Loader.

        // Try accessing the 'content' property and see if that generates
        // a security error. If so, leave it where it is.
        try {
            info.content; // access
        } catch (err :SecurityError) {
            return;
        }

        // remove the loader and add the content directly
        rawChildren.removeChild(info.loader);
        rawChildren.addChild(info.content);

        // transfer the mask, if any
        info.content.mask = info.loader.mask;

END: TODO: maybe keep? **/
    }

    /**
     * Called when the video auto-rewinds.
     */
    protected function videoDidRewind (event :VideoEvent) :void
    {
        (_media as VideoDisplay).play();
    }

    protected function updateContentDimensions (ww :int, hh :int) :void
    {
        // see if the media is scaled
        var xscale :Number = getMediaScaleX();
        var yscale :Number = getMediaScaleY();

        // adjust the content dimensions
        ww = Math.abs(ww * xscale);
        hh = Math.abs(hh * yscale);

        // if scale is negative, the image is flipped and we need to move
        // the origin
        _media.x = (xscale >= 0) ? 0 : ww;
        _media.y = (yscale >= 0) ? 0 : hh;

        // update our saved size, and possibly notify our container
        if (_w != ww || _h != hh) {
            _w = ww;
            _h = hh;
            // we only care about updating the location if we don't already
            // know a hotspot for our media
            if (_desc.hotSpot == null) {
                locationUpdated();
            }
        }
    }

    /**
     * Update the graphics to indicate how much is loaded.
     */
    protected function updateLoadingProgress (
            soFar :Number, total :Number) :void
    {
        var prog :Number = (total == 0) ? 0 : (soFar / total);
        graphics.clear();
        if (prog >= 1) {
            (parent as Container).invalidateDisplayList();
            return; // once we're 100% loaded, we display no progress biz
        }

        var radius :Number = .5 * Math.min(contentWidth, contentHeight);

        graphics.beginFill(0x000000, .5);
        graphics.drawCircle(radius, radius, radius);
        graphics.beginFill(0xFFFFFF, .5);
        graphics.drawCircle(radius, radius, radius * prog);
    }

    public function isInteractive () :Boolean
    {
        return false;
    }

    protected function getHoverColor () :uint
    {
        return 0x40e0e0;
    }

    /**
     * Callback function.
     */
    protected function mouseOver (event :MouseEvent) :void
    {
        if (_glow == null) {
            _glow = new Glow(this);
            _glow.alphaFrom = 0;
            _glow.alphaTo = 1;
            _glow.blurXFrom = 0;
            _glow.blurXTo = 20;
            _glow.blurYFrom = 0;
            _glow.blurYTo = 20;
            _glow.color = getHoverColor();
            _glow.duration = 200;
        }

        _glow.play();
    }

    /**
     * Callback function.
     */
    protected function mouseOut (event :MouseEvent) :void
    {
        if (_glow != null) {
            _glow.end();
            _glow = null;

            // remove the GlowFilter that is added
            // TODO: maybe ensure there are no other filters that
            // need preserving
            filters = new Array();
        }
    }

    /**
     * Callback function.
     */
    protected function mouseClick (event :MouseEvent) :void
    {
        // nada
    }

     /*
    protected function mouseClickCap (event :MouseEvent) :void
    {
        if (!stopClicks) {
            return;
        }
        trace("mouse clicked, will kibosh. target=" + event.target +
            ", phase=" + event.eventPhase +
            ", bubbles=" + event.bubbles);
        event.stopImmediatePropagation();

        var timer :Timer = new Timer(1000, 1);
        timer.addEventListener(TimerEvent.TIMER, function (evt :Event) :void {
            var mousey :MouseEvent = new MouseEvent(
                MouseEvent.CLICK, event.bubbles, event.cancelable,
                event.localX, event.localY, event.relatedObject);
            trace("now dispatching alternate click");
            var oldMouseChildren :Boolean = mouseChildren;
            mouseChildren = true;
            stopClicks = false;
            (event.target as IEventDispatcher).dispatchEvent(mousey);
            //_dispatch.dispatchEvent(mousey);
            mousey = new MouseEvent(
                MouseEvent.CLICK, true, false,
                30, 30, _loader);
            _loader.dispatchEvent(mousey);
            //_loader.dispatchEvent(mousey);
            stopClicks = true;
            mouseChildren = oldMouseChildren;
        });
        timer.start();
    }

    var stopClicks :Boolean = true;
    */

/*
    protected function tick (event :Event) :void
    {
        if (!mouseEnabled) {
            trace("mouse was disabled on media: " + _desc.URL);
            mouseEnabled = true;
        }
        if (mouseChildren) {
            trace("mousechildren enabled on " + _desc.URL);
            // setting this to false makes swfs not capture mouse input
            // so that mouse hover, etc, work.
            mouseChildren = false;
        }
    }
*/

    protected var _id :int;

    protected var _w :int;

    protected var _h :int;

    /** Our Media descripter. */
    protected var _desc :MediaData;

    /** Either a Loader or a VideoDisplay. */
    protected var _media :DisplayObject;

    /** Used to dispatch events down to the swf we contain. */
    protected var _dispatch :EventDispatcher;

    /** The glow effect used for mouse hovering. */
    protected var _glow :Glow;

    /** A single LocalConnection used to communicate with all AVM1 media. */
    protected static var _oldDispatch :LocalConnection;

    protected static var _loadCtx :HashMap = new HashMap();
}

}
