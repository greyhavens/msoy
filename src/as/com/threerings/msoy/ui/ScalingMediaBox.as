package com.threerings.msoy.ui {

import mx.containers.VBox;

/**
 * A flex component that wraps a media container that will not
 * exceed a maximum size.
 */
public class ScalingMediaBox extends VBox
{
    public function ScalingMediaBox (maxWidth :int, maxHeight :int)
    {
        _smc = new ScalingMediaContainer(maxWidth, maxHeight, this);
        rawChildren.addChild(_smc);
    }

    public function setMedia (url :String) :void
    {
        _smc.setMedia(url);
    }

    public function setMediaClass (clazz :Class) :void
    {
        _smc.setMediaClass(clazz);
    }

    public function shutdown () :void
    {
        _smc.shutdown();
    }

    override public function get measuredWidth () :Number
    {
        var cw :Number = _smc.getContentWidth();
        return (cw == 0) ? _smc.maxW : cw;
    }
    
    override public function get measuredHeight () :Number
    {
        var ch :Number = _smc.getContentHeight();
        return (ch == 0) ? _smc.maxH : ch;
    }

    override protected function measure () :void
    {
        // nothing doing
    }

    protected var _smc :ScalingMediaContainer;
}
}

import com.threerings.util.MediaContainer;

import com.threerings.msoy.ui.ScalingMediaBox;

class ScalingMediaContainer extends MediaContainer
{
    public var maxW :int;
    public var maxH :int;

    public function ScalingMediaContainer (
        maxWidth :int, maxHeight :int, daddy :ScalingMediaBox)
    {
        maxW = maxWidth;
        maxH = maxHeight;
        _daddy = daddy;
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
        
        _daddy.invalidateSize();
    }

    protected var _mediaScale :Number = 1;

    protected var _daddy :ScalingMediaBox;
}
