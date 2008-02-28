//
// $Id$

package com.threerings.msoy.client {

import flash.events.ProgressEvent;

import mx.preloaders.DownloadProgressBar

public class Preloader extends DownloadProgressBar
{
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
