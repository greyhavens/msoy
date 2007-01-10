package {

import flash.display.Sprite;

public class Body extends Sprite
    implements Actor
{

    public function Body (juggler:Juggler, space:Space) :void {
       _juggler = juggler;
       _space = space;
       _hands[LEFT] = new Hand(juggler, space, this, LEFT);
       _hands[RIGHT] = new Hand(juggler, space, this, RIGHT);
             
       _positions[LEFT] = new Array(-200, -100);
       _positions[RIGHT] = new Array(100, 200);
   
        _hands[LEFT].x = _positions[LEFT][LEFT];
        _hands[RIGHT].x = _positions[RIGHT][RIGHT];   
   
        _hands[LEFT].setLabel("left hand");
        _hands[RIGHT].setLabel("right hand");
   
       juggler.addChild(this);
       juggler.registerAsActor(this);
       
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

    public function nextFrame() :void {
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
    
    public function addBall() :Boolean
    {
        if (_addBall) {
            return false;
        }
        
        _addBall = true;
        return true;
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
                return _juggler.leftDown();
            case RIGHT:
                return _juggler.rightDown();
        }
        
        return false;
    }

    public function computeReleaseVelocity(hand:Hand, ball:Ball, strength:Number) :void
    {
        switch (hand._id)
        {
            case LEFT:
                ball.setVelocity( new Array(
                    200 * strength,
                    -700 * strength
                ));
                break;
            case RIGHT:
                ball.setVelocity( new Array(
                    -200 * strength,
                    -700 * strength
                ));            
        }
    }

    private static const LEFT:int = 0;

    private static const RIGHT:int = 1;

    public static const HAND_LEVEL:int = 0;

    private var _juggler:Juggler;

    private var _space:Space;

    private var _hands:Array = new Array()

    private var _positions:Array = new Array();
    
    private var _addBall:Boolean = false;
}
}