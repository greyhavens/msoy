package com.threerings.msoy.data {

/**
 * A class containing metadata about a media object.
 */
public class MediaData
{
    /** Temporary constructor. */
    public function MediaData (url :String)
    {
        _url = url;
    }

    /**
     * Get the url at which the media may be loaded.
     */
    public function get URL () :String
    {
        return _url;
    }

    /**
     * @return true if the media is clickable.
     */
    public function isInteractive () :Boolean
    {
        return true;
    }

    /** temp */
    protected var _url :String;
}

}
