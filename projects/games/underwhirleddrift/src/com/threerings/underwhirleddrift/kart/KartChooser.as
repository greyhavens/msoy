package com.threerings.underwhirleddrift.kart {

import flash.display.DisplayObject;
import flash.display.Sprite;
import flash.display.Shape;

import flash.events.MouseEvent;

import flash.filters.BlurFilter;

import flash.text.TextField;
import flash.text.TextFieldAutoSize;

import com.threerings.underwhirleddrift.Camera;
import com.threerings.underwhirleddrift.UnderwhirledDrift;
import com.threerings.underwhirleddrift.UnderwhirledDriftController;
import com.threerings.underwhirleddrift.scene.Ground;
import com.threerings.underwhirleddrift.util.HueFilter;

public class KartChooser 
{
    public function KartChooser (control :UnderwhirledDriftController, blurObj :DisplayObject, 
        camera :Camera, ground :Ground)
    {
        _control = control;
        _blurObj = blurObj;
        _camera = camera;
        _ground = ground;
    }

    public function chooseKart () :Sprite 
    {
        //return oldChooseKart();

        _chooserSprite = new Sprite();
        _chooserSprite.x = UnderwhirledDrift.DISPLAY_WIDTH / 2;
        _chooserSprite.y = UnderwhirledDrift.DISPLAY_HEIGHT / 2;

        // sprites like this one start with nothing but an empty sprite inside them so that
        // they're in a known state for showKartInfo() to fill in
        _chooserSprite.addChild(_activeScreen = new Sprite());
        _activeScreen.addChild(new Sprite());

        var portrait :Sprite = new PORTRAIT_0();
        portrait.x = -286;
        portrait.y = -131;
        UnderwhirledDrift.registerEventListener(portrait, MouseEvent.CLICK, 
            function (evt :MouseEvent) :void {
                showKartInfo(0);
            });
        portrait.buttonMode = true;
        _chooserSprite.addChild(portrait);
        portrait = new PORTRAIT_1();
        portrait.x = -286;
        portrait.y = 0;
        UnderwhirledDrift.registerEventListener(portrait, MouseEvent.CLICK, 
            function (evt :MouseEvent) :void {
                showKartInfo(1);
            });
        portrait.buttonMode = true;
        _chooserSprite.addChild(portrait);
        portrait = new PORTRAIT_2();
        portrait.x = -286;
        portrait.y = 131;
        UnderwhirledDrift.registerEventListener(portrait, MouseEvent.CLICK, 
            function (evt :MouseEvent) :void {
                showKartInfo(2);
            });
        portrait.buttonMode = true;
        _chooserSprite.addChild(portrait);

        var okButton :Sprite = new OK_BUTTON();
        okButton.x = 293;
        okButton.y = 160;
        _chooserSprite.addChild(okButton);

        var colorYs :Array = [ -73, -44, -15 ];
        for (var ii :int = 0; ii < 3; ii++) {
            this["_color" + ii] = new Sprite();
            this["_color" + ii].addChild(new Sprite());
            this["_color" + ii].x = 313;
            this["_color" + ii].y = colorYs[ii];
            UnderwhirledDrift.registerEventListener(this["_color" + ii], MouseEvent.CLICK,
                function (color :int) :Function {
                    return function (evt :MouseEvent) :void {
                        chooseColor(color);
                    }
                }(ii));
            this["_color" + ii].buttonMode = true;
            _chooserSprite.addChild(this["_color" + ii]);
        }

        _activeKart = new Sprite();
        _activeKart.addChild(new Sprite());
        _activeKart.x = 128;
        _activeKart.y = 46;
        _chooserSprite.addChild(_activeKart);

        // defaults to devilite
        showKartInfo(1);

        return _chooserSprite;
    }

    public function showKartInfo (kartNumber :int) :void
    {
        try {
            _activeScreen.removeChildAt(0);
            _activeScreen.addChild(new KartChooser["SCREEN_" + kartNumber]());
            for (var ii :int = 0; ii < 3; ii++) {
                this["_color" + ii].removeChildAt(0);
                this["_color" + ii].addChild(new KartChooser["KART_" + kartNumber + "_COLOR_" + 
                    ii]());
            }
            _activeKart.removeChildAt(0);
            switch(kartNumber) {
            case 0: _activeKart.addChild(new KartSprite(KartSprite.KART_LIGHT, null, 160)); break;
            case 1: _activeKart.addChild(new KartSprite(KartSprite.KART_MEDIUM, null, 160)); break;
            case 2: _activeKart.addChild(new KartSprite(KartSprite.KART_HEAVY, null, 160)); break;
            }
        } catch (re :ReferenceError) {
            Log.getLog(this).warning("Failed to show kart info for kart " + kartNumber + ": " +
                re);
        }
    }

    public function chooseColor (color :int) :void
    {
        var hues :Array = [[ 0, -70, 109 ], [ 0, 114, -119 ], [ 0, 25, -109 ]];
        var kart :KartSprite;
        if (_activeScreen.getChildAt(0) is SCREEN_0) {
            kart = new KartSprite(KartSprite.KART_LIGHT, null, 160);
            kart.filters = [ HueFilter.getFilter(hues[0][color]) ];
        } else if (_activeScreen.getChildAt(0) is SCREEN_1) {
            kart = new KartSprite(KartSprite.KART_MEDIUM, null, 160);
            kart.filters = [ HueFilter.getFilter(hues[1][color]) ];
        } else if (_activeScreen.getChildAt(0) is SCREEN_2) {
            kart = new KartSprite(KartSprite.KART_HEAVY, null, 160);
            kart.filters = [ HueFilter.getFilter(hues[2][color]) ];
        }
        _activeKart.removeChildAt(0);
        _activeKart.addChild(kart);
    }

    public function oldChooseKart () :Sprite
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

    /** OK button to press when you've finished with selection */
    [Embed(source="../../../../../rsrc/kart_selection.swf#ok_button")]
    protected static const OK_BUTTON :Class;

    /** portraits and selection screens for each kart */
    [Embed(source="../../../../../rsrc/kart_selection.swf#soul_jelly_portrait")]
    protected static const PORTRAIT_0 :Class;
    [Embed(source="../../../../../rsrc/kart_selection.swf#devilite_portrait")]
    protected static const PORTRAIT_1 :Class;
    [Embed(source="../../../../../rsrc/kart_selection.swf#pit_brute_portrait")]
    protected static const PORTRAIT_2 :Class;

    [Embed(source="../../../../../rsrc/kart_selection.swf#soul_jelly_screen")]
    protected static const SCREEN_0 :Class;
    [Embed(source="../../../../../rsrc/kart_selection.swf#devilite_screen")]
    protected static const SCREEN_1 :Class;
    [Embed(source="../../../../../rsrc/kart_selection.swf#pit_brute_screen")]
    protected static const SCREEN_2 :Class;

    /** Color dots for kart hue */
    [Embed(source="../../../../../rsrc/kart_selection.swf#soul_jelly_color_1")]
    protected static const KART_0_COLOR_0 :Class;
    [Embed(source="../../../../../rsrc/kart_selection.swf#soul_jelly_color_2")]
    protected static const KART_0_COLOR_1 :Class;
    [Embed(source="../../../../../rsrc/kart_selection.swf#soul_jelly_color_3")]
    protected static const KART_0_COLOR_2 :Class;
    [Embed(source="../../../../../rsrc/kart_selection.swf#devilite_color_1")]
    protected static const KART_1_COLOR_0 :Class;
    [Embed(source="../../../../../rsrc/kart_selection.swf#devilite_color_2")]
    protected static const KART_1_COLOR_1 :Class;
    [Embed(source="../../../../../rsrc/kart_selection.swf#devilite_color_3")]
    protected static const KART_1_COLOR_2 :Class;
    [Embed(source="../../../../../rsrc/kart_selection.swf#pit_brute_color_1")]
    protected static const KART_2_COLOR_0 :Class;
    [Embed(source="../../../../../rsrc/kart_selection.swf#pit_brute_color_2")]
    protected static const KART_2_COLOR_1 :Class;
    [Embed(source="../../../../../rsrc/kart_selection.swf#pit_brute_color_3")]
    protected static const KART_2_COLOR_2 :Class;

    protected var _control :UnderwhirledDriftController;
    protected var _blurObj :DisplayObject;
    protected var _camera :Camera;
    protected var _ground :Ground;
    protected var _chooserSprite :Sprite;

    protected var _activeScreen :Sprite;
    protected var _color0 :Sprite;
    protected var _color1 :Sprite;
    protected var _color2 :Sprite;
    protected var _activeKart :Sprite;
}
}
