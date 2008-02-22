//
// $Id$

package tutorial {

import flash.display.Sprite;
import flash.events.Event;

import com.threerings.util.Log;

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
        if (_activeQuest == null) {
            displayIntro();
        } else {
            _view.displayQuest(_activeQuest);
        }
    }

    public function skipQuest () :void
    {
        if (_activeQuest == null) {
            _view.displayMessage("Eek, no active quest in skipQuest.", null, null);
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

    public function stopTutorial () :void
    {
        // TODO: display chat message encouraging them to restart later
        _control.deactivateGame();
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

        // if this event completes our quest, make that happen
        if (_activeQuest != null && event.value == _activeQuest.trigger) {
            displayQuestCompletion();
        }
    }

    protected function questStateChanged (event :AVRGameControlEvent) :void
    {
        log.debug("Quest state changed [id=" + event.name + ", value=" + event.value + "]");

        // if we've acquired a new quest...
        if (event.value) {
            _activeQuest = Quest.getQuest(event.name);
            if (_activeQuest == null) {
                log.warning("Unknown quest started? [id=" + event.name + "].");
            } else {
                _view.displayQuest(_activeQuest); // ...display it
            }
        }
    }

    protected function displayIntro () :void
    {
        _view.displayMessage(
            "<p class='title'>Decorate Your Room!</p>" +
            "<p class='message'>We're going to show you how to decorate your room in just a " +
            "few simple steps.</p><br>" +
            "<p class='message'>You'll learn about [[Decor]] and [[Furniture]] in " +
            "this quick intro and personalize your room in the process!</p>",
            "Let's Go!",
            function () :void {
                _view.displayNothing();
                startQuest(Quest.getFirstQuest());
            },
            "No thanks!");
    }

    protected function startQuest (quest :Quest) :void
    {
        // if this step has an entry page, don't save it as our current step; if we stop the
        // tutorial now, we'll start back at the previous step
        if (quest.enterPage == null) {
            _control.state.setPlayerProperty(PROP_TUTORIAL_STEP, quest.questId, true);
            _control.quests.offerQuest(quest.questId, null, quest.status);
        } else {
            _view.displayQuest(_activeQuest = quest);
        }
    }

    protected function displayQuestCompletion () :void
    {
        log.info("Marking step complete " + _activeQuest + ".");
        _view.displayNothing();
        _control.quests.completeQuest(_activeQuest.questId, null, 0);
        var nquest :Quest = Quest.getNextQuest(_activeQuest.questId);

        // now move on to the next quest...
        if (nquest != null) {
            startQuest(nquest);

        // ...or the end of the tutorial if we have no more quests
        } else {
            displayTutorialComplete();
        }

        // clear out our active quest so that we don't retrigger its completion
        _activeQuest = null;
    }

    protected function displayTutorialComplete () :void
    {
        _control.state.setPlayerProperty(PROP_TUTORIAL_STEP, "done", true);
        _view.displayMessage(
            "<p class='message'>That's all there is to it! You're now an expert in " +
            "interior decoration.<br><br>" +
            "Click the [[Shop]] button above to take a look at all the great stuff " +
            "you can get for your room.</p>",
            "Close",
            function () :void {
                _control.deactivateGame();
            }, null);
    }

    protected var _activeQuest :Quest;
    protected var _control :AVRGameControl;
    protected var _view :View;

    protected static const PROP_TUTORIAL_STEP :String = "tutorialStep";
}
}
