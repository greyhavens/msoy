package {

import flash.display.MovieClip;

import flash.media.Sound;

import flash.text.TextField;
import flash.text.TextFieldAutoSize;

import flash.geom.ColorTransform;

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

        var scheme :Array = (SCHEMES[playerIdx] as Array);
        _avatar = MovieClip(new AVATAR());
        _shootSound = Sound(new SHOOT_SOUND());

        // TODO: not working: we should only color the recolory child
//        var colorChild :MovieClip =
//            (_avatar.getChildByName("color") as MovieClip);
//        colorChild.transform.colorTransform =
        _avatar.transform.colorTransform =
            new ColorTransform(
                Number(scheme[0]), Number(scheme[1]), Number(scheme[2]));
        addChild(_avatar);

        _nameLabel = new TextField();
        _nameLabel.autoSize = TextFieldAutoSize.CENTER;
        _nameLabel.selectable = false;
        _nameLabel.text = playerName;
        _nameLabel.textColor = uint(0x33CC33);
        // center the label above us
        _nameLabel.y = -1 * (_nameLabel.textHeight + NAME_PADDING);
        _nameLabel.x = (SeaDisplay.TILE_SIZE - _nameLabel.textWidth) / 2;
        addChild(_nameLabel);

        _statsLabel = new TextField();
        _statsLabel.autoSize = TextFieldAutoSize.CENTER;
        _statsLabel.selectable = false;
        _statsLabel.text = "0 0";
        _statsLabel.textColor = uint(0x000000);
        _statsLabel.backgroundColor = uint(0xCCFFCC);
        _statsLabel.background = true;
        _statsLabel.y = _nameLabel.y - (_statsLabel.textHeight + NAME_PADDING);
        _statsLabel.x = (SeaDisplay.TILE_SIZE - _statsLabel.textWidth) / 2;
        addChild(_statsLabel);

        updateVisual();
        updateLocation();
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

    public function gotPlayerCookie (cookie :Object) :void
    {
        var array :Array = (cookie as Array);
        if (array != null) {
            _totalKills += int(array[0]);
            _totalDeaths += int(array[1]);
            updateVisual();
        }
    }

    public function getNewCookie () :Object
    {
        return [ _totalKills, _totalDeaths ];
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
     * Perform the action specified, or return false if unable.
     */
    public function performAction (action :int) :Boolean
    {
        _queuedActions.push(action);
        return true;
    }

    protected static const OK :int = 0;
    protected static const CANT :int = 1;
    protected static const DROP :int = 2;

    protected function performActionInternal (action :int) :int
    {
        if (_dead || action == Action.RESPAWN) {
            if (_dead && action == Action.RESPAWN) {
                _board.respawn(this);
                return OK;
            }
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
                _shootSound.play();
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

        if (_dead) {
            if (--_respawnTicks == 0) {
                performActionInternal(Action.RESPAWN);
            }
        }
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
        _totalKills += kills;
    }

    /**
     * Called to indicate that this sub was hit with a torpedo.
     */
    public function wasKilled () :void
    {
        _dead = true;
        _deaths++;
        _totalDeaths++;
        _queuedActions.length = 0; // drop any queued actions
        updateVisual();
        updateDeath();
        _respawnTicks = AUTO_RESPAWN_TICKS;
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
        alpha = _dead ? 0 : 1;
        // fucking label doesn't alpha out.. so we need to add or remove it
        if (_dead != (_nameLabel.parent == null)) {
            if (_dead) {
                removeChild(_nameLabel);
            } else {
                addChild(_nameLabel);
            }
        }
        _avatar.gotoAndStop(orientToFrame());

        _statsLabel.text = "" + _totalKills + " " + _totalDeaths;
    }

    protected function orientToFrame () :int
    {
        switch (_orient) {
        case Action.DOWN:
        default:
            return 1;

        case Action.LEFT:
            return 2;

        case Action.UP:
            return 3;

        case Action.RIGHT:
            return 4;
        }
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

    protected var _totalKills :int;
    protected var _totalDeaths :int;

    /** A count of how long until we respawn. */
    protected var _respawnTicks :int;

    /** The movie clip that represents us. */
    protected var _avatar :MovieClip;

    /** Our shooty-shoot sound. */
    protected var _shootSound :Sound;

    protected var _nameLabel :TextField;
    protected var _statsLabel :TextField;

    /** Color schemes for each player. */
    protected static const SCHEMES :Array = [
        [ 1, .5, 0],
        [ .5, 0, 1],
        [ 0, 1, .5],
        [ 1, 0, 1],
        [ 0, 1, 1],
        [ 1, 1, 0],
        [ 1, .5, 1],
        [ .5, 1, 1]
    ];

    /** The maximum number of torpedos that may be in-flight at once. */
    protected static const MAX_TORPEDOS :int = 2;

    /** The number of pixels to raise the name above the sprite. */
    protected static const NAME_PADDING :int = 3;

    /** The number of ticks that may elapse before we're auto-respawned. */
    protected static const AUTO_RESPAWN_TICKS :int = 100;

    [Embed(source="trucks_recolor.swf#animations")]
    protected static const AVATAR :Class;

    //[Embed(source="shooting.wav", mimeType="audio/wav")]
    [Embed(source="Error.mp3")]
    protected static const SHOOT_SOUND :Class;
}
}
