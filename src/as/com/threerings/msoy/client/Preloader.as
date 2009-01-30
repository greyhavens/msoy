//
// $Id$

package com.threerings.msoy.client {

import flash.display.Sprite;
import flash.display.MovieClip;

import flash.events.Event;
import flash.events.ProgressEvent;

import flash.net.URLRequest;
import flash.net.URLVariables;
import flash.system.Capabilities;

// NOTE: minimize dependancies outside of flash.*, since this is our preloader...

import com.threerings.util.NetUtil;

import com.threerings.msoy.ui.LoadingSpinner;

import mx.events.FlexEvent;

import mx.preloaders.IPreloaderDisplay

/**
 * Displays a spinny animation during loading, but also validates the required
 * flash version for embedded or stub clients.
 */
public class Preloader extends Sprite
    implements IPreloaderDisplay
{
    /** The minimum flash player version required by whirled. */
    public static const MIN_FLASH_VERSION :Array = [ 9, 0, 115, 0 ];
    //public static const MIN_FLASH_VERSION :Array = [ 10, 0, 12, 36 ];

    /**
     */
    public function Preloader ()
    {
        _spinner = new LoadingSpinner();
        _spinner.setProgress(0, 1);
        addChild(_spinner);
    }

    // from IPreloaderDisplay and stupidly so
    public function set backgroundAlpha (value :Number) :void
    {
        _bgAlpha = value;
    }

    // from IPreloaderDisplay and stupidly so
    public function get backgroundAlpha () :Number
    {
        return _bgAlpha;
    }

    // from IPreloaderDisplay and stupidly so
    public function set backgroundColor (value :uint) :void
    {
        _bgColor = value;
    }

    // from IPreloaderDisplay and stupidly so
    public function get backgroundColor () :uint
    {
        return _bgColor;
    }

    // from IPreloaderDisplay and stupidly so
    public function set backgroundImage (value :Object) :void
    {
        _bgImage = value;
    }

    // from IPreloaderDisplay and stupidly so
    public function get backgroundImage () :Object
    {
        return _bgImage;
    }

    // from IPreloaderDisplay and stupidly so
    public function set backgroundSize (value :String) :void
    {
        _bgSize = value;
    }

    // from IPreloaderDisplay and stupidly so
    public function get backgroundSize () :String
    {
        return _bgSize;
    }

    // from IPreloaderDisplay and stupidly so
    public function set stageHeight (value :Number) :void
    {
        _stageH = value;
    }

    // from IPreloaderDisplay and stupidly so
    public function get stageHeight () :Number
    {
        return _stageH;
    }

    // from IPreloaderDisplay and stupidly so
    public function set stageWidth (value :Number) :void
    {
        _stageW = value;
    }

    // from IPreloaderDisplay and stupidly so
    public function get stageWidth () :Number
    {
        return _stageW;
    }

    // from IPreloaderDisplay
    public function set preloader (value :Sprite) :void
    {
        // TODO: remove the following debugging
        var mc :MovieClip = value.root as MovieClip;
        var working :Boolean = (mc.framesLoaded < mc.totalFrames);
        trace("----> Preloader " + (working ? "DID" : "did NOT") + " work.");
        // END: TODO

        // two error cases: we are in a stub that is forcing the version down
        // or we aren't but the user doesn't have the required version.
        if (checkInOldStub() || checkAutoUpgrade()) {
            return;
        }

        value.addEventListener(ProgressEvent.PROGRESS, handleProgress);
        value.addEventListener(FlexEvent.INIT_COMPLETE, handleComplete);
    }

    // from IPreloaderDisplay
    public function initialize () :void
    {
        _spinner.x = (_stageW - LoadingSpinner.WIDTH) / 2;
        _spinner.y = (_stageH - LoadingSpinner.HEIGHT) / 2;
    }

    /**
     * Check to see if the flash version is being forced downwards by being compiled
     * in an old stub.
     *
     * @return true if we are and it is being handled.
     */
    protected function checkInOldStub () :Boolean
    {
        // TODO: detect when we're in a stub,
        // check to see if it's forcing us to an old player version (???)
        // show a message, redirect to whirled.com

        return false;
    }

    /**
     * Check to see if the we should try auto-upgrading the flash player.
     *
     * @return true if so.
     */
    protected function checkAutoUpgrade () :Boolean
    {
        if (checkFlashVersion()) {
            return false;
        }

        var url :URLRequest = new URLRequest(
            DeploymentConfig.serverURL + "expressinstall/playerProductInstall.swf");
        var vars :URLVariables = new URLVariables();
        vars["MMredirectURL"] = DeploymentConfig.serverURL;
        vars["MMplayerType"] = "PlugIn";
        vars["MMdoctitle"] = "Upgrade";
        url.data = vars;
        if (NetUtil.navigateToURL(url)) {
            return true;
        }

        // TODO
        trace("Shit: could not autoupgrade?");
        return true;
    }

    /**
     * Check the flash version.
     *
     * @return true if we have an adequate version.
     */
    protected function checkFlashVersion () :Boolean
    {
        // the version looks like "LNX 9,0,31,0"
        var bits :Array = Capabilities.version.split(" ");
        bits = (bits[1] as String).split(",");
        while (bits.length < MIN_FLASH_VERSION.length) {
            bits.push(0);
        }

        // now check each portion of the version number
        for (var ii :int = 0; ii < bits.length; ii++) {
            var required :int = int(MIN_FLASH_VERSION[ii]);
            var actual :int = int(bits[ii]);
            if (actual < required) {
                return false;

            } else if (actual > required) {
                break; // no need to check lesser version numbers
            }
            // else: if two are the same, move to the next minor version number.
        }
        return true;
    }

    protected function handleProgress (event :ProgressEvent) :void
    {
        _spinner.setProgress(event.bytesLoaded, event.bytesTotal);
    }

    protected function handleComplete (event :Event) :void
    {
        // signal flex to start up our app
        dispatchEvent(new Event(Event.COMPLETE));
    }

    protected var _spinner :LoadingSpinner;

    protected var _bgAlpha :Number;
    protected var _bgColor :uint;
    protected var _bgImage :Object;
    protected var _bgSize :String;
    protected var _stageH :Number;
    protected var _stageW :Number;
}
}
