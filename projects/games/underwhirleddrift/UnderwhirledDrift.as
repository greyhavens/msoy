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
            var config :Object = _control.getConfig();
            _numLaps = config["number of laps"] != null ? config["number of laps"] : 4;
            _numRounds = config["number of rounds"] != null ? config["number of rounds"] : 3;

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
                        _lightBoard.y -= 5;
                        if (_control.amInControl()) {
                            // base the real race start on the controller's clock
                            _control.sendMessage("raceStarted", true);
                        }
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
        } else if (event.name == "raceStarted") {
            _raceStarted = true;
        } else if (event.name == "positionUpdate") {
            // variables not being scoped directly to all blocks is really wacky
            playerId = event.value.playerId;
            if (playerId != _control.getMyId()) {
                var oppKart :Object = _opponentKarts.get(playerId);
                if (oppKart is KartObstacle) {
                    (oppKart as KartObstacle).setPosition(event.value);
                }
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
        } else if (event.name == "crossedFinishLine") {
            var currentLaps :int = event.value.direction;
            if (_playerLaps.containsKey(event.value.playerId)) {
                currentLaps += _playerLaps.get(event.value.playerId);
            }
            if (currentLaps < _numLaps + 1 && 
                !ArrayUtil.contains(_playersFinished, event.value.playerId)) {
                _playerLaps.put(event.value.playerId, currentLaps);
                if (event.value.playerId == _control.getMyId() && event.value.direction > 0) {
                    if (currentLaps < _numLaps) {
                        _control.localChat("You are on lap " + currentLaps);
                    } else {
                        _control.localChat("You are on your final lap!");
                    }
                }
            } else if (currentLaps == _numLaps + 1) {
                _playerLaps.remove(event.value.playerId);
                _playersFinished.push(event.value.playerId);
                if (_control.amInControl()) {
                    var place :String;
                    if (_playersFinished.length == 1) {
                        place = "1st";
                    } else if (_playersFinished.length == 2) {
                        place = "2nd";
                    } else if (_playersFinished.length == 3) {
                        place = "3rd";
                    } else {
                        place = _playersFinished.length + "th";
                    }
                    _control.sendChat(_control.getOccupantName(event.value.playerId) + 
                        " finished in " + place + " place!");
                    // let the player's controller give that player some flow... this decided here
                    // because the controller is the arbiter of ties
                    _control.sendMessage("awardFlow", 1 - (_playersFinished.length - 1) / 
                        _control.seating.getPlayerIds().length, event.value.playerId);
                    if (_playerLaps.size() == 0) {
                        if (_currentLevel == _numRounds - 1) {
                            _control.sendMessage("gameComplete", true);
                        } else {
                            _control.sendMessage("trackComplete", true);
                        }
                    }
                }
            } 
        } else if (event.name == "awardFlow") {
            var flow :int = Math.ceil(_control.getAvailableFlow() * (event.value as Number));
            _control.awardFlow(flow);
            _control.localChat("You were awarded " + flow + " flow for your performance!");
        } else if (event.name == "trackComplete") {
            _control.localChat("The current track has been completed by all players... now " +
                "loading the next track!");
            _raceStarted = false;
            _kart.killMovement();
            _currentLevel++;
            _level = LevelFactory.createLevel(_currentLevel % LevelFactory.TOTAL_LEVELS, _ground);
            _gameSprite.removeChild(_horizon);
            _gameSprite.addChildAt(_horizon = new Horizon(_level.horizon, _camera), 0);
            _horizon.y += _horizon.height / 2;
            _horizon.x += _horizon.width / 2;
            _level.setStartingPosition(0);
            _ground.getScenery().registerKart(_kart);
            _playerLaps.clear();
            _playersFinished = [];
            if (_bonus != null) {
                _kart.destroyBonus();
            }
            for each (var key :int in _opponentKarts.keys()) {
                _opponentKarts.put(key, _opponentKarts.get(key).kartType);
            }
            if (_control.amInControl()) {
                var playerIds :Array = _control.seating.getPlayerIds();
                // assign everyone a new starting position.
                ArrayUtil.shuffle(playerIds);
                for (ii = 0; ii < playerIds.length; ii++) {
                    playerIds[ii] = { id: playerIds[ii], position: ii };
                }
                _control.sendMessage("playerPositions", playerIds);
            }
        } else if (event.name == "gameComplete") {
            _control.localChat("The game is complete!  Head back to the game lobby for more " + 
                "Undewhirled Drift action!");
        } else if (event.name == "shieldsUp") {
            if (event.value.playerId == _control.getMyId()) {
                _kart.shieldsUp(event.value.up);
            } else {
                _opponentKarts.get(event.value.playerId).shieldsUp(event.value.up);
            }
        } else if (event.name == "fireball") {
            _ground.getScenery().shootFireball(new Point(event.value.x, event.value.y), 
                event.value.angle, event.value.playerId == _control.getMyId());
        } else if (event.name == "bonusGone") {
            _ground.getScenery().removeBonus(new Point(event.value.x, event.value.y));
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
        _kart.addEventListener(KartEvent.CROSSED_FINISH_LINE, function (event :KartEvent) :void {
            if (_control.seating.getPlayerIds().length != 1) {
                _control.sendMessage("crossedFinishLine", { playerId: _control.getMyId(), 
                    direction: event.value });
            }
        });
        _kart.addEventListener(KartEvent.BONUS, function (event :KartEvent) :void {
            var bonus :Bonus = event.value.bonus as Bonus;
            if (bonus != _bonus) {
                _bonus = event.value.bonus as Bonus;
                _bonus.x = _bonus.width;
                _bonus.y = _bonus.height;
                addChild(_bonus);
            }
            _control.sendMessage("bonusGone", { x: event.value.pos.x, y: event.value.pos.y });
        });
        _kart.addEventListener(KartEvent.REMOVE_BONUS, function (event :KartEvent) :void {
            if (_bonus != null) {
                removeChild(_bonus);
                _bonus = null;
            }
        });
        _kart.addEventListener(KartEvent.SHIELD, function (event :KartEvent) :void {
            _control.sendMessage("shieldsUp", { playerId: _control.getMyId(), up: event.value });
        });
        _kart.addEventListener(KartEvent.FIREBALL, function (event :KartEvent) :void {
            _control.sendMessage("fireball", { playerId: _control.getMyId(), x: 
                _ground.getKartLocation().x, y: _ground.getKartLocation().y, 
                angle: _camera.angle - Math.PI / 2 });
        });
        // now that we've set the kart, add the power up frame
        var powerUpFrame :Sprite = new POWER_UP_FRAME();
        powerUpFrame.x = powerUpFrame.width;
        powerUpFrame.y = powerUpFrame.height;
        addChild(powerUpFrame);
    }

    /**
     * TODO: The conditions under which this method is called are too complicated.  This needs 
     * to be rethought.
     */
    protected function updateRaceStarted () :void
    {
        if (_control.seating.getPlayerIds().length == 1) {
            _raceStarted = true;
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
            // not checking character codes and key codes in the same switch is complete bullshit.  
            // flash should be capable of letting me check both at once
            var c :int = event.charCode;
            if (c >= "a".charCodeAt(0) && c <= "z".charCodeAt(0)) {
                switch (c) {
                case "f".charCodeAt(0):
                    if (event.type == KeyboardEvent.KEY_DOWN) {
                        _kart.activateBonus();
                    }
                    break;
                case "n".charCodeAt(0):
                    if (event.type == KeyboardEvent.KEY_DOWN && 
                            // only allow this in single-player mode
                            _control.seating.getPlayerIds().length == 1) {
                        _currentLevel++;
                        _level = LevelFactory.createLevel(_currentLevel % LevelFactory.TOTAL_LEVELS,
                            _ground);
                        _gameSprite.removeChild(_horizon);
                        _gameSprite.addChildAt(_horizon = new Horizon(_level.horizon, _camera), 0);
                        _horizon.y += _horizon.height / 2;
                        _horizon.x += _horizon.width / 2;
                        _level.setStartingPosition(0);
                        _ground.getScenery().registerKart(_kart);
                        if (_bonus != null) {
                        _kart.destroyBonus();
                        }
                    }
                    break;
                }
            } else {
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
                }
            }
        }
    }

    protected static const SEND_THROTTLE :int = 150; // in ms
    
    [Embed(source='rsrc/light_board.swf#light_board')]
    protected static const LIGHT_BOARD :Class;

    [Embed(source='rsrc/power_ups.swf#power_up_frame')]
    protected static const POWER_UP_FRAME :Class;

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
    protected var _playerLaps :HashMap = new HashMap();
    protected var _playersFinished :Array = [];

    /** A flag to indicate that the race has started, so its safe to send position update */
    protected var _raceStarted :Boolean = false;

    protected var _horizon :Horizon;
    protected var _gameSprite :Sprite;
    protected var _camera :Camera;

    protected var _lightBoard :MovieClipAsset;

    protected var _bonus :Bonus;

    protected var _numLaps :int;
    protected var _numRounds :int;
}
}
