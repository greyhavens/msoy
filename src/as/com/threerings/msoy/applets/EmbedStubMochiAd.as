//
// $Id$

package com.threerings.msoy.applets {

import flash.display.Sprite;

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
                init();
                removeChild(dynosprite);
            } });
    }

    override protected function init () :void
    {
        // block init until after we show the ad
        if (this.numChildren > 0) {
            super.init();
        }
    }
}
}

import flash.display.Sprite;

/** Mochi ads must display on a dynamic object, pfffft. */
dynamic class DynoSprite extends Sprite
{
}
