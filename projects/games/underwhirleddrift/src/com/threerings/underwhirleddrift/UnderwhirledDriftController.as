package com.threerings.underwhirleddrift {

import flash.events.KeyboardEvent;

import flash.geom.Point;

import flash.ui.Keyboard;

import com.threerings.ezgame.StateChangedListener;
import com.threerings.ezgame.StateChangedEvent;
import com.threerings.ezgame.MessageReceivedListener;
import com.threerings.ezgame.MessageReceivedEvent;

import com.threerings.util.ArrayUtil;
import com.threerings.util.HashMap;

import com.whirled.WhirledGameControl;

import com.threerings.underwhirleddrift.kart.Kart;
import com.threerings.underwhirleddrift.kart.KartEvent;
import com.threerings.underwhirleddrift.kart.KartObstacle;
import com.threerings.underwhirleddrift.scene.Ground;
import com.threerings.underwhirleddrift.scene.Level;
import com.threerings.underwhirleddrift.scene.LevelFactory;
import com.threerings.underwhirleddrift.scene.Bonus;

public class UnderwhirledDriftController 
    implements StateChangedListener, MessageReceivedListener
{
    // constants for player related messages
    public static const UPDATE_TICK :String = "updateTick";
    public static const POSITION_UPDATE :String = "positionUpdate";
    public static const KART_CHOSEN :String = "kartChosen";
    public static const CROSSED_FINISH_LINE :String = "crossedFinishLine";

    // constants for game control related messages
    public static const PLAYER_POSITIONS :String = "playerPositions";
    public static const START_RACE :String = "startRace";
    public static const RACE_STARTED :String = "raceStarted";
    public static const AWARD_FLOW :String = "awardFlow";
    public static const TRACK_COMPLETE :String = "trackComplete";
    public static const GAME_COMPLETE :String = "gameComplete";

    // constants for bonus related messages
    public static const SHIELDS_UP :String = "shieldsUp";
    public static const FIREBALL :String = "fireball";
    public static const BONUS_GONE :String = "bonusGone";

    public function UnderwhirledDriftController (control :WhirledGameControl, camera :Camera, 
        ground :Ground, view :UnderwhirledDriftView) 
    {
        _camera = camera;
        _ground = ground;
        _view = view;
        _control = control;
        if (_control.isConnected()) {
            var config :Object = _control.getConfig();
            _numLaps = config["number of laps"] != null ? config["number of laps"] : 4;
            _numRounds = config["number of rounds"] != null ? config["number of rounds"] : 3;

            _view.setLevel(_level = LevelFactory.createLevel(_currentLevel = 0, _ground));

            _control.registerListener(this);
            UnderwhirledDrift.registerEventListener(_control, KeyboardEvent.KEY_DOWN, 
                keyEventHandler);
            UnderwhirledDrift.registerEventListener(_control, KeyboardEvent.KEY_UP, 
                keyEventHandler);
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
                _control.sendMessage(PLAYER_POSITIONS, playerIds);
            }
            if (_control.amInControl()) {
                // start the ticker that sets up to sending out network updates
                _control.startTicker(UPDATE_TICK, SEND_THROTTLE);
            }
        }
    }

    // from MessageReceivedListener
    public function messageReceived (event :MessageReceivedEvent) :void
    {
        switch (event.name) {
        case UPDATE_TICK:
        case POSITION_UPDATE:
        case KART_CHOSEN:
        case CROSSED_FINISH_LINE:
            handlePlayerMessage(event.name, event.value); break;

        case PLAYER_POSITIONS:
        case START_RACE:
        case RACE_STARTED:
        case AWARD_FLOW:
        case TRACK_COMPLETE:
        case GAME_COMPLETE:
            handleGameControlMessage(event.name, event.value); break;

        case SHIELDS_UP:
        case FIREBALL:
        case BONUS_GONE:
            handleBonusMessage(event.name, event.value); break;
        default:
            Log.getLog(this).warning("unknown event: " + event.name);
        }
    }

    public function kartPicked (kart :Kart) :void
    {
        _kart = kart;
        _view.setKart(_kart);
        _control.sendMessage(KART_CHOSEN, {playerId: _control.getMyId(),
            kartType: _kart.kartType});
        updateRaceStarted();
        UnderwhirledDrift.registerEventListener(_kart, KartEvent.CROSSED_FINISH_LINE, 
            function (event :KartEvent) :void {
                if (_control.seating.getPlayerIds().length != 1) {
                    _control.sendMessage(CROSSED_FINISH_LINE, { playerId: _control.getMyId(), 
                        direction: event.value });
                }
            });
        UnderwhirledDrift.registerEventListener(_kart, KartEvent.BONUS, 
            function (event :KartEvent) :void {
                _view.setBonus(event.value.bonus as Bonus);
                _control.sendMessage(BONUS_GONE, { x: event.value.pos.x, y: event.value.pos.y });
            });
        UnderwhirledDrift.registerEventListener(_kart, KartEvent.REMOVE_BONUS, 
            function (event :KartEvent) :void {
                _view.clearBonus();
            });
        UnderwhirledDrift.registerEventListener(_kart, KartEvent.SHIELD, 
            function (event :KartEvent) :void {
                _control.sendMessage(SHIELDS_UP, { playerId: _control.getMyId(), up: event.value });
            });
        UnderwhirledDrift.registerEventListener(_kart, KartEvent.FIREBALL, 
            function (event :KartEvent) :void {
                _control.sendMessage(FIREBALL, { playerId: _control.getMyId(), x: 
                    _ground.getKartLocation().x, y: _ground.getKartLocation().y, 
                    angle: _camera.angle - Math.PI / 2 });
            });
    }

    public function lightBoardDone() :void
    {
        if (_control.amInControl()) {
            // base the real race start on the controller's clock
            _control.sendMessage(RACE_STARTED, true);
        }
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
                _control.sendMessage(START_RACE, true);
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
                        _view.setLevel(_level = LevelFactory.createLevel(
                            _currentLevel % LevelFactory.TOTAL_LEVELS, _ground));
                        _level.setStartingPosition(0);
                        _ground.getScenery().registerKart(_kart);
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

    protected function handlePlayerMessage (message :String, value :Object) :void
    {
        switch (message)
        {
        case UPDATE_TICK:
            if (_kart != null) {
                var updateObj :Object = _kart.getUpdate();
                updateObj.playerId = _control.getMyId();
                _control.sendMessage(POSITION_UPDATE, updateObj);
            }
            break;
        case POSITION_UPDATE:
            var playerId :int = value.playerId;
            if (playerId != _control.getMyId()) {
                var oppKart :Object = _opponentKarts.get(playerId);
                if (oppKart is KartObstacle) {
                    (oppKart as KartObstacle).setPosition(value);
                }
            }
            break;
        case KART_CHOSEN:
            playerId = value.playerId;
            if (playerId != _control.getMyId()) {
                var kartType :String = value.kartType;
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
            break;
        case CROSSED_FINISH_LINE:
            var currentLaps :int = value.direction;
            if (_playerLaps.containsKey(value.playerId)) {
                currentLaps += _playerLaps.get(value.playerId);
            }
            if (currentLaps < _numLaps + 1 && 
                !ArrayUtil.contains(_playersFinished, value.playerId)) {
                _playerLaps.put(value.playerId, currentLaps);
                if (value.playerId == _control.getMyId() && value.direction > 0) {
                    if (currentLaps < _numLaps) {
                        _control.localChat("You are on lap " + currentLaps);
                    } else {
                        _control.localChat("You are on your final lap!");
                    }
                }
            } else if (currentLaps == _numLaps + 1) {
                _playerLaps.remove(value.playerId);
                _playersFinished.push(value.playerId);
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
                    _control.sendChat(_control.getOccupantName(value.playerId) + 
                        " finished in " + place + " place!");
                    // let the player's controller give that player some flow... this is decided 
                    // here because the controller is the arbiter of ties
                    _control.sendMessage(AWARD_FLOW, 1 - (_playersFinished.length - 1) / 
                        _control.seating.getPlayerIds().length, value.playerId);
                    if (_playerLaps.size() == 0) {
                        if (_currentLevel == _numRounds - 1) {
                            _control.sendMessage(GAME_COMPLETE, true);
                        } else {
                            _control.sendMessage(TRACK_COMPLETE, true);
                        }
                    }
                }
            } 
            break;
        default:
            Log.getLog(this).warning("found unknown message: " + message);
        }
    }

    protected function handleGameControlMessage (message :String, value :Object) :void
    {
        switch (message)
        {
        case PLAYER_POSITIONS:
            var playerPositions :Array = value as Array;
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
            break;
        case START_RACE:
            _view.startLightBoard();
            break;
        case RACE_STARTED:
            _raceStarted = true;
            break;
        case AWARD_FLOW:
            var flow :int = Math.ceil(_control.getAvailableFlow() * (value as Number));
            _control.awardFlow(flow);
            _control.localChat("You were awarded " + flow + " flow for your performance!");
            break;
        case TRACK_COMPLETE:
            _control.localChat("The current track has been completed by all players... now " +
                "loading the next track!");
            _raceStarted = false;
            _kart.killMovement();
            _currentLevel++;
            _level = LevelFactory.createLevel(_currentLevel % LevelFactory.TOTAL_LEVELS, _ground);
            _view.setLevel(_level);
            _level.setStartingPosition(0);
            _ground.getScenery().registerKart(_kart);
            _playerLaps.clear();
            _playersFinished = [];
            // forget old starting positions
            for each (var key :int in _opponentKarts.keys()) {
                _opponentKarts.put(key, _opponentKarts.get(key).kartType);
            }
            if (_control.amInControl()) {
                // TODO - this is done twice, break out a function for it
                var playerIds :Array = _control.seating.getPlayerIds();
                // assign everyone a new starting position.
                ArrayUtil.shuffle(playerIds);
                for (ii = 0; ii < playerIds.length; ii++) {
                    playerIds[ii] = { id: playerIds[ii], position: ii };
                }
                _control.sendMessage(PLAYER_POSITIONS, playerIds);
            }
            break;
        case GAME_COMPLETE:
            _control.localChat("The game is complete!  Head back to the game lobby for more " + 
                "Undewhirled Drift action!");
            break;
        default:
            Log.getLog(this).warning("found unknown message: " + message);
        }
    }

    protected function handleBonusMessage (message :String, value :Object) :void
    {
        switch (message)
        {
        case SHIELDS_UP:
            if (value.playerId == _control.getMyId()) {
                _kart.shieldsUp(value.up);
            } else {
                _opponentKarts.get(value.playerId).shieldsUp(value.up);
            }
            break;
        case FIREBALL:
            _ground.getScenery().shootFireball(new Point(value.x, value.y), 
                value.angle, value.playerId == _control.getMyId());
            break;
        case BONUS_GONE:
            _ground.getScenery().removeBonus(new Point(value.x, value.y));
            break;
        default:
            Log.getLog(this).warning("found unknown message: " + message);
        }
    }

    protected static const SEND_THROTTLE :int = 250; // in ms

    protected var _camera :Camera;
    protected var _ground :Ground;
    protected var _view :UnderwhirledDriftView;
    protected var _level :Level;

    protected var _kart :Kart;

    protected var _numLaps :int;
    protected var _numRounds :int;

    protected var _control :WhirledGameControl;

    /** A hashmap of the opponent's karts. */
    protected var _opponentKarts :HashMap = new HashMap();
    protected var _playerLaps :HashMap = new HashMap();
    protected var _playersFinished :Array = [];

    protected var _raceStarted :Boolean = false;

    protected var _currentLevel :int;
}
}
