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
        const x:Number = end[0] - start[0];
        const y:Number = end[1] - start[1];
        
        const speed2:Number = speed * speed;
        
        const intermediate:Number = 
            Math.pow(speed,4) - gravity*(gravity*x*x + 2*y*speed2);
        
        if (intermediate < 0) // speed isn't enough to reach the target
        {
            return null;
        }
        
        const root:Number = Math.sqrt(intermediate);
        
        const gravityX:Number = gravity * x;
        
        return new Array(
            Math.atan( (speed2 + root) / gravityX ),
            Math.atan( (speed2 - root) / gravityX ) 
        );
    }
    
    public function initialVector(start:Array, end:Array, speed:Number, gravity:Number) :Array
    {        
        const angles:Array = initialAngle(start, end, speed, gravity);    
        
        if (angles==null) // the speed wasn't greate enough.
        {
            return null;
        }
                
        const v1:Array = polarToVector( angles[0], speed );

        return v1;
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