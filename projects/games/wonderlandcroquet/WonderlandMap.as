package {

import flash.display.Sprite;
import org.cove.ape.Vector;
import org.cove.ape.RectangleParticle;

import mx.core.SpriteAsset;

public class WonderlandMap
{
    // Our background
    public var background :Sprite;

    // Our foreground
    public var foreground :Sprite;

    // A list of all the particles in our map
    public var particles :Array;

    public function WonderlandMap ()
    {
        background = new Sprite();
        foreground = new Sprite();
        particles = [];
    }

    // Applies modifier forces to a ball based on the map
    public function applyModifierForce (ball :BallParticle) :void
    {
        if (ball.velocity.magnitude() == 0) {
            return;
        }

        if (ball.velocity.magnitude() < 0.1) {
            // It's practically stopped, so stop it.
            ball.velocity = STOPPED_VECTOR;
            return;
        }

        var force :Vector = ball.velocity.mult(-1).multEquals(1/ball.velocity.magnitude());

        if (isRough(ball.px, ball.py)) {
            force.multEquals(FRICTION_ROUGH);
            // TODO: jiggle

        } else if (isStone(ball.px, ball.py)) {
            force.multEquals(FRICTION_STONE);
            // TODO: jiggle

        } else {
            force.multEquals(FRICTION_GRASS);
        }

        ball.addMasslessForce(force);
    }

    // Returns whether the given coordinate corresponds to a patch of rough
    protected function isRough (x :Number, y :Number) :Boolean
    {
        return false;
    }

    // Returns whether the given coordinate corresponds to a patch of stone
    protected function isStone (x :Number, y :Number) :Boolean
    {
        return false;
    }

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

    protected function addPlanter (x :Number, y :Number, rotation :Number = 0, 
        bush :Boolean = false, bushRotation :Number = 0) :void
    {
        var planter :SpriteAsset = new planterClass();
        planter.x = x;
        planter.y = y;
        planter.rotation = rotation;

        background.addChild(planter);

        var particle :RectangleParticle = new RectangleParticle(
            x, y, 104, 104, rotation * Math.PI / 180, true);
        particles.push(particle);

        if (bush) {
            addBush(x, y, bushRotation);
        }
    }

    protected function addBush (x :Number, y :Number, rotation :Number) :void
    {
        var bush :SpriteAsset = new bushClass();
        bush.x = x;
        bush.y = y;
        bush.rotation = rotation;

        foreground.addChild(bush);
    }

    protected static const FRICTION_GRASS :Number = 1;
    protected static const FRICTION_STONE :Number = 0;
    protected static const FRICTION_ROUGH :Number = 5;

    protected static const STOPPED_VECTOR :Vector = new Vector(0, 0);

    // How thick to make our walls
    protected static const WALL_WIDTH :int = 100;

    [Embed (source="rsrc/objects.swf#physplanterbox")]
    protected static var planterClass :Class;

    [Embed (source="rsrc/objects.swf#bush")]
    protected static var bushClass :Class;
}
}
