//
// $Id$

package {

import flash.display.DisplayObjectContainer;
import flash.display.SimpleButton;

import flash.geom.Matrix;
import flash.geom.ColorTransform;

import flash.text.TextField;
import flash.text.TextFieldAutoSize;
import flash.text.TextFieldType;
import flash.text.TextFormat;

public /* abstract */ class Button extends SimpleButton
{
    public static const PAD_LEFT :int = 0;
    public static const PAD_RIGHT :int = 1;
    public static const PAD_TOP :int = 2;
    public static const PAD_BOTTOM :int = 3;

    public function add (parent :DisplayObjectContainer, x :Number, y :Number) :void
    {
        parent.addChild(this);
        this.x = x;
        this.y = y;
    }

    protected function makeButtonLabel (
        txt :String, width :Number, height :Number, wordWrap :Boolean, fontSize :int,
        foreground :uint) :TextField
    {
        var field :TextField = new TextField();
        field.x = x;
        field.y = y;
        if (width > 0 && height > 0) {
            field.width = width;
            field.height = height;
            field.autoSize = TextFieldAutoSize.NONE;
        } else {
            field.autoSize = TextFieldAutoSize.CENTER;
        }
        field.wordWrap = wordWrap;

        var format :TextFormat = new TextFormat();
        format.size = fontSize;
        format.color = foreground;
        field.defaultTextFormat = format;

        field.text = txt;
        return field;
    }
}
}
