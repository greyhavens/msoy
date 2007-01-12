package {
import flash.geom.Point;
import flash.geom.Rectangle;
import flash.geom.Matrix;
import flash.display.BitmapData;

public class Camera 
{
    public var angle :Number;
    public var position: Point;
    public var height :Number;
    public var distance :Number;
    
    public function Camera ()
    {
        angle = 0;
        position = new Point(0, Ground.HALF_IMAGE_SIZE + 100);
        height = 20;
        distance = 800;
    }

    public function setGround (ground :Ground) :void 
    {
        _ground = ground;
    }

    public function drivingOnGrass () :Boolean
    {
        // TODO: make this work! 
        return false;
    }

    protected var _ground :Ground;
}
}
