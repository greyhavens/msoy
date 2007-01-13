package {

import Math;

import mx.core.SpriteAsset;

import flash.display.Sprite;
import flash.display.MovieClip;
import flash.external.ExternalInterface;
import flash.events.TimerEvent;
import flash.text.TextField;
import flash.utils.Timer;
import flash.ui.Keyboard;

[SWF(width="700", height="500")]
public class Juggly extends Sprite 
	implements PlayField
{
    public function Juggly () :void
    {        
        addChild(new backgroundClass());

        _space = new Space(0,0, 700,500, 50);                
        _collider = new Collider(this);
        _actors = new Array();
                
        _body = new Body(this, _space);
        _body.x = 350;
        _body.y = 520;
        
        _controller = new JugglingController(_body, this);
        _body.controller = _controller;

        _controller.eventSource = stage;
                        
        _ballBox = new BallBox(this, _space);        
        
        _display = new ScoreDisplay(this);

        _scoreCard = new ScoreCard();
        _scoreCard.display = _display;
        
        startFrameAnimation();
    }

    private function ignore(event:Object) :void
    {
        // do nothing
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

        for each (var actor:Actor in _actors)
        {
            actor.nextFrame();
        }
    }

    private function startFrameAnimation () :void
    {        
        const frameTimer :Timer = new Timer(_space.frameDuration, 0);
        frameTimer.addEventListener(TimerEvent.TIMER, tick);
        frameTimer.start();        
    }
     
	public function get scoreCard() :ScoreCard 
	{
		return _scoreCard;
	}
	
	public function get ballBox() :BallBox
	{
		return _ballBox;
	}
           
    public static const DEBUG_GRAPHICS:Boolean = false;
            
    private var _body :Body;

    private var _actors :Array;

    private var _space :Space;
            
    private var _controller :JugglingController;
            
    private var _collider :Collider;
            
    private var _display:ScoreDisplay;
                
    private var _ballsPlayed:int = 0;
                
    private static const NUM_BALLS :int = 100;
             
    private var _ballBox:BallBox;
     
    private var _scoreCard:ScoreCard; 
     
    [Bindable]
    [Embed(source="rsrc/bg_tent.swf")]
    private var backgroundClass:Class;
   
} 
}
