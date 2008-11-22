//
// $Id$

package com.threerings.msoy.applets.image {

import mx.containers.ViewStack;

import mx.core.Application;
import mx.core.UIComponent;

import com.threerings.util.MessageBundle;

import com.threerings.msoy.applets.AppletContext;

public class ImageContext extends AppletContext
{
    public function ImageContext (app :Application, viewStack :ViewStack)
    {
        super(app);
        _viewStack = viewStack;

        _imageBundle = _msgMgr.getBundle("image");
    }

    /**
     * Access the image message bundle.
     */
    public function get IMAGE () :MessageBundle
    {
        return _imageBundle;
    }

    public function getViewWidth () :int
    {
        return _viewStack.width;
    }

    public function getViewHeight () :int
    {
        return _viewStack.height;
    }

    public function pushView (view :UIComponent) :void
    {
        _viewStack.addChild(view);
        _viewStack.selectedIndex++;
    }

    public function popView () :void
    {
        const oldIndex :int = _viewStack.selectedIndex;
        if (oldIndex > 0) {
            _viewStack.selectedIndex--;
        }
        _viewStack.removeChildAt(oldIndex);
    }

    /** The image message bundle. */
    protected var _imageBundle :MessageBundle;

    protected var _viewStack :ViewStack;
}
}
