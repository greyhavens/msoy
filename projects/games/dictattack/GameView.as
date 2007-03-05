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

/**
 * Manages the whole game view and user input.
 */
public class GameView extends Sprite
{
    public function GameView (control :WhirledGameControl, model :Model)
    {
        _control = control;
        _model = model;

        // create the text field via which we'll accept player input
        _input = new TextField();
        _input.defaultTextFormat = makeInputFormat();
        _input.border = true;
        _input.type = TextFieldType.INPUT;
        _input.x = Content.INPUT_RECT.x;
        _input.y = Content.INPUT_RECT.y;
        _input.width = Content.INPUT_RECT.width;
        _input.height = Content.INPUT_RECT.height;
        addChild(_input);
    }

    public function init (boardSize :int, playerCount :int) :void
    {
        _board = new Board(boardSize, _control, _model);
        _board.x = Content.BOARD_BORDER;
        _board.y = Content.BOARD_BORDER;
        addChild(_board);

        var psize :int = Content.BOARD_BORDER * 2 + _board.getPixelSize();
        for (var pidx :int = 0; pidx < playerCount; pidx++) {
            var shooter :Shooter = new Shooter(pidx);
            shooter.x = SHOOTER_X[pidx] * psize;
            shooter.y = SHOOTER_Y[pidx] * psize;
            addChild(shooter);
            _shooters[pidx] = shooter;
        }
    }

    public function roundDidStart () :void
    {
        addEventListener(KeyboardEvent.KEY_UP, keyReleased);
        _input.selectable = true;
        _input.stage.focus = _input;
    }

    public function roundDidEnd () :void
    {
        removeEventListener(KeyboardEvent.KEY_UP, keyReleased);
        _input.selectable = false;
    }

    protected function keyReleased (event :KeyboardEvent) : void
    {
        if (event.keyCode == 13) {
            if (_model.submitWord(_board, _input.text)) {
                _input.text = "";
            }
        }
    }

    protected static function makeInputFormat () : TextFormat
    {
        var format : TextFormat = new TextFormat();
        format.font = Content.FONT_NAME;
        format.color = Content.FONT_COLOR;
        format.size = Content.FONT_SIZE;
        return format;
    }

    protected var _control :WhirledGameControl;
    protected var _model :Model;

    protected var _input :TextField;

    protected var _board :Board;
    protected var _shooters :Array = new Array();

    protected static const SHOOTER_X :Array = [ 0.5, 0.5, 0, 1 ];
    protected static const SHOOTER_Y :Array = [ 1, 0, 0.5, 0.5 ];
}

}
