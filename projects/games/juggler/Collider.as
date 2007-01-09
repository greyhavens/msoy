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
    
    public function detectCollisions() :void
    {
        //Juggler.log("detecting collisions...");
        
        var collisions:Array = new Array();
        
        // 1: sort the bodies by the center of their x component of velocity
        _bodies.sort(byLeftProjection);
        
 //       Juggler.log("dumping collision data:");
        _bodies.forEach(dumpProjections);
        
        // 2: go each of them in turn looking for possible collisions on the x axis
        for (var i:int = 0; i<_bodies.length; i++) 
        {
            var iBounds:NormalizedBounds = _bodies[i].getNormalizedBounds(_juggler);
            for (var j:int = i+1; j<_bodies.length; j++)
            {
                var jBounds:NormalizedBounds = _bodies[j].getNormalizedBounds(_juggler);;
                if (iBounds.rightProjection() > jBounds.leftProjection())
                {
                    // 3: if we get an overlap on the x axis, check the y axis.
                    if (iBounds.topProjection() > jBounds.topProjection())
                    {
                        if(iBounds.topProjection() < jBounds.bottomProjection()) 
                        {
                            collisions.push(new Array(_bodies[i], _bodies[j]));                            
                        }
                    } 
                    else if (iBounds.bottomProjection() > jBounds.topProjection()) 
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
                
        if (collisions.length>0)
        {
            collisions.forEach(highlight);
            collisions.forEach(dumpPair);
            collisions.forEach(handleCollision);
        }     
    }
    
    private function dumpPair(pair:Array, index:Number, collisions:Array) :void
    {
        Juggler.log(pair[0].getLabel()+" collided with "+pair[1].getLabel());
    }
        
    private function dumpProjections(a:CanCollide, index:Number, bodies:Array) :void
    {
        var body:NormalizedBounds = a.getNormalizedBounds(_juggler);
        Juggler.log(a.getLabel()+
            " left:"+body.leftProjection()+
            " right:"+body.rightProjection()+
            " top:"+body.topProjection()+
            " bottom:"+body.bottomProjection());
    }
    
    private function handleCollision(pair:Array, index:Number, collisions:Array) :void
    {
        var a:CanCollide = pair[0];
        var b:CanCollide = pair[1];
        
        a.collisionWith(b);
    }
        
    /** Sort by left extension (which is basically the center of the x
     * component of velocity - half the width of the object
     */
    private function byLeftProjection(a:CanCollide, b:CanCollide) :int
    {
        var ab:NormalizedBounds = a.getNormalizedBounds(_juggler);
        var bb:NormalizedBounds = b.getNormalizedBounds(_juggler);
        var diff:Number = ab.leftProjection() - bb.leftProjection();
        return (diff==0) ? 0 : ((ab.leftProjection() < bb.leftProjection()) ? -1 : 1);
    }
    
    private function highlight(pair: Array, index: Number, collisions: Array) :void
    {
        pair[0].highlight();
        pair[1].highlight();
    }
            
    public function activeBodies() :int
    {
        return _bodies.length;
    }
  
    private var _juggler: Juggler;
            
    private var _bodies :Array;    
}
}