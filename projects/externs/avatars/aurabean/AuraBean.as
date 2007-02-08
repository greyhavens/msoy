package {

import flash.display.Bitmap;
import flash.display.BitmapData;
import flash.display.BlendMode;
import flash.display.DisplayObject;
import flash.display.Graphics;
import flash.display.Sprite;

import flash.events.Event;
import flash.events.MouseEvent;

[SWF(width="400", height="400")]
public class AuraBean extends Sprite
{
    public function AuraBean ()
    {
        _bit = new BitmapData(1, 1, true, 0x00000000);

        // first add the bean
        var bean :DisplayObject = (new BEAN() as DisplayObject);
        bean.x = (400 - bean.width) / 2;
        bean.y = (400 - bean.height) / 2;
        addChild(bean);

        // create and add the aura, underneath the bean
        _aura = new Sprite();
        _aura.blendMode = BlendMode.LAYER;
        _aura.x = 200;
        _aura.y = 200;
        addChildAt(_aura, 0);

        // then create a bunch of bitmaps and lay them out on the aura
        for (var ii :int = 0; ii < 360; ii += 12) {
            var bmp :Bitmap = new Bitmap(_bit);
            bmp.scaleX = 10;
            bmp.scaleY = 300;
            bmp.rotation = ii;
            _aura.addChild(bmp);
        }

        addEventListener(Event.ENTER_FRAME, handleEnterFrame);
        addEventListener(MouseEvent.MOUSE_OVER, handleMouseIn);
        addEventListener(MouseEvent.MOUSE_OUT, handleMouseOut);
    }

    protected function handleEnterFrame (evt :Event) :void
    {
        _aura.rotation += 1;

        if (_in) {
            _inCounter++;

            var val :uint = Math.min(255, Math.max(0, _inCounter));
            _bit.setPixel32(0, 0, (val << 24));
        }
    }

    protected function handleMouseIn (evt :MouseEvent) :void
    {
        _in = true;
        _inCounter = -20;
    }

    protected function handleMouseOut (event :MouseEvent) :void
    {
        _in = false;
        _bit.setPixel32(0, 0, 0);
    }

    protected var _aura :Sprite;

    protected var _bit :BitmapData;

    protected var _in :Boolean;
    protected var _inCounter :int;

    [Embed(source="bean.png")]
    private static var BEAN :Class;
}
}
