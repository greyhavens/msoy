package {

import flash.display.Bitmap;
import flash.text.TextField;
import flash.text.TextFieldAutoSize;
import flash.events.KeyboardEvent;
import flash.ui.Keyboard;
import flash.media.Sound;

import com.threerings.ezgame.EZGameControl;

public class Kid extends BaseSprite
{   
    /** Images that can be used for kid. */
    public static const IMAGE_VAMPIRE :int = 0;
    public static const IMAGE_GHOST :int = 1;
    
    /** 
     * Directions kid may be facing or moving. These are only useful for 
     * differentiating between directions on a single axis! For the sake of 
     * these constants, LEFT == UP and RIGHT == DOWN.
     */
    public static const LEFT :int = -1;
    public static const RIGHT :int = 1;
    public static const UP :int = -1;
    public static const DOWN :int = 1;
    
    /** Create a new Kid object at the coordinates on the board given. */
    public function Kid (startX :int, startY :int, image :int, playerName :String, board :Board)
    {
        _board = board;
        _playerName = playerName;
        _speed = DEFAULT_SPEED;
        _lives = STARTING_LIVES;
        _dead = false;
        _facing = RIGHT; // TODO: this actually will probably depend on the image
        super(startX, startY, getBitmap(image));
        
        // Print player's name above the kid.
        _nameLabel = new TextField();
        _nameLabel.autoSize = TextFieldAutoSize.CENTER;
        _nameLabel.selectable = false;
        _nameLabel.text = _playerName;
        _nameLabel.textColor = uint(0x33CC33);
        // Center the label above us.
        _nameLabel.y = -1 * (_nameLabel.textHeight + NAME_PADDING);
        _nameLabel.x = (width - _nameLabel.textWidth) / 2;
        addChild(_nameLabel);
        
        // Sound for when we get smashed by a road-rage filled driver.
        _squishSound = Sound(new SQUISH_SOUND());
    }
    
    /** Handle being killed. */
    public function wasKilled () :void
    {
        _dead = true;
        _lives--;
        _squishSound.play();
        if (_lives == 0) {
            // TODO: here we do the final squish animation
        } else {
            // TODO: this is where we have some nice kid being squashed animation.
            _respawnTicks = AUTO_RESPAWN_TICKS;
        }
        trace("Oh nos, " + _playerName + " died. " + _lives + " lives left.");
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
            if (0 <= x + deltaX && x + deltaX + width <= _board.width) {
                x += deltaX;
            }
            if (Board.HORIZON - (height - _nameLabel.height) <= y + deltaY && 
                y + deltaY + (height - _nameLabel.height) <= _board.height) {
                y += deltaY;
            }
            // If we moved, tell the other players.
            if (_moveX != 0 && _moveY != 0) {
                _board.setMyKidLocation(x, y);
                // TODO: stop movement animation and set to static image. Should
                // get an image that is consistent with _facing.
            }
        }
    }
    
    /** 
     * Set variables determining direction of motion appropriately when a key
     * is pressed.
     */
    public function keyDownHandler (event :KeyboardEvent) :void
    {
        switch (event.keyCode) {
        case Keyboard.UP:
            _moveY = UP;
            break;
        case Keyboard.DOWN:
            _moveY = DOWN;
            break;
        case Keyboard.LEFT:
            _moveX = LEFT;
            _facing = LEFT;
            break;
        case Keyboard.RIGHT:
            _moveX = RIGHT;
            _facing = RIGHT;
            break;
        default:
            return;
        }
        // TODO: start animation that is dependent on _facing
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
        x = _board.getSidewalkX();
        y = _board.getSidewalkY() - height;
        _board.setMyKidLocation(x, y);
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
     * Direction we're moving at the moment. If zero, not moving on this
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
    
    /** The player's name. */
    protected var _playerName :String;
    
    /** Label to display the player's name above his/her character. */
    protected var _nameLabel :TextField;
    
    /** The game board. */
    protected var _board :Board;
    
    /** Direction kid is facing at the moment. */
    protected var _facing :int;
    
    /** Sound for squishy death. */
    protected var _squishSound :Sound;
    
    /** Initial number of lives. */
    protected static const STARTING_LIVES :int = 3;
    
    /** Default number of pixels kid can move per tick. */
    protected static const DEFAULT_SPEED :int = 8;
    
    /** The number of ticks that may elapse before we're auto-respawned. */
    protected static const AUTO_RESPAWN_TICKS :int = 20;
    
    /** The number of pixels to raise the name above the sprite. */
    protected static const NAME_PADDING :int = 3;
    
    /** Images for kid. */
    [Embed(source="rsrc/vampire.png")]
    protected static const vampireAsset :Class;
    
    [Embed(source="rsrc/ghost.png")]
    protected static const ghostAsset :Class;
    
    /** Squishy sound. */
    [Embed(source="rsrc/squish.mp3")]
    protected static const SQUISH_SOUND :Class;
}
}
