package com.threerings.msoy.room.client.snapshot {

import flash.geom.Matrix;

/**
 * The framer calculates the transformation necessary to place an image in a frame by providing
 * a _scale factor and offset.
 */
public interface Framer
{
    /**
     * Get the starting matrix for this framer.
     */
    function getMatrix () :Matrix;
}
}
