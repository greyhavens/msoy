package com.threerings.msoy.room.client.snapshot {

import flash.geom.Matrix;

/**
 * The framer calculates the transformation necessary to place an image in a frame by providing
 * a _scale factor and offset.
 */
public class NoopFramer implements Framer
{
    public function getMatrix () :Matrix
    {
        return new Matrix();
    }
}
}
