package {

import flash.display.Shape;
import flash.display.Sprite;

import flash.events.Event;
import flash.events.MouseEvent;
import flash.events.TimerEvent;

import flash.utils.getTimer;

import com.threerings.ezgame.EZGame;
import com.threerings.ezgame.PropertyChangedEvent;

public class Paddle extends Shape
{
    /** The dimensions of the paddle. */
    public static const WIDTH :int = 40, HEIGHT :int = 5;
    
    /** The paddle's radius of curvature for purposes of computing
     * reflection angles. */
    public static const RADIUS_OF_CURVATURE :Number = 50;
    
    /** The distance from the center of curvature to the edge of the
     * paddle. */
    public static const CURVATURE_DIST :Number = Math.sqrt(
        Math.pow(RADIUS_OF_CURVATURE, 2) - Math.pow(WIDTH / 2, 2));
    
    public function Paddle (
        gameObj :EZGame, board :Board, own :Boolean, color :uint)
    {
        _gameObj = gameObj;
        _board = board;
        _own = own;
        
        // draw the paddle
        graphics.beginFill(color);
        graphics.drawRect(0, 0, WIDTH, HEIGHT);
        graphics.endFill();
        
        // center the paddle horizontally
        position = board.width / 2;
        
        // make sure the position property is initialized
        if (gameObj.get(PADDLE_STATES) == null) {
            gameObj.set(PADDLE_STATES, new Array(
                createState(board.width/2), createState(board.width/2)));
        }
        _lastPos = _framePos = _position;
        _speed = Number.MAX_VALUE;
        
        // move our own paddle with the mouse and send periodic updates
        if (own) {
            y = board.height - HEIGHT;
            board.addEventListener(MouseEvent.MOUSE_MOVE, mouseMoveHandler);
            _stimes = new Array();
            
        // move our opponents' paddle in response to property changes
        } else {
            y = 0;
            gameObj.addEventListener(PropertyChangedEvent.TYPE, propChanged);
        }
        _board.addEventListener(Event.ENTER_FRAME, enterFrameHandler);
    }
    
    public function get position () :Number
    {
        return _position;
    }
    
    public function set position (position :Number) :void
    {
        _position = position;
        x = _position - WIDTH / 2;
    }
    
    protected function mouseMoveHandler (event :MouseEvent) :void
    {
        // move own paddle to match mouse position
        if (event.localX >= 0 && event.localX < _board.width) {
            position = event.localX;
        }
    }

    protected function propChanged (event :PropertyChangedEvent) :void
    {
        if (event.name != PADDLE_STATES ||
            event.index != (1 - _gameObj.getMyIndex())) {
            return;
        }
        // converge towards the new state
        var state :Object = event.newValue;
        _lastPos = _board.width - state.pos;
        _speed = Math.max(state.speed, MIN_SPEED);
    }
    
    protected function enterFrameHandler (event :Event) :void
    {
        var time :int = getTimer(),
            dt :Number = (time - _lastFrame);
        if (dt <= 0) {
            return; // make sure some time has actually passed
        }
        _lastFrame = time;
        
        if (_own) {
            // compare actual to last sent state, transmitting when
            // they diverge too much
            _speed = Math.abs((_framePos - _position) / dt);
            if (Math.abs(_lastPos - _position) > _pthreshold &&
                (_stimes.length == 0 || (time -
                    _stimes[_stimes.length-1]) > MIN_UPDATE_DELAY)) {
                sendState();
            }
            _framePos = _position;
            
        } else {
            // move towards target position
            if (_position < _lastPos) {
                position = Math.min(_position + dt * _speed, _lastPos);
            } else if (_position > _lastPos) {
                position = Math.max(_position - dt * _speed, _lastPos);
            }
        }
    }

    protected function sendState () :void
    {
        var time :int = getTimer();
        if (_stimes.length < UPDATE_LIMIT ||
            time >= (_stimes[0] + UPDATE_INTERVAL)) {
            _stimes.push(time);
            if (_stimes.length > UPDATE_LIMIT) {
                _stimes.shift();
            }
            _gameObj.set(PADDLE_STATES, createState(_position, _speed),
                _gameObj.getMyIndex());
            _lastPos = _position;
            
        } else {
            // throttled!
            _pthreshold += POSITION_THRESHOLD_INCREMENT;
        }
    }
    
    protected function createState (
        pos :Number, speed :Number = Number.MAX_VALUE) :Object
    {
        return {pos: pos, speed: speed};
    }
    
    protected var _gameObj :EZGame;
    protected var _board :Board;
    protected var _own :Boolean;
    
    /** The position of the center of the paddle. */
    protected var _position :Number;
    
    /** Tracks the last position sent/received in order to make sure we don't
     * send the location when it hasn't changed and can smoothly transition to
     * the current location. */
    protected var _lastPos :Number;
    protected var _speed :Number;
    
    protected var _lastFrame :int;
    protected var _framePos :Number;
    
    protected var _pthreshold :Number = BASE_POSITION_THRESHOLD;
    
    protected var _stimes :Array;
    
    /** The property used to track paddle states. */
    protected static const PADDLE_STATES :String = "paddleStates";
    
    /** The maximum number of updates we can send in the interval below. */
    protected static const UPDATE_LIMIT :int = 25;
    
    /** The interval over which we count updates. */
    protected static const UPDATE_INTERVAL :int = 5000;
    
    /** The base amount of error we tolerate in positions. */
    protected static const BASE_POSITION_THRESHOLD :Number = 1;
    
    /** The rate at which we raise the position threshold when throttled. */
    protected static const POSITION_THRESHOLD_INCREMENT :Number = 0.01;
    
    /** Must wait at least this long between updates. */
    protected static const MIN_UPDATE_DELAY :int = 50;
    
    /** The minimum speed at which to approach the target position. */
    protected static const MIN_SPEED :Number = 0.25;
}
}
