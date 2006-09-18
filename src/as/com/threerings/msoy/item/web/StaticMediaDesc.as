package com.threerings.msoy.item.web {

/**
 * Provides a "faked" media descriptor for static media (default thumbnails and
 * furni representations).
 */
public class StaticMediaDesc extends MediaDesc
{
    public function StaticMediaDesc (path :String)
    {
        _path = path;
        mimeType = suffixToMimeType(path);
    }

    // from MediaDesc
    override public function getMediaPath () :String
    {
        return _path;
    }

    protected var _path :String;
}
}
