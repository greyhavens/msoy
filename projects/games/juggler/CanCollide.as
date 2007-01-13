package {

import flash.display.DisplayObjectContainer;
    
public interface CanCollide extends Bounds 
{
    function highlight() :void;
    
    function get position() :Array;
    
    function get velocity() :Array;
    
    function set velocity(v:Array) :void;
                    
    function get mass() :Number;
        
    function collisionWith(other:CanCollide) :void;
    
    function get label() :String;

    function boundsInContext(context:Positioned) :Bounds;
    
    function get parent() :DisplayObjectContainer;
}
}