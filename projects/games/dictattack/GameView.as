//
// $Id$

package {

import flash.text.TextField;
import flash.text.TextFieldAutoSize;
import flash.text.TextFieldType;
import flash.text.TextFormat;

import flash.display.Sprite;
import flash.events.KeyboardEvent;

import com.threerings.ezgame.MessageReceivedEvent;
import com.threerings.ezgame.PropertyChangedEvent;

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

        // listen for property changed and message events
        _control.addEventListener(PropertyChangedEvent.TYPE, propertyChanged);
        _control.addEventListener(MessageReceivedEvent.TYPE, messageReceived);
    }

    public function init (boardSize :int, playerCount :int) :void
    {
        _board = new Board(boardSize, _control, _model);
        _board.x = Content.BOARD_BORDER;
        _board.y = Content.BOARD_BORDER;
        addChild(_board);

        var mypidx :int = _control.isConnected() ? _control.seating.getMyPosition() : 0;
        var psize :int = Content.BOARD_BORDER * 2 + _board.getPixelSize();
        for (var pidx :int = 0; pidx < playerCount; pidx++) {
            // the board is rotated so that our position is always at the bottom
            var posidx :int = POS_MAP[mypidx][pidx];
            var shooter :Shooter = new Shooter(posidx, pidx);
            shooter.x = SHOOTER_X[posidx] * psize;
            shooter.y = SHOOTER_Y[posidx] * psize;
            addChild(shooter);
            _shooters[pidx] = shooter;
        }

        if (_control.isConnected()) {
            var names :Array = _control.seating.getPlayerNames();
            for (var ii :int = 0; ii < playerCount; ii++) {
                _shooters[ii].setName(names[ii]);
            }
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

    /**
     * Called when our distributed game state changes.
     */
    protected function propertyChanged (event :PropertyChangedEvent) :void
    {
        if (event.name == Model.SCORES) {
            if (event.index == -1) {
                for (var ii :int = 0; ii < _shooters.length; ii++) {
                    _shooters[ii].setScore(0);
                }
            } else {
                _shooters[event.index].setScore(int(event.newValue));
            }

        } else if (event.name == Model.BOARD_DATA && event.index == -1) {
            // we got our board, update the playable letters display
            _model.updatePlayable(_board);
        }
    }

    /**
     * Called when a message comes in.
     */
    protected function messageReceived (event :MessageReceivedEvent) :void
    {
        if (event.name == Model.WORD_PLAY) {
            // TODO: report to text field somewhere
            _model.updatePlayable(_board);
        }
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

    protected static const POS_MAP :Array = [
        [ 3, 1, 0, 2 ], [ 1, 3, 2, 0 ], [ 2, 0, 3, 1 ], [ 0, 2, 1, 3 ] ];

    protected static const SHOOTER_X :Array = [ 0, 0.5, 1, 0.5 ];
    protected static const SHOOTER_Y :Array = [ 0.5, 0, 0.5, 1 ];
}

}
