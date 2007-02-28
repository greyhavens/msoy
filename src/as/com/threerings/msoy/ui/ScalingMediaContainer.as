package com.threerings.msoy.ui {

import com.threerings.flash.MediaContainer;

public class ScalingMediaContainer extends MediaContainer
{
    public var maxW :int;
    public var maxH :int;

    public function ScalingMediaContainer (maxWidth :int, maxHeight :int)
    {
        maxW = maxWidth;
        maxH = maxHeight;
    }

    override public function getMediaScaleX () :Number
    {
        return _mediaScale;
    }

    override public function getMediaScaleY () :Number
    {
        return _mediaScale;
    }

    override protected function contentDimensionsUpdated () :void
    {
        super.contentDimensionsUpdated();

        _mediaScale = Math.min(1, Math.min(maxW / _w, maxH / _h));
        _media.scaleX = _mediaScale;
        _media.scaleY = _mediaScale;
    }

    protected var _mediaScale :Number = 1;
}
}
