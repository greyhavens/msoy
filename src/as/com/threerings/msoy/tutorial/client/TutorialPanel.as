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
        } else {
            _action.setVisible(true);
            _action.setCallback(buttonFn);
            _action.label = buttonText;
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

        addCentered(0, "tutorialProfessor", new PROFESSOR as DisplayObject);
        addCentered(100, null, makeSpeechBubble());
        addCentered(110, "tutorialText", _text = new Text());
        add(575, 15, "closeButton", imgButton(_onClose, "i.tutorial_close"));
        addCentered(480, "tutorialActionButton", _action = new CommandButton());

        _text.selectable = false;
        _text.width = 370;

        _action.width = 110;
        _action.height = 40;

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
        g.lineStyle(1, OUTLINE);
        g.beginFill(FILL);
        g.drawRoundRect(0, 0, BUBBLE_WIDTH, BUBBLE_HEIGHT, ROUNDING);
        g.endFill();

        // tail interior
        g.lineStyle(1, FILL);
        g.beginFill(FILL);
        g.drawRect(0, TAIL_BASE_Y + 1, 1, TAIL_BASE_HEIGHT - 2);
        drawBubbleTail(g);
        g.endFill();

        // tail exterior
        g.lineStyle(1, OUTLINE);
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
    protected static const ROUNDING :int = 35;
    protected static const OUTLINE :int = 0x000000;
    protected static const FILL :int = 0xffffff;

    protected static const TAIL_BASE_Y :int = 20;
    protected static const TAIL_BASE_HEIGHT :int = 20;
    protected static const TAIL_TIP_Y :int = 45;
    protected static const TAIL_WIDTH :int = 30;
}
}
