package {

import org.cove.flade.DynamicsEngine;
import org.cove.flade.primitives.*;
import org.cove.flade.constraints.*;


public class Cow {
    public var body:SpringBox;
    public var head:CircleParticle;
    public var leg0:RectangleParticle;
    public var leg1:RectangleParticle;
    public var leg2:RectangleParticle;
    public var leg3:RectangleParticle;

    public function Cow (engine: DynamicsEngine, x:Number, y:Number)
    {
        body = new SpringBox(x, y, 10, 20, engine);
        head = new CircleParticle(x, y - 20, 1);
        engine.addPrimitive(head);
        engine.addConstraint(new SpringConstraint(body.p0, head));
        engine.addConstraint(new SpringConstraint(body.p1, head));

        leg0 = new RectangleParticle(body.p0.curr.x - 8 + jitter(),
                                     body.p0.curr.y + jitter(),
                                     1, 1);
        engine.addPrimitive(leg0);
        engine.addConstraint(new SpringConstraint(body.p0, leg0));

        leg1 = new RectangleParticle(body.p1.curr.x + 8 + jitter(),
                                     body.p1.curr.y + jitter(),
                                     1, 1);
        engine.addPrimitive(leg1);
        engine.addConstraint(new SpringConstraint(body.p1, leg1));

        leg2 = new RectangleParticle(body.p2.curr.x + 8 + jitter(),
                                     body.p2.curr.y + jitter(),
                                     1, 1);
        engine.addPrimitive(leg2);
        engine.addConstraint(new SpringConstraint(body.p2, leg2));

        leg3 = new RectangleParticle(body.p3.curr.x - 8 + jitter(),
                                     body.p3.curr.y + jitter(),
                                     1, 1);
        engine.addPrimitive(leg3);
        engine.addConstraint(new SpringConstraint(body.p3, leg3));

        engine.addConstraint(new SpringConstraint(leg0, leg1, false, 0.02));
        engine.addConstraint(new SpringConstraint(leg2, leg3, false, 0.02));

    }

    protected function jitter() :Number
    {
        return Math.random() * 4 - 2;
    }
}
}
