package com.threerings.msoy.room.client.snapshot {

import flash.geom.Matrix;
import flash.geom.Rectangle

/**
 * The framer calculates the transformation necessary to place an image in a frame by providing
 * a _scale factor and offset.
 */
public class CanonicalFramer implements Framer
{
    /**
     * Create a new framer given a rectangle indicating the size of a the source, a frame to
     * fit it to, and an offset value.
     */
    public function CanonicalFramer (source :Rectangle, frame :Rectangle, offset :int)
    {
        _scale = frame.height / source.height;

        // the width that the image is going to end up
        const image_width :int = source.width * _scale;

        // start off by centering the image within the frame
        _x = (frame.width - image_width) / 2

        if (image_width > frame.width) {
            // if the image will be larger than the frame, then we use the scaled offset
            _x = _x - (offset * _scale);
        }
    }

    /**
     * @inheritDoc
     */
    public function getMatrix () :Matrix
    {
        var m :Matrix = new Matrix();
        m.scale(_scale, _scale);
        m.translate(_x, 0);
        return m;
    }

    /**
     * The _scale factor to apply to the source rectangle to place it in the frame.
     */
    protected var _scale :Number;

    /**
     * An offset that should by added to the _x position of the source rectangle to place it
     * in the frame.  Will centralize the image horizontally in the frame.
     */
    protected var _x :int;
}
}
