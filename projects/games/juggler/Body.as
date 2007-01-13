package {

import flash.display.Sprite;
import flash.display.DisplayObject;

public class Body extends Sprite
    implements Actor
{
    public function Body (juggler:PlayField, space:Space) :void 
    {
       _juggler = juggler;
       _space = space;
       
       _hands[LEFT] = new Hand(juggler, space, this, LEFT, leftHand);
       _hands[RIGHT] = new Hand(juggler, space, this, RIGHT, rightHand);
             
       _positions[LEFT] = new Array(-180, -100);
       _positions[RIGHT] = new Array(100, 180);
   
		// these are a hack - the targetting routine is innacurate
		// and we use these as a way to adjust for that.  We really
		// need to fix the targetting routine properly.
        _target[LEFT] = new Array(-155, -85);
        _target[RIGHT] = new Array(85, 155);
   
        _hands[LEFT].x = _positions[LEFT][LEFT];
        _hands[RIGHT].x = _positions[RIGHT][RIGHT];   
   
        _hands[LEFT].label = "left hand";
        _hands[RIGHT].label = "right hand";
   
       juggler.addChild(this);
       juggler.registerAsActor(this);
       
       var art:Object = new pandaBody();
       art.x = -150;
       art.y = -315;
       addChild(art as DisplayObject);
       
//      draw();
    }

    private function draw() :void 
    {
        graphics.beginFill(0xA0A0A0);
        graphics.drawRect(-200, -25, 400, 50);
        graphics.endFill();
        graphics.beginFill(0xC0C0C0);
        graphics.drawRect(-100, -25, 200, 50);
        graphics.endFill();
    }

    public function nextFrame() :void 
    {
        _hands[LEFT].nextFrame();
        _hands[RIGHT].nextFrame();
    }
    
    private function moveHand(which:int, where:int) :void
    {
        if (_addBall && _hands[which]._holding == null)
        {
            _hands[which].addBall();
            _addBall = false;
        }
        
        _hands[which].moveTo(_positions[which][where]);
    }
    
    public function addBall() :void
    {
        _addBall = true;
    }
        
    /** command to move the left hand one position to the left **/
    public function leftHandLeft() :void 
    {
        moveHand(LEFT, LEFT);
    }

    /** command to move the left hand one position to the right **/
    public function leftHandRight() :void
    {
        moveHand(LEFT, RIGHT);
    }

    /** command to move the right hand one position to the left **/
    public function rightHandLeft() :void
    {
        moveHand(RIGHT, LEFT);
    }

    /** command to move the right hand one position to the right **/
    public function rightHandRight() :void
    {
        moveHand(RIGHT, RIGHT);
    }

    public function isCatching(id:int) :Boolean
    {
        switch (id)
        {
            case LEFT:
                return _controller.leftDown();
            case RIGHT:
                return _controller.rightDown();
        }
        
        return false;
    }

    public function computeReleaseVelocity(hand:Hand, ball:Ball, strength:Number) :void
    {
        switch (hand._id)
        {
            case LEFT:
                ball.velocity = new Array(200 * strength, -700 * strength);
                break;
            case RIGHT:
                ball.velocity = new Array(-200 * strength, -700 * strength);            
        }
    }   

    public function ballisticReleaseVelocity(fromHand:Hand, ball:Ball, strength:Number) :void
    {
        var toHand:int = (fromHand._id == LEFT) ? RIGHT : LEFT;
                
        var toPosition:int = 
            (Math.abs(fromHand.x-_positions[fromHand._id][LEFT]) < 
                Math.abs(fromHand.x-_positions[fromHand._id][RIGHT])) ?
            LEFT : RIGHT;    
        
        var v:Array = _ballisticTrajectory.initialVector(
            new Array( ball.x, ball.y ),
            new Array( _target[toHand][toPosition] + x, 
                y + HAND_LEVEL + (_hands[toHand]._height/2)),
            Hand.MAX_SPEED * strength,
            _space.gravity );
        
        if (v==null) // the throw wasn't strong enough - so we'll throw at 45 degrees
        {
            var unit:Number = Math.sqrt((Hand.MAX_SPEED * strength * Hand.MAX_SPEED * strength)
                                / 2);
            v = new Array( unit, unit );
        }
        
        
        // make sure the vector is pointing in the direction we expect
        v[0] = v[0] < 0 ?
            // ball is going left already
            (toHand==LEFT ? v[0] : -v[0]):
            // ball is going right already
            (toHand==LEFT ? -v[0] : v[0]);
        
        // switch up for down if necessary
        v[1] = v[1] > 0 ? -v[1] : v[1]; 
            
        ball.velocity = v;
        
    }

    public function set controller(controller:JugglingController) :void 
    {
        _controller = controller;
    }

    private static const LEFT:int = 0;

    private static const RIGHT:int = 1;

    public static const HAND_LEVEL:int = -160;

    private static var _ballisticTrajectory:BallisticTrajectory = new BallisticTrajectory();

    private var _juggler:PlayField;

    private var _space:Space;
    
    private var _controller:JugglingController;

    private var _hands:Array = new Array()

    private var _positions:Array = new Array();

    private var _target:Array = new Array();
    
    private var _addBall:Boolean = false;

    [Bindable]
    [Embed(source="rsrc/panda_body.swf")]
    private var pandaBody:Class;

    [Bindable]
    [Embed(source="rsrc/panda_hand_left.swf")]
    private var leftHand:Class;
    
    [Bindable]
    [Embed(source="rsrc/panda_hand_right.swf")]
    private var rightHand:Class;
}
}