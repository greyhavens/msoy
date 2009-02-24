//
// $Id$

package com.threerings.msoy.applets {

import flash.display.BitmapData;
import flash.display.DisplayObject;
import flash.display.Loader;
import flash.display.LoaderInfo;
import flash.display.Sprite;

import flash.events.Event;
import flash.events.EventPhase;
import flash.events.IEventDispatcher;

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

//        var sharedChild :IEventDispatcher = loader.contentLoaderInfo.sharedEvents;
//        var sharedParent :IEventDispatcher = this.root.loaderInfo.sharedEvents;
//
//        var handler :Function = function (event :Event) :void {
//            trace("Got event: " + event.type + ", phase: " + event.eventPhase +
//                ", fromChild: " + (event.currentTarget == sharedChild));
//            if (event.eventPhase == EventPhase.BUBBLING_PHASE) {
//                sharedChild.dispatchEvent(event);
//            } else {
//                sharedParent.dispatchEvent(event);
//            }
//        };
//
//        // set up the pass-throughs
//        var event :String;
//        for each (event in PASS_UPS) {
//            sharedChild.addEventListener(event, handler);
//            sharedParent.addEventListener(event, handler);
//        }
//        for each (event in PASS_DOWNS) {
//        }

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

//    protected static const PASS_UPS :Array = [
//        "controlConnect", // for our *Control instances
//
//        // the following are all for the flex 3.2 feature of loading flex into security
//        "bridgeApplicationActivate",
//        "bridgeApplicationUnloading",
//        "bridgeFocusManagerActivate",
//        "bridgeFocusManagerActivate",
//        "bridgeWindowActivate",
//        "brdigeWindowDeactivate",
//
//        "activatePopUpRequest",
//        "canActivateRequestPopUpRequest",
//        "deactivatePopUpRequest",
//        "getVisibleRectRequest",
//        "isBridgeChildRequest",
//        "invalidateRequest",
//        "hideMouseCursorRequest",
//        "showMouseCursorRequest",
//        "resetMouseCursorRequest",
//        "activateFocusRequest",
//        "deactivateFocusRequest",
//        "moveFocusRequest",
//        "createModalWindowRequest",
//        "showModalWindowRequest",
//        "hideModalWindowRequest",
//        "addPopUpRequest",
//        "removePopUpRequest",
//        "addPopUpPlaceHolderRequest",
//        "removePopUpPlaceHolderRequest",
//        "getSizeRequest",
//        "setActualSizeRequest",
//        "setShowFocusIndicatorRequest"
//    ];

//        "bridgeApplicationActivate",
//        "bridgeFocusManagerActivate",
//        "bridgeNewApplication",
//        "bridgeWindowActivate",
//        "brdigeWindowDeactivate",
//        "activatePopUpRequest",
//        "deactivatePopUpRequest",
//        "getVisibleRectRequest",
//        "invalidateRequest",
//        "hideMouseCursorRequest",
//        "showMouseCursorRequest",
//        "resetMouseCursorRequest",
//        // TODO: more? See SWFBridgeRequest.as and SWFBridgeEvent.as
////    ];
////
////    protected static const PASS_DOWNS :Array = [
//        // the following are all for the flex 3.2 feature of loading flex into security
//        "bridgeApplicationUnloading",
//        "bridgeFocusManagerActivate",
//        "canActivateRequestPopUpRequest",
//        "isBridgeChildRequest",
//        "activateFocusRequest",
//        "activateFocusRequest"
//        // TODO: more? See SWFBridgeRequest.as and SWFBridgeEvent.as
//    ];

}
}
