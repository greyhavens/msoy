package {
public class Collider {
    
    public function Collider(juggler:Juggler) 
    {
        _juggler = juggler;
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
        //Juggler.log("detecting collisions...");
        
        var collisions:Array = new Array();
        
        // 1: sort the bodies by the center of their x component of velocity
        _bodies.sort(byLeftProjection);
        
//       Juggler.log("dumping collision data:");
//      _bodies.forEach(dumpProjections);
        
        // 2: go each of them in turn looking for possible collisions on the x axis
        for (var i:int = 0; i<_bodies.length; i++) 
        {
            var iBounds:Bounds = _bodies[i].boundsInContext(_juggler);
            for (var j:int = i+1; j<_bodies.length; j++)
            {
                var jBounds:Bounds = _bodies[j].boundsInContext(_juggler);;
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
    
    private function dumpPair(pair:Array, index:Number, collisions:Array) :void
    {
        Juggler.log(pair[0].getLabel()+" collided with "+pair[1].getLabel());
    }
        
    private function dumpProjections(a:CanCollide, index:Number, bodies:Array) :void
    {
        var body:Bounds = a.boundsInContext(_juggler);
        Juggler.log(a.label+
            " left:"+body.leftProjection+
            " right:"+body.rightProjection+
            " top:"+body.topProjection+
            " bottom:"+body.bottomProjection);
    }
            
    /** Sort by left extension (which is basically the center of the x
     * component of velocity - half the width of the object
     */
    private function byLeftProjection(a:CanCollide, b:CanCollide) :int
    {
        var ab:Bounds = a.boundsInContext(_juggler);
        var bb:Bounds = b.boundsInContext(_juggler);
        var diff:Number = ab.leftProjection - bb.leftProjection;
        return (diff==0) ? 0 : ((ab.leftProjection < bb.leftProjection) ? -1 : 1);
    }
        
    public function activeBodies() :int
    {
        return _bodies.length;
    }
  
    private var _juggler: Juggler;
            
    private var _bodies :Array;    
}
}