//
// $Id$

package artvatar {

import flash.display.Shape;
import flash.display.SimpleButton;
import flash.display.Sprite;

import flash.text.TextField;
import flash.text.TextFieldAutoSize;

/**
 * Displays a simple button with a rounded rectangle for a face.
 */
public class Button extends SimpleButton
{
    public function Button (text :String)
    {
        upState = makeFace(text, FOREGROUND, BACKGROUND);
        overState = makeFace(text, HIGHLIGHT, BACKGROUND);
        downState = makeFace(text, BACKGROUND, HIGHLIGHT);
        hitTestState = upState;
    }

    protected function makeFace (text :String, foreground :uint, background :uint) :Sprite
    {
        var face :Sprite = new Sprite();

        // create the label so that we can measure its size
        var label :TextField = new TextField();
        label.text = text;
        label.textColor = foreground;
        label.autoSize = TextFieldAutoSize.LEFT;

        // create our button background (and outline)
        var button :Shape = new Shape();
        button.graphics.beginFill(background);
        button.graphics.drawRoundRect(
            0, 0, label.textWidth + 2*PADDING, label.textHeight + 2*PADDING, PADDING, PADDING);
        button.graphics.endFill();
        button.graphics.lineStyle(1, foreground);
        button.graphics.drawRoundRect(
            0, 0, label.textWidth + 2*PADDING, label.textHeight + 2*PADDING, PADDING, PADDING);
        face.addChild(button);

        label.x = PADDING;
        label.y = PADDING;
        face.addChild(label);

        return face;
    }

    protected static const PADDING :int = 5;

    protected static const BACKGROUND :uint = uint(0x6699CC);
    protected static const FOREGROUND :uint = uint(0x003366);
    protected static const HIGHLIGHT :uint = uint(0x0066FF);
}
}
