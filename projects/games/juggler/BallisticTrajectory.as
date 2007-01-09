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
        var angles:Array = initialAngle(start, end, speed, gravity);    
        
        if (angles==null) // the speed wasn't greate enough.
        {
            return null;
        }
        
        // if one of the angles is upwards, use it.
        if (angles[0] > Math.PI) 
        {
            return polarToVector( angles[0], speed );
        }         
        else if (angles[1] > Math.PI)
        {
            return polarToVector( angles[1], speed );
        }
        
        // didn't have a usable angle
        return null;
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