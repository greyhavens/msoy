package {

public class Collision
{
    /** The obstacle we've collided with. */
    public var obstacle :Obstacle;

    /** The time along the route we collided. (0-1)*/
    public var time :Number;

    /** Whether we collided horizontally or vertically. */
    public var isHoriz :Boolean;

    public function Collision (obs :Obstacle, time :Number, isHoriz :Boolean)
    {
        this.obstacle = obs;
        this.time = time;
        this.isHoriz = isHoriz;
    }
}
}
