package {

import flash.display.Sprite;

import flash.geom.Matrix;

public class Scenery extends Sprite
{
    public function Scenery () 
    {
    }

    /** 
     * Sets a new transform object for a row of the display.  The transform passed in here may
     * be manipulated by this object, so it should probably be passed a clone.
     */
    public function setTransform(minY :Number, maxY :Number, transform :Matrix) :void
    {
    }

    /** 
     * As in the Track, this slides all the objects down by one IMAGE_SIZE, so that the track
     * can continue forward forever.
     */
    public function moveSceneryForward () :void
    {
    }
}
}
