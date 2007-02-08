package com.threerings.msoy.item.remix.tools {

import flash.display.DisplayObject;
import flash.display.Loader;
import flash.display.LoaderInfo;
import flash.display.Sprite;

import flash.events.Event;
import flash.events.IOErrorEvent;

import flash.net.URLRequest;

import flash.system.ApplicationDomain;
import flash.system.LoaderContext;

import flash.utils.describeType; // function import
import flash.utils.getQualifiedClassName; // function import

import com.threerings.msoy.item.remix.RemixCodes;

[SWF]
public class RemixAnalyzer extends Sprite
{
    public function RemixAnalyzer ()
    {
        trace("ok then");

        var url :String = (this.root.loaderInfo.parameters["swf"] as String);
        if (url == null) {
            output("Error: No 'swf' parameter specified.");
            exit();
        }

        // set up
        _loader = new Loader();
        var info :LoaderInfo = _loader.contentLoaderInfo;
        info.addEventListener(Event.COMPLETE, handleLoadingComplete);
        info.addEventListener(IOErrorEvent.IO_ERROR, handleLoadingError);

        // load it
        _appDom = new ApplicationDomain(ApplicationDomain.currentDomain);
        _loader.load(new URLRequest(url), new LoaderContext(false, _appDom));
    }

    protected function handleLoadingComplete (event :Event) :void
    {
        var c :Class;
        if (_appDom.hasDefinition(RemixCodes.ASSET_CLASS)) {
            c = (_appDom.getDefinition(RemixCodes.ASSET_CLASS) as Class);
        }
        if (c == null) {
            output("Not remixable.");
            exit();
        }

        try {
            var o :Object = new c();
            // describe the properties of the asset class
            output(String(describeType(o)));

            // output a few choice bits of info about the "logic" class.
            var d :DisplayObject = _loader.content;
            output("Size: " + _loader.contentLoaderInfo.width + " " +
                _loader.contentLoaderInfo.height);
            output("Class: " + getQualifiedClassName(d));
            output("Remixable!");

        } catch (e :Error) {
            output("Error: Analyzing: " + e);
        }

        exit();
    }

    protected function handleLoadingError (event :IOErrorEvent) :void
    {
        output("Error: Loading: " + event.text);
        exit();
    }

    /**
     * Write to our output.
     */
    protected static function output (str :String) :void
    {
        trace(str); // TODO, for all that is good and pure
    }

    /**
     * Exit.
     */
    protected static function exit () :void
    {
        output(".EXIT.");
        throw new Error();
    }

    protected var _loader :Loader;

    protected var _appDom :ApplicationDomain;
}
}
