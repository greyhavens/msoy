package {
    
import flash.display.Sprite;
import Math;

public class Ball extends Sprite
    implements CanCollide
{
    
    public function Ball (juggler :Juggler, space :Space) 
    {
        _juggler = juggler;
        _space = space;
        draw();
    }
    
    /** draw the ball in it's regular color **/
    public function draw () :void
    {
//        Juggler.log("drawing ball normal color.");
        graphics.clear();
        graphics.beginFill(REGULAR_COLOR);
        graphics.drawCircle(0,0, _ballRadius);
        graphics.endFill();
    }
    
    /** highlight this particular ball for debugging purposes **/
    public function highlight () :void
    {
//        Juggler.log("drawing ball, highlight color");
        graphics.clear();
        graphics.beginFill(HIGHLIGHT_COLOR);
        graphics.drawCircle(0,0, _ballRadius);
        graphics.endFill();
        
        _highlightFrames = 2;
    }
    
    public function nextFrame() : void
    {
        advance();
        
        // if the ball is highlighted, check whether it should 
        // be set back to normal
        if (_highlightFrames > 0) {
            _highlightFrames -= 1;
            if (_highlightFrames == 0) {
                draw ();
            }
        }
        
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
            y = _space.bottom; // if you don't do this then gravity forces the ball through the floor!
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
    
    
    /** vertical component of velocity **/
    public var dy :Number;
    
    /** horizontal component of velocity **/
    public var dx :Number;
        
    protected static var HIGHLIGHT_COLOR :uint = 0xFF0000;
    
    protected static var REGULAR_COLOR :uint = 0x000080;
        
    protected static var _ballRadius :int = 30;
        
    protected static var _elasticity :Number = 0.975;
    
    protected var _mass :Number = 1;
    
    protected var _juggler :Juggler;
    
    protected var _space :Space;
    
    protected var _highlightFrames :int = 0;
}
}