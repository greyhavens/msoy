//
// $Id$

package tutorial {

import flash.display.Sprite;
import flash.events.Event;

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
        _control.addEventListener(AVRGameControlEvent.SIZE_CHANGED, handleSizeChanged);
        _control.state.addEventListener(AVRGameControlEvent.MESSAGE_RECEIVED, messageReceived);
        _control.quests.addEventListener(AVRGameControlEvent.QUEST_STATE_CHANGED, questStateChanged);

        // look up our active quest
        var questId :String = (_control.state.getPlayerProperty(PROP_TUTORIAL_STEP) as String);
        _activeQuest = Quest.getQuest(questId);

        // create but do not initialize the view
        _view = new View(this);
    }

    public function viewIsReady () :void
    {
        _view.gotoSwirlState(_activeQuest == null ? View.SWIRL_INTRO : View.SWIRL_DEMURE);

        // if we have a pending tutorial completion, do it now that we're ready to display
        if (_pendingCompletion) {
            _pendingCompletion = false;
            displayQuestCompletion();
        }
    }

    public function swirlClicked (swirlState :int) :void
    {
        log.debug("swirlClicked [state=" + swirlState + "]");

        if (_view.isBoxShowing()) {
            _view.displayNothing();

        } else if (_activeQuest == null) {
            displayIntro();

        } else {
            _view.displaySummary(_activeQuest.summary);
        }
    }

    public function skipQuest () :void
    {
        if (_activeQuest == null) {
            log.warning("Eek, no active quest in skipQuest");
            return;
        }
        _control.quests.cancelQuest(_activeQuest.questId);

        var nquest :Quest = Quest.getNextQuest(_activeQuest.questId);
        if (nquest != null) {
            startQuest(nquest);
        } else {
            displayTutorialComplete();
        }
    }

    protected function complete (event :Event) :void
    {
        root.loaderInfo.addEventListener(Event.UNLOAD, handleUnload);
        addChild(_view);
        _view.init();
        // the view will call us back when it's ready
    }

    protected function handleSizeChanged (event :Event) :void
    {
        _view.sizeChanged();
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
        log.debug("Tutorial event [name=" + event.value + ", active=" + _activeQuest + "]");

        // if we have no active quest or this isn't our trigger action, ignore it
        if (_activeQuest == null || event.value != _activeQuest.trigger) {
            return;
        }

        // we may end up here before our view is ready because when we finish playing a game the
        // tutorial is started back up and immediately sent a gamePlayed event
        if (_view.isReady()) {
            displayQuestCompletion();
        } else {
            _pendingCompletion = true;
        }
    }

    protected function questStateChanged (event :AVRGameControlEvent) :void
    {
        log.debug("Quest state changed [id=" + event.name + ", value=" + event.value + "]");

        // if we've acquired a new quest...
        if (event.value) {
            // ...note the quest; display it's summary and smallen our swirl
            _activeQuest = Quest.getQuest(event.name);
            if (_activeQuest == null) {
                log.warning("Unknown quest started? [id=" + event.name + "].");
            } else {
                _view.displaySummary(_activeQuest.summary);
                _view.gotoSwirlState(View.SWIRL_DEMURE);
            }
        }
    }

    protected function displayIntro () :void
    {
        _view.displayMessage(
            "Let's Go!",
            "<p class='title'>Whirled Tour</p><br>" +
            "<p class='message'>This tour will help you get a feel for <b><i>Whirled</i></b> in just a few simple steps.</p><br>" +
            "<p class='message'>You'll learn how to <b>customize</b> your room, <b>buy</b> a new avatar, <b>play</b> games, and <b>connect</b> with friends.</p><br>" +
            "<p class='message'>It's also a quick way to earn some easy <i>flow</i>, the local currency.</p>",
            function () :void {
                _view.displayNothing();
                startQuest(Quest.getFirstQuest());
            });
        _view.gotoSwirlState(View.SWIRL_DEMURE);
    }

    protected function startQuest (quest :Quest) :void
    {
        log.info("Starting quest [id=" + quest + "].");
        _control.state.setPlayerProperty(PROP_TUTORIAL_STEP, quest.questId, true);
        _control.quests.offerQuest(quest.questId, null, quest.status);
    }

    protected function displayQuestCompletion () :void
    {
        log.info("Marking step complete " + _activeQuest + ".");
        _view.displayNothing();
        _control.quests.completeQuest(_activeQuest.questId, null, _activeQuest.payout);
        var nquest :Quest = Quest.getNextQuest(_activeQuest.questId);

        // now move on to the next quest or the end of the tutorial if we have no more quests
        if (nquest != null) {
            _view.displayMessage("Onward", "<p class='message'>" + _activeQuest.outro + "</p>",
                                 function () :void {
                                     startQuest(nquest);
                                 });
        } else {
            displayTutorialComplete();
        }

        // clear out our active quest so that we don't retrigger its completion
        _activeQuest = null;
    }

    protected function displayTutorialComplete () :void
    {
        _view.displayMessage(
            "Finish",
            "<p class='title'>Farewell</p><br>" +
            "<p class='message'><br>This is the end of the tutorial, and you are ready to " +
            "step into the world.</p>",
            function () :void {
                _control.deactivateGame();
            });
    }

    protected var _activeQuest :Quest;
    protected var _control :AVRGameControl;
    protected var _view :View;
    protected var _pendingCompletion :Boolean;

    protected static const PROP_TUTORIAL_STEP :String = "tutorialStep";
}
}
