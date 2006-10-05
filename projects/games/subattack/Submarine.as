package {

import flash.text.TextField;

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
        // center the label above us
        nameLabel.y = -1 * (nameLabel.textHeight + NAME_PADDING);
        nameLabel.x = (SeaDisplay.TILE_SIZE - nameLabel.textWidth) / 2;
        addChild(nameLabel);
    }

    public function getPlayerName () :String
    {
        return _playerName;
    }

    public function getPlayerIndex () :int
    {
        return _playerIdx;
    }

    public function getScore () :int
    {
        return _kills - _deaths;
    }

    /**
     * Is this sub dead?
     */
    public function isDead () :Boolean
    {
        return _dead;
    }

    /**
     * Called to respawn this sub at the coordinates specified.
     */
    public function respawn (xx :int, yy :int) :void
    {
        if (_dead) {
            _dead = false;
            _x = xx;
            _y = yy;
            updateDeath();
            updateLocation();
            updateVisual();
        }
    }

    /**
     * Return the distance from this sub to the specified coordinate.
     */
    public function distance (xx :int, yy :int) :Number
    {
        var dx :Number = xx - _x;
        var dy :Number = yy - _y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Perform the action specified, or return false if unable.
     */
    public function performAction (action :int) :Boolean
    {
        _queuedActions.push(action);

//        if (_queuedActions.length > 0) {
//            if (_queuedActions.length >= 5) {
//                return true; // don't queue?
//            }
//            // TODO: don't queue shoots?
//            _queuedActions.push(action);
//        }
//        var result :int = performActionInternal(action);
//        if (result == CANT) {
//            _queuedActions.push(action);
//        }
        return true;
    }

    protected static const OK :int = 0;
    protected static const CANT :int = 1;
    protected static const DROP :int = 2;

    protected function performActionInternal (action :int) :int
    {
        if (_dead) {
            return DROP;
        }

        // if we've already shot, we can do no more
        if (_shot) {
            return (action == Action.SHOOT) ? DROP : CANT;
        }

        if (action == Action.SHOOT) {
            if (_torpedos.length == MAX_TORPEDOS) {
                // shoot once per tick, max 2 in-flight
                return DROP;

            } else {
                _torpedos.push(new Torpedo(this, _board));
                _shot = true;
                return OK;
            }
        }

        // otherwise, it's a move request, it'll have to happen next tick
        if (_moved) {
            return CANT;
        }

        // we can always re-orient
        if (_orient != action) {
            _orient = action;
            updateVisual();
        }
        if (!advanceLocation()) {
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

        while (_queuedActions.length > 0) {
            var action :int = int(_queuedActions[0]);
            if (CANT == performActionInternal(action)) {
                return;
            }
            _queuedActions.shift();
        }
    }

    public function postTick () :void
    {
//        while (_queuedActions.length > 0) {
//            var action :int = int(_queuedActions[0]);
//            if (CANT == performActionInternal(action)) {
////                if (move != Action.SHOOT) {
//                    return;
////                }
//            }
//            _queuedActions.shift();
//        }
    }

    /**
     * Called by our torpedo to let us know that it's gone.
     */
    public function torpedoExploded (torp :Torpedo, kills :int) :void
    {
        var idx :int = _torpedos.indexOf(torp);
        if (idx == -1) {
            trace("OMG: missing torp!");
            return;
        }

        // remove it
        _torpedos.splice(idx, 1);

        // track the kills
        _kills += kills;
    }

    /**
     * Called to indicate that this sub was hit with a torpedo.
     */
    public function wasKilled () :void
    {
        _dead = true;
        _deaths++;
        _queuedActions.length = 0; // drop any queued actions
        updateVisual();
        updateDeath();
    }

    override protected function updateLocation () :void
    {
        super.updateLocation();

        if (parent != null) {
            (parent as SeaDisplay).subUpdated(this, _x, _y);
        }
    }

    protected function updateDeath () :void
    {
        if (parent != null) {
            (parent as SeaDisplay).deathUpdated(this);
        }
    }

    protected function updateVisual () :void
    {
        graphics.clear();
        if (_dead) {
            return;
        }

        // draw the circle
        graphics.lineStyle(2, 0x000000);
        graphics.beginFill(uint(COLORS[_playerIdx]))
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

    /** Queued actions. */
    protected var _queuedActions :Array = [];

    protected var _dead :Boolean;

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

    protected static const COLORS :Array = [ 0xFFFF00, 0xFF00FF,
        0x00FFFF, 0xFF0000, 0x0000FF, 0x00FF00, 0xFF6600, 0x6600FF ];

    /** The maximum number of torpedos that may be in-flight at once. */
    protected static const MAX_TORPEDOS :int = 2;

    /** The number of pixels to raise the name above the sprite. */
    protected static const NAME_PADDING :int = 3;
}
}
