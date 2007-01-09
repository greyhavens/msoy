package {

import flash.display.DisplayObjectContainer;
    
public interface CanCollide {
    function leftProjection() :Number;
    
    function rightProjection() :Number;
    
    function topProjection() :Number;
    
    function bottomProjection() :Number;

    function highlight() :void;
    
    function getPosition() :Array;
    
    function getVelocity() :Array;
    
    function setVelocity(v:Array) :void;
    
    function getMass() :Number;
        
    function collisionWith(other:CanCollide) :void;
    
    function getLabel() :String;

    function getX() :int;
    
    function getY() :int;

    function getNormalizedBounds(target:DisplayObjectContainer) :NormalizedBounds;
    
    function getParent():DisplayObjectContainer;
}
}