package {
    
import flash.display.DisplayObjectContainer;

public class Collider {
    
    public function Collider(context:Positioned) 
    {
        _context = context;
        _bodies = new Array();
    }
    
    public function addBody(body:CanCollide) :void
    {
        _bodies.push(body);
    }
    
    public function removeBody(body:CanCollide) :void
    {
        Util.removeFromArray(_bodies, body);
    }
    
    public function detectCollisions() :void
    {        
        var collisions:Array = new Array();
        
        // 1: sort the bodies by the center of their x component of velocity
        _bodies.sort(byLeftProjection);
                
        // 2: go each of them in turn looking for possible collisions on the x axis
        for (var i:int = 0; i<_bodies.length; i++) 
        {
            var iBounds:Bounds = _bodies[i].boundsInContext(_context);
            for (var j:int = i+1; j<_bodies.length; j++)
            {
                var jBounds:Bounds = _bodies[j].boundsInContext(_context);
                if (iBounds.rightProjection > jBounds.leftProjection)
                {
                    // 3: if we get an overlap on the x axis, check the y axis.
                    if (iBounds.topProjection > jBounds.topProjection)
                    {
                        if(iBounds.topProjection < jBounds.bottomProjection) 
                        {
                            collisions.push(new Array(_bodies[i], _bodies[j]));                            
                        }
                    } 
                    else if (iBounds.bottomProjection > jBounds.topProjection) 
                    {
                        collisions.push(new Array(_bodies[i], _bodies[j]));                        
                    }                    
                } 
                else // none of the later bodies could possibly overlap now
                {
                    break;
                }
            }
        }
                
        for each (var pair:Array in collisions)
        {
            pair[0].highlight();
            pair[1].highlight();
            pair[0].collisionWith(pair[1]);
        }
    }
        
    /** Sort by left extension (which is basically the center of the x
     * component of velocity - half the width of the object
     */
    private function byLeftProjection(a:CanCollide, b:CanCollide) :int
    {
        var ab:Bounds = a.boundsInContext(_context);
        var bb:Bounds = b.boundsInContext(_context);
        var diff:Number = ab.leftProjection - bb.leftProjection;
        return (diff==0) ? 0 : ((ab.leftProjection < bb.leftProjection) ? -1 : 1);
    }
        
    public function activeBodies() :int
    {
        return _bodies.length;
    }
  
    private var _context: Positioned;
            
    private var _bodies :Array;    
}
}