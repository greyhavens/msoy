//
// $Id$

package com.threerings.msoy.world.client {

import com.threerings.msoy.data.all.MediaDesc;

/**
 * Hacktacular. Do not emulate. I should hide this class somewhere.
 */
public class StudioMediaDesc extends MediaDesc
{
    public function StudioMediaDesc (url :String)
    {
        _url = url;
    }

    override public function getMediaId () :String
    {
        return "studio";
    }

    override public function getMediaPath () :String
    {
        return _url;
    }

    protected var _url :String;
}
}
