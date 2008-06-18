//
// $Id$

package com.threerings.msoy.applets.image {

import com.threerings.util.MessageBundle;

import com.threerings.msoy.applets.AppletContext;

public class ImageContext extends AppletContext
{
    public function ImageContext ()
    {
        super();

        _imageBundle = _msgMgr.getBundle("image");
    }

    /**
     * Access the image message bundle.
     */
    public function get IMAGE () :MessageBundle
    {
        return _imageBundle;
    }

    /** The image message bundle. */
    protected var _imageBundle :MessageBundle;
}
}
