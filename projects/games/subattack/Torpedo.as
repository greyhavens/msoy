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

        advanceLocation();
        updateVisual();

        _board.torpedoAdded(this);
    }

    /**
     * Called by the board to notify us that time has passed.
     */
    public function tick () :void
    {
        if (!advanceLocation()) {
            explode();
        }
    }

    public function explode () :void
    {
        _board.torpedoExploded(this);
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

    /** The sub that shot us. */
    protected var _sub :Submarine;
}
}
