package {

import flash.display.Sprite;
import flash.display.Bitmap;
import flash.display.BitmapData;
import flash.display.IBitmapDrawable;
import flash.display.Shape;

import flash.geom.Matrix;

import com.threerings.ezgame.EZGameControl;

[SWF(width="400", height="400")]
public class UnderworldDrift extends Sprite
{
    /** width of the masked display */
    public static const DISPLAY_WIDTH :int = 711;

    /** height of the masked display */
    public static const DISPLAY_HEIGHT :int = 400;

    public function UnderworldDrift ()
    {
        var masker :Shape = new Shape();
        masker.graphics.beginFill(0xFFFFFF);
        masker.graphics.drawRect(0, 0, DISPLAY_WIDTH, DISPLAY_HEIGHT);
        masker.graphics.endFill();
        this.mask = masker;
        addChild(masker);

        // "sky"
        var colorBackground :Shape = new Shape();
        colorBackground.graphics.beginFill(0x8888FF);
        colorBackground.graphics.drawRect(0, 0, DISPLAY_WIDTH, DISPLAY_HEIGHT / 2);
        colorBackground.graphics.endFill();
        addChild(colorBackground);

        var ground :Ground = new Ground();
        addChild(ground);
    }
}
}
