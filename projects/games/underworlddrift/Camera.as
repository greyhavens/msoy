package {
import flash.geom.Point;

public class Camera 
{
    public var angle :Number;
    public var position: Point;
    public var height :Number;
    public var distance :Number;
    
    public function Camera ()
    {
        angle = 0;
        position = new Point(0, Ground.HALF_IMAGE_SIZE);
        height = 20;
        distance = 800;
    }
}
}
