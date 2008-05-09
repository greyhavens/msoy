// 
// $Id$

package com.threerings.msoy.game.client {

import flash.filters.GlowFilter;

import com.whirled.ui.NameLabelCreator;

import com.whirled.game.client.GamePlayerList;

import com.threerings.msoy.game.client.MsoyGamePlayerRenderer;

public class MsoyGamePlayerList extends GamePlayerList
{
    public function MsoyGamePlayerList (labelCreator :NameLabelCreator = null)
    {
        super(labelCreator);
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
