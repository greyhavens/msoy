package {

import flash.display.Sprite;
import flash.events.Event;
import flash.geom.Point;
import mx.utils.ObjectUtil;
import mx.core.MovieClipAsset;
import org.cove.ape.AbstractParticle;
import org.cove.ape.CircleParticle;
import org.cove.ape.RectangleParticle;
import org.cove.ape.Vector;

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

        var rad :Number = rotation * Math.PI/180;
        particles.push(new RectangleParticle(0.1, -48.5, 113, 10, rad, true));
        particles.push(new RectangleParticle(0.1, 60, 113, 10, rad, true));
        particles.push(new CircleParticle(0.6, -72.7, 26, true));

        // And now rotate/translate its position
        for each (var particle :AbstractParticle in particles) {
            var p :Vector = particle.position;
            p = new Vector(
                Math.cos(rad)*p.x - Math.sin(rad)*p.y,
                Math.cos(rad)*p.y + Math.sin(rad)*p.x);

            p.plusEquals(new Vector(x, y));
            particle.position = p;
        }
    }

    /**
     * Returns a two element array with the end points of a line segment representing the
     * center of this wicket.
     */
    public function getCenterLine () :Array
    {
        var rad :Number = _animation.rotation * Math.PI/180;
        var a :Point = new Point(_animation.x + Math.sin(rad)*48.5, 
                                 _animation.y + Math.cos(rad)*-48.5);
        var b :Point = new Point(_animation.x + Math.sin(rad)*-60,
                                 _animation.y + Math.cos(rad)*60);
        return [a, b];
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
