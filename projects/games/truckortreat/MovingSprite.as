package {

import flash.display.Sprite;
import flash.display.Bitmap;
import flash.display.BitmapData;

/** This class should be considered abstract. */

public class MovingSprite extends Sprite
{   
    /** Create a new sprite at the coordinates on the board given. */
    public function MovingSprite (startX :int, startY :int, speed :int, 
        bitmap :Bitmap, board :Board)
    {
        _board = board;
        _speed = speed;
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
    
    /** Called to move sprite at each clock tick. */
    public function tick () :void
    {
        var deltaX :int = _speed * _moveX;
        var deltaY :int = _speed * _moveY;
        if (0 <= x + deltaX && x + deltaX + _width <= _board.getWidth()) {
            x += deltaX;
        }
        if (0 <= y + deltaY && y + deltaY + _height <= _board.getHeight()) {
            y += deltaY;
        }
    }
    
    /** Set the number of pixels sprite can move in a tick. */
    public function setSpeed(newSpeed :int) :void
    {
        _speed = newSpeed;
    }
    
    /** 
     * Direction we're moving in a given tick. If zero, not moving on this
     * axis. If -1, moving up or left. If 1, moving down or right. 
     */
    protected var _moveY :int = 0;
    protected var _moveX :int = 0;
    
    /** Current speed (in pixels per tick). */
    protected var _speed :int;
    
    /** Dimensions of sprite in pixels. */
    protected var _width :int;
    protected var _height :int;
    
    /** Board we're drawn on top of. */
    protected var _board :Board;
}
}
