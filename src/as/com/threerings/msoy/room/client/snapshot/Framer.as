package com.threerings.msoy.room.client.snapshot {

import flash.geom.Matrix;
import flash.geom.Rectangle

/**
 * The framer calculates the transformation necessary to place an image in a frame by providing
 * a _scale factor and offset.
 */ 
public interface Framer {
 
    function applyTo (matrix :Matrix) :void;

}
}
