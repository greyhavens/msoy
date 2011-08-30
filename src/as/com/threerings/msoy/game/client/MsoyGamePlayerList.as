//
// $Id$

package com.threerings.msoy.game.client {

import flash.filters.GlowFilter;

import com.whirled.game.client.GamePlayerList;
import com.whirled.game.client.GamePlayerRecord;
import com.whirled.ui.NameLabelCreator;

import mx.controls.Label;

public class MsoyGamePlayerList extends GamePlayerList
{
    public static function setLabelStyle (label :Label) :void
    {
        // NOTE: the appearance of this label should match the text field in MsoyNameLabel
        label.filters = [ new GlowFilter(0x000000, 1, 5, 5, 12) ];
        label.setStyle("color", 0xFFFFFF);
        label.setStyle("fontSize", 12);
        label.setStyle("fontFamily", "_sans");
        label.setStyle("letterSpacing", 0.6);
    }

    public function MsoyGamePlayerList (labelCreator :NameLabelCreator = null)
    {
        super(labelCreator);
        scrollBarOnLeft = true;
    }

    override public function setLabel (label :String) :void
    {
        super.setLabel(label);

        if (_label != null) {
            setLabelStyle(_label);
        }
    }

    override protected function getRenderingClass () :Class
    {
        return MsoyGamePlayerRenderer;
    }

    override protected function createNewRecord () :GamePlayerRecord
    {
        return new MsoyGamePlayerRecord();
    }
}
}

import com.whirled.game.client.GamePlayerRenderer;

import com.threerings.msoy.game.client.MsoyGamePlayerList;

class MsoyGamePlayerRenderer extends GamePlayerRenderer
{
    override protected function createChildren () :void
    {
        super.createChildren();

        MsoyGamePlayerList.setLabelStyle(_scoreLabel);
    }
}

