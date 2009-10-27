//
// $Id$

package com.threerings.msoy.tutorial.client {

import flash.display.DisplayObject;
import flash.display.Graphics;
import flash.display.Sprite;

import mx.core.Container;
import mx.core.ScrollPolicy;
import mx.core.UIComponent;
import mx.controls.Text;
import mx.containers.Canvas;

import com.threerings.flex.CommandButton;
import com.threerings.flex.FlexUtil;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.client.PlaceLayer;

public class TutorialPanel extends Canvas
    implements PlaceLayer
{
    public static const WIDTH :int = 600;
    public static const HEIGHT :int = 120;

    public function TutorialPanel (ctx :MsoyContext, onNext :Function, onClose :Function)
    {
        styleName = "tutorialPanel";
        verticalScrollPolicy = ScrollPolicy.OFF;
        horizontalScrollPolicy = ScrollPolicy.OFF;
        _onNext = onNext;
        _onClose = onClose;
    }

    public function setContent (message :String, buttonText :String, buttonFn :Function) :void
    {
        _text.text = message;

        if (buttonText == null) {
            _action.setVisible(false);
            _text.width = TEXT_FULL_WIDTH;
        } else {
            _action.setVisible(true);
            _action.setCallback(buttonFn);
            _action.label = buttonText;
            _text.width = TEXT_WIDTH;
        }
    }

    /**
     * Flashes the next button, so that the user knows something more important is pending.
     */
    public function flashNext () :void
    {
    }

    // from PlaceLayer
    public function setPlaceSize (unscaledWidth :Number, unscaledHeight :Number) :void
    {
        // clip width to avoid a scroll bar showing up in the place box
        width = Math.min(WIDTH, unscaledWidth);

        // the director controls our y coordinate, but we need to center in x
        x = (unscaledWidth - width) / 2;
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        addCentered(PROFESSOR_X, "tutorialProfessor", new PROFESSOR as DisplayObject);
        addCentered(BUBBLE_X, null, makeSpeechBubble());
        addCentered(TEXT_X, "tutorialText", _text = new Text());
        add(CLOSE_X, CLOSE_Y, "closeButton", imgButton(_onClose, "i.tutorial_close"));
        addCentered(BUTTON_X, "tutorialActionButton", _action = new CommandButton());

        _text.selectable = false;
        _text.width = TEXT_WIDTH;

        _action.width = BUTTON_WIDTH;
        _action.height = BUTTON_HEIGHT;

        // set the width and height for all time
        width = WIDTH;
        height = HEIGHT;
    }

    protected function addCentered (x :int, style :String, child :DisplayObject) :void
    {
        var comp :UIComponent = wrap(child, style);
        var hbox :Container = FlexUtil.createHBox(comp);
        hbox.setStyle("verticalAlign", "middle");
        hbox.height = HEIGHT;
        add(x, 0, null, hbox);
    }

    protected function add (x :int, y :int, style :String, child :DisplayObject) :void
    {
        var comp :UIComponent = wrap(child, style);
        comp.x = x;
        comp.y = y;
        addChild(comp);
    }

    protected function wrap (obj :DisplayObject, style :String) :UIComponent
    {
        var comp :UIComponent = obj as UIComponent;
        if (comp == null) {
            comp = FlexUtil.wrapSized(obj);
        }
        comp.styleName = style;
        return comp;
    }

    protected function imgButton (callback :Function, tip :String) :CommandButton
    {
        var button :CommandButton = new CommandButton(null, callback);
        button.toolTip = Msgs.GENERAL.get(tip);
        return button;
    }

    protected static function makeSpeechBubble () :Sprite
    {
        var s :Sprite = new Sprite();
        var g :Graphics = s.graphics;

        // rectangle outline
        g.lineStyle(1, BUBBLE_OUTLINE);
        g.beginFill(BUBBLE_FILL);
        g.drawRoundRect(0, 0, BUBBLE_WIDTH, BUBBLE_HEIGHT, BUBBLE_ROUNDING);
        g.endFill();

        // tail interior
        g.lineStyle(1, BUBBLE_FILL);
        g.beginFill(BUBBLE_FILL);
        g.drawRect(0, TAIL_BASE_Y + 1, 1, TAIL_BASE_HEIGHT - 2);
        drawBubbleTail(g);
        g.endFill();

        // tail exterior
        g.lineStyle(1, BUBBLE_OUTLINE);
        drawBubbleTail(g);

        // make sure our children get the alpha setting too (contrary to flash documentation)
        //blendMode = BlendMode.LAYER;
        return s;
    }

    protected static function drawBubbleTail (g :Graphics) :void
    {
        g.moveTo(0, TAIL_BASE_Y);
        g.lineTo(-TAIL_WIDTH, TAIL_TIP_Y);
        g.lineTo(0, TAIL_BASE_Y + TAIL_BASE_HEIGHT);
    }

    protected var _ctx :MsoyContext;
    protected var _onNext :Function;
    protected var _onClose :Function;
    protected var _action :CommandButton;
    protected var _text :Text;

    [Embed(source="../../../../../../../rsrc/media/skins/tutorial/professor.png")]
    protected static const PROFESSOR :Class;

    protected static const BUBBLE_WIDTH :int = 500;
    protected static const BUBBLE_HEIGHT :int = HEIGHT - 20;
    protected static const BUBBLE_ROUNDING :int = 35;
    protected static const BUBBLE_OUTLINE :int = 0x000000;
    protected static const BUBBLE_FILL :int = 0xffffff;

    protected static const TAIL_BASE_Y :int = 20;
    protected static const TAIL_BASE_HEIGHT :int = 20;
    protected static const TAIL_TIP_Y :int = 45;
    protected static const TAIL_WIDTH :int = 30;

    protected static const PROFESSOR_X :int = 0;
    protected static const BUBBLE_X :int = PROFESSOR_X + 100;
    protected static const TEXT_X :int = BUBBLE_X + 10;
    protected static const TEXT_WIDTH :int = 370;
    protected static const BUTTON_X :int = TEXT_X + TEXT_WIDTH;
    protected static const BUTTON_WIDTH :int = WIDTH - 10 - BUTTON_X;
    protected static const BUTTON_HEIGHT :int = 40;
    protected static const TEXT_FULL_WIDTH :int = BUTTON_X + BUTTON_WIDTH - TEXT_X;
    protected static const CLOSE_X :int = WIDTH - 25;
    protected static const CLOSE_Y :int = 15;
}
}
