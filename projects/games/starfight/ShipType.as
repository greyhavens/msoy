package {

public class ShipType
{
    /** The name of the ship type. */
    public var name :String;

    /** maneuverability statistics. */
    public var turnAccelRate :Number;
    public var forwardAccel :Number;
    public var backwardAccel :Number;
    public var friction :Number;
    public var turnFriction :Number;
    public var hitPower :Number;
    public var armor :Number;

    public function ShipType (name :String, turnAccelRate :Number,
        forwardAccel :Number, backwardAccel :Number, friction :Number,
        turnFriction :Number, hitPower :Number, armor :Number) :void
    {
        this.name = name;
        this.turnAccelRate = turnAccelRate;
        this.forwardAccel = forwardAccel;
        this.backwardAccel = backwardAccel;
        this.friction = friction;
        this.turnFriction = turnFriction;
        this.hitPower = hitPower;
        this.armor = armor;
    }
}
}
