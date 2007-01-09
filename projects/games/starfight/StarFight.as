package {

import flash.display.Sprite;
import flash.display.Shape;
import flash.display.MovieClip;

import mx.core.MovieClipAsset;

import flash.utils.ByteArray;

import flash.external.ExternalInterface;

import flash.events.KeyboardEvent;
import flash.events.TimerEvent;

import flash.utils.Timer;
import flash.utils.getTimer;

import com.threerings.ezgame.EZGameControl;
import com.threerings.ezgame.MessageReceivedEvent;
import com.threerings.ezgame.MessageReceivedListener;
import com.threerings.ezgame.PropertyChangedEvent;
import com.threerings.ezgame.PropertyChangedListener;
import com.threerings.ezgame.StateChangedEvent;
import com.threerings.ezgame.StateChangedListener;

/**
 * The main game class for the client.
 */
[SWF(width="800", height="530")]
public class StarFight extends Sprite
    implements PropertyChangedListener, MessageReceivedListener
{
    public static const WIDTH :int = 800;
    public static const HEIGHT :int = 530;

    /** How often we send updates to the server. */
    public static const FRAMES_PER_UPDATE :int = 3;

    /**
     * Constructs our main view area for the game.
     */
    public function StarFight ()
    {
        _gameCtrl = new EZGameControl(this);
        _gameCtrl.registerListener(this);

        _boardLayer = new Sprite();
        _shipLayer = new Sprite();
        _shotLayer = new Sprite();
        _statusLayer = new Sprite();
        addChild(_boardLayer);
        addChild(_shipLayer);
        addChild(_shotLayer);
        addChild(_statusLayer);

        var mask :Shape = new Shape();
        addChild(mask);
        mask.graphics.clear();
        mask.graphics.beginFill(0xFFFFFF);
        mask.graphics.drawRect(0, 0, WIDTH, HEIGHT);
        mask.graphics.endFill();
        this.mask = mask;

        graphics.beginFill(Codes.BLACK);
        graphics.drawRect(0, 0, StarFight.WIDTH, StarFight.HEIGHT);

        _lastTickTime = getTimer();

        _statusLayer.addChild(_status = new StatusOverlay());

        setGameObject();
        log("Created Game Controller");
    }

    /**
     * For debug logging.
     */
    public function log (msg :String) :void
    {
        Logger.log(msg);
    }

    // from Game
    public function setGameObject () :void
    {
        log("Got game object");
        // set up our listeners
        _gameCtrl.addEventListener(StateChangedEvent.GAME_STARTED, gameStarted);

        _gameCtrl.localChat("Welcome to Zyraxxus!");

        var boardObj :Board;
        var boardBytes :ByteArray =  ByteArray(_gameCtrl.get("board"));
        if (boardBytes != null) {
            boardObj = new Board(0, 0, false);
            boardBytes.position = 0;
            boardObj.readFrom(boardBytes);
        }

        // We don't already have a board and we're the host?  Create it and our
        //  initial ship array too.
        if ((boardObj == null) && (_gameCtrl.getMyIndex() == 0)) {
            boardObj = new Board(50, 50, true);
            _gameCtrl.set("ship", new Array(_gameCtrl.getPlayerCount()));
            _gameCtrl.set("powerup", new Array(MAX_POWERUPS));
            _gameCtrl.set("board", boardObj.writeTo(new ByteArray()));
        }

        // If we now have ourselves a board, do something with it, otherwise
        //  wait til we hear from the EZGame object.
        if (boardObj != null) {
            gotBoard(boardObj);
        }
    }

    /**
     * Do some initialization based on a received board.
     */
    protected function gotBoard (boardObj :Board) :void
    {
        _ships = [];
        _shots = [];
        _powerups = [];

        _board = new BoardSprite(boardObj, _ships, _powerups);
        _boardLayer.addChild(_board);

        // Create our local ship and center the board on it.
        _ownShip = new ShipSprite(_board, this, false, _gameCtrl.getMyIndex());
        _ownShip.setPosRelTo(_ownShip.boardX, _ownShip.boardY);
        _board.setAsCenter(_ownShip.boardX, _ownShip.boardY);
        _shipLayer.addChild(_ownShip);

        // Add ourselves to the ship array.
        _gameCtrl.set("ship", _ownShip.writeTo(new ByteArray()),
            _gameCtrl.getMyIndex());

        // Set up our initial ship sprites.
        var gameShips :Array = (_gameCtrl.get("ship") as Array);

        // The game already has some ships, create sprites for em.
        if (gameShips != null) {
            for (var ii :int = 0; ii < gameShips.length; ii++)
            {
                if (gameShips[ii] == null) {
                    _ships[ii] = null;
                } else if (ii != _gameCtrl.getMyIndex()) {
                    _ships[ii] = new ShipSprite(_board, this, true, ii);
                    gameShips[ii].position = 0;
                    _ships[ii].readFrom(gameShips[ii]);
                    _shipLayer.addChild(_ships[ii]);
                }
            }
        }

        // Set up our initial powerups.
        var gamePows :Array = (_gameCtrl.get("powerup") as Array);

        // The game already has some ships, create sprites for em.
        if (gamePows != null) {
            for (var pp :int = 0; pp < gamePows.length; pp++)
            {
                if (gamePows[pp] == null) {
                    _powerups[pp] = null;
                } else {
                    _powerups[pp] = new Powerup(0, 0, 0);
                    gamePows[pp].position = 0;
                    _powerups[pp].readFrom(gamePows[pp]);
                    Logger.log("Adding powerup child");
                    _board.powerupLayer.addChild(_powerups[pp]);
                }
            }
        }

        // The first player is in charge of adding powerups.
        if (_gameCtrl.getMyIndex() == 0) {
            addPowerup(null);
            var timer :Timer = new Timer(20000, 0);
            timer.addEventListener(TimerEvent.TIMER, addPowerup);
            timer.start();
        }

        _ships[_gameCtrl.getMyIndex()] = _ownShip;

        // Our ship is interested in keystrokes.
        _gameCtrl.addEventListener(KeyboardEvent.KEY_DOWN, _ownShip.keyPressed);
        _gameCtrl.addEventListener(KeyboardEvent.KEY_UP, _ownShip.keyReleased);

        // Set up our ticker that will control movement.
        var screenTimer :Timer = new Timer(REFRESH_RATE, 0); // As fast as possible.
        screenTimer.addEventListener(TimerEvent.TIMER, tick);
        screenTimer.start();
    }

    /**
     * Tells everyone about a new powerup.
     */
    public function addPowerup (event :TimerEvent) :void
    {
        for (var ii :int = 0; ii < MAX_POWERUPS; ii++) {
            if (_powerups[ii] == null) {
                var x :int = Math.random() * _board.boardWidth;
                var y :int = Math.random() * _board.boardHeight;

                while (_board.getCollision(x+0.5, y+0.5, x+0.5, y+0.5,
                           0.1, -1) ||
                    (_board.getPowerupIdx(x+0.5, y+0.5, x+0.5, y+0.5,
                        0.1) != -1)) {
                    x = Math.random() * _board.boardWidth;
                    y = Math.random() * _board.boardHeight;
                }

                _powerups[ii] = new Powerup(1+Math.random()*3, x, y);

                _gameCtrl.set("powerup", _powerups[ii].writeTo(new ByteArray()),
                    ii);
                Logger.log("Adding powerup child");
                _board.powerupLayer.addChild(_powerups[ii]);
                return;
            }
        }

        // If we're all full up, don't do anything.
    }

    public function removePowerup (idx :int) :void
    {
        _gameCtrl.set("powerup", null, idx);
        _board.powerupLayer.removeChild(_powerups[idx]);
        _powerups[idx] = null;
    }

    // from PropertyChangedListener
    public function propertyChanged (event :PropertyChangedEvent) :void
    {
        var name :String = event.name;
        if (name == "board" && (_board == null)) {
            log("Got a board change");
            // Someone else initialized our board.
            var boardBytes :ByteArray =  ByteArray(_gameCtrl.get("board"));
            var boardObj :Board = new Board(0, 0, false);
            boardBytes.position = 0;
            boardObj.readFrom(boardBytes);
            gotBoard(boardObj);
        } else if ((name == "ship") && (event.index >= 0)) {
            if (_ships != null && event.index != _gameCtrl.getMyIndex()) {
                // Someone else's ship - update our sprite for em.
                // TODO: Something to try to deal with latency and maybe smooth
                //  any shifts that occur.
                var ship :ShipSprite = _ships[event.index];
                if (ship == null) {
                    _ships[event.index] =
                        ship = new ShipSprite(_board, this, true, event.index);
                    _shipLayer.addChild(ship);
                }
                var bytes :ByteArray = ByteArray(event.newValue);
                bytes.position = 0;
                var sentShip :ShipSprite = new ShipSprite(_board, this, true, event.index);
                sentShip.readFrom(bytes);
                ship.updateForReport(sentShip);
            }
        } else if ((name =="powerup") && (event.index >= 0)) {
            if (_powerups != null) {
                if (event.newValue == null) {
                    if (_powerups[event.index] != null) {
                        _board.powerupLayer.removeChild(
                            _powerups[event.index]);
                        _powerups[event.index] = null;
                    }
                    return;
                }

                var pow :Powerup = _powerups[event.index];
                if (pow == null) {
                    _powerups[event.index] =
                        pow = new Powerup(0, 0, 0);
                    _board.powerupLayer.addChild(pow);
                }
                var pBytes :ByteArray = ByteArray(event.newValue);
                pBytes.position = 0;
                pow.readFrom(pBytes);
            }
        }
    }

    // from MessageReceivedListener
    public function messageReceived (event :MessageReceivedEvent) :void
    {
        if (event.name == "shot") {
            var val :Array = (event.value as Array);
            addShot(new ShotSprite(val[0], val[1], val[2], val[3], val[4],
                        this));

            if (val[5] == ShotSprite.SPREAD) {
                addShot(new ShotSprite(val[0], val[1], val[2],
                            val[3] + ShotSprite.SPREAD_FACT, val[4],
                            this));
                addShot(new ShotSprite(val[0], val[1], val[2],
                            val[3] - ShotSprite.SPREAD_FACT, val[4],
                            this));
            }
        } else if (event.name == "explode") {
            var arr :Array = (event.value as Array);
            _board.explode(arr[0], arr[1], arr[2], false);
            if (arr[3] == _ownShip.shipId) {
                _status.addScore(KILL_PTS);
            }
        }
    }

    protected function addShot (shot :ShotSprite) :void
    {
        _shots.push(shot);
        shot.setPosRelTo(_ownShip.boardX, _ownShip.boardY);
        _shotLayer.addChild(shot);
    }

    /**
     * Register that a ship was hit at the location.
     */
    public function hitShip (ship :ShipSprite, x :Number, y :Number,
        shooterId :int) :void
    {
        _board.explode(x, y, 0, true);
        if (ship == _ownShip) {
            ship.hit(shooterId);
            _status.setPower(ship.power);
        } else if (shooterId == _ownShip.shipId) {
            // We hit someone!  Give us some points.
            _status.addScore(HIT_PTS);
        }
    }

    /**
     * Register that an obstacle was hit.
     */
    public function hitObs (obs :Obstacle, x :Number, y :Number) :void
    {
        _board.explode(x, y, 0, true);
    }


    /**
     * The game has started - do our initial startup.
     */
    protected function gameStarted (event :StateChangedEvent) :void
    {
        log("Game started");
        _gameCtrl.localChat("GO!!!!");
    }

    /**
     * Send a message to the server about our shot.
     */
    public function fireShot (x :Number, y :Number,
        vel :Number, angle :Number, shipId :int, type :int) :void
    {
        var args :Array = new Array(5);
        args[0] = x;
        args[1] = y;
        args[2] = vel;
        args[3] = angle;
        args[4] = shipId;
        args[5] = type;
        _gameCtrl.sendMessage("shot", args);
    }

    /**
     * Register a big ole' explosion at the location.
     */
    public function explode (x :Number, y :Number, rot :int,
        shooterId :int) :void
    {
        var args :Array = new Array(3);
        args[0] = x;
        args[1] = y;
        args[2] = rot;
        args[3] = shooterId;
        _gameCtrl.sendMessage("explode", args);
    }

    /**
     * When our screen updater timer ticks...
     */
    public function tick (event :TimerEvent) :void
    {
        var now :int = getTimer();
        var time :Number = (now - _lastTickTime)/REFRESH_RATE;

        var ownOldX :Number = _ownShip.boardX;
        var ownOldY :Number = _ownShip.boardY;

        // Update all ships.
        for each (var ship :ShipSprite in _ships) {
            if (ship != null) {
                ship.tick(time);
            }
        }

        // And then shift em based on ownship's new pos.
        for each (ship in _ships) {
            if (ship != null) {
                ship.setPosRelTo(_ownShip.boardX, _ownShip.boardY);
            }
        }

        var powIdx :int = _board.getPowerupIdx(ownOldX, ownOldY,
            _ownShip.boardX, _ownShip.boardY, ShipSprite.COLLISION_RAD);
        while (powIdx != -1) {
            _ownShip.awardPowerup(_powerups[powIdx].type);
            _status.addScore(POWERUP_PTS);
            removePowerup(powIdx);

            powIdx = _board.getPowerupIdx(ownOldX, ownOldY,
                _ownShip.boardX, _ownShip.boardY, ShipSprite.COLLISION_RAD);
        }

        // Recenter the board on our ship.
        _board.setAsCenter(_ownShip.boardX, _ownShip.boardY);
        _status.setPowerups(_ownShip.powerups);

        // Update all live shots.
        var completed :Array = []; // Array<ShotSprite>
        for each (var shot :ShotSprite in _shots) {
            if (shot != null) {
                shot.tick(_board, time);
                if (shot.complete) {
                    completed.push(shot);
                }
                shot.setPosRelTo(_ownShip.boardX, _ownShip.boardY);
            }
        }

        // Remove any that were done.
        for each (shot in completed) {
            _shots.splice(_shots.indexOf(shot), 1);
            _shotLayer.removeChild(shot);
        }

        // Every few frames, broadcast our status to everyone else.
        if (_updateCount++ % FRAMES_PER_UPDATE == 0) {
            _gameCtrl.set("ship", _ownShip.writeTo(new ByteArray()),
                _gameCtrl.getMyIndex());
        }

        _lastTickTime = now;
    }

    /** Our game control object. */
    protected var _gameCtrl :EZGameControl;

    /** Our local ship. */
    protected var _ownShip :ShipSprite;

    /** All the ships. */
    protected var _ships :Array; // Array<ShipSprite>

    /** All the active powerups. */
    protected var _powerups :Array; // Array<Powerup>

    /** Live shots. */
    protected var _shots :Array; // Array<ShotSprite>

    /** The board with all its obstacles. */
    protected var _board :BoardSprite;

    /** Status info. */
    protected var _status :StatusOverlay;

    /** How many frames its been since we broadcasted. */
    protected var _updateCount :int = 0;

    protected var _lastTickTime :int;

    protected var _boardLayer :Sprite;
    protected var _shipLayer :Sprite;
    protected var _shotLayer :Sprite;
    protected var _statusLayer :Sprite;

    /** Constants to control update frequency. */
    protected static const REFRESH_RATE :int = 50;

    /** This could be more dynamic. */
    protected static const MAX_POWERUPS :int = 10;

    /** Points for various things in the game. */
    protected static const POWERUP_PTS :int = 25;
    protected static const HIT_PTS :int = 10;
    protected static const KILL_PTS :int = 50;
}
}
