package com.threerings.msoy.ui {

import flash.display.DisplayObject;
import flash.display.Loader;
import flash.display.LoaderInfo;
import flash.display.Shape;

import flash.events.Event;
import flash.events.MouseEvent;

import flash.system.ApplicationDomain;
import flash.system.LoaderContext;
import flash.system.SecurityDomain;

import flash.net.URLRequest;

import flash.util.trace;

import mx.containers.Box;

import mx.effects.Glow;

import mx.events.EffectEvent;

import com.threerings.msoy.data.MediaData;

/**
 * A wrapper class for all media that will be placed on the screen.
 * Subject to change.
 */
public class ScreenMedia extends Box
{
    /**
     * Constructor.
     */
    public function ScreenMedia (desc :MediaData)
    {
        _desc = desc;
        var loader :Loader = new Loader();

        // if we know the size of the media, create a mask to prevent
        // it from drawing outside those bounds
        if (desc.width != -1 && desc.height != -1) {
            var mask :Shape = new Shape();
            mask.graphics.beginFill(0xFFFFFF);
            mask.graphics.drawRect(0, 0, desc.width, desc.height);
            mask.graphics.endFill();
            // the mask must be added to the display list (which is wacky)
            rawChildren.addChild(mask);
            loader.mask = mask;
        }

        var loadCtx :LoaderContext = new LoaderContext(
                false,
                ApplicationDomain.currentDomain,
                SecurityDomain.currentDomain);
        loader.load(new URLRequest(desc.URL), loadCtx);
        loader.loadeeInfo.addEventListener(Event.COMPLETE, loadingComplete);

        rawChildren.addChild(loader);

        if (desc.isInteractive()) {
            addEventListener(MouseEvent.MOUSE_OVER, mouseOver);
            addEventListener(MouseEvent.MOUSE_OUT, mouseOut);
        }

        // I don't know if these lines are necessary: remove?
        if (desc.width != -1 && desc.height != -1) {
            width = desc.width;
            height = desc.height;
        }
    }

    /**
     * Accessor: media property.
     */
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
     * Callback function.
     */
    protected function loadingComplete (event :Event) :void
    {
        var info :LoaderInfo = (event.target as LoaderInfo);

        // stop listening (good practice)
        info.removeEventListener(Event.COMPLETE, loadingComplete);

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
            _glow.color = 0x40e0e0;
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

    /** Our Media descripter. */
    protected var _desc :MediaData;

    /** The glow effect used for mouse hovering. */
    protected var _glow :Glow;
}

}
