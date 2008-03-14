//
// $Id$

package tutorial {

import com.threerings.util.Log;

public class Quest
{
    public static var log :Log = Log.getLog(Quest);

    public static const NOOP_TRIGGER :String = "noop";

    public var questId :String;
    public var status :String;
    public var summary :String;
    public var reminderLabel :String;
    public var reminderPage :String;
    public var trigger :String;

    public static function getFirstQuest () :Quest
    {
        return _quests[0];
    }

    public static function getQuest (questId :String) :Quest
    {
        for each (var quest :Quest in _quests) {
            if (quest.questId == questId) {
                return quest;
            }
        }
        log.warning("Requested invalid quest " + questId + ".");
        return null;
    }

    public static function getNextQuest (questId :String) :Quest
    {
        for (var ii :int = 0; ii < _quests.length; ii++) {
            var quest :Quest = (_quests[ii] as Quest);
            if (quest.questId == questId) {
                if (ii >= _quests.length-1) {
                    return null;
                } else {
                    return (_quests[ii+1] as Quest);
                }
            }
        }
        return null;
    }

    public function toString () :String
    {
        return questId + " (" + trigger + ")";
    }

    protected static function makeQuest (
        questId :String, status :String, title :String, summary :String, details :Array,
        footer :String, reminderLabel :String, reminderPage :String, trigger :String) :Quest
    {
        var quest :Quest = new Quest();
        quest.questId = questId;
        quest.status = status;
        quest.reminderLabel = reminderLabel;
        quest.reminderPage = reminderPage;
        quest.summary = "<p class='title'>" + title + "</p>" +
            "<p class='message'>" + summary + "</p><br>";
        if (details != null) {
            quest.summary += "<p class='details'>";
            for each (var detail :String in details) {
                quest.summary += "<li>" + detail + "</li>";
            }
            quest.summary += "</p>";
        }
        if (footer != null && footer.length > 0) {
            quest.summary += "<p class='message'>" + footer + "</p>";
        }
        quest.trigger = trigger;
        return quest;
    }

    protected static var _quests :Array = [
        makeQuest(
            "placeFurni",
            "Rearranging the Furniture",
            "Arrange Your Furniture",
            "First let's learn how to rearrange the furniture.<br>" +
            "1. Click the [[Edit room]] button below:<br>" +
            "<img src='/images/tutorial/room_edit.png'><br><br><br>" +
            "2. Click and drag any of the furniture in your room to move it.",
            null,
            "When you're done arranging, close the [[Arranging Room]] window.",
            null,
            null,
            "editorClosed"),

        makeQuest(
            "buyFurni",
            "Shopping for Furniture",
            "Shopping for Furniture",
            "Great! Now let's buy some new furniture that reflects your tastes.",
            [ "Click [[Show Catalog]] below to open the catalog.",
              "Click on any Furniture to see it up close.", 
              "Click [[Buy]] when you're ready." ],
            "If you don't have enough money, take a break and play a game to earn some!",
            "Show Catalog",
            "#shop-3",
            "furniBought"),

        makeQuest(
            "installFurni",
            "Installing Furniture",
            "Add Your Furniture",
            "Great! Click the [[Add to room]] button to add that new Furniture to your room.",
            [ "Click [[Show My Stuff]] below to open your stuff.",
              "Click the [[Add to room]] button to put your Furniture in your room." ],
            "You can get to your Furniture any time from [[My Stuff]] on the [[Me]] page.",
            "Show My Stuff",
            "#stuff-3",
            "furniInstalled"),

        makeQuest(
            "postFurni",
            "More Rearranging",
            "More Rearranging",
            "When you add a new piece of furniture to your room, you automatically enter " +
            "[[Arranging Room]] mode so that you can put it right where you want it.",
            [ "Click and drag your new furniture to position it.",
              "Close the [[Arranging Room]] window when you're done." ],
            "Click [[Onward]] and we'll learn about [[Pets]].",
            null,
            null,
            NOOP_TRIGGER),

        makeQuest(
            "buyPets",
            "Shopping for Pets",
            "Buy a Pet",
            "Pets are friendly little fellers that can hang out in your room.",
            [ "Click [[Show Catalog]] below to open the catalog.",
              "Click on any Pet to read more about them.", 
              "Click [[Buy]] when you find one you like." ],
            "If you don't have enough money, take a break and play a game to earn some!",
            "Show Catalog",
            "#shop-6",
            "petBought"),

        makeQuest(
            "installPet",
            "Calling Your Pet",
            "Calling Your Pet",
            "Great! Click the [[Add to room]] button to call your Pet into your room.",
            [ "Click [[Me]] then [[Pets]] in the [[My Stuff]] setion.",
              "Click the [[Add to room]] button to tell your Pet to join you in your room." ],
            "Click on your Pet to see what sort of things you can do with them. " +
            "When you're ready, click [[Onward]] below.",
            null,
            null,
            NOOP_TRIGGER),
        ];
}
}
