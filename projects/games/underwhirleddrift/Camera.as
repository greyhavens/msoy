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
        position = new Point(0, 0);
        distance = 300;
        height = 20;
    }
}
}
