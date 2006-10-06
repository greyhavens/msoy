package {

import flash.display.MovieClip;

public class Torpedo extends BaseSprite
{
    public function Torpedo (owner :Submarine, board :Board)
    {
        super(board);
        _sub = owner;

        _orient = owner.getOrient();
        _x = owner.getX();
        _y = owner.getY();

        updateLocation();
        updateVisual();

        _board.torpedoAdded(this);

//        // advance it one immediately so that it starts out in front of the sub
//        // NOTE: This may cause it to explode
//        advanceLocation();
    }

    public function getOwner () :Submarine
    {
        return _sub;
    }

    public function willExplode (other :Torpedo) :Boolean
    {
        // we will explode if we're on the same tile
        return ((_x == other.getX()) && (_y == other.getY())) ||
        // or if we're about to pass through each other
            ((_x == other.advancedX()) && (_y == other.advancedY()) &&
             (other.getX() == advancedX()) && (other.getY() == advancedY()));
    }

    /**
     * Called by the board to notify us that time has passed.
     */
    public function tick () :void
    {
        advanceLocation();
    }

    public function explode () :void
    {
        // tell the board we exploded
        var subsKilled :int = _board.torpedoExploded(this);
        // tell our sub, too
        _sub.torpedoExploded(this, subsKilled);
    }

    // overridden: it's always legal for a torpedo to advance, so
    // this returns false if the torpedo has exploded
    override protected function advanceLocation () :Boolean
    {
        var adv :Boolean = super.advanceLocation();
        if (adv) {
            return true;
        }

        // advance our coordinates anyway so that we're on the tile 
        // to explode upon
        _x = advancedX();
        _y = advancedY();
        explode();
        return false;
    }

    protected function updateVisual () :void
    {
        var missile :MovieClip = MovieClip(new MISSILE());
        missile.gotoAndStop(orientToFrame());
        missile.x = SeaDisplay.TILE_SIZE / 2;
        missile.y = SeaDisplay.TILE_SIZE;
        addChild(missile);
    }

    protected function orientToFrame () :int
    {
        switch (_orient) {
        case Action.DOWN:
        default:
            return 1;

        case Action.LEFT:
            return 2;

        case Action.UP:
            return 3;

        case Action.RIGHT:
            return 4;
        }
    }

    override public function toString () :String
    {
        return "Torpedo[" + _id + "]";
    }

    protected var _id :Number = Math.random();

    /** The sub that shot us. */
    protected var _sub :Submarine;

    [Embed(source="missile.swf#animations")]
    protected static const MISSILE :Class;
}
}
