package com.threerings.msoy.ui {

import com.threerings.util.MediaContainer;

public class ScalingMediaContainer extends MediaContainer
{
    public function ScalingMediaContainer (maxWidth :int, maxHeight :int)
    {
        _maxW = maxWidth;
        _maxH = maxHeight;
    }

    override protected function contentDimensionsUpdated () :void
    {
        super.contentDimensionsUpdated();

        scaleX = scaleY = Math.min(1, Math.min(_maxW / _w, _maxH / _h));
    }

    protected var _maxW :int;
    protected var _maxH :int;
}
}
