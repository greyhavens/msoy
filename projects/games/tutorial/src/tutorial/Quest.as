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
            "editProfile",
            "profileEdited",
            "Edit Your Profile",
            "<p class='title'>Edit Your Profile!</p>" +
            "<p class='summary'>Everyone in Whirled has a Profile page to share interests, find friends, show off or just express themselves.</p>" +
            "<p class='details'>" +
            "<br><li>Choose Me -> My Profile to see your Whirled Profile page.</li><br>" +
            "<br><li>Click <b>Edit</b> and enter your information.</li><br>" +
            "<br><li>Finally click the <b>Done</b> button.</li>" +
            "</p>",
            "Congratulations! You updated your profile and received 500 flow.",
            500));
        _quests.push(new Quest(
            "buyDecor",
            "decorBought",
            "Buy New Decor",
            "<p class='title'>Change Your Decor</p>" +
            "<p class='summary'>" +
            "<br>The decor is the most fundamental element of your room's appearance. Every other item in your room appears on top of the decor." +
            "</p><p class='details'>" +
            "<br><li>Choose <b><i>Catalog -> Decor</i></b> for a selection of new room settings.</li><br>" +
            "<br><li>Browse through and buy one you like.</li>" +
            "</p>",
            "Fantastic! You now own a piece of decor.",
            0));
        _quests.push(new Quest(
            "installDecor",
            "decorInstalled",
            "Change Your Decor",
            "<p class='title'>Install the new Decor.</p>" +
            "<p class='summary'>Let's get your new decor in your room.</p>" +
            "<p class='details'>" +
            "<br><li>Choose <b><i>My Stuff -> Decor</i></b> to see the decor you own.</li><br>" +
            "<br><li>Apply your new decor by clicking the <b>Add to Room</b> button.</li><br>" +
            "<br><li>Click the close box to return to your room.</li>" +
            "</p>",
            "Congratulations! You received 200 flow for changing your decor.",
            200));
        _quests.push(new Quest(
            "buyFurni",
            "furniBought",
            "Buy Furniture",
            "<p class='title'>Buy Furniture</p>" +
            "<p class='summary'>Furniture adds depth and personality to a room. Let's shop some more.</p>" +
            "<p class='details'>" +
            "<br><li>Choose <b><i>Catalog -> Furniture</i></b> to find something you like.</li><br>" +
            "<br><li>Click <b><i>Buy!</i></b> when you find something you like.</li><br>" +
            "</p>",
            "You now have furniture to install in your room.",
            0));
        _quests.push(new Quest(
            "installFurni",
            "furniInstalled",
            "Install your furniture",
            "<p class='title'>Install Your Furniture</p>" +
            "<p class='summary'>The furniture won't show up until you add it to your room.</p>" +
            "<p class='details'>" +
            "<br><li>Choose <b><i>My Stuff -> Furniture</i></b> to browse your furniture.</li><br>" +
            "<br><li>Clicking <b>Add to Room</b> will place the item in the center of your room.</li><br>" +
            "</p>",
            "Excellent. You received 300 flow for adding furniture to your room.",
            300));
        _quests.push(new Quest(
            "placeFurni",
            "editorClosed",
            "Place your furniture",
            "<p class='title'>Place Your Furniture</p>" +
            "<p class='summary'>The new furniture appears in the middle of the room until you drag it to where you want it to be. </p>" +
            "<p class='details'>" +
            "<br><li>You should be in Room Editing Mode. If not, click the small hammer icon on the lower Whirled toolbar.</li><br>" +
            "<br><li>Click and drag your furni to place it.</li><br>" +
            "<br><li>Click the <b>Close</b> box on the Room Editing dialog box to return to your room.</li><br>" +
            "</p>",
            "Congratulations! You received 150 flow for adjusting the furniture's position.",
            150));
        _quests.push(new Quest(
            "buyAvatar",
            "avatarBought",
            "Buy a new Avatar",
            "<p class='title'>Change Your Avatar</p>" +
            "<p class='summary'>Find a new face. There's lots to choose from in the catalog.</p>" +
            "<p class='details'>" +
            "<br><li>Click on <b><i>Catalog -> Avatars</i></b> to browse the selection.</li><br>" +
            "<br><li>Purchase one you like.</li><br>" +
            "</p>",
            "Great. You're ready to switch into your new avatar.",
            0));
        _quests.push(new Quest(
            "wearAvatar",
            "avatarInstalled",
            "Wear your new Avatar",
            "<p class='title'>Change Your Avatar</p>" +
            "<p class='summary'>Just as with decor and furni, your avatar won't show in the world until you add it.</p>" +
            "<p class='details'>" +
            "<br><li>Choose <b><i>My Stuff -> Avatars</i></b> to view your avatars.</li><br>" +
            "<br><li>Click the <b>Wear Avatar</b> button to change your avatar.</li><br>" +
            "</p>",
            "Congratulations! You received 200 flow for changing your avatar.",
            200));
    }

    protected static var _quests :Array;
}
}
