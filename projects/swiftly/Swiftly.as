package {

import flash.display.Sprite;
import flash.text.TextField;
import flash.text.TextFieldAutoSize;
import flash.text.TextFormat;

[SWF(width="530", height="530")]
public class Swiftly extends Sprite
{
    private var label:TextField;
    private var labelText:String = "Hello world.";

    public function Swiftly() {
        configureLabel();
        setLabel(labelText);
    }

    public function setLabel(str:String):void {
        label.text = str;
    }

    private function configureLabel():void {
        label = new TextField();
        label.autoSize = TextFieldAutoSize.LEFT;
        label.background = true;
        label.border = true;

        var format:TextFormat = new TextFormat();
        format.font = "Verdana";
        format.color = 0xFF0000;
        format.size = 10;
        format.underline = true;

        label.defaultTextFormat = format;
        addChild(label);
    }
}
}
