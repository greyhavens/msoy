package {

import flash.display.Sprite;
import flash.display.Bitmap;

/** This class should be considered abstract. */

public class BaseSprite extends Sprite
{   
    /** Create a new sprite at the coordinates on the board given. */
    public function BaseSprite (startX :int, startY :int, bitmap :Bitmap)
    {
        // Draw sprite at starting coordinates given.
        x = startX;
        y = startY;
        addChild(bitmap);
    }
}
}
