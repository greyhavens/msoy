package {

import Math;

import flash.display.Sprite;
import flash.display.MovieClip;
import flash.external.ExternalInterface;
import flash.events.TimerEvent;
import flash.utils.Timer;
import flash.events.KeyboardEvent;
import flash.ui.Keyboard;

import com.threerings.ezgame.Game;
import com.threerings.ezgame.EZGame;
import com.threerings.ezgame.MessageReceivedEvent;
import com.threerings.ezgame.MessageReceivedListener;
import com.threerings.ezgame.PropertyChangedEvent;
import com.threerings.ezgame.PropertyChangedListener;
import com.threerings.ezgame.StateChangedEvent;
import com.threerings.ezgame.StateChangedListener;
    
[SWF(width="600", height="600")]
public class Juggler extends Sprite 
    implements Game, PropertyChangedListener, StateChangedListener
{
    public function Juggler ()
    {
        log("juggler v0.03 constructed");
    }
        
    protected function setUpBalls () :void
    {
        _space = new Space(0,0, 600,600, 50);                
        _collider = new Collider(this);
        _actors = new Array();
        
        _body = new Body(this, _space);
        _body.x = 300;
        _body.y = 500;
                
        log("registered "+_collider.activeBodies()+" bodies for collision detection.");
    }

    public function registerAsActor(actor:Actor) :void
    {
        _actors.push(actor);
    }

    public function deregisterAsActor(target:Actor) :void
    {
        Util.removeFromArray(_actors, target);
    }

    public function registerForCollisions(body:CanCollide) :void
    {
        _collider.addBody(body);
    }

    public function deregisterForCollisions(body:CanCollide) :void
    {
        _collider.removeBody(body);
    }

    public function tick(event :TimerEvent) : void 
    {
        event.updateAfterEvent();
        _collider.detectCollisions();
        _actors.forEach(nextFrame);
    }

    private function nextFrame(actor :Actor, index:Number, balls:Array) :void 
    {
        if (actor!=null) actor.nextFrame();
    }

    public function setGameObject (gameObj : EZGame) :void
    {
        log("set game object called");
        _ezgame = gameObj;
        setUpBalls();
        
        var frameTimer :Timer = new Timer(_space.frameDuration, 0);
        frameTimer.addEventListener(TimerEvent.TIMER, tick);
        frameTimer.start();
        
        stage.addEventListener(KeyboardEvent.KEY_DOWN, keyDown);
        stage.addEventListener(KeyboardEvent.KEY_UP, keyUp);
        
        log("started frame events");    
    }
    
    public function keyUp(event: KeyboardEvent) :void
    {
        switch (event.keyCode)
        {
            case KEY_Q:
                Qdown = false;
                break;
            case KEY_W:
                Wdown = false;
                break;
            case KEY_O:
                Odown = false;
                break;
            case KEY_P:
                Pdown = false;
        }    
    }
    
    public function keyDown(event :KeyboardEvent) :void
    {
        switch (event.keyCode)
        {
            case KEY_Q:
                Qdown = true;
                _body.leftHandLeft();
                break;
            case KEY_W:
                Wdown = true;
                _body.leftHandRight();
                break;
            case KEY_O:
                Odown = true;
                _body.rightHandLeft();
                break;
            case KEY_P:
                Pdown = true;
                _body.rightHandRight();
                break;
            case KEY_SPACE:
                addBall();
        }
    }
    
    public function addBall() :void
    {
        if (_ballsPlayed <= NUM_BALLS) 
        {
            if (_body.addBall()) {
                _ballsPlayed += 1;
            }
        }
    }
    
    public function leftDown() :Boolean 
    {
        return (Qdown || Wdown);
    }
    
    public function rightDown() :Boolean
    {
        return (Odown || Pdown);
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
//        trace(msg);
        //ExternalInterface.call("console.debug", msg);
    }
    
    protected var _body :Body;

    protected var _actors :Array;

    protected var _space :Space;
            
    protected var _collider :Collider;
            
    /** The game object for this event. */
    protected var _ezgame :EZGame;
                
    private var _ballsPlayed:int = 0;
                
    private static const NUM_BALLS :int = 100;
    
    private static const KEY_Q:uint = 81;
    
    private static const KEY_W:uint = 87;
    
    private static const KEY_O:uint = 79;
    
    private static const KEY_P:uint = 80;
    
    private static const KEY_SPACE:uint = 32;
    
    private var Qdown:Boolean = false;
    
    private var Wdown:Boolean = false;
    
    private var Odown:Boolean = false;

    private var Pdown:Boolean = false;   
} 
}