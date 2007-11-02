//
// $Id$

package tutorial {

public class Quest
{
    public var questId :String;
    public var trigger :String;
    public var status :String;
    public var outro :String;
    public var summary :String;
    public var payout :int;

    public function Quest (questId :String, trigger :String, status :String, 
                           summary :String, outro :String, payout :uint)
    {
        this.questId = questId;
        this.trigger = trigger;
        this.status = status;
        this.summary = summary;
        this.outro = outro;
        this.payout = payout;
    }

    public static function getQuestCount () :uint
    {
        fillQuests();
        return _quests.length;
    }

    public static function getQuest (step :uint) :Quest
    {
        fillQuests();
        return _quests[step];
    }

    protected static function fillQuests () :void
    {
        if (_quests) {
            return;
        }
        _quests = new Array();
	  _quests.push(new Quest(
            "buyAvatar",
            "avatarBought",
            "Buy a new Avatar",
            "<p class='title'>Get a New Avatar</p>" +
            "<p class='summary'><br>Find a new look! There are lots to choose from in the catalog.</p>" +
            "<p class='details'>" +
            "<br><li>Click on <b><i>Catalog -> Avatars</i></b> to check them out.</li><br>" +
            "<li>Pick one you like and buy it.</li><br>" +
            "</p>",
            "Okay! You're ready to switch into your new avatar.",
            0));
        _quests.push(new Quest(
            "wearAvatar",
            "avatarInstalled",
            "Wear your new Avatar",
            "<p class='title'>Wear Your Avatar</p>" +
            "<p class='summary'><br>Show off your extreme makeover.</p>" +
            "<p class='details'>" +
            "<br><li>Choose <b><i>My Stuff -> Avatars</i></b> to see your avatars.</li><br>" +
            "<li>All items bought in Whirled are stored in <b><i>My Stuff</i></b>.</li><br>" +
            "<li>Click the <b>Wear Avatar</b> button to change your avatar.</li><br>" +
            "</p>",
            "Slick! Here's 200 flow for more shopping.",
            200));
	  _quests.push(new Quest(
            "walkAround",
            "playerMoved",
            "Strut Your Stuff",
            "<p class='title'>Strut Your Stuff!</p>" +      
            "<p class='summary'><br>Try walking around your room in your new avatar.</p>" +
            "<p class='details'>" +
            "<br><li>Point and click your mouse where you'd like to walk to.</li><br>" +
            "</p>",
            "Good! Here's 100 flow for more shopping.",
            100));
	  _quests.push(new Quest(
            "talk",
            "playerSpoke",
            "Wear your new Avatar",
            "<p class='title'>Find Your Voice!</p>" +      
            "<p class='summary'><br>Chatting with friends in your room is easy.</p>" +
            "<p class='details'>" +
            "<br><li>Place your cursor in the chat box in the lower left corner of the Whirled toolbar.</li><br>" +
		"<li>Type a message and click <b><i>Send</i></b> to see it displayed in your room.</li><br>" +
		"<li>You can also press Enter on your keyboard to send chat messages.</li><br>" +
            "</p>",
            "Excellent! Here's 100 flow for more shopping.",
            100));
	  _quests.push(new Quest(
            "playGame",
            "gamePlayed",
            "Play a Game!",
            "<p class='title'>Play a Game!</p>" +
            "<p class='summary'><br>Whirled is full of fun games to play that earn you flow. You can play by yourself or with friends.</p>" +
            "<p class='details'>" +
            "<br><li>Click on <b><i>Places -> Whirledwide</i></b> to see the top games in Whirled.</li><br>" +
            "<li>Pick one from the list on the left to see more about it.</li><br>" +
            "<li>Click <b><i>Play!</i></b> to try it.</li><br>" +
            "</p>",
            "Great! Playing games is a fun way to earn flow.",
            0));
        _quests.push(new Quest(
            "buyDecor",
            "decorBought",
            "Buy New Decor",
            "<p class='title'>Shop For Decor</p>" +
            "<p class='summary'>" +
            "<br>The decor is the most fundamental element of your room's appearance. Every other item in your room appears on top of the decor." +
            "</p><p class='details'>" +
            "<br><li>Choose <b><i>Catalog -> Decor</i></b> for a selection of new room settings.</li><br>" +
            "<li>Browse through and buy one you like.</li><br>" +
            "</p>",
            "Fantastic! You now own a piece of decor.",
            0));
        _quests.push(new Quest(
            "installDecor",
            "decorInstalled",
            "Change Your Decor",
            "<p class='title'>Use Your New Decor</p>" +
            "<p class='summary'><br>Let's get your new decor in your room.</p>" +
            "<p class='details'>" +
            "<br><li>Choose <b><i>My Stuff -> Decor</i></b> to see the decor you own.</li><br>" +
            "<li>Apply your new decor by clicking the <b>Add to Room</b> button.</li><br>" +
            "<li>Click the close box to return to your room.</li><br>" +
            "</p>",
            "Congratulations! Here's 200 flow for learning how to change your decor.",
            200));
        _quests.push(new Quest(
            "buyFurni",
            "furniBought",
            "Buy Furniture",
            "<p class='title'>Buy Furniture</p>" +
            "<p class='summary'><br>Furniture adds personality to a room. Let's shop some more.</p>" +
            "<p class='details'>" +
            "<br><li>Choose <b><i>Catalog -> Furniture</i></b> to start shopping.</li><br>" +
            "<li>Click <b><i>Buy!</i></b> when you find something you like.</li><br>" +
            "</p>",
            "You now have furniture to place in your room.",
            0));
        _quests.push(new Quest(
            "installFurni",
            "furniInstalled",
            "Install your furniture",
            "<p class='title'>Add Your Furniture</p>" +
            "<p class='summary'><br>Now let's add your furniture to the room.</p>" +
            "<p class='details'>" +
            "<br><li>Choose <b><i>My Stuff -> Furniture</i></b> to browse your furniture.</li><br>" +
            "<li>Clicking <b>Add to Room</b> will place the item in the center of your room.</li><br>" +
            "<li>The Build Panel will open. Click the 'X' to close it.</li><br>" +
            "</p>",
            "Excellent! You got 300 flow for adding furniture to your room.",
            300));
        _quests.push(new Quest(
            "placeFurni",
            "editorClosed",
            "Place your furniture",
            "<p class='title'>Place Your Furniture</p>" +
            "<p class='summary'><br>The new furniture appears in the middle of the room until you drag it to where you want it to be. </p>" +
            "<p class='details'>" +
            "<br><li>Click the hammer icon on the toolbar to enter Build Mode.</li><br>" +
            "<li>Click and drag your furniture to put it anywhere you want.</li><br>" +
            "<li>Click the <b>Close</b> box on the Build Panel to return to your room.</li><br>" +
            "</p>",
            "Congratulations! Here's 150 flow toward getting more furniture.",
            150));
	  _quests.push(new Quest(
            "editProfile",
            "profileEdited",
            "Edit Your Profile",
            "<p class='title'>Edit Your Profile!</p>" +
            "<p class='summary'><br>Everyone in Whirled has a Profile page to share interests, find friends, show off or just express themselves.</p>" +
            "<p class='details'>" +
            "<br><li>Choose Me -> My Profile to see your Whirled Profile page.</li><br>" +
            "<li>Click <b>Edit</b> and enter your information.</li><br>" +
            "<li>Finally click the <b>Done</b> button.</li><br>" +
            "</p>",
            "Congratulations! Here's 500 flow.",
            500));
	  _quests.push(new Quest(
            "visitMyWhirled",
            "myWhirledVisited",
            "My Whirled",
            "<p class='title'>My Whirled</p>" +
            "<p class='summary'><br>My Whirled is an easy way to see what your friends are doing and join in on the fun.</p>" +
            "<p class='details'>" +
            "<br><li>Click on <b><i>Places -> My Whirled</i></b> to see which friends are online now.</li><br>" +
            "<li>Click a name to go to your friend.</li><br>" +
            "<li>If you have no friends online, click on Whirledwide to see popular spots and meet new people.</li><br>" +
            "</p>",
            "Great! Now you can use My Whirled to keep up with your friends.",
            500));
	  _quests.push(new Quest(
            "findFriends",
            "friendsSought",
            "Invite Your Friends!",
            "<p class='title'>Find Friends!</p>" +
            "<p class='summary'><br>You can search for friends that are already Whirled players.</p>" +
            "<p class='details'>" +
            "<br><li>Click on <b><i>People -> Profiles -> Find People</i></b> and enter your friend's name.</li><br>" +
            "<li>You can also search by their Whirled display name or Email.</li><br>" +
            "<li>Click <b><i>Search</i></b>.</li><br>" +
            "</p>",
            "If your friends aren't on Whirled yet, go ahead and invite them!",
            0));
	  _quests.push(new Quest(
            "inviteFriends",
            "friendInvited",
            "Invite Your Friends!",
            "<p class='title'>Invite Your Friends!</p>" +
            "<p class='summary'><br>Invite your friends to Whirled so you can play games, chat and make cool new rooms. Let's send some invitations!</p>" +
            "<p class='details'>" +
            "<br><li>Click on <b><i>People -> Invitations</i></b> and enter your friends' e-mail addresses.</li><br>" +
            "<li>Add a custom message if you like.</li><br>" +
            "<li>Click <b><i>Send Invites!</i></b>.</li><br>" +
            "</p>",
            "Thanks! You received 500 flow for inviting friends. You'll get an extra bonus for each friend that joins!",
            500));
    }

    protected static var _quests :Array;
}
}