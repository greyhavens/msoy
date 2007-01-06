package {
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
}
}