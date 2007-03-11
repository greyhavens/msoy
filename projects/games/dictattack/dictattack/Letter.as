//
// $Id$

package dictattack {

import flash.text.TextField;
import flash.text.TextFormat;
import flash.text.TextFieldAutoSize;

import flash.display.Shape;
import flash.display.Sprite;

public class Letter extends Sprite
{
    public function Letter (type :int)
    {
        _type = type;
        setPlayable(false);

        _label = new TextField();
        _label.autoSize = TextFieldAutoSize.CENTER;
        _label.selectable = false;
        _label.defaultTextFormat = makePlainFormat();
        _label.x = 0;
        _label.width = Content.TILE_SIZE;
        addChild(_label);
    }

    public function setText (text :String) :void
    {
        _label.text = text;
        _label.y = (Content.TILE_SIZE - _label.height)/2 - FONT_ADJUST_HACK;
    }

    public function setPlayable (playable :Boolean) :void
    {
        if (_square != null) {
            removeChild(_square);
        }

        _square = new Shape();
        _square.graphics.beginFill(playable ? Content.PLAYABLE_COLOR : Content.TILE_COLOR);
        _square.graphics.drawRect(0, 0, Content.TILE_SIZE, Content.TILE_SIZE);
        _square.graphics.endFill();
        _square.graphics.lineStyle(2, Content.TILE_OUTLINE_COLORS[_type]);
        _square.graphics.drawRect(1, 1, Content.TILE_SIZE-2, Content.TILE_SIZE-2);
        addChildAt(_square, 0);
    }

    public function setHighlighted (highlighted :Boolean) :void
    {
        _label.setTextFormat(highlighted ? makeHighlightFormat() : makePlainFormat());
    }

    protected static function makePlainFormat () : TextFormat
    {
        var format : TextFormat = new TextFormat();
        format.font = Content.FONT_NAME;
        format.color = Content.FONT_COLOR;
        format.size = Content.TILE_FONT_SIZE;
        return format;
    }

    protected static function makeHighlightFormat () : TextFormat
    {
        var format : TextFormat = new TextFormat();
        format.font = Content.FONT_NAME;
        format.color = Content.HIGH_FONT_COLOR;
        format.size = Content.TILE_FONT_SIZE;
        return format;
    }

    protected var _type :int;
    protected var _square :Shape;
    protected var _label :TextField;

    protected static const FONT_ADJUST_HACK :int = 1;
}

}
