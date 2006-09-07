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

        var largerRatio :Number = Math.max(_w / _maxW, _h / _maxH);
        if (largerRatio > 1) {
            scaleX = scaleY = 1 / largerRatio;
        }
    }

    protected var _maxW :int;
    protected var _maxH :int;
}
}
