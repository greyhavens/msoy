package {

public class Level extends Sprite implements DrivingSurface 
{
    public function Level ()
    {
    }

    public function getDisplayObject () :DisplayObject
    {
        return (this as DisplayObject);
    }

    public function drivingOnRoad () :Boolean
    {
        return true;
    }
}
}
