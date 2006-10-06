package {

import flash.display.Sprite;

/** This class should be considered abstract. */

public class MovingSprite extends Sprite
{
    /** Dimensions of the sprite on the board. */
    public static const WIDTH :int = 20;
    public static const HEIGHT :int = 30;
    
    /** Create a new sprite at the coordinates on the board given. */
    public function MovingSprite (startX :int, startY :int)
    {
        x = startX;
        y = startY;
        setPosition();
    }
    
    /** Called to move sprite at each clock tick. */
    public function tick () :void
    {
        var deltaX :int = _speed * _moveX;
        var deltaY :int = _speed * _moveY;
        if (0 <= x + deltaX && x + deltaX + WIDTH <= Board.WIDTH) {
            x += deltaX;
        }
        if (0 <= y + deltaY && y + deltaY + HEIGHT <= Board.HEIGHT) {
            y += deltaY;
        }
    }
    
    /** Set the number of pixels sprite can move in a tick. */
    public function setSpeed(newSpeed :int) :void
    {
        _speed = newSpeed;
    }
    
    /** Set location coordinates and draw the sprite on the board there. */
    protected function setPosition () :void
    {
        // Draw a boring blue rectangle for this default sprite.
        graphics.beginFill(0x0000FF)
        graphics.drawRect(0, 0, WIDTH, HEIGHT);
    }
    
    /** 
     * Direction we're moving in a given tick. If zero, not moving on this
     * axis. If -1, moving up or left. If 1, moving down or right. 
     */
    protected var _moveY :int = 0;
    protected var _moveX :int = 0;
    
    /** Current speed (in pixels per tick). */
    protected var _speed :int;
}
}
