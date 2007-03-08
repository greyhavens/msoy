package {

import flash.display.MovieClip;
import flash.display.Sprite;

import flash.events.Event;

import flash.geom.Point;

import mx.utils.ObjectUtil;

import org.cove.ape.AbstractParticle;
import org.cove.ape.CircleParticle;
import org.cove.ape.RectangleParticle;
import org.cove.ape.Vector;

import com.threerings.util.EmbeddedSwfLoader;

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

        this.x = x;
        this.y = y;
        this.rotation = rotation;


        var loader :EmbeddedSwfLoader = new EmbeddedSwfLoader();
        loader.addEventListener(Event.COMPLETE, function (event :Event) :void {
            _animation = new (loader.getClass("card" + number))();
            _animation.addEventListener(Event.ENTER_FRAME, endAnimation);
            addChild(_animation);
        });

        loader.load(new WICKET_ART_CLASS());

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
        var rad :Number = rotation * Math.PI/180;
        var a :Point = new Point(x + Math.sin(rad)*48.5, 
                                 y + Math.cos(rad)*-48.5);
        var b :Point = new Point(x + Math.sin(rad)*-60,
                                 y + Math.cos(rad)*60);
        return [a, b];
    }

    protected function endAnimation (event :Event) :void
    {
        if (_animation.currentFrame >= _animation.currentScene.numFrames) {
            _animation.stop();
        }
    }

    /** The card's number. */
    protected var _number :int;

    /** The animation for this card. */
    protected var _animation :MovieClip;

    [Embed(source="rsrc/cards.swf", mimeType="application/octet-stream")]
    protected static const WICKET_ART_CLASS :Class;
}
}
