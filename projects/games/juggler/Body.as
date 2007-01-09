package {

import flash.display.Sprite;

public class Body extends Sprite
    implements Actor
{

    public function Body (juggler:Juggler, space:Space) :void {
       _juggler = juggler;
       _space = space;
       _hands[LEFT] = new Hand(juggler, space, this);
       _hands[RIGHT] = new Hand(juggler, space, this);
             
       _positions[LEFT] = new Array(-200, -100);
       _positions[RIGHT] = new Array(100, 200);
   
        _hands[LEFT].x = _positions[LEFT][LEFT];
        _hands[RIGHT].x = _positions[RIGHT][RIGHT];   
   
        _hands[LEFT].setLabel("left hand");
        _hands[RIGHT].setLabel("right hand");
   
       juggler.addChild(this);
       juggler.registerAsActor(this);
       
       draw();
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
    
    public function moveHand(hand:Hand, pos:int) :void
    {
        hand.moveTo(pos);
    }
    
    /** command to move the left hand one position to the left **/
    public function leftHandLeft() :void 
    {
        _hands[LEFT].catchKeys++;
        moveHand(_hands[LEFT], _positions[LEFT][LEFT]);
    }

    public function leftHandLeftUp() :void
    {
        _hands[LEFT].catchKeys--;
    }

    /** command to move the left hand one position to the right **/
    public function leftHandRight() :void
    {
        _hands[LEFT].catchKeys++;
        moveHand(_hands[LEFT], _positions[LEFT][RIGHT]);
    }

    public function leftHandRightUp() :void
    {
        _hands[LEFT].catchKeys--;
    }

    /** command to move the right hand one position to the left **/
    public function rightHandLeft() :void
    {
        _hands[RIGHT].catchKeys++;
        moveHand(_hands[RIGHT], _positions[RIGHT][LEFT]);
    }

    public function rightHandLeftUp() :void
    {
        _hands[RIGHT].catchKeys--;
    }

    /** command to move the right hand one position to the right **/
    public function rightHandRight() :void
    {
        _hands[RIGHT].catchKeys++;
        moveHand(_hands[RIGHT], _positions[RIGHT][RIGHT]);
    }

    public function rightHandRightUp() :void
    {
        _hands[RIGHT].catchKeys--;
    }

    private static const LEFT:int = 0;

    private static const RIGHT:int = 1;

    public static const HAND_LEVEL:int = 0;

    private var _juggler:Juggler;

    private var _space:Space;

    private var _hands:Array = new Array()

    private var _positions:Array = new Array();
}
}