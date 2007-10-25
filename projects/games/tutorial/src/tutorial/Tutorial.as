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

import com.whirled.AVRGameControl;
import com.whirled.AVRGameControlEvent;

[SWF(width="640", height="400")]
public class Tutorial extends Sprite
{
    public static var log :Log = Log.getLog(Tutorial);

    public function Tutorial ()
    {
        root.loaderInfo.addEventListener(Event.COMPLETE, complete);

        // immediately set up the control and listen for all relevant events
        _control = new AVRGameControl(this);
        _control.addEventListener(
            AVRGameControlEvent.PLAYER_PROPERTY_CHANGED, playerPropertyChanged);
        _control.addEventListener(
            AVRGameControlEvent.MESSAGE_RECEIVED, messageReceived);
        _control.addEventListener(
            AVRGameControlEvent.QUEST_STATE_CHANGED, questStateChanged);

        // create but do not initialize the view
        _view = new View(this);
    }

    public function skipQuest () :void
    {
        if (_activeQuest == null) {
            log.warning("Eek, no active quest in skipQuest [step=" + getStep() + "]");
            return;
        }
        _control.cancelQuest(_activeQuest);
    }

    public function swirlClicked (swirlState :int) :void
    {
        var step :int = getStep();
        var quest :Quest = Quest.getQuest(step);

        if (testCompletedStep(step)) {
            _control.completeQuest(quest.questId, quest.outro, quest.payout);
            return;
        }
        if (_activeQuest) {
            _view.displaySummary(_view.isShowingSummary() ? null : quest.summary);
            return;
        }
        if (step == 0) {
            _view.displayMessage(
                "Let's Go!",
                "<p class='summary'>Follow these steps to get a feel for Whirled and earn some easy flow, the currency in Whirled.</p><br>",
                function () :void {
                    _control.offerQuest(quest.questId, null, quest.status);
                });
            return;
        }
        log.warning("Eek, swirly clicked without active quest [swirlState=" + swirlState +
                    ", step=" + getStep() + "]");
    }

    public function viewIsReady () :void
    {
        _viewSetup = true;
        initialize();
    }

    protected function complete (event :Event) :void
    {
        root.loaderInfo.addEventListener(Event.UNLOAD, handleUnload);

        // then initialize the actual view
        _view.init(_control.getStageBounds(), _control.getRoomBounds());
        addChild(_view);

        // now surrender control until we find out whether or not we're minimized
    }

    protected function handleUnload (event :Event) :void
    {
        _view.unload();
    }

    protected function messageReceived (event :AVRGameControlEvent) :void
    {
        // the only message we expect to receive is the special tutorialEvent one
        if (event.name != "tutorialEvent") {
            return;
        }

        var step :int = getStep();

//        log.debug("Tutorial event [name=" + event.value + ", step=" + step + "]");

        if (event.value == "willMinimize" ||event.value == "willUnminimize") {
            // we don't really care anymore; just start things up
            initialize();

        } else if (event.value == Quest.getQuest(step).trigger) {
            _control.setPlayerProperty(PROP_STEP_COMPLETED, step, true);

        } else {
            log.warning("Unknown tutorial event: " + event.value);
        }
    }

    protected function playerPropertyChanged (event: AVRGameControlEvent) :void
    {
        // log.debug("property changed: " + event.name + "=" + event.value);
        if (event.name == PROP_STEP_COMPLETED) {
            initialize();

        } else if (event.name == PROP_TUTORIAL_STEP) {
            // we have arrived at a new tutorial step; reinitialize our state
            initialize();
        }
    }

    protected function questStateChanged (event :AVRGameControlEvent) :void
    {
//        log.debug("quest state changed [id=" + event.name + ", value=" + event.value + "]");
        // we've acquired a new active quest or dropped an old one; update our display

        var step :int = getStep();
        var quest :Quest = Quest.getQuest(step);
        if (event.value) {
            // accepting a new quest triggers the summary box
            _view.displaySummary(quest.summary);
            // we'll let initialize() set _activeQuest and figure out swirl state
            initialize();
            return;
        }
        // else the player dropped the quest or clicked the completion popup's "OK" button
        if (event.name == quest.questId) {
            // either way we move on to the next
            _activeQuest = null;
            _control.setPlayerProperty(PROP_STEP_COMPLETED, null, true);
            _control.setPlayerProperty(PROP_TUTORIAL_STEP, getStep() + 1, true);
            return;
        }
        log.warning("Deactivation of unexpected quest [questId=" + event.name +
                    ", current=" + quest.questId + "]");
        return;
    }

    protected function initialize () :void
    {
        if (_viewSetup == false) {
            return;
        }
        // figure out which quest we ought to be on
        var step :int = getStep();
        if (step >= Quest.getQuestCount()) {
            _view.displayMessage(
                "Finish",
                "<p class='title'>Farewell</p>" +
                "<p class='summary'><br>This is the end of the tutorial, and you are ready to step into the world.</p>",
                function () :void {
                    _control.deactivateGame();
                });
            return;
        }

        var quest :Quest = Quest.getQuest(step);

        // check against our current active quests
        var quests :Array = _control.getActiveQuests();
        for (var ii :int = 0; ii < quests.length; ii ++) {
            var tuple :Array = quests[ii];
            if (tuple[0] == quest.questId) {
                _activeQuest = quest.questId;
                if (testCompletedStep(step)) {
                    _view.gotoSwirlState(View.SWIRL_BOUNCY);
                    _view.displayNothing();
                } else {
                    _view.gotoSwirlState(View.SWIRL_DEMURE);
                }
                return;
            }
        }
        if (step == 0) {
            _view.gotoSwirlState(View.SWIRL_INTRO);
            return;
        }
        // this quest will be automatically accepted, which in turn will trigger a call
        // back here to initialize(), setting activeQuest and swirl state as per above
        _control.offerQuest(quest.questId, null, quest.status);
    }

    protected function getStep () :int
    {
        return int(_control.getPlayerProperty(PROP_TUTORIAL_STEP));
    }

    protected function testCompletedStep (step :int) :Boolean
    {
        var tmp :Object = _control.getPlayerProperty(PROP_STEP_COMPLETED);
        return tmp != null && int(tmp) == step;
    }

    protected var _activeQuest :String;
    protected var _control :AVRGameControl;
    protected var _clientIsMinimized :Boolean = true;
    protected var _view :View;
    protected var _viewSetup :Boolean;

    protected static const PROP_STEP_COMPLETED :String = "stepCompleted";
    protected static const PROP_TUTORIAL_STEP :String = "tutorialStep";
}
}


