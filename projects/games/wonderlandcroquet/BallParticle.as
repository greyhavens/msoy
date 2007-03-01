package {

import flash.display.Sprite;
import flash.geom.Point;
import org.cove.ape.CircleParticle;
import org.cove.ape.Vector;

import com.threerings.ezgame.EZGameControl;

/**
 * A particle type that acts like a circle, but moves around a sprite rather 
 * than drawing a boring circle.
 */
public class BallParticle extends CircleParticle
{
    /** The sprite representing this ball. */
    public var ball :Ball;

    /** Our top level WonderlandCroquet object. */
    public var wc :WonderlandCroquet;

    public function BallParticle (x:Number, y:Number, radius:Number, color:int,
        fixed:Boolean, mass:Number = 1, elasticity:Number = 0.3, friction:Number = 0)
    {
					
        super(x, y, radius, fixed, mass, elasticity, friction);

        ball = new Ball(this, color);
        ball.x = x;
        ball.y = y;
    }

    /**
     * Updates our sprite to match our position. Returns true if we move, false if we don't.
     */
    public function tick() :Boolean
    {
        if (ball.x == px && ball.y == py) {
            // If I'm in the same place, chances are my angle hasn't changed either, so I shouldn't
            // bother doing anything
            return false;
        }

        var turnHolderIdx :int = wc.gameCtrl.seating.getPlayerPosition(wc.gameCtrl.getTurnHolder());

        if (wc.gameCtrl.isMyTurn() && ball.playerIdx == wc.myIdx) {
            // Since I can't trust a stupid hit test to work if our ball is off the screen, let's
            // see if our line intersects the center line of the wicket we're aiming for.
            var wicket :Wicket = wc.map.wickets[wc.gameCtrl.get("wickets", turnHolderIdx)];
            var points :Array = wicket.getCenterLine();
            var scored :Boolean = passedWicket(new Point(ball.x, ball.y), new Point(px, py), 
                points[0], points[1]);

            if (scored) {
                wc.passedWicket();
            }
        }

        // Just move our sprite to the new location
        ball.x = px;
        ball.y = py;
        var speed :Number = velocity.magnitude();

        if (speed < 0.01) {
            velocity = new Vector(0, 0);
            // We don't really care
            ball.stop();
            return false;
        }

        ball.play();

        // and then rotate it to match the direction we're moving
        var angle :Number = (180 / Math.PI) * Math.acos(new Vector(0,-1).dot(velocity) / speed);

        if (velocity.x < 0) {
            angle = 360 - angle;
        }

        ball.rotation = angle;

        // TODO: tell the ball about our velocity so the animation can switch to an appropriate 
        // speed. Suprisingly enough, it doesn't look terrible with the animation at a constant
        // speed regardless of the ball's motion, so we might be able to indefinitely avoid fixing
        // this.
        return true;
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
    public function addHitForce (x :Number, y :Number) :void
    {
        addMasslessForce(new Vector(x, y));
    }

    /**
     * Looks to see if the line segments described by p1 -> p2, p3 -> p4 intersect.
     */
    protected function passedWicket (p1 :Point, p2 :Point, p3 :Point, p4 :Point) :Boolean
    {
        var denom :Number = (p4.y - p3.y)*(p2.x - p1.x) - (p4.x - p3.x)*(p2.y - p1.y);
        var anum :Number = (p4.x - p3.x)*(p1.y - p3.y) - (p4.y - p3.y)*(p1.x - p3.x);
        var bnum :Number = (p2.x - p1.x)*(p1.y - p3.y) - (p2.y - p1.y)*(p1.x - p3.x);
        
        if (denom == 0) {
            // Lines are parallel
            if (anum == bnum == 0) {
                // coincident
                return true;
            }
            
            return false;
        }

        var ua :Number = anum/denom;
        var ub :Number = bnum/denom;

        if ((ua < 1 && ua > 0) && (ub < 1 && ub > 0)) {
            return true;
        }

        return false;
    }

}

}
