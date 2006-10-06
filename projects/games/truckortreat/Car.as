package {

import flash.display.Bitmap;

public class Car extends BaseSprite
{   
    /** Default number of pixels car can move per tick. */
    public static const DEFAULT_SPEED :int = 10;
    
    /** Constants to keep track of whether car is going up or down. */
    public static const UP :int = -1;
    public static const DOWN :int = 1;
    
    /** Create a new Car object at the coordinates on the board given. */
    public function Car (startX :int, startY :int, direction :int, board :Board)
    {
        _boardHeight = board.getHeight();
        _speed = DEFAULT_SPEED;
        var bitmap :Bitmap;
        // Set image based on direction we're driving.
        if (direction == UP) {
            bitmap = Bitmap(new carBackAsset());
            _direction = UP;
        } else if (direction == DOWN) {
            bitmap = Bitmap(new carFrontAsset());
            _direction = DOWN;
        }
        super(startX, startY, bitmap);
    }
    
    /** 
     * Move up or down on the board, as appropriate. If we go past the edge, 
     * set y coordinate to other end to start over again.
     */
    public function tick () :void
    {
        y += _speed * _direction;
        if (y > _boardHeight) {
            y = Board.HORIZON - _height;
        } else if (y < 0) {
            y = _boardHeight;
        }
    }
    
    /** Number of pixels car moves per tick. */
    protected var _speed :int;
    
    /** Keeps track of the direction car is going. */
    protected var _direction :int;
    
    /** Height of board we're drawn on, in pixels. */
    protected var _boardHeight :int;
        
    /** Car images. */
    [Embed(source="rsrc/car_front.png")]
    protected static const carFrontAsset :Class;
    
    [Embed(source="rsrc/car_back.png")]
    protected static const carBackAsset :Class;    
}
}
