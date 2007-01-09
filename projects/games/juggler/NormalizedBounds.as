package{

import flash.display.DisplayObjectContainer;

public class NormalizedBounds {
    
    public function NormalizedBounds(targetContainer:DisplayObjectContainer, body:CanCollide) {
        
        target = targetContainer;
        _body = body;
                
        if (body.getParent() != targetContainer)
        {
            var parent:DisplayObjectContainer = body.getParent();
            do {
                _ancestors.push(parent);
                parent = parent.parent;
            }
            while(parent != targetContainer);
        }
        
        Juggler.log("computed normalized bounds for "+body.getLabel()+" x="+getX()+", y="+getY());
    }
    
    public function leftProjection() :Number
     {
         return _body.leftProjection() + getXoffset();
     }

     public function rightProjection() :Number
     {
         return _body.rightProjection() + getXoffset();
     }

     public function topProjection() :Number
     {
         return _body.topProjection() + getYoffset();
     }

     public function bottomProjection() :Number
     {
         return _body.bottomProjection() + getYoffset();
     }
     
     private function getXoffset() : int
     {
         var x:int = 0;
         for (var i:int = 0; i<_ancestors.length; i++)
         {
             x += _ancestors[i].x;
         }
         return x;
     }
     
     private function getYoffset() : int
     {
         var y:int = 0;
         for (var i:int =0; i<_ancestors.length; i++)
         {
             y += _ancestors[i].y;             
         }
         return y;
     }
     
     public function getX() :int
     {
         return _body.getX() + getXoffset();
     }
     
     public function getY() :int
     {
         return _body.getY() + getYoffset();
     }
     
     public var target:DisplayObjectContainer;
     
     private var _body:CanCollide;

     private var _ancestors:Array = new Array();
}
}