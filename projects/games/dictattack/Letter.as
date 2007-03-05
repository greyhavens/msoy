//
// $Id$

package {

import flash.text.TextField;
import flash.text.TextFormat;
import flash.text.TextFieldAutoSize;

import flash.display.Shape;
import flash.display.Sprite;

public class Letter extends Sprite
{
    public function Letter (color :uint)
    {
        var square :Shape = new Shape();
        square.graphics.beginFill(color);
        // square.graphics.lineStyle(borderSize, borderColor);
        square.graphics.drawRect(0, 0, Content.TILE_SIZE, Content.TILE_SIZE);
        square.graphics.endFill();
        addChild(square);

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
        _label.y = (Content.TILE_SIZE - _label.height)/2;
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
        format.size = Content.FONT_SIZE;
        return format;
    }

    protected static function makeHighlightFormat () : TextFormat
    {
        var format : TextFormat = new TextFormat();
        format.font = Content.FONT_NAME;
        format.color = Content.HIGH_FONT_COLOR;
        format.size = Content.FONT_SIZE;
        return format;
    }

    protected var _label :TextField;
}

}
