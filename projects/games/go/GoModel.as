//
// $Id$

package {

import com.whirled.WhirledGameControl;

public class GoModel
{
    /** The name of the board data distributed value. */
    public static const BOARD_DATA :String = "boardData";

    public static const COLOR_BLACK :int = 0;
    public static const COLOR_WHITE :int = 1;
    public static const COLOR_NONE :int = 2;

    public function GoModel (size :int, control :WhirledGameControl)
    {
        _size = size;
        _control = control;
    }

    /**
     * Called when a game starts.
     */
    public function gameDidStart () :void
    {
        // if we are in control, create a board and publish it
        if (_control.amInControl()) {
            var pieces :Array = new Array();
            pieces.length = _size * _size;

            for (var ii :int = 0; ii < _size*_size; ii ++) {
                pieces[ii] = COLOR_NONE;
            }
            _control.set(BOARD_DATA, pieces);

            _colors = [ COLOR_BLACK, COLOR_WHITE ];
        }
    }

    /**
     * Called when the game ends.
     */
    public function gameDidEnd () :void
    {
    }

    public function stoneClicked (xx :int, yy :int) :void
    {
        _control.set(BOARD_DATA, _colors[_control.seating.getMyPosition()], getPosition(xx, yy));
        _control.endTurn();
    }

    public function getPosition (xx :int, yy :int) :int
    {
        return yy * _size + xx;
    }

    public function getStone (xx :int, yy :int) :int
    {
        var data :Array = (_control.get(BOARD_DATA) as Array);
        return data[getPosition(xx, yy)];
    }

    public function getColor (pIdx :int) :int
    {
        return _colors[pIdx];
    }

    protected var _size :int;
    protected var _control :WhirledGameControl;

    protected var _colors :Array;
}

}
