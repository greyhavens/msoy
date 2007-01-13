package {

import flash.display.Sprite;
import flash.display.DisplayObjectContainer;
import flash.display.DisplayObject;
    
public class Hand extends Sprite 
    implements Actor, CanCollide
{
    public function Hand(juggler:PlayField, space:Space, body:Body, id:int, artwork:Class) :void 
    {
        _id = id;
        _juggler = juggler;
        _space = space;
        _body = body;
            
        y = Body.HAND_LEVEL;        
        _maximumPullback = y + MAXIMUM_PULLBACK;
        
        body.addChild(this);
        juggler.registerForCollisions(this);
        
        var art:Object = new artwork();
        art.x = -27;
        art.y = -23;
        addChild(art as DisplayObject);
        
        draw();
    }

    public function draw() :void
    {
        redraw(NORMAL_COLOR);
    }

    private function redraw(color:uint) :void
    {
        if (Juggler.DEBUG_GRAPHICS)
        {
            graphics.clear();
            graphics.beginFill(color, 0.8);
            const x:int = -_width/2;
            const y:int = -_height/2;
            graphics.drawRoundRect(x, y, _width, _height, 
                CORNER_WIDTH, CORNER_HEIGHT);
            graphics.endFill();
        }
    }

    public function highlight() :void
    {
        return;
        redraw(HIGHLIGHT_COLOR);
        _highlightFrames = 2;        
    }
    
    /** start moving to the given horizontal position **/
    public function moveTo(x:int) :void
    {
        targetX = x;
        targetFrames = MOVE_FRAMES;
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
    }
    
    /** function defining the throwing motion 
     */
    public function throwBall() :void
    {
        if (targetFrames == 0) {
            y = Body.HAND_LEVEL;
            _motion = stationary;
            releaseBall();
        } 
        else 
        {
            y += (Body.HAND_LEVEL - y) / targetFrames;
            targetFrames -=1;
        }
    } 
    
    /** function definition a 'stationary' hand.
     */
    public function stationary() :void
    {
        // don't move!
    }
    
    public function collisionWith(other:CanCollide) :void 
    {
        // we only care about collisions with balls
        if (other is Ball) 
        {
            touchBall(other as Ball);
        }
    }
    
    /** release the ball we're holding **/
    private function releaseBall() :void
    {        
        if (_releaseStrength > .90)
        {
            _body.ballisticReleaseVelocity(this, _holding, .85);
        } else
        {
            _body.ballisticReleaseVelocity(this, _holding, .73);
        }
        
        _holding.release();
        _holding = null;
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
                _juggler.scoreCard.ballCaught();
                ball.caughtBy(this);
            }
        }
    }

    /** return true if we're trying to catch the next ball **/
    private function catching() :Boolean
    {
        return _body.isCatching(_id);
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
        
        if (_motion != throwBall) 
        // if we're throwing, we don't care about pulling back or
        // letting go.
        {
            // decide what to do if we're holding a ball
            if (_holding != null)
            {            
                if (catching()) // if we're still 'catching' then pull back
                {
                    pullback();
                }
                else // otherwise let go.
                {   
                    targetFrames = THROW_FRAMES;
                    _releaseStrength = (y-Body.HAND_LEVEL) / (_maximumPullback-Body.HAND_LEVEL);
                    _motion = throwBall;
                }
            }
        }
    }
    
    // pull the hand back a bit more, ready for a throw
    public function pullback() :void
    {
        if (y < _maximumPullback) 
        {
            y += (_maximumPullback - y) / PULLBACK_RATE;
        }
    }
    
    // add a ball starting at this hand
    public function addBall() :void
    {
	 	const bounds:Bounds = boundsInContext(_juggler);
        const targetX:int = bounds.x;
        const targetY:int = bounds.topProjection - Ball.BALL_RADIUS;
	
        var ball:Ball = _juggler.ballBox.provideBall(targetX, targetY);
        _holding = ball;
        _juggler.scoreCard.ballAdded();
        ball.placeInHand(this);
    }
    
    public function get topProjection () :Number 
    {
        return y - (_height/2);
    }
    
    public function get bottomProjection () :Number 
    {
        return y + (_height/2);
    }

    public function get leftProjection () :Number 
    {
        return x - (_width/2);
    }
    
    public function get rightProjection () :Number 
    {
        return x + (_width/2);
    }

    public function get position () :Array
    {
        return new Array(x, y);
    }

    public function set velocity (v:Array) :void
    {
        // hand doesn't change velocity
    }

    public function get velocity () :Array
    {
        return new Array(0, 0);
    }

    public function get mass() :Number
    {
        return _mass;
    }

    public function set label (label:String) :void
    {
        _label = label;        
    }

    public function get label() :String
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

    private static const CORNER_WIDTH:int = 20;
    
    private static const CORNER_HEIGHT:int = 20;

    private static const NORMAL_COLOR:uint = 0x008000;

    private static const HIGHLIGHT_COLOR:uint = 0xFF0000;
    
    private static const MAXIMUM_PULLBACK:int = 50;
    
    private static const MOVE_FRAMES:int = 6;
    
    private static const THROW_FRAMES:int = 3;
 
    private static const PULLBACK_RATE:int = 6;

    public static const MAX_SPEED:int = 1100;

    public var _holding:Ball;

    private var _boundsInContext:BoundsInContext;

    private var _highlightFrames:int = 0;

    public var _width:Number = 47;
    
    public var _height:Number = 40;
    
    private const _mass:Number = 3; // metal bat!
    
    private var _space:Space;
    
    private var _juggler:PlayField;

    private var _body:Body;
    
    private var _motion:Function = stationary;
 
    private var targetX:int = x;
    
    private var targetFrames:int = 0;
        
    private var _label:String = " a hand";
    
    private var _maximumPullback:int;
 
    private var _releaseStrength:Number;
 
    public var _id:int;
 }
}