package {

import flash.display.Sprite;
import flash.display.Shape;

import flash.geom.Point;

import flash.events.KeyboardEvent;
import flash.events.Event;

import flash.ui.Keyboard;

import flash.text.TextField;
import flash.text.TextFieldAutoSize;

import mx.core.MovieClipAsset;

import com.threerings.ezgame.StateChangedListener;
import com.threerings.ezgame.StateChangedEvent;
import com.threerings.ezgame.MessageReceivedListener;
import com.threerings.ezgame.MessageReceivedEvent;

import com.threerings.util.ArrayUtil;
import com.threerings.util.HashMap;

import com.whirled.WhirledGameControl;

[SWF(width="711", height="400")]
public class UnderwhirledDrift extends Sprite
    implements StateChangedListener, MessageReceivedListener
{
    /** width of the masked display */
    public static const DISPLAY_WIDTH :int = 711;

    /** height of the masked display */
    public static const DISPLAY_HEIGHT :int = 400;

    /** height of the sky */
    public static const SKY_HEIGHT :int = DISPLAY_HEIGHT * 0.35;

    /** Kart location, relative to the ground coordinates */
    public static const KART_LOCATION :Point = new Point (355, 200);

    /** Kart offset from its effective location */
    public static const KART_OFFSET :int = 32;

    public static const KEY_EVENT :String = "keyEvent";

    public function UnderwhirledDrift ()
    {
        _gameSprite = new Sprite();
        var masker :Shape = new Shape();
        masker.graphics.beginFill(0xFFFFFF);
        masker.graphics.drawRect(0, 0, DISPLAY_WIDTH, DISPLAY_HEIGHT);
        masker.graphics.endFill();
        _gameSprite.mask = masker;
        _gameSprite.addChild(masker);
        addChild(_gameSprite);

        _control = new WhirledGameControl(this);
        if (_control.isConnected()) {
            _camera = new Camera();

            _ground = new Ground(_camera);
            _ground.y = SKY_HEIGHT;
            _gameSprite.addChild(_ground);

            _level = LevelFactory.createLevel(_currentLevel = 0, _ground);
            _gameSprite.addChildAt(_horizon = new Horizon(_level.horizon, _camera), 0);
            _horizon.y += _horizon.height / 2;
            _horizon.x += _horizon.width / 2;

            _control.registerListener(this);
            _control.addEventListener(KeyboardEvent.KEY_DOWN, keyEventHandler);
            _control.addEventListener(KeyboardEvent.KEY_UP, keyEventHandler);
    
            var chooser :KartChooser = new KartChooser(this, _gameSprite, _camera, _ground);
            chooser.chooseKart();

            // names above characters is good, but they should fade out after the race 
            // starts
            /*
            var nameText :TextField = new TextField();
            nameText.text = _control.getOccupantName(_control.getMyId());
            nameText.selectable = false;
            nameText.autoSize = TextFieldAutoSize.CENTER;
            nameText.scaleX = nameText.scaleY = 2.5;
            nameText.x = _kart.x - nameText.width / 2;
            nameText.y = _kart.y - _kart.height - 5;
            addChild(nameText);*/
        } else {
            // TODO: Display some kind of splash screen.
        }
    }

    // from StateChangedListener
    public function stateChanged (event :StateChangedEvent) :void
    {
        if (event.type == StateChangedEvent.GAME_STARTED) {
            var playerIds :Array = _control.seating.getPlayerIds();
            if (_control.amInControl() || playerIds.length == 1) {
                // assign everyone a starting position.
                ArrayUtil.shuffle(playerIds);
                for (var ii :int = 0; ii < playerIds.length; ii++) {
                    playerIds[ii] = { id: playerIds[ii], position: ii };
                }
                _control.sendMessage("playerPositions", playerIds);
            }
            if (_control.amInControl()) {
                // start the ticker that sets up to sending out network updates
                _control.startTicker("updateTick", SEND_THROTTLE);
            }
        }
    }

    // from MessageReceivedListener
    public function messageReceived (event :MessageReceivedEvent) :void
    {
        if (event.name == "updateTick") {
            if (_kart != null) {
                var updateObj :Object = _kart.getUpdate();
                updateObj.playerId = _control.getMyId();
                _control.sendMessage("positionUpdate", updateObj);
            }
        } else if (event.name == "playerPositions") {
            var playerPositions :Array = event.value as Array;
            for (var ii: int = 0; ii < playerPositions.length; ii++) {
                if (playerPositions[ii].id == _control.getMyId()) {
                    _level.setStartingPosition(playerPositions[ii].position);
                } else {
                    var playerId :int = playerPositions[ii].id;
                    var position :int = playerPositions[ii].position;
                    var kartType :String = _opponentKarts.get(playerId);
                    if (kartType != null) {
                        _opponentKarts.put(playerId, _level.addOpponentKart(position, kartType));
                    } else {
                        _opponentKarts.put(playerId, position);
                    }
                }
            }
            if (playerPositions.length != 1) {
                updateRaceStarted();
            }
        } else if (event.name == "startRace") {
            _lightBoard = new LIGHT_BOARD();
            var boardTravelHeight :int = _lightBoard.height;
            var boardTravelStart :int = -boardTravelHeight / 2;
            _lightBoard.x = DISPLAY_WIDTH / 2;
            _lightBoard.y = boardTravelStart;
            var boardFrameListener :Function = function (evt :Event) :void {
                if (_lightBoard.currentFrame == _lightBoard.totalFrames) {
                    if (_lightBoard.y == boardTravelStart) {
                        _lightBoard.stop();
                        _raceStarted = true;
                        _lightBoard.y -= 5;
                    } else {
                        if (_lightBoard.y > boardTravelStart - boardTravelHeight) {
                            _lightBoard.y -= 5;
                        } else {
                            _lightBoard.removeEventListener(Event.ENTER_FRAME, boardFrameListener);
                        } 
                    }
                }
            };
            _lightBoard.addEventListener(Event.ENTER_FRAME, boardFrameListener);
            addChild(_lightBoard);
        } else if (event.name == "positionUpdate") {
            // variables not being scoped directly to all blocks is really wacky
            playerId = event.value.playerId;
            if (playerId != _control.getMyId()) {
                (_opponentKarts.get(playerId) as KartObstacle).setPosition(event.value);
            }
        } else if (event.name == "kartChosen") {
            playerId = event.value.playerId;
            if (playerId != _control.getMyId()) {
                kartType = event.value.kartType;
                var positionObj :Object = _opponentKarts.get(playerId);
                if (positionObj != null) {
                    _opponentKarts.put(playerId, _level.addOpponentKart(positionObj as int,  
                        kartType));
                } else {
                    _opponentKarts.put(playerId, kartType);
                }
                if (_kart != null) {
                    updateRaceStarted();
                }
            }
        }
    }

    public function setKart (kart :Kart) :void
    {
        _kart = kart;
        _kart.x = KART_LOCATION.x;
        // tack on a few pixels to account for the front of the kart
        _kart.y = KART_LOCATION.y + SKY_HEIGHT + KART_OFFSET;
        addChild(_kart);
        _control.sendMessage("kartChosen", {playerId: _control.getMyId(),
            kartType: _kart.kartType});
        updateRaceStarted();
    }

    /**
     * TODO: The conditions under which this method is called are too complicated.  This needs 
     * to be rethought.
     */
    protected function updateRaceStarted () :void
    {
        if (_control.seating.getPlayerIds().length == 1) {
            _control.sendMessage("startRace", true);
        } else if (_control.amInControl()) {
            var keys :Array = _opponentKarts.keys();
            for (var ii :int = 0; ii < keys.length; ii++) {
                if (!(_opponentKarts.get(keys[ii]) is KartObstacle)) {
                    break;
                }
            }
            if (ii == keys.length) {
                _control.sendMessage("startRace", true);
            }
        }
    }

    /** 
     * Handles KEY_DOWN. 
     */
    protected function keyEventHandler (event :KeyboardEvent) :void
    {
        if (_kart != null && _raceStarted) {
            switch (event.keyCode) {
            case Keyboard.UP:
                _kart.moveForward(event.type == KeyboardEvent.KEY_DOWN);
                break;
            case Keyboard.DOWN:
                _kart.moveBackward(event.type == KeyboardEvent.KEY_DOWN);
                break;
            case Keyboard.LEFT:
                _kart.turnLeft(event.type == KeyboardEvent.KEY_DOWN);
                break;
            case Keyboard.RIGHT:
                _kart.turnRight(event.type == KeyboardEvent.KEY_DOWN);
                break;
            case Keyboard.SPACE:
                if (event.type == KeyboardEvent.KEY_DOWN) {
                    _kart.jump();
                }
                break;
            case Keyboard.SHIFT:
                if (event.type == KeyboardEvent.KEY_DOWN && 
                        // only allow this in single-player mode
                        _control.seating.getPlayerIds().length == 1) {
                    _currentLevel = (_currentLevel + 1) % 2;
                    _level = LevelFactory.createLevel(_currentLevel, _ground);
                    _gameSprite.removeChild(_horizon);
                    _gameSprite.addChildAt(_horizon = new Horizon(_level.horizon, _camera), 0);
                    _horizon.y += _horizon.height / 2;
                    _horizon.x += _horizon.width / 2;
                    _level.setStartingPosition(0);
                    _ground.getScenery().registerKart(_kart);
                }
                break;
            default:
            // do nothing
            }
        }
    }

    protected static const SEND_THROTTLE :int = 150; // in ms

    [Embed(source='rsrc/light_board.swf#light_board')]
    protected static const LIGHT_BOARD :Class;

    /** the game control. */
    protected var _control :WhirledGameControl;

    /** The level object */
    protected var _level :Level;
    protected var _currentLevel :int;

    /** The kart. */
    protected var _kart :Kart;

    protected var _ground :Ground;
    
    /** A hashmap of the opponent's karts. */
    protected var _opponentKarts :HashMap = new HashMap();

    /** A flag to indicate that the race has started, so its safe to send position update */
    protected var _raceStarted :Boolean = false;

    protected var _horizon :Horizon;
    protected var _gameSprite :Sprite;
    protected var _camera :Camera;

    protected var _lightBoard :MovieClipAsset;
}
}
