//
// $Id$

package com.threerings.msoy.tutorial.client {

import flash.display.Sprite;

import mx.core.UIComponent;

import caurina.transitions.Tweener;

import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.client.PlaceBox;
import com.threerings.msoy.client.TopPanel;

public class TutorialDirector
{
    public function TutorialDirector (ctx :MsoyContext)
    {
        _ctx = ctx;
        _panel = new TutorialPanel (_ctx);
    }

    public function popupHint (text :String, icon :Sprite) :void
    {
        var animate :Boolean = _panel.parent == null;
        if (_panel.parent == null) {
            _ctx.getTopPanel().getPlaceContainer().addOverlay(_panel, PlaceBox.LAYER_TUTORIAL);
            _panel.x = TopPanel.RIGHT_SIDEBAR_WIDTH;
        }

        _panel.setContent(text, icon);

        if (animate) {
            _panel.y = -_panel.height;
            Tweener.addTween(_panel, {y :0, time: 0.6});
        }
    }

    public function test () :void
    {
        var box :Sprite = new Sprite();
        box.graphics.beginFill(0xff0000);
        box.graphics.drawRect(0, 0, 80, 80);
        box.graphics.endFill();
        var test :String = "The quick brown fox jumps over the lazy dog. ";
        popupHint("1... 2... 3... testing 1... 2... 3... " + test + test, box);
    }

    protected var _ctx :MsoyContext;
    protected var _panel :TutorialPanel;
}

}
