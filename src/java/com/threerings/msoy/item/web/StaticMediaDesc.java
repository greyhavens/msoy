//
// $Id$

package com.threerings.msoy.item.web;

/**
 * Provides a "faked" media descriptor for static media (default thumbnails and
 * furni representations).
 */
public class StaticMediaDesc extends MediaDesc
{
    public StaticMediaDesc (String path)
    {
        _path = path;
        mimeType = (byte)suffixToMimeType(path);
    }

    // @Override // from MediaDesc
    public String getMediaPath ()
    {
        return _path;
    }

    protected String _path;
}
