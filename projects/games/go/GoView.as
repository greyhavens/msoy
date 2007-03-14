//
// $Id$

package {

import flash.text.TextField;
import flash.text.TextFieldAutoSize;
import flash.text.TextFieldType;
import flash.text.TextFormat;

import flash.display.Sprite;
import flash.events.KeyboardEvent;

import com.whirled.WhirledGameControl;

public class GoView extends Sprite
{
    public function GoView (control :WhirledGameControl, model :GoModel)
    {
        _control = control;
        _model = model;
    }

    public function init (boardSize :int) :void
    {
        _board = new BoardView(boardSize, _control, _model);
        _board.x = 10;
        _board.y = 10;
        addChild(_board);

        var names :Array = _control.seating.getPlayerNames();

        with (graphics) {
            beginFill(0x404040);
            drawRect(0, 0, 540, 420);
            endFill();
            beginFill(0x106000);
            drawRect(420, 10, 100, 200);
            endFill();
        }
        var foo :TextField = new TextField();
        foo.text = "Player 1: " + names[0];
        foo.textColor = 0xF0F0F0;
        foo.x = 425;
        foo.y = 15;
        addChild(foo);

        foo = new TextField();
        foo.text = "Player 2: N/A";
        foo.textColor = 0xF0F0F0;
        foo.x = 425;
        foo.y = 30;
        addChild(foo);
    }

    public function gameDidStart () :void
    {
//        addEventListener(KeyboardEvent.KEY_UP, keyReleased);
    }

    public function gameDidEnd () :void
    {
//        removeEventListener(KeyboardEvent.KEY_UP, keyReleased);
    }

    public function turnChanged () :void
    {

    }

    protected function keyReleased (event :KeyboardEvent) : void
    {
        if (event.keyCode == 13) {
//            if (_model.submitWord(_board, _input.text)) {
//                _input.text = "";
//            }
        }
    }

    protected var _control :WhirledGameControl;
    protected var _model :GoModel;

    protected var _board :BoardView;
}

}
