package com.threerings.msoy.applets {

import flash.display.Loader;
import flash.display.LoaderInfo;
import flash.display.Sprite;
import flash.display.StageAlign;
import flash.display.StageScaleMode;
import flash.events.ErrorEvent;
import flash.events.Event;
import flash.events.IOErrorEvent;
import flash.events.SecurityErrorEvent;
import flash.external.ExternalInterface;
import flash.net.LocalConnection;
import flash.net.URLLoader;
import flash.net.URLRequest;
import flash.system.ApplicationDomain;
import flash.system.LoaderContext;
import flash.system.Security;
import flash.text.TextField;
import flash.text.TextFormat;

/**
 * Extends EmbedStub with MochiAds.
 */
[SWF(width="700", height="575")]
public class EmbedStubMochiAd extends EmbedStub
{
    /** This gets replaced. */
    public static const MOCHI_AD_ID :String = "&&mochiadid&&";

    public function EmbedStubMochiAd ()
    {
        var dynosprite :DynoSprite = new DynoSprite();
        addChild(dynosprite);

        MochiAd.showPreGameAd({
            clip: dynosprite, id: MOCHI_AD_ID, res: WIDTH + "x" + HEIGHT,
            no_progress_bar: true, ad_started: function () :void {},
            ad_finished: function () :void {
                removeChild(dynosprite);
                init();
            } });
    }
}
}

import flash.display.Sprite;

/** Mochi ads must display on a dynamic object, pfffft. */
dynamic class DynoSprite extends Sprite
{
}
