package {
    
import flash.display.DisplayObjectContainer;    
import flash.display.DisplayObject;    
import flash.display.Sprite;
import Math;

public class Ball extends Sprite
    implements Actor, CanCollide
{    
    public function Ball (juggler :PlayField, space :Space, art:Object, xpos:Number, ypos:Number) 
    {
		x = xpos;
		y = ypos;
	
        _juggler = juggler;
        _space = space;
                
        _juggler.addChild(this);
        _juggler.registerAsActor(this);
        _juggler.registerForCollisions(this);
        
        if (art!=null)
        {
            addChild(art as DisplayObject);
        }
        
        draw();
    }
    
    /** draw the ball in it's regular color **/
    public function draw () :void
    {
        redraw(REGULAR_COLOR);
    }
        
    /** highlight this particular ball for debugging purposes **/
    public function highlight () :void
    {
        redraw(HIGHLIGHT_COLOR);
        _highlightFrames = 2;
    }
    
    private function redraw (color:uint, alpha:Number = 0.8) :void
    {
        graphics.clear();
        graphics.beginFill(color, alpha);
        graphics.drawCircle(0,0, BALL_RADIUS);
        graphics.endFill();
        
    }
    
    public function randomizePosition () :void
    {
        x = (Math.random() * _space.width()) + _space.left;
        y = (Math.random() * _space.height()) + _space.top
        dx = (Math.random() * 16) - 8 * _space.frameRate;
        dy =  (Math.random() * 16) - 8 * _space.frameRate;
    }    
    
    public function placeInHand(hand:Hand) :void
    {
        _hand = hand;
        _motion = caught;
        _catchFrames = 0;
    }
    
    public function caughtBy (hand:Hand) :void
    {
        _hand = hand;
        _motion = caught;
        _catchFrames = 3;
    }
    
    public function release () :void
    {
        _hand = null;
        _motion = free;
    }
    
    public function nextFrame () : void
    {
        // if the ball is highlighted, check whether it should 
        // be set back to normal
        if (_highlightFrames > 0) {
            _highlightFrames -= 1;
            if (_highlightFrames == 0) {
                draw();
            }
        }
        
        _motion();
    }
        
    /** define the motion of a ball when it's caught **/
    private function caught () :void
    {
        const bounds:Bounds = _hand.boundsInContext(_juggler);
                
        if(_catchFrames > 0) 
        {
            _catchFrames -=1;
            
            const targetX:int = bounds.x;
            const targetY:int = bounds.topProjection - BALL_RADIUS;
            
            x += (targetX - x) / 2
            y += (targetY - y) / 2
        } 
        else
        {
            x = bounds.x;
            y = bounds.topProjection -BALL_RADIUS;
        }
        
        nextX = x
        nextY = y;    
    }
        
    /** define the motion of the ball when it's free **/
    private function free () :void
    {
        if (_velocityChanged) 
        {
            _velocityChanged = false;
            project();
        }
        
        advance();
        
        // deal with elastic collision with floor or walls;
        if (x > _space.right) {
            dx = -Math.abs(dx) * _elasticity;
        } else if (x < _space.left) {
            dx = Math.abs(dx) * _elasticity;
        }
    
        if (y < _space.top) {
            dy = Math.abs(dy) * _elasticity;
        } else if (y > _space.bottom) {
            if (! _dropped) {
                drop();
            } else {
                dy = - Math.abs(dy) * _elasticity;
                y = _space.bottom;
            }
        }
        
        // update velocity based on constants;
        
        // friction always opposes motion
        dx += (dx>0) ? -_space.frictionPerFrame : _space.frictionPerFrame;
        dy += (dy>0) ? -_space.frictionPerFrame : _space.frictionPerFrame;
        
        // gravity is always in one direction
        dy += _space.gravityPerFrame;
        
        project();      
    }
     
    private function drop () :void
    {
        dy = DEATH_BOUNCE;
        y = _space.bottom;  // if you don't do this then gravity forces the ball 
                    //through the floor!
        startFading();
        _dropped = true;
        _juggler.scoreCard.ballDropped();
    }
        
    /** start fading out this ball **/
    private function startFading () :void
    {
        _juggler.deregisterForCollisions(this);
        _fadeFrames = FADE_DURATION;
        _motion = fading;
    }
        
    private function fading () :void
    {
        _fadeFrames -= 1;
        free();
        const alpha:Number = _fadeFrames / FADE_DURATION;
        redraw(REGULAR_COLOR, alpha * alpha);
        
        if (_fadeFrames <= 0)
        {
            _juggler.deregisterAsActor(this);
            _juggler.removeChild(this);
        }
    }
        
    /** calculate the expected position next frame **/
    private function project () :void
    {        
        nextX = x + (dx / _space.frameRate);
        nextY = y + (dy / _space.frameRate);
    }
        
    private function advance () :void
    {
        x = nextX;
        y = nextY;
    }
        
    public function get leftProjection () :Number
    {
        return nextX - BALL_RADIUS;
    }
    
    public function get rightProjection () :Number
    {
        return nextX + BALL_RADIUS;
    }
    
    public function get topProjection () :Number
    {
        return nextY - BALL_RADIUS;
    }
    
    public function get bottomProjection () :Number
    {
        return nextY + BALL_RADIUS;
    }
    
    public function get mass() :Number
    {
        return _mass;
    }
    
    public function get position () :Array
    {
        return new Array(x, y);
    }
    
    public function get velocity () :Array
    {
        return new Array(dx, dy);
    }
    
    public function set velocity (v: Array) :void
    {
        dx = v[0];
        dy = v[1];
        _velocityChanged = true;
    }
     
    public function collisionWith(other:CanCollide) :void
    {
        if (other is Ball)
        {        
            const results:Array = _elasticCollision.collide( 
                this.position, this.velocity, this.mass,
                other.position, other.velocity, this.mass
            );
        
            this.velocity = results[0];
            other.velocity = results[1];
        }
        else if (other is Hand) 
        {
            Hand(other).touchBall(this);
        }
    }
        
    public function set label (label:String) :void
    {
        _label = label;        
    }

    public function get label () :String
    {
        return _label;
    }
        
    public function boundsInContext(context:Positioned) :Bounds
    {
        if (_boundsInContext==null || _boundsInContext.context != context) {
            _boundsInContext = new BoundsInContext(context, this);
        } 
        
        return _boundsInContext;
    }
    
    private static var _elasticCollision :ElasticCollision = new ElasticCollision();
    
    private var _boundsInContext:BoundsInContext;
    
    /** vertical component of velocity **/
    private var dy :Number; // pixels per second
    
    /** horizontal component of velocity **/
    private var dx :Number; // pixels per second
        
    /** position at next frame assuming no collision **/
    private var nextX :Number; 
    
    /** position at next frame assuming no collision **/
    private var nextY :Number;
            
    private static const HIGHLIGHT_COLOR :uint = 0xFF0000;
    
    private static const REGULAR_COLOR :uint = 0x000080;
    
    private static const FADE_DURATION:int = 100;
        
    private static const DEATH_BOUNCE:Number = -200;
        
    public static const BALL_RADIUS :int = 15;
        
    private static const _elasticity :Number = 0.95;
    
    private var _label:String = "a ball";
    
    private const _mass :Number = 1;
    
    private var _juggler :PlayField;
    
    private var _space :Space;
    
    private var _highlightFrames :int = 0;
    
    private var _motion:Function = free;
    
    private var _hand:Hand; 
    
    private var _catchFrames:int;
    
    private var _velocityChanged:Boolean = true;
        
    private var _fadeFrames:int = -1;
    
    private var _dropped:Boolean = false;
}
}