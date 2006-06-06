package com.threerings.msoy.ui {

import flash.display.DisplayObject;
import flash.display.DisplayObjectContainer;
import flash.display.Loader;
import flash.display.LoaderInfo;
import flash.display.Shape;

import flash.events.Event;
import flash.events.EventDispatcher;
import flash.events.IEventDispatcher;
import flash.events.IOErrorEvent;
import flash.events.MouseEvent;
import flash.events.ProgressEvent;
import flash.events.SecurityErrorEvent;
import flash.events.StatusEvent;
import flash.events.TextEvent;

import flash.media.Video;

import flash.net.LocalConnection;
import flash.net.NetConnection;
import flash.net.NetStream;

import flash.system.ApplicationDomain;
import flash.system.LoaderContext;
import flash.system.SecurityDomain;

import flash.net.URLRequest;

import flash.utils.Timer;

import mx.containers.Box;

import mx.effects.Glow;

import mx.events.EffectEvent;

import com.threerings.media.image.ImageUtil;

import com.threerings.msoy.client.RoomView;
import com.threerings.msoy.data.MediaData;
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
public class ScreenMedia extends Box
{
    /** The current logical coordinate of this media. */
    public const loc :MsoyLocation = new MsoyLocation();

    /**
     * Constructor.
     */
    public function ScreenMedia (desc :MediaData)
    {
        _desc = desc;
        _id = int(Math.random() * int.MAX_VALUE);

        var url :String = desc.URL;
        if (desc.isAVM1) {
            // TODO
            url += "?oid=" + _id;
        }

/** Experimental
        if (url.toLowerCase().lastIndexOf(".flv") ==
                url.length - ".flv".length) {
            var nc :NetConnection = new NetConnection();
            nc.connect(url);

            var stream :NetStream = new NetStream(nc);

            var video :Video = new Video();
            video.attachNetStream(stream);

            rawChildren.addChild(video);
            stream.play();

            return;
        }
**/

        // create our loader and set up some event listeners
        var loader :Loader = new Loader();
        loader.contentLoaderInfo.addEventListener(
            Event.COMPLETE, loadingComplete);
        loader.contentLoaderInfo.addEventListener(
            IOErrorEvent.IO_ERROR, loadError);
        loader.contentLoaderInfo.addEventListener(
            ProgressEvent.PROGRESS, loadProgress);

        // grab hold of the EventDispatcher we'll use for comm
        _dispatch = loader.contentLoaderInfo.sharedEvents;

        // if we know the size of the media, create a mask to prevent
        // it from drawing outside those bounds
//        if (desc.width != -1 && desc.height != -1) {
            var mask :Shape = new Shape();
            mask.graphics.beginFill(0xFFFFFF);
            mask.graphics.drawRect(0, 0, maxContentWidth, maxContentHeight);
            mask.graphics.endFill();
            // the mask must be added to the display list (which is wacky)
            rawChildren.addChild(mask);
            loader.mask = mask;
//        }

        // start it loading, add it as a child
        loader.load(new URLRequest(url), getContext(url));
        rawChildren.addChild(loader);

        if (isInteractive()) {
            addEventListener(MouseEvent.MOUSE_OVER, mouseOver);
            addEventListener(MouseEvent.MOUSE_OUT, mouseOut);
            addEventListener(MouseEvent.CLICK, mouseClick);

/*
            loader.addEventListener(MouseEvent.CLICK, mouseClickCap, true);
            loader.addEventListener(MouseEvent.CLICK, mouseClickCap);
            addEventListener(MouseEvent.CLICK, mouseClickCap);
            */
        }
    }

    override public function hitTestPoint (
            x :Number, y :Number, shapeFlag :Boolean = false) :Boolean
    {
        trace("hitTest(" + x + ", " + y + ")");
        return super.hitTestPoint(x, y, shapeFlag);
    }

    public function get contentWidth () :int
    {
        return Math.min(_w, maxContentWidth);
    }

    public function get contentHeight () :int
    {
        return Math.min(_h, maxContentHeight);
    }

    public function get maxContentWidth () :int
    {
        return 800;
    }

    public function get maxContentHeight () :int
    {
        return 600;
    }

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
        mouseEnabled = active;
        mouseChildren = active;
    }

/* Commented out: this doesn't fucking work, this method should be called
 * parentWillChange. For now, just require folks to add the component first
 * and then set hte location.

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

/*
    // documentation inherited
    override public function set x (newValue :Number) :void
    {
        // TODO: test
        trace("set x to " + newValue);
        super.x = newValue - _desc.originX;
        trace("super.x is now " + super.x + " and x is just " + x);
    }

    // documentation inherited
    override public function get x () :Number
    {
        // TODO: test
        return (super.x + _desc.originX);
    }

    // documentation inherited
    override public function set y (newValue :Number) :void
    {
        // TODO: test
        super.y = newValue - _desc.originY;
    }

    // documentation inherited
    override public function get y () :Number
    {
        // TODO: test
        return (super.y + _desc.originY);
    }
*/

    /**
     * Accessor: media property.
     */
    // Probably this should be removed.
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
            Log.getLog(ScreenMedia).debug("Unable to communicate with media " +
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
        var info :LoaderInfo = (event.target as LoaderInfo);
        try {
            _w = info.width;
            _h = info.height;
            locationUpdated();
        } catch (err :Error) {
            // an error is thrown trying to access these props before they're
            // ready
        }
    }

    /**
     * Callback function.
     */
    protected function loadingComplete (event :Event) :void
    {
        var info :LoaderInfo = (event.target as LoaderInfo);
        removeListeners(info);

        _w = info.width;
        _h = info.height;
        locationUpdated();

        // Try accessing the 'content' property and see if that generates
        // a security error. If so, leave it where it is.
        try {
            info.content; // access
        } catch (err :SecurityError) {
            return;
        }

        // remove all our "raw" children, which will remove the loader
        for (var ii :int = rawChildren.numChildren - 1; ii >= 0; ii--) {
            rawChildren.removeChildAt(ii);
        }

        // now add the content as our child, letting the Loader get gc'd.
        rawChildren.addChild(info.content);

        // transfer the mask, if any
        info.content.mask = info.loader.mask;
        if (info.content.mask != null) {
            rawChildren.addChild(info.content.mask);
        }
    }

    protected function isInteractive () :Boolean
    {
        return _desc.isInteractive();
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

    /** Used to dispatch events down to the swf we contain. */
    protected var _dispatch :EventDispatcher;

    /** The glow effect used for mouse hovering. */
    protected var _glow :Glow;

    /** A single LocalConnection used to communicate with all AVM1 media. */
    protected static var _oldDispatch :LocalConnection;

    protected static var _loadCtx :HashMap = new HashMap();
}

}
