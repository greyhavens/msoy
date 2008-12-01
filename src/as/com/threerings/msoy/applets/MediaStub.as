package com.threerings.msoy.applets {

import flash.display.BitmapData;
import flash.display.DisplayObject;
import flash.display.Loader;
import flash.display.LoaderInfo;
import flash.display.Sprite;

import flash.events.Event;

import flash.geom.Matrix;
import flash.geom.Rectangle;

import flash.net.URLRequest;

import flash.system.ApplicationDomain;
import flash.system.LoaderContext;
import flash.system.Security;

import flash.utils.ByteArray;

import com.threerings.flash.LoaderUtil;

/**
 * The MediaStub is built and placed on the media server. It is then loaded
 * up by MsoyMediaContainers that need to display a non-image, and the
 * stub is then notified of the url or bytes through which to load the
 * actual media.
 */
[SWF(width="0", height="0")]
public class MediaStub extends Sprite
{
    public function MediaStub ()
    {
        // Allow the swf that loaded us to cross-script us.
        Security.allowDomain(this.root.loaderInfo.loaderURL);
        this.root.loaderInfo.addEventListener(Event.UNLOAD, handleUnload);
    }

    /**
     * Load media from the specified URL.
     */
    public function load (url :String) :LoaderInfo
    {
        var loader :Loader = initLoader();
        loader.load(new URLRequest(url));
            //, new LoaderContext(false, ApplicationDomain.currentDomain));
        return loader.contentLoaderInfo;
    }

    /**
     * Load media from the provided bytes.
     */
    public function loadBytes (bytes :ByteArray) :LoaderInfo
    {
        var loader :Loader = initLoader();
        loader.loadBytes(bytes);
        return loader.contentLoaderInfo;
    }

    /**
     * Take a snapshot of the media we've loaded.
     */
    public function snapshot (bitmapData :BitmapData, matrix :Matrix, clip :Rectangle = null) :void
    {
        bitmapData.draw(this, matrix, null, null, clip, true);
    }

    protected function initLoader () :Loader
    {
        var loader :Loader = new Loader();
        addChild(loader);

        // set up the pass-through
        loader.contentLoaderInfo.sharedEvents.addEventListener("controlConnect",
            this.root.loaderInfo.sharedEvents.dispatchEvent);

        return loader;
    }

    /**
     * Clean up as best we can.
     */
    protected function handleUnload (event :Event) :void
    {
        for (var ii :int = numChildren - 1; ii >= 0; ii--) {
            var disp :DisplayObject = getChildAt(ii);
            if (disp is Loader) {
                LoaderUtil.unload(Loader(disp));
            }
        }
    }
}
}
