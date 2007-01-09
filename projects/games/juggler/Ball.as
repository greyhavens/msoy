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
    
    private function redraw (color:uint) :void
    {
        graphics.clear();
        graphics.beginFill(color);
        graphics.drawCircle(0,0, _ballRadius);
        graphics.endFill();
        
    }
    
    public function randomizePosition() :void
    {
        x = (Math.random() * _space.width()) + _space.left;
        y = (Math.random() * _space.height()) + _space.top
        dx = (Math.random() * 16) - 8;
        dy =  (Math.random() * 16) - 8;
    }    
    
    public function caughtBy(hand:Hand) :void
    {
        _hand = hand;
        _motion = caught;
        _catchFrames = 3;
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
            y = bounds.topProjection() - _ballRadius;
        }
    }
        
    /** define the motion of the ball when it's free **/
    private function free() :void
    {
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
            dy = -Math.abs(dy) * _elasticity;
            y = _space.bottom;  // if you don't do this then gravity forces the ball 
                                //through the floor!
        }
        
        // update velocity based on constants;
        
        // friction always opposes motion
        dx += (dx>0) ? -_space.frictionPerFrame : _space.frictionPerFrame;
        dy += (dy>0) ? -_space.frictionPerFrame : _space.frictionPerFrame;
        
        // gravity is always in one direction
        dy += _space.gravityPerFrame;            
    }
        
    private function advance () :void
    {
        // update positions based on velocity;
        x = x+dx;
        y = y+dy;
    }
        
    public function leftProjection() :Number
    {
        return x + dx - _ballRadius;
    }
    
    public function rightProjection() :Number
    {
        return x + dx + _ballRadius;
    }
    
    public function topProjection() :Number
    {
        return y + dy - _ballRadius;
    }
    
    public function bottomProjection() :Number
    {
        return y + dy + _ballRadius;
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
    private var dy :Number;
    
    /** horizontal component of velocity **/
    private var dx :Number;
        
    private static var HIGHLIGHT_COLOR :uint = 0xFF0000;
    
    private static var REGULAR_COLOR :uint = 0x000080;
        
    private static var _ballRadius :int = 30;
        
    private static var _elasticity :Number = 0.975;
    
    private var _label:String = "a ball";
    
    private var _mass :Number = 1;
    
    private var _juggler :Juggler;
    
    private var _space :Space;
    
    private var _highlightFrames :int = 0;
    
    private var _motion:Function = free;
    
    private var _hand:Hand; 
    
    private var _catchFrames:int;
}
}