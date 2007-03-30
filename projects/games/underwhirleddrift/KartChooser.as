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
    public function KartChooser (control :UDControl, blurObj :DisplayObject, 
        camera :Camera, ground :Ground)
    {
        _control = control;
        _blurObj = blurObj;
        _camera = camera;
        _ground = ground;
    }

    public function chooseKart () :Sprite
    {
        setBlur(true);

        _chooserSprite = new Sprite();
        _chooserSprite.x = (UnderwhirledDrift.DISPLAY_WIDTH - WIDTH) / 2;
        _chooserSprite.y = (UnderwhirledDrift.DISPLAY_HEIGHT - HEIGHT) / 2;

        var selectText :TextField = new TextField();
        selectText.text = "Select Your Kart";
        selectText.selectable = false;
        selectText.autoSize = TextFieldAutoSize.CENTER;
        selectText.scaleX = selectText.scaleY = 3;
        selectText.x = (WIDTH - selectText.width) / 2;
        selectText.y -= 15;
        selectText.textColor = TEXT_COLOR;
        _chooserSprite.addChild(selectText);
        _chooserSprite.addChild(createKartSelection(LEFT, new KartSprite(KartSprite.KART_LIGHT, 
            null, 160), "Soul Jelly", 
            "The smallest and lightest\n" +  
            "of the karts, Soul Jellies\n" +
            "have excellent acceleration\n" + 
            "but the lowest of all top\n" + 
            "speeds."));
        _chooserSprite.addChild(createKartSelection(CENTER, new KartSprite(KartSprite.KART_MEDIUM,
            null, 180), "Devilite", 
            "Preferring to drive down\n" + 
            "the middle of the road,\n" + 
            "Devilites have average stats\n" + 
            "across the board.  A good\n" + 
            "choice for beginners."));
        _chooserSprite.addChild(createKartSelection(RIGHT, new KartSprite(KartSprite.KART_HEAVY,
            null, 200), "Pit Brute", 
            "The big lugs can barely\n" + 
            "squeeze into the karts\n"+ 
            "they drive. While their\n" + 
            "acceleration is poor, their\n" + 
            "blazing top speed can leave\n" + 
            "all other racers in the dust."));
        return _chooserSprite;
    }

    protected function createKartSelection (loc :int, kart :Sprite, title :String, 
        desc :String) :Sprite
    {
        var selection :Sprite = new Sprite();
        var titleText :TextField = new TextField();
        titleText.text = title;
        titleText.selectable = false;
        titleText.autoSize = TextFieldAutoSize.CENTER;
        titleText.scaleX = titleText.scaleY = 2.5;
        titleText.x = -50;
        titleText.y = -HEIGHT + 30;
        titleText.textColor = TEXT_COLOR;
        selection.addChild(titleText);

        selection.addChild(kart);
        UnderwhirledDrift.registerEventListener(kart, MouseEvent.CLICK, selectedKart);
        kart.y = -HEIGHT + 230;

        var descText :TextField = new TextField();
        descText.text = desc;
        descText.selectable = false;
        descText.autoSize = TextFieldAutoSize.CENTER;
        descText.x = -(SELECTION_WIDTH - titleText.width);
        descText.y = -125;
        descText.scaleX = descText.scaleY = 1.5;
        descText.textColor = TEXT_COLOR;
        selection.addChild(descText);

        selection.y = HEIGHT;
        switch (loc) {
        case LEFT:
            selection.x = SELECTION_WIDTH / 2;
            titleText.x = -70;
            break;
        case CENTER:
            titleText.x = -55;
            selection.x = WIDTH / 2;
            break;
        case RIGHT:
            titleText.x = -70;
            selection.x = WIDTH - SELECTION_WIDTH / 2;
            break
        }
        return selection;
    }

    protected function selectedKart (event :MouseEvent) :void
    {
        _chooserSprite.parent.removeChild(_chooserSprite);
        _control.kartPicked(new Kart((event.currentTarget as KartSprite).kartType, _camera, 
            _ground));
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

    protected static const WIDTH :int = 675;
    protected static const HEIGHT :int = 350;
    protected static const SELECTION_WIDTH :int = 200;

    protected static const LEFT :int = 1;
    protected static const CENTER :int = 2;
    protected static const RIGHT :int = 3;

    protected static const TEXT_COLOR :int = 0xEAEFF1;

    protected var _control :UDControl;
    protected var _blurObj :DisplayObject;
    protected var _camera :Camera;
    protected var _ground :Ground;
    protected var _chooserSprite :Sprite;
}
}
