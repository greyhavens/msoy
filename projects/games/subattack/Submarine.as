package {

import flash.text.TextField;

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
        _orient = (_x == 0) ? Action.RIGHT : Action.LEFT;

        updateVisual();
        updateLocation();

        var nameLabel :TextField = new TextField();
        nameLabel.text = playerName;
        nameLabel.y = -1 * (nameLabel.textHeight + NAME_PADDING);
        nameLabel.x = (SeaDisplay.TILE_SIZE - nameLabel.textWidth) / 2;
        addChild(nameLabel);
    }

    /**
     * Perform the action specified, or return false if unable.
     */
    public function performAction (action :int) :Boolean
    {
        if (_queuedMoves.length > 0) {
            // TODO: don't queue shoots?
            _queuedMoves.push(action);
        }
        var result :int = performActionInternal(action);
        if (result == CANT) {
            _queuedMoves.push(action);
        }
        return true;
    }

    protected static const OK :int = 0;
    protected static const CANT :int = 1;
    protected static const DROP :int = 2;

    protected function performActionInternal (action :int) :int
    {
        // TEMP: until I sort out a few things...
        if (_shot || _moved) {
            return (action == Action.SHOOT) ? DROP : CANT;
        }
        // END: temp

        if (action == Action.SHOOT) {
            if (_shot || _torpedos.length == MAX_TORPEDOS) {
                // shoot once per tick, max 2 in-flight
                return CANT;

            } else {
                _torpedos.push(new Torpedo(this, _board));
                _shot = true;
                return OK;
            }
        }

        // otherwise, it's a move request

        // we can always re-orient
        if (_orient != action) {
            _orient = action;
            updateVisual();
            return OK;

        // but we can't move twice in the same tick
        } else if (_moved) {
            return CANT;

        // try to move, blocking on non-traversable tiles
        } else if (!advanceLocation()) {
            return DROP;
        }

        // we did it!
        _moved = true;
        return OK;
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

    public function postTick () :void
    {
        while (_queuedMoves.length > 0) {
            var action :int = int(_queuedMoves[0]);
            if (CANT == performActionInternal(action)) {
//                if (move != Action.SHOOT) {
                    return;
//                }
            }
            _queuedMoves.shift();
        }
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
        case Action.UP:
            yy = 0;
            break;

        case Action.DOWN:
            yy = SeaDisplay.TILE_SIZE;
            break;

        case Action.LEFT:
            xx = 0;
            break;

        case Action.RIGHT:
            xx = SeaDisplay.TILE_SIZE;
            break;
        }
        graphics.lineTo(xx, yy);
    }

    /** Queued moves. */
    protected var _queuedMoves :Array = [];

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
