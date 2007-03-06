//
// $Id$

package {

import flash.text.TextField;
import flash.text.TextFormat;
import flash.text.TextFieldAutoSize;

import flash.display.Sprite
import flash.display.Shape;

public class Shooter extends Sprite
{
    public function Shooter (posidx :int, pidx :int)
    {
        _posidx = posidx;

        var square :Shape = new Shape();
        square.graphics.beginFill(Content.SHOOTER_COLOR[pidx]);
        // square.graphics.lineStyle(borderSize, borderColor);
        square.graphics.moveTo(0, -Content.SHOOTER_SIZE/2);
        square.graphics.lineTo(0, Content.SHOOTER_SIZE/2);
        square.graphics.lineTo(Content.SHOOTER_SIZE/2, 0);
        square.graphics.lineTo(0, -Content.SHOOTER_SIZE/2);
        square.graphics.endFill();
        square.alpha = 0.5;
        square.rotation = posidx * 90;
        addChild(square);

        _name = new TextField();
        _name.text = "0";
        _name.selectable = false;
        _name.defaultTextFormat = makeTextFormat();
        _name.autoSize = (posidx == 0) ? TextFieldAutoSize.LEFT : TextFieldAutoSize.RIGHT;
        addChild(_name);

        _score = new TextField();
        _score.text = "0";
        _score.selectable = false;
        _score.defaultTextFormat = makeTextFormat();
        _score.autoSize = (posidx == 0) ? TextFieldAutoSize.LEFT : TextFieldAutoSize.RIGHT;
        addChild(_score);
    }

    public function setName (name :String) :void
    {
        if (_posidx % 2 == 0 && name.length > 10) {
            name = name.substring(0, 10) + "...";
        }
        _name.text = name;
        if (_posidx == 2) {
            _name.x = NAME_X[_posidx] * Content.SHOOTER_SIZE - _name.width;
        } else {
            _name.x = NAME_X[_posidx] * Content.SHOOTER_SIZE;
        }
        if (_posidx == 1) {
            _name.y = NAME_Y[_posidx] * Content.SHOOTER_SIZE;
        } else  {
            _name.y = NAME_Y[_posidx] * Content.SHOOTER_SIZE - Content.FONT_SIZE -
                _name.getLineMetrics(0).ascent;
        }
    }

    public function setScore (score :int) :void
    {
        _score.text = ("" + score);
        if (_posidx == 0) {
            _score.x = SCORE_X[_posidx] * Content.SHOOTER_SIZE;
        } else {
            _score.x = SCORE_X[_posidx] * Content.SHOOTER_SIZE - _score.width;
        }
        if (_posidx == 3) {
            _score.y = SCORE_Y[_posidx] * Content.SHOOTER_SIZE - Content.FONT_SIZE -
                _score.getLineMetrics(0).ascent;
        } else {
            _score.y = SCORE_Y[_posidx] * Content.SHOOTER_SIZE;
        }
    }

    protected static function makeTextFormat () : TextFormat
    {
        var format : TextFormat = new TextFormat();
        format.font = Content.FONT_NAME;
        format.color = Content.FONT_COLOR;
        format.size = Content.FONT_SIZE;
        return format;
    }

    protected var _posidx :int;
    protected var _name :TextField;
    protected var _score :TextField;

    protected static const SCORE_X :Array = [ 0, -0.5, 0, -0.5 ];
    protected static const SCORE_Y :Array = [ 0.5, 0, 0.5, 0 ];

    protected static const NAME_X :Array = [ 0, 0.5, 0, 0.5 ];
    protected static const NAME_Y :Array = [ -0.5, 0, -0.5, 0 ];
}

}
