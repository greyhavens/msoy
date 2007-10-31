//
// $Id$

package tutorial {

import flash.display.*;
import flash.text.*;
import flash.geom.*;
import flash.events.*;
import flash.filters.*;
import flash.net.*;
import flash.ui.*;
import flash.utils.*;

import com.threerings.util.EmbeddedSwfLoader;
import com.threerings.flash.SimpleTextButton;

public class View extends Sprite
{
    public static const SWIRL_NONE :int = 1;
    public static const SWIRL_DEMURE :int = 2;
    public static const SWIRL_INTRO :int = 3;
    public static const SWIRL_BOUNCY :int = 4;

    public function View (tutorial :Tutorial)
    {
        _tutorial = tutorial;

        var swirlBytes :ByteArray = ByteArray(new Content.SWIRL());

        _textBox = new TextBox(this, swirlBytes, maybeFinishUI);
        _swirl = new Swirl(this, swirlBytes, maybeFinishUI);
    }

    public function init () :void
    {
        maybeFinishUI();
    }

    public function unload () :void
    {
        _swirl.unload();
        _textBox.unload();
    }

    public function gotoSwirlState (state :int) :void
    {
        _swirl.gotoState(state);
    }

    public function isBoxShowing () :Boolean
    {
        return _boxShowing;
    }

    public function displayNothing () :void
    {
        _textBox.hideBox();
        _boxShowing = false;
    }

    public function displaySummary (summary :String) :void
    {
        log.debug("displaySummary [summary=" + summary + "]");
        if (!summary) {
            displayNothing();
            return;
        }
        _textBox.showBox(summary);
        _boxShowing = true;

        _textBox.addButton("Hide", true, function () :void {
            displayNothing();
        });

        _textBox.addButton("Skip", false, function () :void {
            displayNothing();
            _tutorial.skipQuest();
        });
    }

    public function displayMessage (button :String, message :String, pressed :Function) :void
    {
        _textBox.showBox(message);
        _textBox.addButton(button, true, pressed);
        _boxShowing = true;
    }

    public function swirlClicked (state :int) :void
    {
        // when the swirly is big, clicking it offers the first quest
        _tutorial.swirlClicked(state);
    }

    public function sizeChanged () :void
    {
        _textBox.sizeChanged();
    }

    protected function maybeFinishUI () :void
    {
        // if all initializations are complete, actually add the bits
        if (this.parent && _swirl.isReady() && _textBox.isReady()) {
            this.addChild(_swirl);
            this.addChild(_textBox);

            _tutorial.viewIsReady();
        }
    }

    protected var _tutorial :Tutorial;

    protected var _swirl :Swirl;

    protected var _textBox :TextBox;
    protected var _boxShowing :Boolean;

    protected static const log :Log = Log.getLog(View);

}
}
