package {

import flash.display.Sprite;
import flash.events.MouseEvent;

import com.whirled.WhirledGameControl;

public class Stone extends Sprite
{
    public static const SIZE :int = 10;

    public static const COLORS :Array = [ 0x000000, 0xFFFFFF, 0xFF00FF ];

    public function Stone (control :WhirledGameControl, model :GoModel, x :int, y :int)
    {
        _control = control;
        _model = model;
        _xx = x;
        _yy = y;

        addEventListener(MouseEvent.CLICK, mouseClick);
        addEventListener(MouseEvent.ROLL_OVER, rollOver);
        addEventListener(MouseEvent.ROLL_OUT, rollOut);

        _stone = GoModel.COLOR_NONE;
        _hover = false;
        updateStoneView();
    }

    public function updateStone () :void
    {
        _stone = _model.getStone(_xx, _yy);
        updateStoneView();
    }

    protected function updateStoneView () :void
    {
        var color :int;
        if (_stone == GoModel.COLOR_NONE) {
            alpha = _hover && _control.isMyTurn() ? 0.5 : 0;
            color = _model.getColor (_control.getTurnHolder());
        } else {
            alpha = 1.0;
            color = _stone;
        }

        graphics.clear();
        graphics.beginFill(uint(COLORS[color]));
        graphics.drawCircle(0, 0, 20);
    }

    protected function mouseClick (event :MouseEvent) :void
    {
        if (_stone == GoModel.COLOR_NONE) {
            _model.stoneClicked(_xx, _yy);
        }
    }

    protected function rollOver (event :MouseEvent) :void
    {
        if (_stone == GoModel.COLOR_NONE) {
            _hover = true;
            updateStoneView();
        }
    }

    protected function rollOut (event :MouseEvent) :void
    {
        if (_stone == GoModel.COLOR_NONE) {
            _hover = false;
            updateStoneView();
        }
    }

    protected var _control :WhirledGameControl;
    protected var _model :GoModel;
    protected var _xx :int;
    protected var _yy :int;
    protected var _stone :int;
    protected var _hover :Boolean;
}
}
