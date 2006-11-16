package com.threerings.msoy.ui {

import com.threerings.util.MediaContainer;

public class ScalingMediaContainer extends MediaContainer
{
    public function ScalingMediaContainer (maxWidth :int, maxHeight :int)
    {
        _maxW = maxWidth;
        _maxH = maxHeight;
    }

    override public function get measuredWidth () :Number
    {
        return (_w == 0) ? _maxW : getContentWidth();
    }
    
    override public function get measuredHeight () :Number
    {
        return (_h == 0) ? _maxH : getContentHeight();
    }

    override public function getMediaScaleX () :Number
    {
        return _mediaScale;
    }

    override public function getMediaScaleY () :Number
    {
        return _mediaScale;
    }

    override protected function measure () :void
    {
        // nothing doing
    }

    override protected function contentDimensionsUpdated () :void
    {
        super.contentDimensionsUpdated();

        _mediaScale = Math.min(1, Math.min(_maxW / _w, _maxH / _h));
        _media.scaleX = _mediaScale;
        _media.scaleY = _mediaScale;
        invalidateSize();
    }

    protected var _maxW :int;
    protected var _maxH :int;

    protected var _mediaScale :Number = 1;

}
}
