//
// $Id$

package com.threerings.msoy.applets.remixer {

import com.threerings.msoy.applets.util.Downloader;

/**
 * Extends Downloader and returns a simple filename instead of a nasty
 * media hash filename.
 */
public class PhotoDownloader extends Downloader
{
    public function PhotoDownloader ()
    {
        super();
    }

    override protected function makeFilename (url :String) :String
    {
        var lastDot :int = url.lastIndexOf(".");
        return "photo" + ((lastDot == -1) ? "" : url.substr(lastDot));
    }
}
}
