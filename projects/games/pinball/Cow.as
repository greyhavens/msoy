package {

import org.cove.flade.DynamicsEngine;
import org.cove.flade.util.*;
import org.cove.flade.primitives.*;
import org.cove.flade.constraints.*;

import flash.display.*;

public class Cow {
    public static const BODY_WIDTH :int = 60;
    public static const BODY_LENGTH :int = 30;
    public static const LEG_LENGTH :int = 12;

    public var leg0:RectangleParticle;
    public var leg1:RectangleParticle;
    public var leg2:RectangleParticle;
    public var leg3:RectangleParticle;
    public var p0:RectangleParticle;
    public var p1:RectangleParticle;
    public var p2:RectangleParticle;
    public var p3:RectangleParticle;

    public function reconfigureCow (x:Number, y:Number, cowAngle:Number) :void
    {
        var cCs :Number = Math.cos(cowAngle);
        var cSn :Number = Math.sin(cowAngle);
        var dx:Number = BODY_WIDTH/2;
        var dy:Number = BODY_LENGTH/2;
        p0.setPos(x - cCs*dx - cSn*dy, y - cCs*dy + cSn*dx);
        p1.setPos(x + cCs*dx - cSn*dy, y - cCs*dy - cSn*dx);
        p2.setPos(x + cCs*dx + cSn*dy, y + cCs*dy - cSn*dx);
        p3.setPos(x - cCs*dx + cSn*dy, y + cCs*dy + cSn*dx);
        return;
//        var lCs :Number = Math.cos(cowAngle - legAngle);
//        var lSn :Number = Math.sin(cowAngle - legAngle);
        var lCs :Number = cCs;
        var lSn :Number = cSn;
        leg0.setPos(p3.curr.x + lSn*LEG_LENGTH,
                    p3.curr.y + lCs*LEG_LENGTH);
        leg1.setPos(p2.curr.x + lSn*LEG_LENGTH,
                    p2.curr.y + lCs*LEG_LENGTH);
        return;
        // right legs
//        lCs = Math.cos(cowAngle + legAngle);
//        lSn = Math.sin(cowAngle + legAngle);
        leg1.setPos(p1.curr.x + lCs*LEG_LENGTH,
                    p1.curr.y - lSn*LEG_LENGTH);
        leg2.setPos(p2.curr.x + lCs*LEG_LENGTH,
                    p2.curr.y - lSn*LEG_LENGTH);
    }

    public function launch (cowAngle :Number, speed :Number) :void
    {
        var diff :Vector = new Vector(Math.cos(cowAngle) * speed,
                                      -Math.sin(cowAngle) * speed);
        p0.curr.plus(diff);
        p1.curr.plus(diff);
        p2.curr.plus(diff);
        p3.curr.plus(diff);
        leg0.curr.plus(diff);
        leg1.curr.plus(diff);
    }

    public function Cow (engine: DynamicsEngine, x:Number, y:Number)
    {
        // top left
        p0 = new RectangleParticle(jitter() + x - BODY_WIDTH / 2,
                                   jitter() + y - BODY_LENGTH / 2, 1, 1);
        // top right
        p1 = new RectangleParticle(jitter() + x + BODY_WIDTH / 2,
                                   jitter() + y - BODY_LENGTH / 2, 1, 1);
        // bottom right
        p2 = new RectangleParticle(jitter() + x + BODY_WIDTH / 2,
                                   jitter() + y + BODY_LENGTH / 2, 1, 1);
        // bottom left
        p3 = new RectangleParticle(jitter() + x - BODY_WIDTH / 2,
                                   jitter() + y + BODY_LENGTH / 2, 1, 1);

        p0.setVisible(false);
        p1.setVisible(false);
        p2.setVisible(false);
        p3.setVisible(false);

        engine.addPrimitive(p0);
        engine.addPrimitive(p1);
        engine.addPrimitive(p2);
        engine.addPrimitive(p3);

        // edges
        engine.addConstraint(new SpringConstraint(p0, p1, false));
        engine.addConstraint(new SpringConstraint(p1, p2, false));
        engine.addConstraint(new SpringConstraint(p2, p3, false));
        engine.addConstraint(new SpringConstraint(p3, p0, false));

        // crossing braces
        engine.addConstraint(new SpringConstraint(p0, p2, false));
        engine.addConstraint(new SpringConstraint(p1, p3, false));

        leg0 = new RectangleParticle(p3.curr.x,
                                     p3.curr.y + LEG_LENGTH,
                                     1, 1);
        leg0.setVisible(false);
        engine.addPrimitive(leg0);
        engine.addConstraint(new SpringConstraint(p3, leg0, false));

        leg1 = new RectangleParticle(p2.curr.x,
                                     p2.curr.y + LEG_LENGTH,
                                     1, 1);
        leg1.setVisible(false);
        engine.addPrimitive(leg1);
        engine.addConstraint(new SpringConstraint(p2, leg1, false));
        return;
        leg2 = new RectangleParticle(p2.curr.x + LEG_LENGTH + jitter(),
                                     p2.curr.y + jitter(),
                                     1, 1);
        engine.addPrimitive(leg2);
        engine.addConstraint(new SpringConstraint(p2, leg2));

        leg3 = new RectangleParticle(p3.curr.x - LEG_LENGTH + jitter(),
                                     p3.curr.y + jitter(),
                                     1, 1);
        engine.addPrimitive(leg3);
        engine.addConstraint(new SpringConstraint(p3, leg3));

//        engine.addConstraint(new SpringConstraint(leg2, leg
    }

    protected function jitter() :Number
    {
        return 0;
        return Math.random() * 4 - 2;
    }
}
}
