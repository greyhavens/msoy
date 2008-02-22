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
    public var enterPage :String;
    public var summary :String;
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
        questId :String, status :String, enterPage :String, title :String, summary :String,
        details :Array, footer :String, trigger :String) :Quest
    {
        var quest :Quest = new Quest();
        quest.questId = questId;
        quest.status = status;
        quest.enterPage = enterPage;
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
            "introDecor",
            "Introducing Decor",
            null,
            "Introducing Decor",
            "The decor is the background image for your room. Everything in your room " +
            "appears on top of the decor.",
            null,
            "Click [[Onward]] and we'll take you to the Catalog where you can pick out " +
            "your first piece of Decor.",
            NOOP_TRIGGER),

        makeQuest(
            "buyDecor",
            "Shopping for Decor",
            "#shop-9",
            "Buy Decor",
            "Browse the catalog and choose one you like.",
            [ "Click the preview image to see more info about the Decor.", 
              "Click [[Buy]] when you're ready." ],
            "We'll give you your first Decor for free but after that you'll have to pay for 'em!",
            "decorBought"),

        makeQuest(
            "installDecor",
            "Changing Decor",
            null,
            "Use Your New Decor",
            "Great! Click the 'Add to room' button to use your new Decor in your room:<br>" +
            "<img src='/images/tutorial/add_to_room.png'><br>",
            null,
            "You can view your purchased Decor any time in [[My Stuff]] on the [[Me]] page.",
            "decorInstalled"),

        makeQuest(
            "introFurni",
            "Introducing Furniture",
            null,
            "Introducing Furniture",
            "Now let's put some furniture in your room! Furniture adds personality and " +
            "really brings a room together.",
            null,
            "Click [[Onward]] and we'll head back to the Catalog where you can pick out " +
            "a piece of Furniture.",
            NOOP_TRIGGER),

        makeQuest(
            "buyFurni",
            "Shopping for Furniture",
            "#shop-3",
            "Shopping for Furniture",
            "Browse the catalog and choose a piece of Furniture that you like.",
            [ "Click the preview image to see more info about the Furniture.", 
              "Click [[Buy]] when you're ready." ],
            "We'll give you your first piece of Furniture for free but after that you'll " +
            "have to pay for it!",
            "furniBought"),

        makeQuest(
            "installFurni",
            "Installing Furniture",
            null,
            "Add Your Furniture",
            "Great! Click the 'Add to room' button to add that new Furniture to your room:<br>" +
            "<img src='/images/tutorial/add_to_room.png'><br>",
            null,
            "You can view your purchased Furniture any time in [[My Stuff]] on the [[Me]] page.",
            "furniInstalled"),

        makeQuest(
            "placeFurni",
            "Rearranging the Furniture",
            null,
            "Arrange Your Furniture",
            "Click and drag your new furniture to where you want it in your room.",
            [ "You can scale and rotate it with the buttons in the [[Arranging Room]] window." ],
            "When you're done arranging, close the [[Arranging Room]] window.",
            "editorClosed"),
        ];
}
}
