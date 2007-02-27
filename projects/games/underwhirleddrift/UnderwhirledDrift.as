package {

import flash.display.Sprite;
import flash.display.Shape;

import flash.geom.Point;

import flash.events.KeyboardEvent;

import flash.ui.Keyboard;

import flash.text.TextField;
import flash.text.TextFieldAutoSize;

import com.threerings.ezgame.EZGameControl;
import com.threerings.ezgame.HostCoordinator;
import com.threerings.ezgame.HostEvent;
import com.threerings.ezgame.PropertyChangedListener;
import com.threerings.ezgame.PropertyChangedEvent;
import com.threerings.ezgame.StateChangedListener;
import com.threerings.ezgame.StateChangedEvent;
import com.threerings.ezgame.MessageReceivedListener;
import com.threerings.ezgame.MessageReceivedEvent;

import com.threerings.util.ArrayUtil;
import com.threerings.util.HashMap;

[SWF(width="711", height="400")]
public class UnderwhirledDrift extends Sprite
    implements PropertyChangedListener, StateChangedListener, MessageReceivedListener
{
    /** width of the masked display */
    public static const DISPLAY_WIDTH :int = 711;

    /** height of the masked display */
    public static const DISPLAY_HEIGHT :int = 400;

    /** height of the sky */
    public static const SKY_HEIGHT :int = DISPLAY_HEIGHT * 0.4;

    /** Kart location, relative to the ground coordinates */
    public static const KART_LOCATION :Point = new Point (355, 200);

    /** Kart offset from its effective location */
    public static const KART_OFFSET :int = 32;

    public static const KEY_EVENT :String = "keyEvent";

    public function UnderwhirledDrift ()
    {
        var gameSprite :Sprite = new Sprite();
        var masker :Shape = new Shape();
        masker.graphics.beginFill(0xFFFFFF);
        masker.graphics.drawRect(0, 0, DISPLAY_WIDTH, DISPLAY_HEIGHT);
        masker.graphics.endFill();
        gameSprite.mask = masker;
        gameSprite.addChild(masker);
        addChild(gameSprite);

        // "sky"
        var colorBackground :Shape = new Shape();
        colorBackground.graphics.beginFill(0x8888FF);
        colorBackground.graphics.drawRect(0, 0, DISPLAY_WIDTH, DISPLAY_HEIGHT);//SKY_HEIGHT + 10);
        colorBackground.graphics.endFill();
        gameSprite.addChild(colorBackground);

        var camera :Camera = new Camera();

        var ground :Ground = new Ground(camera);
        ground.y = SKY_HEIGHT;
        gameSprite.addChild(ground);

        _level = LevelFactory.createLevel(0, ground);
        ground.setLevel(_level);

        _gameCtrl = new EZGameControl(this);
        _gameCtrl.registerListener(this);
        _gameCtrl.addEventListener(KeyboardEvent.KEY_DOWN, keyEventHandler);
        _gameCtrl.addEventListener(KeyboardEvent.KEY_UP, keyEventHandler);

        _coord = new HostCoordinator(_gameCtrl);

        var chooser :KartChooser = new KartChooser(this, gameSprite, camera, ground);
        chooser.chooseKart();

        // names above characters is good, but they should fade out after the race 
        // starts
        /*
        var nameText :TextField = new TextField();
        nameText.text = _gameCtrl.getOccupantName(_gameCtrl.getMyId());
        nameText.selectable = false;
        nameText.autoSize = TextFieldAutoSize.CENTER;
        nameText.scaleX = nameText.scaleY = 2.5;
        nameText.x = _kart.x - nameText.width / 2;
        nameText.y = _kart.y - _kart.height - 5;
        addChild(nameText);*/
    }

    // from StateChangedListener
    public function stateChanged (event :StateChangedEvent) :void
    {
        if (event.type == StateChangedEvent.GAME_STARTED) {
            var playerIds :Array = _gameCtrl.seating.getPlayerIds();
            if (_coord.status == HostCoordinator.STATUS_HOST || playerIds.length == 1) {
                // assign everyone a starting position.
                ArrayUtil.shuffle(playerIds);
                for (var ii :int = 0; ii < playerIds.length; ii++) {
                    playerIds[ii] = { id: playerIds[ii], position: ii };
                }
                _gameCtrl.set("playerPositions", playerIds);
                
                _gameCtrl.startTicker("tick", SEND_THROTTLE);
            }
        }
    }

    // from PropertyChangedListener
    public function propertyChanged (event :PropertyChangedEvent) :void
    {
        var name :String = event.name;
        if (name == "playerPositions") {
            var playerPositions :Array = event.newValue as Array;
            for (var ii: int = 0; ii < playerPositions.length; ii++) {
                if (playerPositions[ii].id == _gameCtrl.getMyId()) {
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
        }
    }

    // from MessageReceivedListener
    public function messageReceived (event :MessageReceivedEvent) :void
    {
        if (event.name == "tick") {
            if (_raceStarted && _kart != null) {
                var obj :Object = _kart.getUpdate();
                obj.playerId = _gameCtrl.getMyId();
                _gameCtrl.sendMessage("positionUpdate", obj);
            }
        } else if (event.name == "raceStarted") {
            _raceStarted = true;
        } else if (event.name == "positionUpdate") {
            var playerId :int = event.value.playerId;
            if (playerId != _gameCtrl.getMyId()) {
                (_opponentKarts.get(playerId) as KartObstacle).setPosition(event.value);
            }
        } else if (event.name == "kartChosen") {
            playerId = event.value.playerId;
            if (playerId != _gameCtrl.getMyId()) {
                var kartType :String = event.value.kartType;
                var position :Object = _opponentKarts.get(playerId);
                if (position != null) {
                    _opponentKarts.put(playerId, _level.addOpponentKart(position as int,  
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
        _gameCtrl.sendMessage("kartChosen", {playerId: _gameCtrl.getMyId(), 
            kartType: _kart.kartType});
        updateRaceStarted();
    }

    /**
     * TODO: The conditions under which this method is called are too complicated.  This needs 
     * to be rethought.
     */
    protected function updateRaceStarted () :void
    {
        if (_gameCtrl.seating.getPlayerIds().length == 1) {
            _raceStarted = true;
        } else if (_coord.status == HostCoordinator.STATUS_HOST) { 
            var keys :Array = _opponentKarts.keys();
            for (var ii :int = 0; ii < keys.length; ii++) {
                if (!(_opponentKarts.get(keys[ii]) is KartObstacle)) {
                    break;
                }
            }
            if (ii == keys.length) {
                _gameCtrl.sendMessage("raceStarted", true);
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
            default:
            // do nothing
            }
        }
    }

    protected static const SEND_THROTTLE :int = 120;

    /** the game control. */
    protected var _gameCtrl :EZGameControl;

    /** The host coordinator */
    protected var _coord :HostCoordinator;

    /** The level object */
    protected var _level :Level;

    /** The kart. */
    protected var _kart :Kart;
    
    /** A hashmap of the opponent's karts. */
    protected var _opponentKarts :HashMap = new HashMap();

    /** A flag to indicate that the race has started, so its safe to send position update */
    protected var _raceStarted :Boolean = false;
}
}
