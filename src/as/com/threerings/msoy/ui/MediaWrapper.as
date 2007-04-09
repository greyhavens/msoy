package com.threerings.msoy.ui {

import com.threerings.flash.MediaContainer;

import mx.core.Container;

/**
 * Wraps a MediaContainer into a UIComponent.
 */
public class MediaWrapper extends Container
{
    /**
     * @param cont the container to wrap
     * @param altReportedWidth a width to report when the media width is 0.
     * @param altReportedHeight a height to report when the media height is 0.
     */
    public function MediaWrapper (
        cont :MediaContainer,
        altReportedWidth :Number = 0, altReportedHeight :Number = 0)
    {
        _cont = cont;
        _altWidth = altReportedWidth;
        _altHeight = altReportedHeight;
        rawChildren.addChild(cont);
        cont.addEventListener(MediaContainer.SIZE_KNOWN, handleMediaSizeChanged,
            false, 0, true);
    }

    public function getMediaContainer () :MediaContainer
    {
        return _cont;
    }

    override protected function measure () :void
    {
        // nothing needed
    }

    override protected function updateDisplayList (unscaledWidth: Number, 
        unscaledHeight :Number) :void
    {
        _cont.containerDimensionsUpdated(unscaledWidth, unscaledHeight);
    }

    override public function get measuredWidth () :Number
    {
        _cont.scaleX = getMaxDimensionScale();
        return (_cont.getContentWidth() == 0) ? _altWidth : _cont.width;
    }

    override public function get measuredHeight () :Number
    {
        _cont.scaleY = getMaxDimensionScale();
        return (_cont.getContentHeight() == 0) ? _altHeight : _cont.height;
    }

    protected function getMaxDimensionScale () :Number
    {
        var w :Number = _cont.getContentWidth();
        var h :Number = _cont.getContentHeight();
        var widthScale :Number = 1;
        var heightScale :Number = 1;
        if (w > maxWidth) {
            widthScale = maxWidth / w;
        }
        if (h > maxHeight) {
            heightScale = maxHeight / w;
        }
        // returns 1 is max dimensions haven't been messed with
        return widthScale < heightScale ? widthScale : heightScale;
    }

    /**
     * React to changes in the size of the MediaContainer.
     */
    protected function handleMediaSizeChanged (evt :Object) :void
    {
        invalidateSize();
    }

    protected var _cont :MediaContainer;

    protected var _altWidth :Number;
    protected var _altHeight :Number;
}
}
