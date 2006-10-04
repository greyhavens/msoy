package {

import flash.text.TextField;

import flash.ui.Keyboard;

import com.threerings.ezgame.EZGame;

public class Submarine extends BaseSprite
{
    public function Submarine (
        playerIdx :int, playerName :String, startx :int, starty :int,
        board :Board)
    {
        super(board);

        _playerIdx = playerIdx;
        _playerName = playerName;
        _x = startx;
        _y = starty;
        _orient = (_x == 0) ? Keyboard.RIGHT : Keyboard.LEFT;

        updateVisual();
        updateLocation();

        var nameLabel :TextField = new TextField();
        nameLabel.text = playerName;
        nameLabel.y = -1 * (nameLabel.textHeight + NAME_PADDING);
        nameLabel.x = (SeaDisplay.TILE_SIZE - nameLabel.textWidth) / 2;
        addChild(nameLabel);
    }

    /**
     * Perform the action specified by the keycode, or return false
     * if unable.
     */
    public function performAction (keyCode :int) :Boolean
    {
        if (keyCode == Keyboard.SPACE) {
            if (_shot || _torpedos.length == MAX_TORPEDOS) {
                // shoot once per tick, max 2 in-flight
                return false;

            } else {
                _torpedos.push(new Torpedo(this, _board));
                _shot = true;
                return true;
            }
        }

        // otherwise, it's a move request

        // we can always re-orient
        if (keyCode != _orient) {
            _orient = keyCode;
            updateVisual();
            return true;

        // but we can't move twice in the same tick
        } else if (_moved) {
            return false;

        // try to move, blocking on non-traversable tiles
        } else if (!advanceLocation()) {
            return false;
        }

        // we did it!
        _moved = true;
        return true;
    }

    /**
     * Called by the board to notify us that time has passed.
     */
    public function tick () :void
    {
        // reset our move counter
        _moved = false;
        _shot = false;
    }

    /**
     * Called by our torpedo to let us know that it's gone.
     */
    public function torpedoExploded (torp :Torpedo) :void
    {
        var idx :int = _torpedos.indexOf(torp);
        if (idx == -1) {
            trace("OMG: missing torp!");
            return;
        }

        // remove it
        _torpedos.splice(idx, 1);
    }

    override protected function updateLocation () :void
    {
        super.updateLocation();

        if (parent != null) {
            (parent as SeaDisplay).subUpdated(this, _x, _y);
        }
    }

    protected function updateVisual () :void
    {
        // draw the circle
        graphics.lineStyle(2, 0x000000);
        graphics.beginFill((_playerIdx == 0) ? 0xFFFF00 : 0x00FFFF);
        graphics.drawCircle(SeaDisplay.TILE_SIZE / 2, SeaDisplay.TILE_SIZE / 2,
            SeaDisplay.TILE_SIZE / 2);

        // draw our orientation
        var xx :int = SeaDisplay.TILE_SIZE / 2;
        var yy :int = xx;
        graphics.moveTo(xx, yy);
        switch (_orient) {
        case Keyboard.UP:
            yy = 0;
            break;

        case Keyboard.DOWN:
            yy = SeaDisplay.TILE_SIZE;
            break;

        case Keyboard.LEFT:
            xx = 0;
            break;

        case Keyboard.RIGHT:
            xx = SeaDisplay.TILE_SIZE;
            break;
        }
        graphics.lineTo(xx, yy);
    }

    /** The player index that this submarine corresponds to. */
    protected var _playerIdx :int;

    /** The name of the player controlling this sub. */
    protected var _playerName :String;

    /** Have we moved this tick yet? */
    protected var _moved :Boolean;

    /** Have we shot this tick? */
    protected var _shot :Boolean;

    /** Our currently in-flight torpedos. */
    protected var _torpedos :Array = [];

    /** The number of kills we've had. */
    protected var _kills :int;

    /** The number of times we've been killed. */
    protected var _deaths :int;

    /** The maximum number of torpedos that may be in-flight at once. */
    protected static const MAX_TORPEDOS :int = 2;

    /** The number of pixels to raise the name above the sprite. */
    protected static const NAME_PADDING :int = 3;
}
}
