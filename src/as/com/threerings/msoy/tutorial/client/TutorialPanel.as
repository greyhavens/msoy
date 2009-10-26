//
// $Id$

package com.threerings.msoy.tutorial.client {

import flash.display.DisplayObject;
import flash.display.Sprite;

import mx.core.Container;
import mx.core.ScrollPolicy;
import mx.core.UIComponent;
import mx.controls.Text;
import mx.containers.HBox;

import com.threerings.flex.CommandButton;
import com.threerings.flex.FlexUtil;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyContext;

public class TutorialPanel extends HBox
{
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
            _action.label = buttonText;
            _action.setCallback(buttonFn);
        }
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        var right :Container;
        addChild(style(FlexUtil.wrapSized(new PROFESSOR as DisplayObject), "tutorialProfessor"));
        addChild(style(_text = new Text(), "tutorialText"));
        addChild(right = FlexUtil.createVBox(
            FlexUtil.createHBox(
                imgButton("tutorialNextButton", _onNext, "i.tutorial_next"),
                imgButton("tutorialCloseButton", _onClose, "i.tutorial_close")),
            FlexUtil.createSpacer(1, 20),
            _action = new CommandButton()));

        right.setStyle("horizontalAlign", "right");
        right.setStyle("top", "0");
        right.setStyle("bottom", "0");

        _text.selectable = false;
        _text.width = 400;
    }

    protected function imgButton (styleName :String, callback :Function, tip :String) :CommandButton
    {
        var button :CommandButton = new CommandButton(null, callback);
        button.toolTip = Msgs.GENERAL.get(tip);
        return style(button, styleName) as CommandButton;
    }

    protected function style (comp :UIComponent, styleName :String) :UIComponent
    {
        comp.styleName = styleName;
        return comp;
    }

    protected var _ctx :MsoyContext;
    protected var _onNext :Function;
    protected var _onClose :Function;
    protected var _action :CommandButton;
    protected var _text :Text;

    [Embed(source="../../../../../../../rsrc/media/skins/tutorial/professor.png")]
    protected static const PROFESSOR :Class;
}

}
