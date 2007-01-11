package {
    
import flash.display.DisplayObjectContainer;    
import flash.display.Sprite;
import Math;

public class Ball extends Sprite
    implements Actor, CanCollide
{    
    public function Ball (juggler :Juggler, space :Space) 
    {
        _juggler = juggler;
        _space = space;
                
        _juggler.addChild(this);
        _juggler.registerAsActor(this);
        _juggler.registerForCollisions(this);
        
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
    
    private function redraw (color:uint, alpha:Number = 1.0) :void
    {
        graphics.clear();
        graphics.beginFill(color, alpha);
        graphics.drawCircle(0,0, _ballRadius);
        graphics.endFill();
        
    }
    
    public function randomizePosition() :void
    {
        x = (Math.random() * _space.width()) + _space.left;
        y = (Math.random() * _space.height()) + _space.top
        dx = (Math.random() * 16) - 8 * _space.frameRate;
        dy =  (Math.random() * 16) - 8 * _space.frameRate;
    }    
    
    public function caughtBy(hand:Hand) :void
    {
        _hand = hand;
        _motion = caught;
        _catchFrames = 3;
    }
    
    public function release() :void
    {
        _hand = null;
        _motion = free;
    }
    
    public function nextFrame() : void
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
    private function caught() :void
    {
        Juggler.log(_label+" (pre caught) velocity : dx="+dx+", dy="+dy);
        Juggler.log(_label+" x="+x+",y="+y);                
        var bounds:NormalizedBounds = _hand.getNormalizedBounds(_juggler);
                
        if(_catchFrames > 0) 
        {
            _catchFrames -=1;
            
            var targetX:int = bounds.getX();
            var targetY:int = bounds.topProjection() - _ballRadius;
            
            x += (targetX - x) / 2
            y += (targetY - y) / 2
        } 
        else
        {
            x = bounds.getX();
            y = bounds.topProjection() -_ballRadius;
        }
        
        nextX = x
        nextY = y;    
        
        Juggler.log(_label+" (caught) velocity : dx="+dx+", dy="+dy);
        Juggler.log(_label+" x="+x+",y="+y);        
            
    }
        
    /** define the motion of the ball when it's free **/
    private function free() :void
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
            if (_fadeFrames < 0) {
                dy = DEATH_BOUNCE;
                y = _space.bottom;  // if you don't do this then gravity forces the ball 
                                //through the floor!
                startFading();
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
        
    /** start fading out this ball **/
    private function startFading() :void
    {
        _juggler.deregisterForCollisions(this);
        _fadeFrames = FADE_DURATION;
        _motion = fading;
    }
        
    private function fading() :void
    {
        _fadeFrames -= 1;
        free();
        var alpha:Number = _fadeFrames / FADE_DURATION;
        redraw(REGULAR_COLOR, alpha * alpha);
        
        if (_fadeFrames <= 0)
        {
            _juggler.deregisterAsActor(this);
        }
    }
        
    /** calculate the expected position next frame **/
    private function project () :void
    {        
        nextX = x + (dx / _space.frameRate);
        nextY = y + (dy / _space.frameRate);
//        Juggler.log(_label+" nextX="+nextX+",nextY="+nextY);
    }
        
    private function advance () :void
    {
        x = nextX;
        y = nextY;
    }
        
    public function leftProjection() :Number
    {
        return nextX - _ballRadius;
    }
    
    public function rightProjection() :Number
    {
        return nextX + _ballRadius;
    }
    
    public function topProjection() :Number
    {
        return nextY - _ballRadius;
    }
    
    public function bottomProjection() :Number
    {
        return nextY + _ballRadius;
    }
    
    public function getMass() :Number
    {
        return _mass;
    }
    
    public function getPosition() :Array
    {
        return new Array(x, y);
    }
    
    public function getVelocity() :Array
    {
        return new Array(dx, dy);
    }
    
    public function setVelocity(v: Array) :void
    {
        dx = v[0];
        dy = v[1];
        Juggler.log(_label+" new velocity : dx="+dx+", dy="+dy);
        Juggler.log(_label+" x="+x+",y="+y);        
        _velocityChanged = true;
    }
     
    public function collisionWith(other:CanCollide) :void
    {
        if (other is Ball)
        {        
            var results:Array = _elasticCollision.collide( 
                this.getPosition(), this.getVelocity(), this.getMass(),
                other.getPosition(), other.getVelocity(), other.getMass()
            );
        
            this.setVelocity(results[0]);
            other.setVelocity(results[1]);
        }
        else if (other is Hand) 
        {
            Hand(other).touchBall(this);
        }
    }
        
    public function setLabel(label:String) :void
    {
        _label = label;        
    }

    public function getLabel() :String
    {
        return _label;
    }
    
    public function getParent() : DisplayObjectContainer
    {
        return parent;
    }
    
    public function getX() :int
    {
        return x;
    }
    
    public function getY() :int
    {
        return y;
    }
    
    public function getNormalizedBounds(target:DisplayObjectContainer) :NormalizedBounds
    {
        if (_normalizedBounds==null || _normalizedBounds.target != target) {
            _normalizedBounds = new NormalizedBounds(target, this);
        } 
        
        return _normalizedBounds;
    }
    
    private static var _elasticCollision :ElasticCollision = new ElasticCollision();
    
    private var _normalizedBounds:NormalizedBounds;
    
    /** vertical component of velocity **/
    private var dy :Number; // pixels per second
    
    /** horizontal component of velocity **/
    private var dx :Number; // pixels per second
        
    /** position at next frame assuming no collision **/
    private var nextX :Number; 
    
    /** position at next frame assuming no collision **/
    private var nextY :Number;
            
    private static var HIGHLIGHT_COLOR :uint = 0xFF0000;
    
    private static var REGULAR_COLOR :uint = 0x000080;
    
    private static var FADE_DURATION:int = 100;
        
    private static var DEATH_BOUNCE:Number = -200;
        
    private static var _ballRadius :int = 15;
        
    private static var _elasticity :Number = 0.95;
    
    private var _label:String = "a ball";
    
    private var _mass :Number = 1;
    
    private var _juggler :Juggler;
    
    private var _space :Space;
    
    private var _highlightFrames :int = 0;
    
    private var _motion:Function = free;
    
    private var _hand:Hand; 
    
    private var _catchFrames:int;
    
    private var _velocityChanged:Boolean = true;
        
    private var _fadeFrames:int = -1;
}
}