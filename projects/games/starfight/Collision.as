package {

public class Collision
{
    /** What we've hit. */
    public var hit :Object;

    /** The time along the route we collided. (0-1)*/
    public var time :Number;

    /** Whether we collided horizontally or vertically. */
    public var isHoriz :Boolean;

    public function Collision (hit :Object, time :Number, isHoriz :Boolean)
    {
        this.hit = hit;
        this.time = time;
        this.isHoriz = isHoriz;
    }
}
}
