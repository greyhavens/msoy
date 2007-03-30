package {

import flash.display.Sprite;
import flash.display.Shape;

import flash.events.IEventDispatcher;

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
    implements UDView
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

    /**
     * Adds the given event listener to the given dispatcher for the given event - this should
     * be used for all listeners, as it remembers them locally, and unregisters them when the 
     * game sprite is unloaded. 
     */
    public static function registerEventListener(dispatcher :IEventDispatcher, event :String, 
        listener :Function) :void 
    {
        dispatcher.addEventListener(event, listener);
        _eventHandlers.push({dispatcher: dispatcher, event: event, func: listener});
    }

    public function UnderwhirledDrift ()
    {
        registerEventListener(root.loaderInfo, Event.UNLOAD, handleUnload);

        _gameSprite = new Sprite();
        var masker :Shape = new Shape();
        masker.graphics.beginFill(0xFFFFFF);
        masker.graphics.drawRect(0, 0, DISPLAY_WIDTH, DISPLAY_HEIGHT);
        masker.graphics.endFill();
        _gameSprite.mask = masker;
        _gameSprite.addChild(masker);
        addChild(_gameSprite);

        var gameControl :WhirledGameControl = new WhirledGameControl(this);
        if (gameControl.isConnected()) {
            _camera = new Camera();

            _ground = new Ground(_camera);
            _ground.y = SKY_HEIGHT;
            _gameSprite.addChild(_ground);

            _control = new UDControl(gameControl, _camera, _ground, this);

            addChild((new KartChooser(_control, _gameSprite, _camera, _ground)).chooseKart());
        } else {
            // TODO: Display some kind of splash screen.
        }
    }

    // from UDView
    public function setKart (kart :Kart) :void
    {
        _kart = kart;
        _kart.x = KART_LOCATION.x;
        // tack on a few pixels to account for the front of the kart
        _kart.y = KART_LOCATION.y + SKY_HEIGHT + KART_OFFSET;
        addChild(_kart);

        // now that we've set the kart, add the power up frame
        var powerUpFrame :Sprite = new POWER_UP_FRAME();
        powerUpFrame.x = powerUpFrame.width;
        powerUpFrame.y = powerUpFrame.height;
        addChild(powerUpFrame);
    }

    // from UDView
    public function setBonus (bonus :Bonus) :void
    {
        if (bonus != _bonus) {
            _bonus = bonus;
            _bonus.x = _bonus.width;
            _bonus.y = _bonus.height;
            addChild(_bonus);
        }
    }

    // from UDView
    public function clearBonus () :void
    {
        if (_bonus != null) {
            removeChild(_bonus);
            _bonus = null;
        }
    }

    // from UDView
    public function setLevel (level :Level) :void
    {
        _level = level;
        if (_horizon != null) {
            _gameSprite.removeChild(_horizon);
        }
        _gameSprite.addChildAt(_horizon = new Horizon(_level.horizon, _camera), 0);
        _horizon.y += _horizon.height / 2;
        _horizon.x += _horizon.width / 2;
        if (_bonus != null) {
            _kart.destroyBonus();
        }
    }

    // from UDView
    public function startLightBoard () :void
    {
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
                    _control.lightBoardDone();
                } else {
                    if (_lightBoard.y > boardTravelStart - boardTravelHeight) {
                        _lightBoard.y -= 5;
                    } else {
                        _lightBoard.removeEventListener(Event.ENTER_FRAME, boardFrameListener);
                    } 
                }
            }
        };
        UnderwhirledDrift.registerEventListener(_lightBoard, Event.ENTER_FRAME, boardFrameListener);
        addChild(_lightBoard);
    }

    protected function handleUnload (evt :Event) :void
    {
        for each (var handler :Object in _eventHandlers) {
            handler.dispatcher.removeEventListener(handler.event, handler.func);
        }
    }

    [Embed(source='rsrc/light_board.swf#light_board')]
    protected static const LIGHT_BOARD :Class;

    [Embed(source='rsrc/power_ups.swf#power_up_frame')]
    protected static const POWER_UP_FRAME :Class;

    /** All event listeners register in UD - these need to be cleaned up when the game is 
     * unloaded */
    protected static var _eventHandlers :Array = [];

    /** The level object */
    protected var _level :Level;

    /** The kart. */
    protected var _kart :Kart;

    protected var _ground :Ground;
    
    protected var _horizon :Horizon;
    protected var _gameSprite :Sprite;
    protected var _camera :Camera;

    protected var _lightBoard :MovieClipAsset;

    protected var _bonus :Bonus;

    protected var _control :UDControl;
}
}
