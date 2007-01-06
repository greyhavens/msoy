package {

import Math;

import flash.display.Sprite;
import flash.display.MovieClip;
import flash.external.ExternalInterface;
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
    
[SWF(width="700", height="700")]
public class Juggler extends Sprite 
    implements Game, PropertyChangedListener, StateChangedListener
{
    public function Juggler ()
    {
        log("juggler v0.03 constructed");
    }
        
    protected function setUpBalls () :void
    {
        log("setting up space");
        _space = new Space(0,0, 500,500, 50);        
        
        log("setting up collider");
        _collider = new Collider();
        
        log("setting up ball");        
        _balls = new Array();
        
        for (var i:int = 0; i<NUM_BALLS; i++) {
            var ball : Ball = new Ball(this, _space);
            ball.x = (Math.random() * _space.width()) + _space.left;
            ball.y = (Math.random() * _space.height()) + _space.top
            ball.dx = (Math.random() * 16) - 8;
            ball.dy =  (Math.random() * 16) - 8;
            
            _balls.push(ball);
            addChild(ball);
            _collider.addBody(ball);
        }
    }

    public function tick(event :TimerEvent) : void 
    {
        event.updateAfterEvent();
        _collider.detectCollisions();
        _balls.forEach(nextFrame);
    }

    private function nextFrame(ball :Ball, index:Number, balls:Array) :void 
    {
        ball.nextFrame();
    }

    public function setGameObject (gameObj : EZGame) :void
    {
        log("set game object called");
        _ezgame = gameObj;
        setUpBalls();
        
        var frameTimer :Timer = new Timer(_space.frameDuration, 0);
        frameTimer.addEventListener(TimerEvent.TIMER, tick);
        frameTimer.start();
        log("started frame events");    
    }
    
    public function stateChanged (event: StateChangedEvent) :void
    {
        // do nothing for now
    }
    
    public function propertyChanged (event: PropertyChangedEvent) :void
    {
        // do nothing for now
    }
    
    public static function log (msg :String) :void
    {
        trace(msg);
        //ExternalInterface.call("console.debug", msg);
    }

    protected var _balls :Array;

    protected var _space :Space;
            
    protected var _collider :Collider;
            
    /** The game object for this event. */
    protected var _ezgame :EZGame;
                
    protected static const NUM_BALLS :int = 5;
} 
}