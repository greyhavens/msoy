package {
    
import Math;
    
/**
 * Perform calculations associated with ballistic trajectories
 */
public class BallisticTrajectory {

    /**
     * calculate the initial angle required to hit a target, given
     * the starting position, target position, gravity, and desired
     * starting speed.
     */
    private function initialAngle(start:Array, end:Array, speed:Number, gravity:Number) :Array
    {
        var x:Number = end[0] - start[0];
        var y:Number = end[1] - start[1];
        
        var speed2:Number = speed * speed;
        
        var intermediate:Number = 
            Math.pow(speed,4) - gravity*(gravity*x*x + 2*y*speed2);
        
        if (intermediate < 0) // speed isn't enough to reach the target
        {
            return null;
        }
        
        var root:Number = Math.sqrt(intermediate);
        
        var gravityX:Number = gravity * x;
        
        return new Array(
            Math.atan( (speed2 + root) / gravityX ),
            Math.atan( (speed2 - root) / gravityX ) 
        );
    }
    
    public function initialVector(start:Array, end:Array, speed:Number, gravity:Number) :Array
    {
        Juggler.log(" calculating throw from: "+start+" to: "+end+" speed: "+speed+" gravity: "+gravity);
        
        var angles:Array = initialAngle(start, end, speed, gravity);    
        
        if (angles==null) // the speed wasn't greate enough.
        {
            return null;
        }
        
        Juggler.log("angles of throw: "+degrees(angles[0])+", "+degrees(angles[1]));
        
        var v1:Array = polarToVector( angles[0], speed );

        return v1;

//        if (v1[1] > 1) 
//        {
//            v1[1] = -v1[1];
//            Juggler.log("returning vector: "+v1);
//            return v1;
//        } else
//        {
//            return v1;
//        }
        
//        var v2:Array = polarToVector( angles[1], speed );
        
//        if (v2[1] > 1) 
//        {
//            v2[1] = -v2[1];
//            Juggler.log("returning vector: "+v2);
//            return v2;
//        }
        
        // didn't have a usable angle
//        return null;
    }
    
    private function degrees(r:Number) :int
    {
        return r / Math.PI * 180;
    }
    
    private function polarToVector(theta:Number, speed:Number) :Array
    {
        return new Array(
            speed * Math.cos(theta),
            speed * Math.sin(theta)
        );
    }
}
}