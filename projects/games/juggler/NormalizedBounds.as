package{

import flash.display.DisplayObjectContainer;

public class NormalizedBounds implements Bounds {
    
    public function NormalizedBounds(targetContainer:DisplayObjectContainer, body:CanCollide) {
        
        target = targetContainer;
        _body = body;
                
        if (body.parent != targetContainer)
        {
            var parent:DisplayObjectContainer = body.parent;
            do {
                _ancestors.push(parent);
                parent = parent.parent;
            }
            while(parent != targetContainer);
        }
        
        Juggler.log("computed normalized bounds for "+body.label+" x="+x+", y="+y);
    }
    
    public function get leftProjection() :Number
     {
         return _body.leftProjection + Xoffset;
     }

     public function get rightProjection() :Number
     {
         return _body.rightProjection + Xoffset
     }

     public function get topProjection() :Number
     {
         return _body.topProjection + Yoffset;
     }

     public function get bottomProjection() :Number
     {
         return _body.bottomProjection + Yoffset;
     }
     
     private function get Xoffset() : int
     {
         var x:int = 0;
         for (var i:int = 0; i<_ancestors.length; i++)
         {
             x += _ancestors[i].x;
         }
         return x;
     }
     
     private function get Yoffset() : int
     {
         var y:int = 0;
         for (var i:int =0; i<_ancestors.length; i++)
         {
             y += _ancestors[i].y;             
         }
         return y;
     }
     
     public function get x() :Number
     {
         return _body.x + Xoffset;
     }
     
     public function get y() :Number
     {
         return _body.y + Yoffset;
     }
     
     public var target:DisplayObjectContainer;
     
     private var _body:CanCollide;

     private var _ancestors:Array = new Array();
}
}