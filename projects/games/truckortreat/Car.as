package {

import flash.display.Sprite;

public class Car extends MovingSprite
{
    /** Dimensions of the sprite on the board. */
    public static const WIDTH :int = 30;
    public static const HEIGHT :int = 45;
    
    /** Default number of pixels car can move per tick. */
    public static const DEFAULT_SPEED :int = 10;
    
    /** Constants to keep track of whether car is going up or down. */
    public static const UP :int = 0;
    public static const DOWN :int = 1;
    
    /** Create a new Car object at the coordinates on the board given. */
    public function Car (startX :int, startY :int, direction :int)
    {
        super(startX, startY);
        setSpeed(DEFAULT_SPEED);
        // Set direction we're driving.
        if (direction == UP) {
            _moveY = -1;
        } else if (direction == DOWN) {
            _moveY = 1;
        }
    }
    
    /** 
     * Move up or down on the board, as appropriate. If we go past the edge, 
     * set y coordinate to other end to start over again.
     */
    override public function tick () :void
    {
        y += _speed * _moveY;
        if (y > Board.HEIGHT) {
            y = 0;
        } else if (y < 0) {
            y = Board.HEIGHT;
        }
    }
    
}
}
