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
    public var skippable :Boolean;

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
        questId :String, trigger :String, status :String, title :String, summary :String,
        details :Array, footer :String, outro :String, payout :uint, skippable :Boolean) :Quest
    {
        var quest :Quest = new Quest();
        quest.questId = questId;
        quest.trigger = trigger;
        quest.status = status;
        quest.summary = "<p class='title'>" + title + "</p><p class='shim'>&nbsp;</p>" +
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
        quest.skippable = skippable;
        return quest;
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
            null,
            0, false),

        makeQuest(
            "talk",
            "playerSpoke",
            "Learning to Speak",
            "Find Your Voice!",
            "Nice work! Now let's learn how to talk.<br><br>" +
            "Chatting with friends in your room is easy:",
            [ "Click in the chat box in the lower left corner of the [[Whirled]] toolbar.",
              "Type a message and click [[Send]] or press the [[Enter]] key." ],
            "",
            "Excellent! We'll give you [[200 flow]] for your efforts.<br><br>" +
            "Notice in the upper right of the page, next to your name, it shows you how much " +
            "[[flow]] you have.<br><br>" +
            "Click [[Onward]] and we'll show you how to spend that flow on " +
            "something fun!",
            200, false),

        makeQuest(
            "buyAvatar",
            "avatarBought",
            "Shopping for an Avatar",
            "Get a New Avatar",
            "In Whirled, you can change your avatar as easily as you can change " +
            "your mind. Let's go shopping and pick out a new one:",
            [ "Click on [[Catalog -> Avatars]] at the top of the page.",
              "Pick one you like and [[click on it]].",
              "Press the [[Buy]] button below the avatar image to buy it." ],
            "",
            null,
            0, false),

        makeQuest(
            "wearAvatar",
            "avatarInstalled",
            "Wearing an Avatar",
            "Wear Your Avatar",
            "Now that you own a new avatar, you're going to want to wear it. Here's how:",
            [ "All items bought in [[Whirled]] are stored in [[My Stuff]].",
              "Choose [[My Stuff -> Avatars]] to see your avatars.",
              "Click the [[Wear avatar]] button next to your new avatar to wear it." ],
            "",
            "Now you're looking mighty fine! Try walking around in your new duds and strut " +
            "your stuff.<br><br>" +
            "We've given you another [[200 flow]] to do some more shopping.<br><br>" +
            "But don't run off just yet! Let's learn how to [[find our friends]].",
            200, false),

        makeQuest(
            "findFriends",
            "willUnminimize",
            "Finding Friends",
            "Find Your Friends",
            "You can easily search for friends that are already Whirled players. Here's how:",
            [ "Click on [[People -> Profiles -> Find People]].", 
              "You can search by their [[real name]], [[Whirled name]] or [[Email address]].",
              "Enter your friend's name and click [[Search]]." ],
            "If you find someone you know, [[click their name]] then click [[Invite To Be " +
            "Your Friend]]. If you don't find anyone you know, don't worry! We'll show you " +
            "how to invite your friends to [[Whirled]] in the next step.<br><br>" + 
            "When you're ready to continue, click back in this window.",
            null,
            0, false),

        makeQuest(
            "inviteFriends",
            "friendInvited",
            "Inviting Friends",
            "Invite Your Friends",
            "If your friends aren't on Whirled yet, you can invite them! It's easy:",
            [ "Click on [[People -> Invitations]].",
              "Enter your friends' e-mail addresses.",
              "Add a custom message if you like.", 
              "Click [[Send Invites]]." ],
            "If you don't want to send invites right now, don't worry. Just click [[Skip]] and " +
            "you can send invites later.",
            "Swell! Here's an extra [[500 flow]] for inviting your friends.<br><br>" +
            "Next we'll show you how to find out whether your friends are online.",
            500, true),

        makeQuest(
            "visitMyWhirled",
            "willUnminimize",
            "Using My Whirled",
            "My Whirled",
            "My Whirled is an easy way to see what your friends are doing and join in on the fun.",
            [ "Click on [[Places -> My Whirled]] or the [[logo]] in the upper left to see " +
              "which of your friends are online now.", 
              "Click any friend's name to [[go to where they are]].", 
              "If you have no friends online, click [[Whirledwide]] to find popular spots and " +
              "meet new people." ],
            "When you're ready, close [[My Whirled]] by clicking back in this window.",
            null,
            0, false),

        makeQuest(
            "playGame",
            "gamePlayed",
            "Playing a Game",
            "Play a Game!",
            "Whirled is full of fun games to play that earn you flow. " +
            "You can play solo or with friends.",
            [ "Click on [[Places -> Whirledwide]] to see the most popular games.", 
              "Pick a game from [[Top Games]] and [[click on it]] to read about it.", 
              "Click [[Play!]] to try it." ],
            "When you're done come back home using [[Me -> My Home]]. If you don't feel like " +
            "playing a game right now, just click [[Skip]] and we'll move on.",
            "You're back! Playing games is a fun way to earn flow. You can also " +
            "[[earn Trophies]] and get onto [[Top Ranked lists]].<br><br>" +
            "But now let's get back to some home improvement.",
            0, true),

        makeQuest(
            "buyDecor",
            "decorBought",
            "Shopping for Decor",
            "Shop For Decor",
            "The decor is the background image for your room. Everything in your room " +
            "appears on top of the decor.",
            [ "Choose [[Catalog -> Decor]] for a selection of new room settings.", 
              "Browse through and buy one you like." ],
            "",
            null,
            0, false),

        makeQuest(
            "installDecor",
            "decorInstalled",
            "Changing Decor",
            "Use Your New Decor",
            "Great! Let's get that new Decor into your room.",
            [ "Choose [[My Stuff -> Decor]] to see the decor you own.", 
              "Apply your new decor by clicking the [[Add to room]] button." ],
            "",
            "Nice work! Here's another [[200 flow]] for future shopping adventures.<br><br>" +
            "Click [[Onward]] and we'll pick up a little something to go with your new decor.",
            200, false),

        makeQuest(
            "buyFurni",
            "furniBought",
            "Shopping for Furniture",
            "Buy Furniture",
            "Furniture adds personality to a room. You might say it really brings a room " +
            "together. Let's head to the Catalog and pick some up:",
            [ "Choose [[Catalog -> Furniture]] to see what's available.", 
              "Click [[Buy!]] when you find something you like." ],
            "",
            null,
            0, false),

        makeQuest(
            "installFurni",
            "furniInstalled",
            "Installing Furniture",
            "Add Your Furniture",
            "Excellent! Now let's add that new Furniture to your room.",
            [ "Choose [[My Stuff -> Furniture]] to browse your furniture.", 
              "Clicking [[Add to Room]] will place the item in the center of your room." ],
            "",
            "Excellent! Here's another [[300 flow]] to pick up more furniture later.<br><br>" +
            "But before you do that, let's learn how to [[rearrange]] our furniture.",
            300, false),

        makeQuest(
            "placeFurni",
            "editorClosed",
            "Rearranging the Furniture",
            "Place Your Furniture",
            "The new furniture appears in the middle of the room until you drag it to where " +
            "you want it to be. ",
            [ "Click the hammer icon on the toolbar to enter [[Arranging Room Mode]].", 
              "[[Click and drag]] your furniture to put it anywhere you want.", 
              "Click the [[Close]] box on the [[Arranging Room]] to return to your room." ],
            "",
            "You're a born interior decorator! Here's another [[200 flow]] to celebrate your " +
            "newfound skills.<br><br>" +
            "We're almost done, but we've got one more thing to show you before we turn you " +
            "loose in the [[Whirled]].",
            200, false),

        makeQuest(
            "editProfile",
            "profileEdited",
            "Editing Your Profile",
            "Edit Your Profile!",
            "Everyone in Whirled has a Profile page to share interests, find friends, show off " +
            "or just express themselves.",
            [ "Choose [[Me -> My Profile]] to see your [[Whirled Profile]] page.", 
              "Click [[Edit]] and enter your information.", 
              "Finally click the [[Done]] button.", ],
            "",
            "Awesome! Now your friends will know it's you if they see your profile.<br><br>" +
            "Plus you get [[500 flow]] for editing your profile for the first time.",
            0, false),
        ];
}
}
