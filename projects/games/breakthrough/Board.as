package {

import flash.display.Sprite;

import flash.events.TimerEvent;

import flash.geom.Rectangle;

import flash.utils.Timer;
import flash.utils.getTimer;

import com.threerings.ezgame.EZGame;
import com.threerings.ezgame.MessageReceivedEvent;

public class Board extends Sprite
{
    public function Board (gameObj :EZGame)
    {
        _gameObj = gameObj;
        
        // draw the background
        graphics.beginFill(0x000000);
        graphics.drawRect(0, 0, 300, 400);
        graphics.endFill();
        
        // clip everything against the board boundaries
        scrollRect = new Rectangle(0, 0, 300, 400);
        
        // add the paddle and ball objects (which will respond to UI and
        // network events)
        addChild(_ownPaddle = new Paddle(gameObj, this, true, OWN_COLOR));
        addChild(_oppPaddle = new Paddle(gameObj, this, false, OPP_COLOR));
        addChild(_ownBall = new Ball(gameObj, this, true, OWN_COLOR));
        addChild(_oppBall = new Ball(gameObj, this, false, OPP_COLOR));
        
        // subscribe for pings and pongs
        _gameObj.addEventListener(MessageReceivedEvent.TYPE,
            messageReceivedHandler);
        
        // create the ping timer
        var timer :Timer = new Timer(PING_DELAY);
        timer.addEventListener(TimerEvent.TIMER, timerHandler);
        timer.start();
    }

    public function get ownPaddle () :Paddle
    {
        return _ownPaddle;
    }
    
    public function get oppPaddle () :Paddle
    {
        return _oppPaddle;
    }
    
    public function get latency () :int
    {
        return _latency;
    }
    
    protected function timerHandler (event :TimerEvent) :void
    {
        _gameObj.sendMessage(PING, getTimer(), 1 - _gameObj.getMyIndex());
    }
    
    protected function messageReceivedHandler (
        event :MessageReceivedEvent) :void
    {
        if (event.name == PING) {
            _gameObj.sendMessage(PONG, event.value, 1 - _gameObj.getMyIndex());
        } else if (event.name == PONG) {
            var delay :int = (getTimer() - int(event.value)) / 2;
            if (_latency == 0) {
                _latency = delay;
            } else {
                _latency = (_latency * 7 + delay) / 8;
            }
        }
    }
    
    protected var _gameObj :EZGame;
    
    /** The paddles and balls on the board. */
    protected var _ownPaddle :Paddle, _oppPaddle :Paddle;
    protected var _ownBall :Ball, _oppBall :Ball;

    /** The current latency estimate. */
    protected var _latency :int;
    
    /** Colors used for our own and for opponents' paddles. */
    protected static const OWN_COLOR :uint = 0x00FF00,
        OPP_COLOR :uint = 0x00FFFF;
        
    /** Messages used to measure communication latency. */
    protected static const PING :String = "ping",
        PONG :String = "pong";
    
    /** The delay between pings. */
    protected static const PING_DELAY :int = 5000;
}
}
