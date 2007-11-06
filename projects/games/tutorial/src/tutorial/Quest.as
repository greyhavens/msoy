//
// $Id$

package tutorial {

public class Quest
{
    public static var log :Log = Log.getLog(Quest);

    public var questId :String;
    public var trigger :String;
    public var status :String;
    public var outro :String;
    public var summary :String;
    public var payout :int;

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

    protected static function makeQuest (questId :String, trigger :String, status :String,
                                         title :String, summary :String, details :Array,
                                         footer :String, outro :String, payout :uint) :Quest
    {
        var quest :Quest = new Quest();
        quest.questId = questId;
        quest.trigger = trigger;
        quest.status = status;
        quest.summary = "<p class='title'>" + title + "</p>" +
            "<p class='summary'>" + summary + "</p><p class='details'><br>";
        for each (var detail :String in details) {
            quest.summary += "<li>" + detail + "</li>";
        }
        if (footer != null && footer.length > 0) {
            quest.summary += "<br><p>" + footer + "</p>";
        }
        quest.summary += "</p>";
        quest.outro = outro;
        quest.payout = payout;
        return quest;
    }

    protected static function em (text :String) :String
    {
        return "<b><i>" + text + "</i></b>";
    }

    protected static var _quests :Array = [
        makeQuest(
            "walkAround",
            "playerMoved",
            "Learning to Walk",
            "Take a Walk Around",
            "In Whirled, everyone has an Avatar which represents them and can walk around. " +
            "Try moving around your room:",
            [ "Move your mouse on the floor and you'll see a little dot.",
              "Click your mouse and you'll walk to the dot." ],
            "",
            "Nice work. Now let's learn how to talk.<br><br>" +
            "Click the " + em("Onward") + " button below...",
            0),

        makeQuest(
            "talk",
            "playerSpoke",
            "Learning to Speak",
            "Find Your Voice!",
            "Chatting with friends in your room is easy:",
            [ "Click in the chat box in the lower left corner of the Whirled toolbar.",
              "Type a message and click " + em("Send") + " or press the " + em("Enter") + " key." ],
            "",
            "Excellent! We'll give you " + em("200 flow") + " for your efforts.<br><br>" +
            "Notice in the upper right of the page, next to your name, it shows you how much " +
            em("flow") + " you have.<br><br>" +
            "Click " + em("Onward") + " and we'll show you how to spend that flow on something fun!",
            200),

        makeQuest(
            "buyAvatar",
            "avatarBought",
            "Shopping for an Avatar",
            "Get a New Avatar",
            "In " + em("Whirled") + ", you can change your avatar as easily as you can change " +
            "your mind. Let's go shopping and pick out a new one:",
            [ "Click on " + em("Catalog -> Avatars") + " at the top of the page.",
              "Pick one you like and click on it.",
              "Press the " + em("Buy") + " button below the avatar image to buy it." ],
            "",
            "Okay! You're ready to switch into your new avatar.<br><br>" +
            "Click anywhere in this window to return to the " + em("Whirled") +
            " and then click " + em("Onward") + ".",
            0),

        makeQuest(
            "wearAvatar",
            "avatarInstalled",
            "Wearing an Avatar",
            "Wear Your Avatar",
            "Now that you own a new avatar, you're going to want to wear it. Here's how:",
            [ "Choose " + em("My Stuff -> Avatars") + " to see your avatars.",
              "Click the " + em("Wear Avatar") + " button to change your avatar.",
              "All items bought in Whirled are stored in " + em("My Stuff") + "." ],
            "",
            "Now you're looking mighty fine!<br><br>" +
            "We've given you another " + em("200 flow") + " to do some more shopping.<br><br>" +
            "But don't run off just yet! Let's learn how to " + em("find our friends") + ".",
            200),

        makeQuest(
            "findFriends",
            "friendsSought",
            "Finding Friends",
            "Find Your Friends",
            "You can easily search for friends that are already " + em("Whirled") + " players. " +
            "Here's how:",
            [ "Click on " + em("People -> Profiles -> Find People") + ".", 
              "You can search by their " + em("real name") + ", " + em("Whirled name") + " or " +
              em("Email address") + ".",
              "Enter your friend's name and click " + em("Search") + "." ],
            "",
            "If your friends aren't on Whirled yet, you can " + em("invite them!") + "<br><br>" +
            "Click " + em("Onward") + " and we'll show you how.",
            0),

        makeQuest(
            "inviteFriends",
            "friendInvited",
            "Inviting Friends",
            "Invite Your Friends!",
            "Invite your friends to Whirled and you can play games and chat with them. It's easy:",
            [ "Click on " + em("People -> Invitations") + ".",
              "Enter your friends' e-mail addresses.",
              "Add a custom message if you like.", 
              "Click " + em("Send Invites") + "." ],
            "If you don't want to send invites right now, don't worry. Just click " + em("Skip") +
            " and you can send invites later.",
            "Swell! Here's an extra " + em("500 flow") + " for inviting your friends.<br><br>" +
            "Next we'll show you how to find out whether your friends are online.",
            500),

        makeQuest(
            "visitMyWhirled",
            "willUnminimize",
            "Using My Whirled",
            "My Whirled",
            "My Whirled is an easy way to see what your friends are doing and join in on the fun.",
            [ "Click on " + em("Places -> My Whirled") + " or the logo in the upper left to see " +
              "which of your friends are online now.", 
              "Click any friend's name to " + em("go to where they are") + ".", 
              "If you have no friends online, click on " + em("Whirledwide") + " to find " +
              "popular spots and meet new people." ],
            "When you're ready, close " + em("My Whirled") + " by clicking back in this window.",
            "Great! Now you can use " + em("My Whirled") + " to keep up with your friends.",
            500),

        makeQuest(
            "playGame",
            "gamePlayed",
            "Playing a Game",
            "Play a Game!",
            "Whirled is full of fun games to play that earn you flow. " +
            "You can play by yourself or with friends.",
            [ "Click on " + em("Places -> Whirledwide") + " to see the top games in Whirled.", 
              "Pick one from the list on the left to see more about it.", 
              "Click " + em("Play!") + " to try it." ],
            "When you're done come back home using " + em("Me -> My Home") + ".",
            "You're back! Playing games is a fun way to earn flow. You can also " +
            em("earn Trophies") + " and get onto " + em("Top Ranked lists") + ".<br><br>" +
            "But now let's get back to some home improvement.",
            0),

        makeQuest(
            "buyDecor",
            "decorBought",
            "Shopping for Decor",
            "Shop For Decor",
            "The decor is the most fundamental element of your room's appearance. " +
            "Every other item in your room appears on top of the decor.",
            [ "Choose " + em("Catalog -> Decor") + " for a selection of new room settings.", 
              "Browse through and buy one you like." ],
            "",
            "Fantastic! You now own a piece of decor. Next we'll show you how to use that " +
            em("Decor") + " in your room.",
            0),

        makeQuest(
            "installDecor",
            "decorInstalled",
            "Changing Decor",
            "Use Your New Decor",
            "Let's get your new decor in your room.",
            [ "Choose " + em("My Stuff -> Decor") + " to see the decor you own.", 
              "Apply your new decor by clicking the <b>Add to Room</b> button.", 
              "Click the close box to return to your room." ],
            "",
            "Congratulations! Here's 200 flow for learning how to change your decor.",
            200),

        makeQuest(
            "buyFurni",
            "furniBought",
            "Shopping for Furniture",
            "Buy Furniture",
            "Furniture adds personality to a room. Let's shop some more.",
            [ "Choose " + em("Catalog -> Furniture") + " to start shopping.", 
              "Click " + em("Buy!") + " when you find something you like." ],
            "",
            "You now have furniture to place in your room.",
            0),

        makeQuest(
            "installFurni",
            "furniInstalled",
            "Installing Furniture",
            "Add Your Furniture",
            "Now let's add your furniture to the room.",
            [ "Choose " + em("My Stuff -> Furniture") + " to browse your furniture.", 
              "Clicking <b>Add to Room</b> will place the item in the center of your room.", 
              "The Build Panel will open. Click the 'X' to close it." ],
            "",
            "Excellent! You got 300 flow for adding furniture to your room.",
            300),

        makeQuest(
            "placeFurni",
            "editorClosed",
            "Rearranging the Furniture",
            "Place Your Furniture",
            "The new furniture appears in the middle of the room until you drag it to where " +
            "you want it to be. ",
            [ "Click the hammer icon on the toolbar to enter Build Mode.", 
              "Click and drag your furniture to put it anywhere you want.", 
              "Click the <b>Close</b> box on the Build Panel to return to your room." ],
            "",
            "Congratulations! Here's 150 flow toward getting more furniture.",
            150),

        makeQuest(
            "editProfile",
            "profileEdited",
            "Editing Your Profile",
            "Edit Your Profile!",
            "Everyone in Whirled has a Profile page to share interests, find friends, show off " +
            "or just express themselves.",
            [ "Choose Me -> My Profile to see your Whirled Profile page.", 
              "Click <b>Edit</b> and enter your information.", 
              "Finally click the <b>Done</b> button.", ],
            "",
            "Congratulations! Here's 500 flow.",
            500),
        ];
}
}
