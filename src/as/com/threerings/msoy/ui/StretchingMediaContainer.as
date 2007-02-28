package com.threerings.msoy.ui {

import com.threerings.flash.MediaContainer;

/**
 * A MediaContainer that will stretch its underlying image to take up the full area allotted to it
 * by the surrounding MediaWrapper.
 */
public class StretchingMediaContainer extends MediaContainer
{
    public function StretchingMediaContainer (url :String = null)
    {
        super(url);
    }

    override public function getMediaScaleX () :Number
    {
        return _mediaScaleX;
    }

    override public function getMediaScaleY () :Number
    {
        return _mediaScaleY;
    }

    override public function containerDimensionsUpdated (newWidth :Number, newHeight :Number) :void
    {
        _mediaScaleX = _w == 0 ? 1 : newWidth / _w;
        scaleX = _mediaScaleX;
        _mediaScaleY = _h == 0 ? 1 : newHeight / _h;
        scaleY = _mediaScaleY;
    }

    protected var _mediaScaleX :Number = 1;
    protected var _mediaScaleY :Number = 1;
}
}
