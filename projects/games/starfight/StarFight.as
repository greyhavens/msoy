package {

import flash.display.Sprite;
import flash.display.Shape;
import flash.display.MovieClip;

import flash.utils.ByteArray;

import flash.external.ExternalInterface;

import flash.events.KeyboardEvent;
import flash.events.TimerEvent;

import flash.utils.Timer;

import com.threerings.ezgame.Game;
import com.threerings.ezgame.EZGame;
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
    implements Game, PropertyChangedListener
{
    public static const WIDTH :int = 800;
    public static const HEIGHT :int = 530;

    /**
     * Constructs our main view area for the game.
     */
    public function StarFight ()
    {
        var mask :Shape = new Shape();
        addChild(mask);
        mask.graphics.clear();
        mask.graphics.beginFill(0xFFFFFF);
        mask.graphics.drawRect(0, 0, WIDTH, HEIGHT);
        mask.graphics.endFill();
        this.mask = mask;

        log("Created Game Object");
    }

    /**
     * For debug logging.
     */
    public function log (msg :String) :void
    {
        Logger.log(msg);
    }

    // from Game
    public function setGameObject (gameObj :EZGame) :void
    {
        log("Got game object");
        // set up our listeners
        _gameObj = gameObj;
        _gameObj.addEventListener(StateChangedEvent.GAME_STARTED, gameStarted);

        _gameObj.localChat("Welcome to <game to be named later>!");

        var boardObj :Board;
        var boardBytes :ByteArray =  ByteArray(_gameObj.get("board"));
        if (boardBytes != null) {
            boardObj = new Board(0, 0, false);
            boardBytes.position = 0;
            boardObj.readFrom(boardBytes);
        }

        // We don't already have a board and we're the host?  Create it and our
        //  initial ship array too.
        if ((boardObj == null) && (_gameObj.getMyIndex() == 0)) {
            boardObj = new Board(50, 50, true);
            _gameObj.set("ship", new Array(2));
            _gameObj.set("board", boardObj.writeTo(new ByteArray()));
        }

        // If we now have ourselves a board, do something with it, otherwise
        //  wait til we hear from the EZGame object.
        if (boardObj != null) {
            gotBoard(boardObj);
        }

        // Our ship is interested in keystrokes.
        stage.addEventListener(KeyboardEvent.KEY_DOWN, _ownShip.keyPressed);
        stage.addEventListener(KeyboardEvent.KEY_UP, _ownShip.keyReleased);
    }

    /**
     * Do some initialization based on a received board.
     */
    protected function gotBoard (boardObj :Board) :void
    {
        _board = new BoardSprite(boardObj);
        addChild(_board);

        // Create our local ship and center the board on it.
        _ownShip = new ShipSprite(_board, false);
        _ownShip.setPosRelTo(_ownShip.boardX, _ownShip.boardY);
        _board.setAsCenter(_ownShip.boardX, _ownShip.boardY);
        addChild(_ownShip);

        // Add ourselves to the ship array.
        _gameObj.set("ship", _ownShip.writeTo(new ByteArray()),
            _gameObj.getMyIndex());

        // Set up our initial ship sprites.
        var gameShips :Array = (_gameObj.get("ship") as Array);
        _ships = [];

        // The game already has some ships, create sprites for em.
        if (gameShips != null) {
            for (var ii :int = 0; ii < gameShips.length; ii++)
            {
                if (gameShips[ii] == null) {
                    _ships[ii] = null;
                } else {
                    _ships[ii] = new ShipSprite(_board, true);
                    gameShips[ii].position = 0;
                    _ships[ii].readFrom(gameShips[ii]);
                }
            }
        }

        _ships[_gameObj.getMyIndex()] = _ownShip;

        // Set up our ticker that will control movement.
        var screenTimer :Timer = new Timer(REFRESH_RATE, 0);
        screenTimer.addEventListener(TimerEvent.TIMER, tick);
        screenTimer.start();
    }

    // from PropertyChangedListener
    public function propertyChanged (event :PropertyChangedEvent) :void
    {
        var name :String = event.name;
        if (name == "board" && (_board == null)) {
            log("Got a board change");
            // Someone else initialized our board.
            var boardBytes :ByteArray =  ByteArray(_gameObj.get("board"));
            var boardObj :Board = new Board(0, 0, false);
            boardBytes.position = 0;
            boardObj.readFrom(boardBytes);
            gotBoard(boardObj);
        } else if ((name == "ship") && (event.index >= 0)) {
            if (_ships != null && event.index != _gameObj.getMyIndex()) {
                // Someone else's ship - update our sprite for em.
                // TODO: Something to try to deal with latency and maybe smooth
                //  any shifts that occur.
                var ship :ShipSprite = _ships[event.index];
                if (ship == null) {
                    _ships[event.index] = ship = new ShipSprite(_board, true);
                    addChild(ship);
                }
                var bytes :ByteArray = ByteArray(event.newValue);
                bytes.position = 0;
                ship.readFrom(bytes);
            }
        }
    }

    /**
     * The game has started - do our initial startup.
     */
    protected function gameStarted (event :StateChangedEvent) :void
    {
        log("Game started");
        _gameObj.localChat("GO!!!!");
    }

    /**
     * When our screen updater timer ticks...
     */
    public function tick (event :TimerEvent) :void
    {
        for each (var ship :ShipSprite in _ships) {
            if (ship != null) {
                ship.tick();
                ship.setPosRelTo(_ownShip.boardX, _ownShip.boardY);
            }
        }

        // Recenter the board on our ship.
        _board.setAsCenter(_ownShip.boardX, _ownShip.boardY);

        // Every few frames, broadcast our status to everyone else.
        if (_updateCount++ % FRAMES_PER_UPDATE == 0) {
            _gameObj.set("ship", _ownShip.writeTo(new ByteArray()),
                _gameObj.getMyIndex());
        }
        
    }

    /** The game data. */
    protected var _gameObj :EZGame;

    /** Our local ship. */
    protected var _ownShip :ShipSprite;

    /** All the ships. */
    protected var _ships :Array; // Array<ShipSprite>

    /** The board with all its obstacles. */
    protected var _board :BoardSprite;

    /** How many frames its been since we broadcasted. */
    protected var _updateCount :int = 0;

    /** Constants to control update frequency. */
    protected static const REFRESH_RATE :int = 50;
    protected static const FRAMES_PER_UPDATE :int = 2;
}
}
