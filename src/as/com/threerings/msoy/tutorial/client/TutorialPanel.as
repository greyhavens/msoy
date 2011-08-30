//
// $Id$

package com.threerings.msoy.tutorial.client {

import flash.display.DisplayObject;
import flash.display.Graphics;
import flash.display.Sprite;

import caurina.transitions.Tweener;

import mx.containers.Canvas;
import mx.containers.HBox;
import mx.controls.Text;
import mx.core.Container;
import mx.core.ScrollPolicy;
import mx.core.UIComponent;

import com.threerings.util.MultiLoader;
import com.threerings.util.Util;

import com.threerings.flex.CommandButton;
import com.threerings.flex.CommandLinkButton;
import com.threerings.flex.FlexUtil;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.Prefs;

/**
 * Panel for the tutorial. A note on layout: the tutorial panel is expected to be a child of the
 * top panel and with its y position set to -HEIGHT. All other layout and animation is taken care
 * of internally.
 */
public class TutorialPanel extends Canvas
{
    public static const NWIDTH :int = 600;
    public static const NHEIGHT :int = 120;
    public static const MWIDTH :int = 320;
    public static const MHEIGHT :int = 250;
    public static const ROLL_TIME :Number = 0.6;

    public function TutorialPanel (onClose :Function)
    {
        verticalScrollPolicy = ScrollPolicy.OFF;
        horizontalScrollPolicy = ScrollPolicy.OFF;
        mouseEnabled = false;
        _onClose = onClose;

        MultiLoader.getContents(PROFESSOR, function (result :DisplayObject) :void {
            _professor = FlexUtil.wrapSized(result);
            _professor.styleName = "tutorialProfessor";
        });
    }

    public function setContent (item :TutorialItem) :void
    {
        Tweener.removeTweens(_glower);
        _glower.reset();
        _close.toolTip = Msgs.GENERAL.get("i.tutorial_close");
        _currentItem = item;
        _finishing = false;
        updateContent();
        if (item.popupHelper != null) {
            item.popupHelper.popup();
        }

        Tweener.addTween(_main, {y :_topMargin + getContentHeight(), time: ROLL_TIME,
                                 transition: "easeoutquart"});
    }

    /**
     * Flashes the next button, so that the user knows something more important is pending.
     */
    public function flashCloseButton () :void
    {
        if (_currentItem != null && _currentItem.buttonCloses) {
            return;
        }

        _glower.level = 0;
        _close.toolTip = Msgs.GENERAL.get("i.tutorial_close_pending");
        Tweener.addTween(_glower, {level: 1, time: .5, transition: "easeinoutsine"});
        Tweener.addTween(_glower, {level: 0, delay: .5, time: .5,
            transition: "easeinoutsine", onComplete: flashCloseButton });
    }

    /**
     * Lets the panel know how much horizontal space is available so that it can reposition.
     */
    public function setAvailableWidth (w :Number) :void
    {
        var wasMinimized :Boolean = _minimized;
        _minimized = w < NWIDTH - PADDING * 2; // clip a little before minimizing

        const margin :int = 5;
        x = w - getContentWidth();
        if (x > margin) {
            x = x / 2;
        } else {
            x -= margin;
        }

        if (wasMinimized != _minimized) {
            layout();
        }
    }

    /**
     * Sets the size of the header bar.
     */
    public function setTopMargin (margin :Number) :void
    {
        _topMargin = margin;
        height = getContentHeight() * 2 + _topMargin;
    }

    internal function handleClose () :void
    {
        Tweener.removeTweens(_glower);
        _glower.reset();
        if (_currentItem != null && _currentItem.popupHelper != null) {
            _currentItem.popupHelper.popdown();
        }
        _onClose();

        Tweener.addTween(_main, {y :0, time: ROLL_TIME, transition: "easeinquart"});
        _currentItem = null;
    }

    protected function getContentHeight () :int
    {
        return _minimized ? MHEIGHT : NHEIGHT;
    }

    protected function getContentWidth () :int
    {
        return _minimized ? MWIDTH : NWIDTH;
    }

    protected function updateContent () :void
    {
        var item :TutorialItem = _currentItem;
        _text.text = _finishing ? item.finishText : item.text;

        var tw :int = getContentWidth() - TEXT_X - PADDING;
        var th :int = getContentHeight() - PADDING * 4;
        if (item.buttonText == null || _finishing) {
            _action.setVisible(false);
            if (!_minimized) {
                tw -= CLOSE_SIZE;
            }
        } else {
            _action.setVisible(true);
            _action.label = item.buttonText;
            if (_minimized) {
                th -= BUTTON_HEIGHT + PADDING;
            } else {
                tw -= BUTTON_WIDTH + PADDING;
            }
        }
        _text.width = tw;
        _text.height = th;

        _close.setVisible(!item.hideClose || _finishing);
        _ignore.setVisible(item.ignorable);
    }

    protected function handleAction () :void
    {
        var item :TutorialItem = _currentItem;
        if (item != null) {
            if (item.buttonCloses) {
                handleClose();
            }
            if (item.finishText != null) {
                _finishing = true;
                updateContent();
            }
            if (item.onClick != null) {
                item.onClick();
            }
        }
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        _main = new Canvas();
        _main.styleName = "tutorialPanel";
        _main.verticalScrollPolicy = ScrollPolicy.OFF;
        _main.horizontalScrollPolicy = ScrollPolicy.OFF;
        addChild(_main);

        _text = new Text();
        _text.styleName = "tutorialText";
        _text.selectable = false;
        _text.mouseEnabled = false;

        _close = imgButton(handleClose, "i.tutorial_close");
        _close.styleName = "closeButton";
        _glower = new Glower(_close);

        _action = new CommandButton(null, handleAction);
        _action.styleName = "tutorialActionButton";
        _action.width = BUTTON_WIDTH;
        _action.height = BUTTON_HEIGHT;

        _ignore = new CommandLinkButton(Msgs.GENERAL.get("b.tutorial_ignore"), handleIgnore);
        _ignore.styleName = "tutorialIgnoreLink";
        _ignore.toolTip = Msgs.GENERAL.get("i.tutorial_ignore");
        _ignore.width = IGNORE_WIDTH;
        _ignore.height = IGNORE_HEIGHT;

        // add everything
        layout();
    }

    protected function layout () :void
    {
        for (var ii :int = _main.numChildren - 1; ii >= 0; --ii) {
            var child :UIComponent = _main.getChildAt(ii) as UIComponent;
            if (child is HBox) {
                HBox(child).removeAllChildren();
            }
            _main.removeChildAt(ii);
        }

        _main.width = getContentWidth();
        _main.height = getContentHeight();
        width = getContentWidth();
        this.y = -getContentHeight();
        // re-initialize the top margin
        setTopMargin(_topMargin);

        var cw :int = getContentWidth();
        var ch :int = getContentHeight();

        add(PROFESSOR_X, PROFESSOR_Y, _professor);
        addCentered(BUBBLE_X, makeSpeechBubble(cw - BUBBLE_X, ch - PADDING * 2));

        if (_minimized) {
            add(TEXT_X, CLOSE_Y + CLOSE_SIZE, _text);
            add(TEXT_X, ch - BUTTON_HEIGHT - PADDING * 2, _action);
        } else {
            addCentered(TEXT_X, _text);
            addCentered(cw - PADDING - BUTTON_WIDTH, _action);
        }

        add(cw - CLOSE_SIZE, CLOSE_Y, _close);
        add(cw - PADDING - IGNORE_WIDTH, ch - PADDING - IGNORE_HEIGHT, _ignore);

        if (_currentItem != null) {
            updateContent();
            _main.y = ch + _topMargin;
            Tweener.removeTweens(_main);
        }
    }

    protected function addCentered (x :int, child :UIComponent) :void
    {
        var hbox :Container = FlexUtil.createHBox(child);
        hbox.setStyle("verticalAlign", "middle");
        hbox.height = getContentHeight();
        add(x, 0, hbox);
    }

    protected function add (x :int, y :int, child :UIComponent) :void
    {
        child.x = x;
        child.y = y;
        _main.addChild(child);
    }

    protected function imgButton (callback :Function, tip :String) :CommandButton
    {
        var button :CommandButton = new CommandButton(null, callback);
        button.toolTip = Msgs.GENERAL.get(tip);
        return button;
    }

    protected function handleIgnore () :void
    {
        if (_currentItem != null && _currentItem.ignorable) {
            Prefs.ignoreTutorial(_currentItem.id);
            handleClose();
        }
    }

    protected static function makeSpeechBubble (width :int, height :int) :UIComponent
    {
        var s :Sprite = new Sprite();
        var g :Graphics = s.graphics;

        // rectangle outline
        g.lineStyle(1, BUBBLE_OUTLINE);
        g.beginFill(BUBBLE_FILL);
        g.drawRoundRect(0, 0, width, height, BUBBLE_ROUNDING);
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

        return FlexUtil.wrapSized(s);
    }

    protected static function drawBubbleTail (g :Graphics) :void
    {
        g.moveTo(0, TAIL_BASE_Y);
        g.lineTo(-TAIL_WIDTH, TAIL_TIP_Y);
        g.lineTo(0, TAIL_BASE_Y + TAIL_BASE_HEIGHT);
    }

    protected var _main :Canvas;
    protected var _topMargin :Number; // NaN
    protected var _minimized :Boolean;
    protected var _onClose :Function;
    protected var _action :CommandButton;
    protected var _ignore :CommandLinkButton;
    protected var _close :CommandButton
    protected var _glower :Glower;
    protected var _text :Text;
    protected var _professor :UIComponent;
    protected var _currentItem :TutorialItem;
    protected var _finishing :Boolean;

    [Embed(source="../../../../../../../rsrc/media/skins/tutorial/professor.swf",
           mimeType="application/octet-stream")]
    protected static const PROFESSOR :Class;

    protected static const TAIL_BASE_Y :int = 20;
    protected static const TAIL_BASE_HEIGHT :int = 20;
    protected static const TAIL_TIP_Y :int = 45;
    protected static const TAIL_WIDTH :int = 25;
    protected static const CLOSE_SIZE :int = 25;

    protected static const PROFESSOR_WIDTH :int = 100;
    protected static const BUTTON_WIDTH :int = 110;
    protected static const BUTTON_HEIGHT :int = 40;
    protected static const IGNORE_WIDTH :int = BUTTON_WIDTH;
    protected static const IGNORE_HEIGHT :int = 30;
    protected static const TAIL_OVERLAP :int = 10;
    protected static const PADDING :int = 10;

    protected static const PROFESSOR_X :int = 0;
    protected static const PROFESSOR_Y :int = 10;
    protected static const BUBBLE_X :int = PROFESSOR_X + PROFESSOR_WIDTH + TAIL_WIDTH - TAIL_OVERLAP;
    protected static const TEXT_X :int = BUBBLE_X + 10;
    protected static const CLOSE_Y :int = 15;

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
