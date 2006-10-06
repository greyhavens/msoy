package {

import flash.display.Sprite;
import flash.display.Bitmap;
import flash.display.BitmapData;

/** This class should be considered abstract. */

public class BaseSprite extends Sprite
{   
    /** Create a new sprite at the coordinates on the board given. */
    public function BaseSprite (startX :int, startY :int, bitmap :Bitmap)
    {
        // Get dimensions of bitmap in pixels.
        _width = bitmap.bitmapData.width;
        _height = bitmap.bitmapData.height;
        // Draw sprite at starting coordinates given.
        x = startX;
        y = startY;
        addChild(bitmap);
    }
    
    /** Get dimensions of sprite's bitmap. */
    public function getWidth () :int
    {
        return _width;
    }
    
    public function getHeight () :int
    {
        return _height;
    }
    
    /** Dimensions of sprite in pixels. */
    protected var _width :int;
    protected var _height :int;
}
}
