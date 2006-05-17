package com.threerings.msoy.data {

/**
 * A class containing metadata about a media object.
 */
public class MediaData
{
    public var isAVM1 :Boolean;

    /** Temp: a universal id for this media. */
    public var id :int;

    /** Temporary constructor. */
    public function MediaData (url :String, width :int = -1, height :int = -1)
    {
        _url = url;
        _width = width;
        _height = height;
    }

    /**
     * Get the url at which the media may be loaded.
     */
    public function get URL () :String
    {
        return _url;
    }

    /**
     * Return the width of the media, or -1 if unknown.
     */
    public function get width () :int
    {
        return _width;
    }

    public function get height () :int
    {
        return _height;
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

    /** Width and height, if known. */
    protected var _width :int, _height :int;
}

}
