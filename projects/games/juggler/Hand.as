package {

import flash.display.Sprite;
import flash.display.DisplayObjectContainer;
    
public class Hand extends Sprite 
    implements Actor, CanCollide
{
    public function Hand(juggler:Juggler, space:Space, body:Body) :void 
    {
        _juggler = juggler;
        _space = space;
        _body = body;
        
        y = Body.HAND_LEVEL;        
        
        body.addChild(this);
        Juggler.log("hand registering for collisions");
        juggler.registerForCollisions(this);
        draw();
    }

    public function draw() :void
    {
        Juggler.log("drawing hand normally");
        redraw(NORMAL_COLOR);
    }

    private function redraw(color:uint) :void
    {
        graphics.clear();
        graphics.beginFill(color);
        var x:int = -_width/2;
        var y:int = -_height/2;
        graphics.drawRoundRect(x, y, _width, _height, 
            CORNER_WIDTH, CORNER_HEIGHT);
        graphics.endFill();
    }

    public function highlight() :void
    {
        return;
        Juggler.log("hightlighting hand");
        redraw(HIGHLIGHT_COLOR);
        _highlightFrames = 2;        
    }
    
    /** start moving to the given horizontal position **/
    public function moveTo(x:int) :void
    {
        targetX = x;
        targetFrames = 3;
        _motion = converge;
    } 
    
    /** function defining a motion converging on a specific x location in a fixed
      * number of frames.
      */
    private function converge() :void
    {
        if (targetFrames == 0) {
            x = targetX;
            _motion = stationary;
        }
        
        targetFrames -=1;
        x += (targetX - x) / 2
        
        Juggler.log("moving "+_label+" to "+x);
    }
    
    /** function definition a 'stationary' hand.
     */
    public function stationary() :void
    {
    }
    
    public function collisionWith(other:CanCollide) :void 
    {
        // we only care about collisions with balls
        if (other is Ball) 
        {
            touchBall(other as Ball);
        }
    }
    
    
    /* a is in contact with us */
    public function touchBall(ball:Ball) :void
    {
        // if we're not holding anything, and we are trying to catch
        // then we can catch the ball.
        if (_holding == null)
        {
            if (catching()) 
            {
                _holding = ball;
                ball.caughtBy(this);
            }
        }
    }

    /** return true if we're trying to catch the next ball **/
    public function catching() :Boolean
    {
        return catchKeys>0;
    }
    
    public function nextFrame() :void
    {
        // if the hand is highlighted, check whether it should 
        // be set back to normal
        if (_highlightFrames > 0) {
            _highlightFrames -= 1;
            if (_highlightFrames == 0) {
                draw();
            }
        }
        
        // call whatever the current motion function is
        _motion();
    }
    
    public function topProjection() :Number 
    {
        return y - (_height/2);
    }
    
    public function bottomProjection() :Number 
    {
        return y + (_height/2);
    }

    public function leftProjection() :Number 
    {
        return x - (_width/2);
    }
    
    public function rightProjection() :Number 
    {
        return x + (_width/2);
    }

    public function getPosition() :Array
    {
        return new Array(x, y);
    }

    public function setVelocity(v:Array) :void
    {
        Juggler.log("attempt to set velocity of hand to "+v);
    }

    public function getVelocity() :Array
    {
        return new Array(0, 0);
    }

    public function getMass() :Number
    {
        return _mass;
    }

    public function setLabel(label:String) :void
    {
        _label = label;        
    }

    public function getLabel() :String
    {
        return _label;
    }

    public function getParent():DisplayObjectContainer 
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

    private static var CORNER_WIDTH:int = 10;
    
    private static var CORNER_HEIGHT:int = 10;

    private static var NORMAL_COLOR:uint = 0x008000;

    private static var HIGHLIGHT_COLOR:uint = 0xFF0000;

    private static var _ballisticTrajectory:BallisticTrajectory = new BallisticTrajectory();

    private var _holding:Ball;

    private var _normalizedBounds:NormalizedBounds;

    private var _highlightFrames:int = 0;

    private var _width:Number = 100;
    
    private var _height:Number = 20;
    
    private var _mass:Number = 3; // metal bat!
    
    private var _space:Space;
    
    private var _juggler:Juggler;

    private var _body:Body;
    
    private var _motion:Function = stationary;
 
    private var targetX:int = x;
    
    private var targetFrames:int = 0;
        
    private var _label:String = " a hand";
    
    public var catchKeys:int = 0;
}
}