//
// $Id$

package dictattack {

import flash.text.TextField;
import flash.text.TextFieldAutoSize;
import flash.text.TextFormat;

import flash.display.Shape;
import flash.display.Sprite

public class Shooter extends Sprite
{
    public function Shooter (posidx :int, pidx :int)
    {
        _pidx = pidx;
        _posidx = posidx;

        _rotor = new Sprite();
        _rotor.rotation = posidx * 90;
        addChild(_rotor);

        var shooter :Shape = new Shape();
        shooter.graphics.beginFill(Content.SHOOTER_COLOR[pidx]);
        shooter.graphics.moveTo(0, -Content.SHOOTER_SIZE/2);
        shooter.graphics.lineTo(0, Content.SHOOTER_SIZE/2);
        shooter.graphics.lineTo(Content.SHOOTER_SIZE/2, 0);
        shooter.graphics.lineTo(0, -Content.SHOOTER_SIZE/2);
        shooter.graphics.endFill();
        _rotor.addChild(shooter);

        _name = new TextField();
        _name.text = "";
        _name.selectable = false;
        _name.defaultTextFormat = makeTextFormat(Content.FONT_COLOR, false);
        _name.autoSize = (posidx == 0) ? TextFieldAutoSize.LEFT : TextFieldAutoSize.RIGHT;
        addChild(_name);

        _score = new TextField();
        _score.autoSize = TextFieldAutoSize.CENTER;
        _score.selectable = false;
        _score.defaultTextFormat = makeTextFormat(uint(0xFFFFFF), true);
        addChild(_score);
        setScore(0);
    }

    public function setName (name :String) :void
    {
        if (_posidx % 2 == 0 && name.length > 10) {
            name = name.substring(0, 10) + "...";
        }
        _name.text = name;
        if (_posidx == 1 || _posidx == 2) {
            _name.x = NAME_X[_posidx] * Content.SHOOTER_SIZE - _name.width;
        } else {
            _name.x = NAME_X[_posidx] * Content.SHOOTER_SIZE;
        }
        if (_posidx < 2) {
            _name.y = NAME_Y[_posidx] * Content.SHOOTER_SIZE;
        } else  {
            _name.y = NAME_Y[_posidx] * Content.SHOOTER_SIZE - Content.FONT_SIZE -
                _name.getLineMetrics(0).ascent;
        }
    }

    public function setPoints (points :int, maxPoints :int) :void
    {
        if (_points != null) {
            _rotor.removeChild(_points);
        }

        _points = new Shape();
        _points.x = 0;
        _points.y = -Content.SHOOTER_SIZE/2 - POINTS_HEIGHT - 5;
        _points.graphics.beginFill(Content.SHOOTER_COLOR[_pidx]);
        var filled :int = Math.min(POINTS_HEIGHT, points * POINTS_HEIGHT / maxPoints);
        _points.graphics.drawRect(0, POINTS_HEIGHT-filled, POINTS_WIDTH, filled);
        _points.graphics.endFill();
        _points.graphics.lineStyle(1, uint(0x000000));
        _points.graphics.drawRect(0, 0, POINTS_WIDTH, POINTS_HEIGHT);
        _rotor.addChild(_points);
    }

    public function setScore (score :int) :void
    {
        _score.text = ("" + score);
        _score.x = SCORE_X[_posidx] * _score.width;
        _score.y = SCORE_Y[_posidx] * _score.height;
    }

    protected static function makeTextFormat (color :uint, bold :Boolean) : TextFormat
    {
        var format : TextFormat = new TextFormat();
        format.font = Content.FONT_NAME;
        format.color = color;
        format.size = Content.FONT_SIZE;
        format.bold = bold;
        return format;
    }

    protected var _pidx :int;
    protected var _posidx :int;
    protected var _rotor :Sprite;
    protected var _points :Shape;
    protected var _name :TextField;
    protected var _score :TextField;

    protected static const SCORE_X :Array = [ 0, -0.5, -1, -0.5 ];
    protected static const SCORE_Y :Array = [ -0.5, 0, -0.5, -1 ];

    protected static const NAME_X :Array = [ 0, -0.5, 0, 0.5 ];
    protected static const NAME_Y :Array = [ 0.5, 0, -0.5, 0 ];

    protected static const POINTS_WIDTH :int = 15;
    protected static const POINTS_HEIGHT :int = 50;
}

}
