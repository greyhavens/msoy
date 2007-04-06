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
        _camera = camera;
        _ground = ground;
    }

    public function chooseKart () :Sprite 
    {
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
        UnderwhirledDrift.registerEventListener(okButton, MouseEvent.CLICK,
            function (evt :MouseEvent) :void {
                _chooserSprite.parent.removeChild(_chooserSprite);
                var kartSprite :KartSprite = _activeKart.getChildAt(0) as KartSprite;
                _control.kartPicked(new Kart(kartSprite.kartType, kartSprite.color, _camera, 
                    _ground));
                evt.stopImmediatePropagation();
            });
        okButton.buttonMode = true;
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
            case 0: 
                _activeKart.addChild(new KartSprite(KartSprite.KART_LIGHT, 0, null, 160)); 
                break;
            case 1: 
                _activeKart.addChild(new KartSprite(KartSprite.KART_MEDIUM, 0, null, 160)); 
                break;
            case 2: 
                _activeKart.addChild(new KartSprite(KartSprite.KART_HEAVY, 0, null, 160)); 
                break;
            }
        } catch (re :ReferenceError) {
            Log.getLog(this).warning("Failed to show kart info for kart " + kartNumber + ": " +
                re);
        }
    }

    public function chooseColor (color :int) :void
    {
        _activeKart.removeChildAt(0);
        if (_activeScreen.getChildAt(0) is SCREEN_0) {
            _activeKart.addChild(new KartSprite(KartSprite.KART_LIGHT, color, null, 160));
        } else if (_activeScreen.getChildAt(0) is SCREEN_1) {
            _activeKart.addChild(new KartSprite(KartSprite.KART_MEDIUM, color, null, 160));
        } else if (_activeScreen.getChildAt(0) is SCREEN_2) {
            _activeKart.addChild(new KartSprite(KartSprite.KART_HEAVY, color, null, 160));
        }
    }

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
