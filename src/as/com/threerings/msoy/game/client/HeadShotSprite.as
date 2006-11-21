//
// $Id$

package com.threerings.msoy.game.client {

import mx.controls.Label;

import com.threerings.util.MediaContainer;
import com.threerings.util.Name;
import com.threerings.util.Util;

import com.threerings.msoy.item.web.MediaDesc;

public class HeadShotSprite extends MediaContainer
{
    public function HeadShotSprite ()
    {
        super(null);

        _label = new Label();
        _label.includeInLayout = false;
        _label.setStyle("textAlign", "center");
        _label.text = "|";
        addChild(_label);
    }

    override protected function measure () :void
    {
        measuredWidth = _w;
        measuredHeight = _h + _label.textHeight;
    }

    override public function validateDisplayList () :void
    {
        super.validateDisplayList();

        _label.width = _w;
        _label.y = _h;
    }

    public function setUser (name :Name, desc :MediaDesc) :void
    {
        _name = name;
        _label.text = name.toString();
        if (!Util.equals(desc, _desc)) {
            _desc = desc;
            setMedia(desc.getMediaPath());
        }
    }

    override protected function contentDimensionsUpdated () :void
    {
        super.contentDimensionsUpdated();
        invalidateSize();
    }

    protected var _label :Label;

    protected var _name :Name;
    protected var _desc :MediaDesc;
}
}
