//
// $Id$

package com.threerings.msoy.client {

import flash.display.Sprite;
import flash.display.MovieClip;

import flash.events.ProgressEvent;

import mx.preloaders.DownloadProgressBar

public class Preloader extends DownloadProgressBar
{
    override public function set preloader (value :Sprite) :void
    {
        super.preloader = value;

        var mc :MovieClip = value.root as MovieClip;
        var working :Boolean = (mc.framesLoaded < mc.totalFrames);
        trace("----> Preloader " + (working ? "DID" : "did NOT") + " work.");
    }

    override protected function showDisplayForDownloading (elapsed :int, event :ProgressEvent) :Boolean
    {
        return true; // show the fucker!
    }

    override protected function showDisplayForInit (elapsed :int, count :int) :Boolean
    {
        return true; // show the fucker!
    }
}
}
