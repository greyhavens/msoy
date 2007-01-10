package {

import flash.display.Sprite;
import flash.geom.Point;
import org.cove.ape.CircleParticle;
import org.cove.ape.Vector;

/**
 * A particle type that acts like a circle, but moves around a sprite rather 
 * than drawing a boring circle.
 */
public class BallParticle extends CircleParticle
{
    /** The sprite representing this ball. */
    public var ball :Ball;

    public function BallParticle (x:Number, y:Number, radius:Number, color:int,
        fixed:Boolean, mass:Number = 1, elasticity:Number = 0.3, friction:Number = 0)
    {
					
        super(x, y, radius, fixed, mass, elasticity, friction);

        ball = new Ball(this, color);
        ball.x = x;
        ball.y = y;
    }

    // documentation inherited
    override public function paint() :void
    {
        if (ball.x == px && ball.y == py) {
            // If I'm in the same place, chances are my angle hasn't changed either, so I shouldn't
            // bother doing anything
            return;
        }

        // Just move our sprite to the new location
        ball.x = px;
        ball.y = py;
        var speed :Number = velocity.magnitude();

        if (speed < 0.01) {
            // We don't really care
            ball.stop();
            return;
        }

        ball.play();

        // and then rotate it to match the direction we're moving
        var angle :Number = (180 / Math.PI) * Math.acos(new Vector(0,-1).dot(velocity) / speed);

        if (velocity.x < 0) {
            angle = 360 - angle;
        }

        ball.rotation = angle;

        // TODO: tell the ball about our velocity so the animation can switch 
        // to an appropriate speed
    }

    /**
     * Applies a random force to this ball.
     */
    public function addRandomForce (oompf :Number = 100) :void
    {
        var angle :Number = Math.random() * 2 * Math.PI;
        
        addMasslessForce(new Vector(Math.cos(angle) * oompf, Math.sin(angle) * oompf));
    }

    /**
     * Applies a force based on a hit to this ball.
     */
    public function addHitForce (p :Point) :void
    {
        var angle :Number = Math.PI + Math.atan(p.y / p.x);
        var strength :Number = Math.sqrt(p.x*p.x + p.y*p.y) * 2;

        strength = Math.min(strength, 200);

        if (p.x < 0) {
            angle += Math.PI;
        }

        angle += ball.rotation * Math.PI/180;
        
        addMasslessForce(new Vector(Math.cos(angle) * strength, 
                                    Math.sin(angle) * strength));
    }
}

}
