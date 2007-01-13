package {

import flash.display.DisplayObject;
import flash.display.Sprite;
import flash.text.TextField;
import flash.text.TextFieldAutoSize;
import flash.events.KeyboardEvent;
import flash.ui.Keyboard;
import flash.media.Sound;

import mx.core.MovieClipAsset;
import mx.core.BitmapAsset;

import com.threerings.ezgame.EZGameControl;

public class Kid extends Sprite
{   
    /** Offsets for each avatar. Offset + animation = animation to use. */
    public static const VAMPIRE :int = 0;
    public static const GHOST :int = 8;
    
    /** Animation types. */
    public static const IDLE_RIGHT :int = 0;
    public static const IDLE_LEFT :int = 1;
    public static const WALK_RIGHT :int = 2;
    public static const WALK_LEFT :int = 3;
    public static const SQUISH_RIGHT :int = 4;
    public static const SQUISH_LEFT :int = 5;
    public static const FINAL_SQUISH_RIGHT :int = 6;
    public static const FINAL_SQUISH_LEFT :int = 7;
    
    /** Specific animations for each avatar. */
    public static const VAMPIRE_IDLE_RIGHT :int = 0;
    public static const VAMPIRE_IDLE_LEFT :int = 1;
    public static const VAMPIRE_WALK_RIGHT :int = 2;
    public static const VAMPIRE_WALK_LEFT :int = 3;
    public static const VAMPIRE_SQUISH_RIGHT :int = 4;
    public static const VAMPIRE_SQUISH_LEFT :int = 5;
    public static const VAMPIRE_FINAL_SQUISH_RIGHT :int = 6;
    public static const VAMPIRE_FINAL_SQUISH_LEFT :int = 7;
    
    public static const GHOST_IDLE_RIGHT :int = 8;
    public static const GHOST_IDLE_LEFT :int = 9;
    public static const GHOST_WALK_RIGHT :int = 10;
    public static const GHOST_WALK_LEFT :int = 11;
    public static const GHOST_SQUISH_RIGHT :int = 12;
    public static const GHOST_SQUISH_LEFT :int = 13;
    public static const GHOST_FINAL_SQUISH_RIGHT :int = 14;
    public static const GHOST_FINAL_SQUISH_LEFT :int = 15;
    
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
    public function Kid (startX :int, startY :int, avatarType :int, playerName :String, board :Board)
    {
        _board = board;
        // We need to keep track of what the dimensions of the board at game
        // start time, because apparently the height at least can change if a 
        // kid tries to move off of the bottom of the board.
        _boardHeight = board.height;
        _boardWidth = board.width;
        _playerName = playerName;
        _avatarType = avatarType;
        _speed = DEFAULT_SPEED;
        _lives = STARTING_LIVES;
        _dead = false;
        _facing = RIGHT;
        x = startX;
        y = startY;
        // Don't need to tell other players to update animation this time.
        setAnimation(avatarType + IDLE_RIGHT, false);
        
        // Print player's name above the kid.
        _nameLabel = new TextField();
        _nameLabel.autoSize = TextFieldAutoSize.CENTER;
        _nameLabel.selectable = false;
        _nameLabel.text = _playerName;
        _nameLabel.textColor = uint(0x33CC33);
        // Center the label above us.
        _nameLabel.y = -1 * (_nameLabel.textHeight + NAME_PADDING);
        _nameLabel.x = (_avatar.width - _nameLabel.textWidth) / 2;
        addChild(_nameLabel);
        
        // Sound for when we get smashed by a road-rage filled driver.
        _squishSound = Sound(new squishSoundAsset());
    }
    
    /** Handle being killed. */
    public function wasKilled () :void
    {
        _dead = true;
        _lives--;
        _squishSound.play();
        if (_lives == 0) {
            if (_facing == LEFT) {
                setAnimation(_avatarType + FINAL_SQUISH_LEFT, true);
            } else {
                setAnimation(_avatarType + FINAL_SQUISH_RIGHT, true);
            }
        } else {
            if (_facing == LEFT) {
                setAnimation(_avatarType + SQUISH_LEFT, true);
            } else {
                setAnimation(_avatarType + SQUISH_RIGHT, true);
            }
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
            
            if (0 <= x + deltaX && x + deltaX + _avatar.width <= _boardWidth) {
                x += deltaX;
            }
            if (Board.HORIZON - (_avatar.height) <= y + deltaY && 
                y + deltaY + (_avatar.height) <= _boardHeight) {
                y += deltaY;
            }
            // If we moved, tell the other players.
            if (_moveX != 0 || _moveY != 0) {
                _board.setMyKidLocation(x, y);
                // Set _facing if we moved left or right.
                if (_moveX != 0) {
                    _facing = _moveX;
                }
                // Update animation if necessary. NB: this is trick to check  
                // class only works because we're casting the idle _avatars as 
                // BitmapAssets. 
                if (_avatar is BitmapAsset) {
                    if (_facing == LEFT) {
                        setAnimation(_avatarType + WALK_LEFT, true);
                    } else {
                        setAnimation(_avatarType + WALK_RIGHT, true);
                    }
                }
            } else {
                // Set animation to idle if it is currently a MovieClipAsset.
                if (_avatar is MovieClipAsset) {
                    if (_facing == LEFT) {
                        setAnimation(_avatarType + IDLE_LEFT, true);
                    } else {
                        setAnimation(_avatarType + IDLE_RIGHT, true);
                    }
                }
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
            break;
        case Keyboard.RIGHT:
            _moveX = RIGHT;
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
    
    /** 
     * Return the total height of the kid avatar plus its name label. Note that 
     * just the height parameter of the kid sprite does not always seem to be
     * the same as this for some mysterious reason.
     */
    public function getHeight () :int
    {
        return _avatar.height + _nameLabel.height;
    }
    
    /** Set the kid's avatar animation/image to the given type. */
    public function setAnimation (animationType :int, tell :Boolean) :void
    {
        if (_avatar != null) {
            removeChild(_avatar);
        }
        
        // I really hope there is a better way to do this.
        switch (animationType) {
        case VAMPIRE_IDLE_RIGHT:
            _avatar = BitmapAsset(new vampireIdleRightAsset());
            break;
        case VAMPIRE_IDLE_LEFT:
            _avatar = BitmapAsset(new vampireIdleLeftAsset());
            break;
        case VAMPIRE_WALK_RIGHT:
            _avatar = MovieClipAsset(new vampireWalkRightAsset());
            break;
        case VAMPIRE_WALK_LEFT:
            _avatar = MovieClipAsset(new vampireWalkLeftAsset());
            break;
        case VAMPIRE_SQUISH_RIGHT:
            _avatar = MovieClipAsset(new vampireSquishRightAsset());
            break;
        case VAMPIRE_SQUISH_LEFT:
            _avatar = MovieClipAsset(new vampireSquishLeftAsset());
            break;
        case VAMPIRE_FINAL_SQUISH_RIGHT:
            _avatar = MovieClipAsset(new vampireFinalSquishRightAsset());
            break;
        case VAMPIRE_FINAL_SQUISH_LEFT:
            _avatar = MovieClipAsset(new vampireFinalSquishLeftAsset());
            break;
        case GHOST_IDLE_RIGHT:
            _avatar = BitmapAsset(new ghostIdleRightAsset());
            break;
        case GHOST_IDLE_LEFT:
            _avatar = BitmapAsset(new ghostIdleLeftAsset());
            break;
        case GHOST_WALK_RIGHT:
            _avatar = MovieClipAsset(new ghostWalkRightAsset());
            break;
        case GHOST_WALK_LEFT:
            _avatar = MovieClipAsset(new ghostWalkLeftAsset());
            break;
        case GHOST_SQUISH_RIGHT:
            _avatar = MovieClipAsset(new ghostSquishRightAsset());
            break;
        case GHOST_SQUISH_LEFT:
            _avatar = MovieClipAsset(new ghostSquishLeftAsset());
            break;
        case GHOST_FINAL_SQUISH_RIGHT:
            _avatar = MovieClipAsset(new ghostFinalSquishRightAsset());
            break;
        case GHOST_FINAL_SQUISH_LEFT:
            _avatar = MovieClipAsset(new ghostFinalSquishLeftAsset());
            break;
        default:
            return;
        }
        // Replace _avatar and tell other players to do the same, if desired.
        addChild(_avatar);
        if (tell) {
            _board.setKidAnimation(animationType);
        }
    }
    
    /** Respawn at a random sidewalk location. */
    protected function respawn () :void
    {
        _dead = false;
        x = _board.getSidewalkX();
        y = _board.getSidewalkY() - (_avatar.height + _nameLabel.height);
        _board.setMyKidLocation(x, y);
        _facing = RIGHT;
        setAnimation(_avatarType + IDLE_RIGHT, true);
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
    
    /** Dimensions of board. */
    protected var _boardHeight :int;
    protected var _boardWidth :int;
    
    /** Direction kid is facing at the moment. */
    protected var _facing :int;
    
    /** Sound for squishy death. */
    protected var _squishSound :Sound;
    
    /** The current movie clip or bitmap that the kid is using as an avatar. */
    protected var _avatar :DisplayObject;
    
    /** The type of avatar. Ghost, vampire, etc. */
    protected var _avatarType :int;
    
    /** Initial number of lives. */
    protected static const STARTING_LIVES :int = 3;
    
    /** Default number of pixels kid can move per tick. */
    protected static const DEFAULT_SPEED :int = 8;
    
    /** The number of ticks that may elapse before we're auto-respawned. */
    protected static const AUTO_RESPAWN_TICKS :int = 20;
    
    /** The number of pixels to raise the name above the sprite. */
    protected static const NAME_PADDING :int = 3;
        
    /** Vampire animations. */
    [Embed(source="rsrc/vampire/idle_right.png")]
    protected static const vampireIdleRightAsset :Class;
    
    [Embed(source="rsrc/vampire/idle_left.png")]
    protected static const vampireIdleLeftAsset :Class;
    
    [Embed(source="rsrc/vampire/walk_right.swf")]
    protected static const vampireWalkRightAsset :Class;
    
    [Embed(source="rsrc/vampire/walk_left.swf")]
    protected static const vampireWalkLeftAsset :Class;
    
    [Embed(source="rsrc/vampire/squish_right.swf")]
    protected static const vampireSquishRightAsset :Class;
    
    [Embed(source="rsrc/vampire/squish_left.swf")]
    protected static const vampireSquishLeftAsset :Class;
    
    [Embed(source="rsrc/vampire/final_squish_right.swf")]
    protected static const vampireFinalSquishRightAsset :Class;
    
    [Embed(source="rsrc/vampire/final_squish_left.swf")]
    protected static const vampireFinalSquishLeftAsset :Class;
    
    /** Ghost animations. */
    [Embed(source="rsrc/ghost/idle_right.png")]
    protected static const ghostIdleRightAsset :Class;
    
    [Embed(source="rsrc/ghost/idle_left.png")]
    protected static const ghostIdleLeftAsset :Class;
    
    [Embed(source="rsrc/ghost/walk_right.swf")]
    protected static const ghostWalkRightAsset :Class;
    
    [Embed(source="rsrc/ghost/walk_left.swf")]
    protected static const ghostWalkLeftAsset :Class;
    
    [Embed(source="rsrc/ghost/squish_right.swf")]
    protected static const ghostSquishRightAsset :Class;
    
    [Embed(source="rsrc/ghost/squish_left.swf")]
    protected static const ghostSquishLeftAsset :Class;
    
    [Embed(source="rsrc/ghost/final_squish_right.swf")]
    protected static const ghostFinalSquishRightAsset :Class;
    
    [Embed(source="rsrc/ghost/final_squish_left.swf")]
    protected static const ghostFinalSquishLeftAsset :Class;
    
    /** Squishy sound. */
    [Embed(source="rsrc/squish.mp3")]
    protected static const squishSoundAsset :Class;
}
}
