//
// $Id$

package com.threerings.msoy.item.client {

import flash.events.ErrorEvent;
import flash.events.Event;
import flash.events.IOErrorEvent;
import flash.events.SecurityErrorEvent;

import flash.net.URLLoader;
import flash.net.URLLoaderDataFormat;
import flash.net.URLRequest;

import com.threerings.util.Log;

public class ExternalMediaUtil
{
    public static function fetch (
        url :String, display :ExternalMediaDisplayer, errorListener :Function = null) :void
    {
        var loader :URLLoader = new URLLoader();
        loader.dataFormat = URLLoaderDataFormat.VARIABLES;
        loader.addEventListener(Event.COMPLETE, function (event :Event) :void {
            display.displayExternal(loader.data as Object);
        });

        var errHandler :Function = function (event :ErrorEvent) :void {
            Log.getLog(ExternalMediaUtil).warning("Error loading media", "msg", event.text);
            if (errorListener != null) {
                errorListener(event);
            }
        };
        loader.addEventListener(SecurityErrorEvent.SECURITY_ERROR, errHandler);
        loader.addEventListener(IOErrorEvent.IO_ERROR, errHandler);
        loader.load(new URLRequest(url));
    }
}
}
