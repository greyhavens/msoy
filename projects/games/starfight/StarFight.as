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

[SWF(width="800", height="530")]
public class StarFight extends Sprite
    implements Game, PropertyChangedListener
{
    public static const WIDTH :int = 800;
    public static const HEIGHT :int = 530;

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

    public static function log (msg :String) :void
    {
        ExternalInterface.call("console.debug", msg);
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
            boardObj.readFrom(boardBytes);
        }

        if (boardObj == null) {
            if (_gameObj.getMyIndex() == 0) {
                boardObj = new Board(50, 50, true);
                _gameObj.set("ship", new Array());
                _gameObj.set("board", boardObj.writeTo(new ByteArray()));
            }
        }

        var gameShips :Array = (_gameObj.get("ship") as Array);
        _ships = [];
        if (gameShips != null) {
            for (var ii :int = 0; ii < gameShips.length; ii++)
            {
                _ships[ii] = new ShipSprite(_board);
                _ships[ii].readFrom(gameShips[ii]);
            }
        }

        if (boardObj != null) {
            gotBoard(boardObj);
        }

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

        _ownShip = new ShipSprite(_board);
        _ownShip.setPosRelTo(_ownShip.boardX, _ownShip.boardY);
        
        _board.setAsCenter(_ownShip.boardX, _ownShip.boardY);
        addChild(_ownShip);

        _gameObj.set("ship", _ownShip.writeTo(new ByteArray()),
            _gameObj.getMyIndex());

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
            boardObj.readFrom(boardBytes);
            gotBoard(boardObj);
        } else if ((name == "ship") && (event.index >= 0)) {
            var ship :ShipSprite = _ships[event.index];
            if (ship == null) {
                _ships[event.index] = ship = new ShipSprite(_board);
            }
            ship.readFrom(ByteArray(event.newValue));
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
            ship.tick();
        }
        _board.setAsCenter(_ownShip.boardX, _ownShip.boardY);

        _gameObj.set("ship", _ownShip.writeTo(new ByteArray()), _gameObj.getMyIndex());
    }

    protected var _gameObj :EZGame;

    protected var _ownShip :ShipSprite;

    protected var _ships :Array;

    protected var _board :BoardSprite;

    protected static const REFRESH_RATE :int = 50;
}
}
