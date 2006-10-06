package {

import flash.display.Sprite;
import flash.display.Bitmap;
import flash.events.KeyboardEvent;
import flash.ui.Keyboard;

public class Kid extends MovingSprite
{   
    /** Images that can be used for kid. */
    public static const IMAGE_VAMPIRE :int = 0;
    public static const IMAGE_GHOST :int = 1;
    
    /** Default number of pixels kid can move per tick. */
    public static const DEFAULT_SPEED :int = 5;
    
    /** Create a new Kid object at the coordinates on the board given. */
    public function Kid (startX :int, startY :int, image :int, board :Board)
    {
        _health = STARTING_HEALTH;
        super(startX, startY, DEFAULT_SPEED, getBitmap(image), board);
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
    
    protected function getBitmap(image :int) :Bitmap
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

    /** Kid's health level. */
    protected var _health :int;
    
    /** Initial health level. */
    protected static const STARTING_HEALTH :int = 3;
    
    /** Images for kid. */
    [Embed(source="rsrc/vampire.png")]
    protected static const vampireAsset :Class;
    
    [Embed(source="rsrc/ghost.png")]
    protected static const ghostAsset :Class;
}
}
