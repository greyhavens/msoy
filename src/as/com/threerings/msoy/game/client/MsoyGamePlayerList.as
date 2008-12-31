//
// $Id$

package com.threerings.msoy.game.client {

import flash.filters.GlowFilter;

import com.whirled.ui.NameLabelCreator;

import com.whirled.game.client.GamePlayerList;

public class MsoyGamePlayerList extends GamePlayerList
{
    public function MsoyGamePlayerList (labelCreator :NameLabelCreator = null)
    {
        super(labelCreator);
        scrollBarOnLeft = true;
    }

    override public function setLabel (label :String) :void
    {
        super.setLabel(label);

        if (_label != null) {
            _label.filters = [ new GlowFilter(0, 1, 2, 2, 255) ];
            _label.setStyle("color", 0xFFFFFF);
            _label.setStyle("fontSize", 12);
            _label.setStyle("fontWeight", "bold");
            _label.setStyle("fontFamily", "Ariel");
        }
    }

    override protected function getRenderingClass () :Class
    {
        return MsoyGamePlayerRenderer;
    }
}
}

import flash.filters.GlowFilter;

import com.whirled.game.client.GamePlayerRenderer;

class MsoyGamePlayerRenderer extends GamePlayerRenderer
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

