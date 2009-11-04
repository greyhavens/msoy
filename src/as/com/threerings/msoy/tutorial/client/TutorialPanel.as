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

import caurina.transitions.Tweener;

import com.threerings.util.Util;

import com.threerings.flex.CommandLinkButton;
import com.threerings.flex.CommandButton;
import com.threerings.flex.FlexUtil;

import com.threerings.util.MultiLoader;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.client.PlaceLayer;
import com.threerings.msoy.client.Prefs;

public class TutorialPanel extends Canvas
    implements PlaceLayer
{
    public static const WIDTH :int = 600;
    public static const HEIGHT :int = 120;

    public function TutorialPanel (ctx :MsoyContext, onClose :Function)
    {
        styleName = "tutorialPanel";
        verticalScrollPolicy = ScrollPolicy.OFF;
        horizontalScrollPolicy = ScrollPolicy.OFF;
        _onClose = onClose;

        MultiLoader.getContents(PROFESSOR, function (result :DisplayObject) :void {
            _professor = result;
        });
    }

    public function setContent (item :TutorialItem) :void
    {
        Tweener.removeTweens(_glower);
        _glower.reset();

        _close.toolTip = Msgs.GENERAL.get("i.tutorial_close");

        _text.text = item.text;

        if (item.buttonText == null) {
            _action.setVisible(false);
            _text.width = TEXT_FULL_WIDTH;
        } else {
            _action.setVisible(true);
            _action.setCallback(item.onClick);
            _action.label = item.buttonText;
            _text.width = TEXT_WIDTH;
        }

        _currentItem = item;
    }

    /**
     * Flashes the next button, so that the user knows something more important is pending.
     */
    public function flashCloseButton () :void
    {
        _glower.level = 0;
        _close.toolTip = Msgs.GENERAL.get("i.tutorial_close_pending");
        Tweener.addTween(_glower, {level: 1, time: .5, transition: "easeinoutsine"});
        Tweener.addTween(_glower, {level: 0, delay: .5, time: .5,
            transition: "easeinoutsine", onComplete: flashCloseButton });
    }

    // from PlaceLayer
    public function setPlaceSize (unscaledWidth :Number, unscaledHeight :Number) :void
    {
        // clip width to avoid a scroll bar showing up in the place box
        width = Math.min(WIDTH, unscaledWidth);

        // the director controls our y coordinate, but we need to center in x
        x = (unscaledWidth - width) / 2;
    }

    protected function handleClose () :void
    {
        Tweener.removeTweens(_glower);
        _glower.reset();
        _onClose();
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        addCentered(PROFESSOR_X, "tutorialProfessor", _professor);
        addCentered(BUBBLE_X, null, makeSpeechBubble());
        addCentered(TEXT_X, "tutorialText", _text = new Text());
        add(CLOSE_X, CLOSE_Y, "closeButton", _close = imgButton(handleClose, "i.tutorial_close"));
        addCentered(BUTTON_X, "tutorialActionButton", _action = new CommandButton());
        add(BUTTON_X, HEIGHT - PADDING - IGNORE_HEIGHT, "tutorialIgnoreLink",
            _ignore = new CommandLinkButton(Msgs.GENERAL.get("b.tutorial_ignore"),
                                            Util.adapt(handleIgnore)));

        _text.selectable = false;
        _text.width = TEXT_WIDTH;

        _action.width = BUTTON_WIDTH;
        _action.height = BUTTON_HEIGHT;

        _ignore.width = BUTTON_WIDTH;
        _ignore.height = IGNORE_HEIGHT;

        _glower = new Glower(_close);

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

    protected function handleIgnore () :void
    {
        if (_currentItem != null) {
            Prefs.ignoreTutorial(_currentItem.id);
            _onClose();
        }
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

        return s;
    }

    protected static function drawBubbleTail (g :Graphics) :void
    {
        g.moveTo(0, TAIL_BASE_Y);
        g.lineTo(-TAIL_WIDTH, TAIL_TIP_Y);
        g.lineTo(0, TAIL_BASE_Y + TAIL_BASE_HEIGHT);
    }

    protected var _ctx :MsoyContext;
    protected var _onClose :Function;
    protected var _action :CommandButton;
    protected var _ignore :CommandLinkButton;
    protected var _close :CommandButton
    protected var _glower :Glower;
    protected var _text :Text;
    protected var _professor :DisplayObject;
    protected var _currentItem :TutorialItem;

    [Embed(source="../../../../../../../rsrc/media/skins/tutorial/professor.swf",
           mimeType="application/octet-stream")]
    protected static const PROFESSOR :Class;

    protected static const TAIL_BASE_Y :int = 20;
    protected static const TAIL_BASE_HEIGHT :int = 20;
    protected static const TAIL_TIP_Y :int = 45;
    protected static const TAIL_WIDTH :int = 25;

    protected static const PROFESSOR_WIDTH :int = 100;
    protected static const BUTTON_WIDTH :int = 110;
    protected static const BUTTON_HEIGHT :int = 40;
    protected static const IGNORE_HEIGHT :int = 30;
    protected static const TAIL_OVERLAP :int = 10;
    protected static const PADDING :int = 10;

    protected static const PROFESSOR_X :int = 0;
    protected static const BUBBLE_X :int = PROFESSOR_X + PROFESSOR_WIDTH + TAIL_WIDTH - TAIL_OVERLAP;
    protected static const TEXT_X :int = BUBBLE_X + 10;
    protected static const BUTTON_X :int = WIDTH - PADDING - BUTTON_WIDTH;
    protected static const TEXT_WIDTH :int = BUTTON_X - TEXT_X - PADDING;
    protected static const TEXT_FULL_WIDTH :int = WIDTH - PADDING - TEXT_X;
    protected static const CLOSE_X :int = WIDTH - 25;
    protected static const CLOSE_Y :int = 15;

    protected static const BUBBLE_WIDTH :int = WIDTH - BUBBLE_X;
    protected static const BUBBLE_HEIGHT :int = HEIGHT - PADDING * 2;
    protected static const BUBBLE_ROUNDING :int = 35;
    protected static const BUBBLE_OUTLINE :int = 0x000000;
    protected static const BUBBLE_FILL :int = 0xffffff;
}
}

import flash.display.DisplayObject;
import flash.filters.GlowFilter;

class Glower
{
    public function Glower (target :DisplayObject)
    {
        _target = target;
        _level = 0;
    }

    public function reset () :void
    {
        _level = 0;
        _target.filters = [];
    }

    public function set level (value :Number) :void
    {
        _level = value;
        var filters :Array = _target.filters;
        var glow :GlowFilter;
        if (filters != null && filters.length > 0) {
            glow = filters[0] as GlowFilter;
        } else {
            glow = new GlowFilter(0x94c7e3, 1, 6, 6, 12);
        }

        glow.blurX = 4 + 4 * level;
        glow.blurY = 4 + 4 * level;
        glow.strength = 2 + 4 * level;
        _target.filters = [glow];
    }

    public function get level () :Number
    {
        return _level;
    }

    protected var _target :DisplayObject;
    protected var _level :Number;
}
