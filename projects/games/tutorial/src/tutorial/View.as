//
// $Id$

package tutorial {

import flash.display.Sprite;
import flash.utils.ByteArray;

import com.threerings.util.Log;

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

    public function isReady () :Boolean
    {
        return (this.parent && _swirl.isReady() && _textBox.isReady());
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

    public function displayQuest (quest :Quest) :void
    {
        log.debug("displayQuest [quest=" + quest + "]");
        if (!quest) {
            displayNothing();
            return;
        }

        _textBox.showBox(quest.summary, true);
        _boxShowing = true;

        _textBox.addButton("Hide", true, function () :void {
            displayNothing();
        });

        if (quest.skippable) {
            _textBox.addButton("Skip", false, function () :void {
                    displayNothing();
                    _tutorial.skipQuest();
                });
        }
    }

    public function displayMessage (button :String, message :String, pressed :Function) :void
    {
        _textBox.showBox(message, false);
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
        if (isReady()) {
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
