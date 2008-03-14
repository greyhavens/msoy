//
// $Id$

package tutorial {

import flash.display.Sprite;
import flash.utils.ByteArray;

import com.threerings.util.Log;
import com.threerings.util.NetUtil;

public class View extends Sprite
{
    public function View (tutorial :Tutorial)
    {
        _tutorial = tutorial;

        var swirlBytes :ByteArray = ByteArray(new Content.SWIRL());
        _textBox = new TextBox(swirlBytes, maybeFinishUI);
    }

    public function init () :void
    {
        maybeFinishUI();
    }

    public function isReady () :Boolean
    {
        return (this.parent && _textBox.isReady());
    }

    public function unload () :void
    {
        _textBox.unload();
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

        _textBox.showBox(quest.summary);
        _boxShowing = true;

        _textBox.addButton("Stop Tutorial", false, function () :void {
            _tutorial.stopTutorial();
        });

        // this quest has no trigger, so add a button that skips to the next step
        if (quest.trigger == Quest.NOOP_TRIGGER) {
            _textBox.addButton("Onward", true, function () :void {
                _tutorial.skipQuest();
            });
        } else if (quest.reminderPage != null) {
            _textBox.addButton(quest.reminderLabel, true, function () :void {
                NetUtil.navigateToURL(quest.reminderPage);
            });
        }
    }

    public function displayMessage (message :String, nextButton :String, pressed :Function,
                                    stopButton :String = "Stop Tutorial") :void
    {
        _textBox.showBox(message);
        if (nextButton != null) {
            _textBox.addButton(nextButton, true, pressed);
        }
        if (stopButton != null) {
            _textBox.addButton(stopButton, false, function () :void {
               _tutorial.stopTutorial();
            });
        }
        _boxShowing = true;
    }

    public function sizeChanged () :void
    {
        _textBox.sizeChanged();
    }

    protected function maybeFinishUI () :void
    {
        // if all initializations are complete, actually add the bits
        if (isReady()) {
            this.addChild(_textBox);
            _tutorial.viewIsReady();
        }
    }

    protected var _tutorial :Tutorial;
    protected var _textBox :TextBox;
    protected var _boxShowing :Boolean;

    protected static const log :Log = Log.getLog(View);
}
}
