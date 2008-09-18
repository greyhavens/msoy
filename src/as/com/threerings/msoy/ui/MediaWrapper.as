//
// $Id$

package com.threerings.msoy.ui {

import mx.core.Container;

import com.threerings.flash.MediaContainer;

import com.threerings.msoy.data.all.MediaDesc;

/**
 * Wraps a MediaContainer into a UIComponent.
 */
public class MediaWrapper extends Container
{
    /**
     * Factory to create a MediaWrapper configured to view media represented by a MediaDesc
     * at the specified size.
     */
    public static function createView (desc :MediaDesc, size :int = MediaDesc.THUMBNAIL_SIZE)
        :MediaWrapper
    {
        var smc :ScalingMediaContainer = ScalingMediaContainer.createView(desc, size);
        return new MediaWrapper(smc, smc.maxW, smc.maxH, true);
    }

    // ---- End: static methods

    /**
     * @param cont the container to wrap
     * @param altReportedWidth a width to report when the media width is 0.
     * @param altReportedHeight a height to report when the media height is 0.
     */
    public function MediaWrapper (cont :MsoyMediaContainer, altReportedWidth :Number = 0,
                                  altReportedHeight :Number = 0, alwaysUseAlt :Boolean = false)
    {
        _cont = cont;
        _altWidth = altReportedWidth;
        _altHeight = altReportedHeight;
        _alwaysUseAlt = alwaysUseAlt;
        rawChildren.addChild(cont);
        if (!alwaysUseAlt) {
            cont.addEventListener(MediaContainer.SIZE_KNOWN, handleMediaSizeChanged, false, 0, true);
        }
    }

    public function getMediaContainer () :MsoyMediaContainer
    {
        return _cont;
    }

    public function setMediaDesc (desc :MediaDesc) :void
    {
        _cont.setMediaDesc(desc);
    }

    public function shutdown () :void
    {
        _cont.shutdown();
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
        var w :Number = _cont.getContentWidth();
        return (w == 0 || _alwaysUseAlt) ? _altWidth : w;
    }

    override public function get measuredHeight () :Number
    {
        var h :Number = _cont.getContentHeight();
        return (h == 0 || _alwaysUseAlt) ? _altHeight : h;
    }

    /**
     * React to changes in the size of the MediaContainer.
     */
    protected function handleMediaSizeChanged (evt :Object) :void
    {
        invalidateSize();
    }

    protected var _cont :MsoyMediaContainer;

    protected var _altWidth :Number;
    protected var _altHeight :Number;

    protected var _alwaysUseAlt :Boolean;
}
}
