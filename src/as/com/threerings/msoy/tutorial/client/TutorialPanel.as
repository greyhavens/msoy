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

import com.threerings.flex.FlexWrapper;

import com.threerings.msoy.client.MsoyContext;

public class TutorialPanel extends HBox
{
    public function TutorialPanel (ctx :MsoyContext)
    {
        styleName = "tutorialPanel";
        verticalScrollPolicy = ScrollPolicy.OFF;
        horizontalScrollPolicy = ScrollPolicy.OFF;
    }

    public function setContent (message :String, sprite :Sprite) :void
    {
        removeAllChildren();

        var text :Text = new Text();
        text.text = message;
        text.selectable = false;
        text.width = 400;

        place(new FlexWrapper(new PROFESSOR as DisplayObject, true), "tutorialProfessor");
        place(text, "tutorialText");
        place(new FlexWrapper(sprite, true), "tutorialIcon");

        // TODO: why do I need to set the width and height here? HBox should be doing it?
        width = 600;
        height = 120;
    }

    protected function place (obj :UIComponent, style :String) :void
    {
        obj.styleName = style;
        addChild(obj);
    }

    protected var _ctx :MsoyContext;

    [Embed(source="../../../../../../../rsrc/media/skins/tutorial/professor.png")]
    protected static const PROFESSOR :Class;
}

}
