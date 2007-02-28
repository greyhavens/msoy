//
// $Id$

package com.threerings.msoy.game.client {

import flash.text.TextField;
import flash.text.TextFieldAutoSize;
import flash.text.TextFormat;

import com.threerings.util.Name;
import com.threerings.util.Util;

import com.threerings.flash.MediaContainer;

import com.threerings.msoy.item.web.MediaDesc;

public class HeadShotSprite extends MediaContainer
{
    public function HeadShotSprite ()
    {
        super(null);

        var labelFormat :TextFormat = new TextFormat();
        labelFormat.bold = true;
        _label = new TextField();
        _label.autoSize = TextFieldAutoSize.CENTER;
        _label.defaultTextFormat = labelFormat;
        addChild(_label);
    }

    public function setUser (name :Name, desc :MediaDesc) :void
    {
        _name = name;
        _label.text = name.toString();
        _label.width = _label.textWidth;
        if (!Util.equals(desc, _desc)) {
            _desc = desc;
            setMedia(desc.getMediaPath());
        }
    }

    override public function getContentHeight () :int
    {
        return super.getContentHeight() + _label.textHeight;
    }

    override protected function contentDimensionsUpdated () :void
    {
        super.contentDimensionsUpdated();
        recheckLabel();
    }

    protected function recheckLabel () :void
    {
        _label.x = (_w - _label.textWidth) / 2;
        _label.y = _h;
    }

    protected var _label :TextField;

    protected var _name :Name;
    protected var _desc :MediaDesc;
}
}
