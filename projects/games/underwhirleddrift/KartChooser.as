package 
{

import flash.display.DisplayObject;
import flash.display.Sprite;
import flash.display.Shape;

import flash.events.MouseEvent;

import flash.filters.BlurFilter;

import flash.text.TextField;
import flash.text.TextFieldAutoSize;

public class KartChooser 
{
    public function KartChooser (udInst :UnderwhirledDrift, blurObj :DisplayObject, 
        camera :Camera, ground :Ground)
    {
        _udInst = udInst;
        _blurObj = blurObj;
        _camera = camera;
        _ground = ground;
    }

    public function chooseKart () :void
    {
        setBlur(true);

        _chooserSprite = new Sprite();
        _chooserSprite.x = (UnderwhirledDrift.DISPLAY_WIDTH - WIDTH) / 2;
        _chooserSprite.y = (UnderwhirledDrift.DISPLAY_HEIGHT - HEIGHT) / 2;
        _udInst.addChild(_chooserSprite);


        var selectText :TextField = new TextField();
        selectText.text = "Select Your Kart";
        selectText.selectable = false;
        selectText.autoSize = TextFieldAutoSize.CENTER;
        selectText.scaleX = selectText.scaleY = 4;
        selectText.x = (WIDTH - selectText.width) / 2;
        _chooserSprite.addChild(selectText);
        var kartM :KartSprite = new KartSprite(KartSprite.KART_MEDIUM, 160);
        kartM.x = kartM.width / 2;
        kartM.y = HEIGHT; 
        kartM.addEventListener(MouseEvent.CLICK, selectedKart);
        _chooserSprite.addChild(kartM);
        var kartL :KartSprite = new KartSprite(KartSprite.KART_LIGHT, 200);
        kartL.x = WIDTH - kartL.width / 2;
        kartL.y = HEIGHT;
        kartL.addEventListener(MouseEvent.CLICK, selectedKart);
        _chooserSprite.addChild(kartL);
    }

    protected function selectedKart (event :MouseEvent) :void
    {
        _udInst.removeChild(_chooserSprite);
        _udInst.setKart(new Kart((event.currentTarget as KartSprite).kartType, _camera, _ground));
        setBlur(false);
        // prevent NPE on further click event handlers
        event.stopImmediatePropagation();
    }

    protected function setBlur (doBlur :Boolean) :void
    {
        var blurIndex :int = -1;
        var ourFilters :Array = _blurObj.filters;
        if (ourFilters != null) {
            for (var ii :int = 0; ii < ourFilters.length; ii++) {
                if (ourFilters[ii] is BlurFilter) {
                    blurIndex = ii;
                    break;
                }
            }
        }

        if (doBlur == (blurIndex != -1)) {
            return;
        }

        if (doBlur) {
            if (ourFilters == null) {
                ourFilters = [];
            }
            var blur :BlurFilter = new BlurFilter(15, 15);
            ourFilters.push(blur);
            _blurObj.filters = ourFilters;
        } else {
            ourFilters.splice(blurIndex, 1);
            _blurObj.filters = ourFilters;
        }
    }

    protected static const WIDTH :int = 400;
    protected static const HEIGHT :int = 200;

    protected var _udInst :UnderwhirledDrift;
    protected var _blurObj :DisplayObject;
    protected var _camera :Camera;
    protected var _ground :Ground;
    protected var _chooserSprite :Sprite;
}
}
