package {

import flash.display.Bitmap;
import flash.events.KeyboardEvent;
import flash.ui.Keyboard;

public class Kid extends BaseSprite
{   
    /** Images that can be used for kid. */
    public static const IMAGE_VAMPIRE :int = 0;
    public static const IMAGE_GHOST :int = 1;
    
    /** Create a new Kid object at the coordinates on the board given. */
    public function Kid (startX :int, startY :int, image :int, board :Board)
    {
        _boardWidth = board.width;
        _boardHeight = board.height;
        _speed = DEFAULT_SPEED;
        _lives = STARTING_LIVES;
        _dead = false;
        super(startX, startY, getBitmap(image));
    }
    
    /** Handle being killed. */
    public function wasKilled () :void
    {
        _dead = true;
        _lives--;
        // TODO: this is where we have some nice kid being squashed animation.
        trace("Oh nos, death! " + _lives + " lives left.");
        _respawnTicks = AUTO_RESPAWN_TICKS;
    }
    
    /** 
     * Set variables determining direction of motion appropriately when a key
     * is pressed.
     */
    public function keyDownHandler (event :KeyboardEvent) :void
    {
        trace("got a key down press");
        switch (event.keyCode) {
        case Keyboard.UP:
            _moveY = -1;
            break;
        case Keyboard.DOWN:
            _moveY = 1;
            break;
        case Keyboard.LEFT:
            _moveX = -1;
            break;
        case Keyboard.RIGHT:
            _moveX = 1;
            break;
        default:
            return;
        }
    }
    
    /** Stop motion in given direction when the key is released. */
    public function keyUpHandler (event :KeyboardEvent) :void
    {
        if (event.keyCode == Keyboard.UP || Keyboard.DOWN) {
            _moveY = 0;
        }
        if (event.keyCode == Keyboard.LEFT || Keyboard.RIGHT) {
            _moveX = 0;
        }
    }
    
    /** Called to move kid at each clock tick. */
    public function tick () :void
    {
        if (_dead) {
            _respawnTicks--;
            if (_respawnTicks == 0) {
                respawn();
            }
        } else {
            // Only move if we're not dead.
            var deltaX :int = _speed * _moveX;
            var deltaY :int = _speed * _moveY;
            if (0 <= x + deltaX && x + deltaX + width <= _boardWidth) {
                x += deltaX;
            }
            if (Board.HORIZON - height <= y + deltaY && 
                y + deltaY + height <= _boardHeight) {
                y += deltaY;
            }
        }
    }
    
    /** Return true if we're not dead, false if we are. */
    public function isAlive() :Boolean
    {
        return !_dead;
    }
    
    /** Return the number of remaining lives. */
    public function livesLeft() :int
    {
        return _lives;
    }
    
    /** Set the kid's speed to a new value. */
    public function setSpeed (newSpeed :int) :void
    {
        _speed = newSpeed;
    }
    
    /** Respawn at a random sidewalk location. */
    protected function respawn () :void
    {
        _dead = false;
        // TODO: set these to some actually randomized coordinates
        x = 180;
        y = 200;
    }
    
    /** Get the bitmap used to draw the kid. */
    protected function getBitmap (image :int) :Bitmap
    {
        switch (image) {
          case IMAGE_VAMPIRE:
            return Bitmap(new vampireAsset());
          case IMAGE_GHOST:
            return Bitmap(new ghostAsset());
          default:
            return Bitmap(new vampireAsset());
        }
    }
    
    /** 
     * Direction we're moving in a given tick. If zero, not moving on this
     * axis. If -1, moving up or left. If 1, moving down or right. 
     */
    protected var _moveY :int = 0;
    protected var _moveX :int = 0;
    
    /** Current speed (in pixels per tick). */
    protected var _speed :int;

    /** Are we dead? */
    protected var _dead :Boolean;

    /** Number of lives. */
    protected var _lives :int;
    
    /** A count of how long until we respawn. */
    protected var _respawnTicks :int;
    
    /** Dimensions of board we're drawn on. */
    protected var _boardHeight :int;
    protected var _boardWidth :int;
    
    /** Initial number of lives. */
    protected static const STARTING_LIVES :int = 3;
    
    /** Default number of pixels kid can move per tick. */
    protected static const DEFAULT_SPEED :int = 8;
    
    /** The number of ticks that may elapse before we're auto-respawned. */
    protected static const AUTO_RESPAWN_TICKS :int = 20;
    
    /** Images for kid. */
    [Embed(source="rsrc/vampire.png")]
    protected static const vampireAsset :Class;
    
    [Embed(source="rsrc/ghost.png")]
    protected static const ghostAsset :Class;
}
}
