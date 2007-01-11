package {

import flash.display.Sprite;
import flash.geom.Point;

import org.cove.ape.Vector;
import org.cove.ape.RectangleParticle;

import mx.core.SpriteAsset;

public class WonderlandMap
{
    // Where we should spawn new croquet balls at the beginning of the game
    public var startPoint :Point;

    // Our background
    public var background :Sprite;

    // Our foreground
    public var foreground :Sprite;

    // A list of all the particles in our map
    public var particles :Array;

    // Our wickets
    public var wickets :Array;

    public function WonderlandMap ()
    {
        startPoint = new Point(0, 0);
        background = new Sprite();
        foreground = new Sprite();
        foreground.mouseChildren = false;
        foreground.mouseEnabled = false;
        particles = [];
        wickets = [];

        addObjects();
        addWickets();

        // FIXME: This is all very cludgy and awkward, and will need to get revamped
        // when we have fancier things than just planters and wickets...
        for each (var wicket :Wicket in wickets) {
            particles = particles.concat(wicket.particles);
        }
    }

    /**
     * Applies modifier forces to a ball based on the type of terrain it's currently on.
     */
    public function applyModifierForce (ball :BallParticle) :void
    {
        var speed :Number = ball.velocity.magnitude();
        if (speed == 0) {
            return;
        }

        if (speed < 0.1) {
            // It's practically stopped, so stop it.
            ball.velocity = STOPPED_VECTOR;
            return;
        }

        var force :Vector = ball.velocity.mult(-1).multEquals(1/speed);

        if (isRough(ball.px, ball.py)) {
            force.multEquals(Math.min(speed, FRICTION_ROUGH));
            //force = rotateFriction (force, ball.velocity, Math.PI);

        } else if (isStone(ball.px, ball.py)) {
            force.multEquals(Math.min(speed, FRICTION_STONE));
            //force = rotateFriction (force, ball.velocity, Math.PI/1.25);

        } else {
            force.multEquals(Math.min(speed, FRICTION_GRASS));
        }

        ball.addMasslessForce(force);
    }

    /**
     * Returns a new vector which corresponds to applying the same friction vector, but also 
     * rotating the velocity vector the specified amount.
     */
    protected function rotateFriction (friction :Vector, velocity :Vector, angle :Number) :Vector
    {
        var v :Vector = new Vector(0,0);
        v.x = Math.cos(angle) * (velocity.x + friction.x) - 
              Math.sin(angle) * (velocity.y + friction.y);
        v.y = Math.cos(angle) * (velocity.y + friction.y) +
              Math.sin(angle) * (velocity.x + friction.x);

        return v.minus(velocity);
    }

    /**
     * Returns whether the given coordinate corresponds to a patch of rough.
     */
    protected function isRough (x :Number, y :Number) :Boolean
    {
        return false;
    }

    /**
     * Returns whether the given coordinate corresponds to a patch of stone.
     */
    protected function isStone (x :Number, y :Number) :Boolean
    {
        return false;
    }

    /**
     * Add physical obsticles to the map.
     */
    protected function addObjects () :void
    {
        // default one has no objects
    }

    /**
     * Add the wickets to the map.
     */
    protected function addWickets () :void
    {
        // default one has no wickets
    }

    /**
     * Adds thick, bunker walls surrounding the board, defining the play area as a rectangle
     * with the specified dimensions.
     */
    protected function addWalls (width :int, height :int) :void
    {
        particles.push(new RectangleParticle(
            width/2, 0 - WALL_WIDTH/2, width + WALL_WIDTH*2, WALL_WIDTH, 0, true));
        particles.push(new RectangleParticle(
            width/2, height + WALL_WIDTH/2, width + WALL_WIDTH*2, WALL_WIDTH, 0, true));

        particles.push(new RectangleParticle(
            0 - WALL_WIDTH/2, height/2, WALL_WIDTH, height + WALL_WIDTH*2, 0, true));
        particles.push(new RectangleParticle(
            width + WALL_WIDTH/2, height/2, WALL_WIDTH, height + WALL_WIDTH*2, 0, true));

    }

    /**
     * Adds a planter box at the specified coordinate and rotation. Can optionally also place
     * a bush in it for you.
     */
    protected function addPlanter (x :Number, y :Number, rotation :Number = 0, 
        bush :Boolean = false, bushRotation :Number = 0) :void
    {
        var planter :SpriteAsset = new Planter();
        planter.x = x;
        planter.y = y;
        planter.rotation = rotation;

        background.addChild(planter);

        var particle :RectangleParticle = new RectangleParticle(
            x, y, PLANTER_WIDTH, PLANTER_HEIGHT, rotation * Math.PI / 180, true);
        particles.push(particle);

        if (bush) {
            addBush(x, y, bushRotation);
        }
    }

    /**
     * Adds a bush at the specified coordinate and rotation.
     */
    protected function addBush (x :Number, y :Number, rotation :Number) :void
    {
        var bush :SpriteAsset = new Bush();
        bush.x = x;
        bush.y = y;
        bush.rotation = rotation;

        foreground.addChild(bush);
    }

    // Friction constants for the various terrain types
    protected static const FRICTION_GRASS :Number = 3;
    protected static const FRICTION_STONE :Number = 1.5;
    protected static const FRICTION_ROUGH :Number = 7;

    // A 0,0 vector for convenience
    protected static const STOPPED_VECTOR :Vector = new Vector(0, 0);

    // How thick to make our walls
    protected static const WALL_WIDTH :int = 100;

    // Dimensions for planters
    protected static const PLANTER_WIDTH :int = 104;
    protected static const PLANTER_HEIGHT :int = 104;

    // Artwork for the standard planter boxes
    [Embed (source="rsrc/objects.swf#physplanterbox")]
    protected static var Planter :Class;

    // Artwork for the standard bushes
    [Embed (source="rsrc/objects.swf#bush")]
    protected static var Bush :Class;
}
}
