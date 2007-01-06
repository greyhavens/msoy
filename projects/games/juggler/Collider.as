package {
public class Collider {
    
    public function Collider() 
    {
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
        
        //_bodies.forEach(dumpProjections);
        
        // 2: go each of them in turn looking for possible collisions on the x axis
        for (var i:int = 0; i<_bodies.length; i++) 
        {
            for (var j:int = i+1; j<_bodies.length; j++)
            {
                if (_bodies[i].rightProjection() > _bodies[j].leftProjection())
                {
                    // 3: if we get an overlap on the x axis, check the y axis.
                    if (_bodies[i].topProjection() > _bodies[j].topProjection())
                    {
                        if(_bodies[i].topProjection() < _bodies[j].bottomProjection()) 
                        {
                            collisions.push(new Array(_bodies[i], _bodies[j]));                            
                        }
                    } 
                    else if (_bodies[i].bottomProjection() > _bodies[j].topProjection()) 
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
        
        // 5: calculate the effect of the collisions we detected
        /// collisions.forEach(calculateImpact);
        
        if (collisions.length>0)
        {
            collisions.forEach(highlight);
            collisions.forEach(elasticCollison);
        }     
    }
    
    private function elasticCollison(pair:Array, index:Number, collisions:Array) :void
    {        
        var a:CanCollide = pair[0];
        var b:CanCollide = pair[1];
        
        var results:Array = _elasticCollision.collide( 
            a.getPosition(), a.getVelocity(), a.getMass(),
            b.getPosition(), b.getVelocity(), b.getMass()
        );
        
        pair[0].setVelocity(results[0]);
        pair[1].setVelocity(results[1]);
    }
    
    /** Sort by left extension (which is basically the center of the x
     * component of velocity - half the width of the object
     */
    private function byLeftProjection(a:CanCollide, b:CanCollide) :int
    {
        var diff:Number = a.leftProjection() - b.leftProjection();
        return (diff==0) ? 0 : ((a.leftProjection() < b.leftProjection()) ? -1 : 1);
    }
    
    private function highlight(pair: Array, index: Number, collisions: Array) :void
    {
        pair[0].highlight();
        pair[1].highlight();
    }
        
    private var _elasticCollision :ElasticCollision = new ElasticCollision();
    
    private var _bodies :Array;    
}
}