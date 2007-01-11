package {

import flash.display.Sprite;
import flash.events.Event;
import mx.utils.ObjectUtil;
import mx.core.MovieClipAsset;
import org.cove.ape.RectangleParticle;

/**
 * A Wicket.
 */
public class Wicket extends Sprite
{
    // The particles corresponding to this card.
    public var particles :Array = [];

    public function Wicket (number :int, x :Number, y :Number, rotation :Number)
    {
        mouseChildren = false;
        mouseEnabled = false;
        _number = number;
        _animation = new cardAnimations[number - 1];
        _animation.x = x;
        _animation.y = y;
        _animation.rotation = rotation;
        _animation.addEventListener(Event.ENTER_FRAME, endAnimation);
        addChild(_animation);

        // TODO: add particles
    }

    protected function endAnimation (event :Event) :void
    {
        if (_animation.currentFrame >= _animation.currentScene.numFrames) {
            _animation.stop();
        }
    }

    // The card's number.
    protected var _number :int;

    // The animation for this card.
    protected var _animation :MovieClipAsset;

    // The card artwork.
    [Embed(source="rsrc/cards.swf#carda")]
    protected static var Card1 :Class;
    [Embed(source="rsrc/cards.swf#card2")]
    protected static var Card2 :Class;
    [Embed(source="rsrc/cards.swf#card3")]
    protected static var Card3 :Class;
    [Embed(source="rsrc/cards.swf#card4")]
    protected static var Card4 :Class;
    [Embed(source="rsrc/cards.swf#card5")]
    protected static var Card5 :Class;
    [Embed(source="rsrc/cards.swf#card6")]
    protected static var Card6 :Class;
    [Embed(source="rsrc/cards.swf#card7")]
    protected static var Card7 :Class;
    [Embed(source="rsrc/cards.swf#card8")]
    protected static var Card8 :Class;
    [Embed(source="rsrc/cards.swf#card9")]
    protected static var Card9 :Class;
    [Embed(source="rsrc/cards.swf#card10")]
    protected static var Card10 :Class;

    protected static var cardAnimations :Array = [
        Card1, Card2, Card3, Card4, Card5, Card6, Card7, Card8, Card9, Card10,
    ];

}

}
