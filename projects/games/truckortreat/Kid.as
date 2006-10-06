package {

import flash.display.Sprite;
import flash.events.KeyboardEvent;
import flash.ui.Keyboard;

public class Kid extends Sprite
{
    /** Dimensions of the sprite on the board. */
    public static const WIDTH :int = 10;
    public static const HEIGHT :int = 15;
    
    /** Default number of pixels kid can move per tick. */
    public static const DEFAULT_SPEED :int = 5;
    
    /** Create a new Kid object at the coordinates on the board given. */
    public function Kid (startX :int, startY :int)
    {
        _health = STARTING_HEALTH;
        x = startX;
        y = startY;
        setPosition();
    }
    
    /** Move kid if direction keys are being held down. */
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
    
    public function isDead () :Boolean
    {
        if (_health <= 0) {
            return true;
        } else {
            return false;
        }
    }
    
    /** 
     * Set variables determining direction of motion appropriately when a key
     * is pressed.
     */
    public function keyDownHandler(event :KeyboardEvent) :void
    {
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
    public function keyUpHandler(event :KeyboardEvent) :void
    {
        if (event.keyCode == Keyboard.UP || Keyboard.DOWN) {
            _moveY = 0;
        }
        if (event.keyCode == Keyboard.LEFT || Keyboard.RIGHT) {
            _moveX = 0;
        }
    }
    
    /**
     * Set location coordinates and draw the kid on the board there.
     */
    protected function setPosition () :void
    {
        // Draw a boring blue ellipse for the kid until we have actual art.
        graphics.beginFill(0x0000FF)
        graphics.drawEllipse(0, 0, WIDTH, HEIGHT);
    }
    
    /** 
     * Direction we're moving in a given tick. If zero, not moving on this
     * axis. If -1, moving up or left. If 1, moving down or right. 
     */
    protected var _moveY :int = 0;
    protected var _moveX :int = 0;
    
    /** Current speed (in pixels per tick). */
    protected var _speed :int = DEFAULT_SPEED;

    /** Kid's health level. */
    protected var _health :int;
    
    /** Initial health level. */
    protected static const STARTING_HEALTH :int = 3;
}
}
