package com.threerings.msoy.room.client.snapshot {

import flash.geom.Matrix;
import flash.geom.Rectangle

/**
 * The framer calculates the transformation necessary to place an image in a frame by providing
 * a _scale factor and offset.
 */ 
public class Framer {
  
    /**
     * Create a new framer given a rectangle indicating the size of a the source, and a frame to
     * fit it to.
     */
    public function Framer (source :Rectangle, frame :Rectangle) 
    {
        _source = source;
        _frame = frame;
        
        // at this point the results can't change after construction, so we calculate immediately.
        calculate();
    }
    
    /**
     * Apply the transformation calculated by this framer to the matri_x provided.
     */
    public function applyTo (matrix :Matrix) :void 
    {
        matrix.scale(_scale, _scale);
        matrix.translate(_x, 0);
    }
    
    /** 
     * Calculate the transformation.
     */
    protected function calculate () :void
    {
        _scale = _frame.height / _source.height;
        _x = (_frame.width - (_source.width * _scale)) / 2
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
 
    protected var _source :Rectangle;
    protected var _frame :Rectangle;    
}
}