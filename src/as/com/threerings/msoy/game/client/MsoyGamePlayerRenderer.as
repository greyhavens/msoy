//
// $Id$

package com.threerings.msoy.game.client {

import flash.filters.GlowFilter;

import com.whirled.game.client.GamePlayerRenderer;

public class MsoyGamePlayerRenderer extends GamePlayerRenderer
{
    override protected function createChildren () :void
    {
        super.createChildren();

        _scoreLabel.filters = [ new GlowFilter(0, 1, 2, 2, 255) ];
        _scoreLabel.setStyle("color", 0xFFFFFF);
        _scoreLabel.setStyle("fontSize", 12);
        _scoreLabel.setStyle("fontWeight", "bold");
        _scoreLabel.setStyle("fontFamily", "Ariel");
    }
}
}
