//
// $Id$

package {

import flash.display.DisplayObject;
import flash.display.DisplayObjectContainer;
import flash.display.SimpleButton;
import flash.display.Shape;
import flash.display.Sprite;

import flash.geom.Matrix;
import flash.geom.ColorTransform;

import flash.text.TextField;
import flash.text.TextFieldAutoSize;
import flash.text.TextFieldType;
import flash.text.TextFormat;
import flash.text.TextFormatAlign;

public class TextButton extends Button
{
    public function TextButton (
        txt :String, width :Number = 0, height :Number = 0, wordWrap :Boolean = true,
        fontSize :int = 16, foreground :uint = 0x003366, background :uint = 0x6699CC,
        highlight :uint = 0x0066Ff, padding :Number = 5)
    {
        var static :Boolean = width > 0 && height > 0;

        this.upState = makeButtonFace(
            makeButtonLabel(txt, width, height, wordWrap, fontSize, foreground),
            foreground, background, padding, static);
        this.overState = makeButtonFace(
            makeButtonLabel(txt, width, height, wordWrap, fontSize, highlight),
            highlight, background, padding, static);
        this.downState = makeButtonFace(
            makeButtonLabel(txt, width, height, wordWrap, fontSize, background),
            background, highlight, padding, static);
        this.hitTestState = this.upState;
    }

    protected function makeButtonFace (
        label :TextField, foreground :uint, background :uint,
        padding :int, static :Boolean) :Sprite
    {
        var face :Sprite = new Sprite();

        var w :Number = label.width + (!static ? 2*padding : 0);
        var h :Number = label.height + (!static ? 2*padding : 0);

        // create our button background (and outline)
        var button :Shape = new Shape();
        button.graphics.beginFill(background);
        button.graphics.drawRoundRect(0, 0, w, h, padding, padding);
        button.graphics.endFill();
        button.graphics.lineStyle(1, foreground);
        button.graphics.drawRoundRect(0, 0, w, h, padding, padding);

        face.addChild(button);

        label.x = padding;
        label.y = padding;
        face.addChild(label);

        return face;
    }
}
}
