package {

import flash.ui.Keyboard;

public class Torpedo extends BaseSprite
{
    public function Torpedo (owner :Submarine, board :Board)
    {
        super(board);
        _sub = owner;

        _orient = owner.getOrient();
        _x = owner.getX();
        _y = owner.getY();

        updateVisual();

        _board.torpedoAdded(this);

//        // advance it one immediately so that it starts out in front of the sub
//        // NOTE: This may cause it to explode
//        advanceLocation();
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
        var hitSubs :Array = _board.torpedoExploded(this);
        for each (var hitSub :Submarine in hitSubs) {
            // TODO
        }

        // tell our originating sub that we exploded
        _sub.torpedoExploded(this);
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
        switch (_orient) {
        case Keyboard.DOWN:
            _y++;
            break;

        case Keyboard.UP:
            _y--;
            break;

        case Keyboard.LEFT:
            _x--;
            break;

        case Keyboard.RIGHT:
            _x++;
            break;
        }

        explode();
        return false;
    }

    protected function updateVisual () :void
    {
        graphics.clear();
        graphics.lineStyle(3, 0xFF0000);

        var mid :int = SeaDisplay.TILE_SIZE / 2;
        var low :int = SeaDisplay.TILE_SIZE / 4;
        var high :int = mid + low;

        switch (_orient) {
        case Keyboard.UP:
        case Keyboard.DOWN:
            graphics.moveTo(mid, low);
            graphics.lineTo(mid, high);
            break;

        default:
            graphics.moveTo(low, mid);
            graphics.lineTo(high, mid);
            break;
        }
    }

    override public function toString () :String
    {
        return "Torpedo[" + _id + "]";
    }

    protected var _id :Number = Math.random();

    /** The sub that shot us. */
    protected var _sub :Submarine;
}
}
