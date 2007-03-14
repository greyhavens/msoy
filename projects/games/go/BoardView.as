//
// $Id$

package {

import flash.display.Graphics;
import flash.display.Shape;
import flash.display.Sprite;

import flash.events.TimerEvent;
import flash.utils.Timer;

import com.threerings.ezgame.PropertyChangedEvent;
import com.whirled.WhirledGameControl;
import flash.display.Bitmap;

public class BoardView extends Sprite
{
    public const WIDTH :Number = 400;
    public const HEIGHT :Number = 400;

    public function BoardView (size :int, control :WhirledGameControl, model :GoModel)
    {
        _size = size;
        _control = control;
        _model = model;

        var background :Bitmap = new _background();
        background.width = WIDTH;
        background.height = HEIGHT;
        addChild(background);

        var bits :Sprite = new Sprite();
//        bits.width = WIDTH;
//        bits.height = HEIGHT;
        addChild(bits);

        _control.localChat("foo: " + toPix(2));

        var dotRow :int = (_size < 13) ? 3 : 4;

        with(bits.graphics) {
            beginFill(0x000000);
            drawCircle(toPix(dotRow-1), toPix(dotRow-1), 4);
            drawCircle(toPix(size-dotRow), toPix(dotRow-1), 4);
            drawCircle(toPix(size-dotRow), toPix(size-dotRow), 4);
            drawCircle(toPix(dotRow-1), toPix(size-dotRow), 4);
            endFill();
        }

        for (var i :int = 0; i < size; i ++) {
            with(bits.graphics) {
                lineStyle(1, 0x000000);
                moveTo(toPix(0), toPix(i)); lineTo(toPix(size-1), toPix(i));
                moveTo(toPix(i), toPix(0)); lineTo(toPix(i), toPix(size-1));
            }
        }

        _stones = new Array();
        for (var yy :int = 0; yy < size; yy ++) {
            for (var xx :int = 0; xx < size; xx ++) {
                var stone :Stone = new Stone(_control, _model, xx, yy);
                bits.addChild(stone);
                stone.x = toPix(xx);
                stone.y = toPix(yy);
//                stone.height = stone.width = WIDTH / (_size + 1);
                _stones[getPosition(xx, yy)] = stone;
            }
        }

        // listen for property changed events
        _control.addEventListener(PropertyChangedEvent.TYPE, propertyChanged);
    }

    protected function toPix (coord :int) :Number
    {
        return ((coord + 1) * WIDTH) / (_size + 1);
    }

    public function getPosition (xx :int, yy :int) :int
    {
        return yy * _size + xx;
    }

    /**
     * Called when our distributed game state changes.
     */
    protected function propertyChanged (event :PropertyChangedEvent) :void
    {
        if (event.name == GoModel.BOARD_DATA) {
            if (event.index == -1) {
                // display the board
                for (var yy :int = 0; yy < _size; yy++) {
                    for (var xx :int = 0; xx < _size; xx++) {
                        _stones[getPosition(xx, yy)].updateStone();
                    }
                }
            } else {
                _stones[event.index].updateStone();
            }
        }
    }

    protected var _size :int;
    protected var _control :WhirledGameControl;
    protected var _model :GoModel;
    protected var _stones :Array;

    [Embed(source="rsrc/honeywoodgrain.jpg")]
    protected var _background :Class;

}
}
