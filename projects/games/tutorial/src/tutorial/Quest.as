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
            "<p class='summary'>This is the paragraph where we summarize for the user what the profile is, how and when it's displayed, and somehow motivate them to enter some interesting information.</p>" +
            "<p class='details'>" +
            "<br><li>Choose Me -> My Profile to see your Whirled Profile page.<br>" +
            "<br><li>Click <b>Edit</b> and enter your information.<br>" +
            "<br><li>Finally click the <b>Done</b> button." +
            "</p>",
            "Congratulations! You updated your profile and received 500 flow.",
            500));
        _quests.push(new Quest(
            "buyDecor",
            "decorBought",
            "Buy new Decor",
            "<p class='title'>Change Your Decor!</p>" +
            "<p class='summary'>" +
            "<br>The decor is the most fundamental element of your room's appearance. Every other item in your room appears on top the decor." +
            "</p><p class='details'>" +
            "<br><li>Choose <b><i>Catalog -> Decor</i></b> for a selection of new room settings.<br>" +
            "<br><li>Browse through and buy one you like." +
            "</p>",
            "Good. You now own a piece of decor.",
            0));
        _quests.push(new Quest(
            "installDecor",
            "decorInstalled",
            "Change Decor",
            "<p class='title'>Install the new Decor.</p>" +
            "<p class='summary'>This is the paragraph where we summarize for the user what this step is about and motivate them to enter some interesting information.</p>" +
            "<p class='details'>" +
            "<br><li>Choose <b><i>My Stuff -> Decor</i></b> to see the decor you own.<br>" +
            "<br><li>Apply your new decor by clicking the <b>Add to Room</b> button.<br>" +
            "<br><li>Click the close box to return to your room." +
            "</p>",
            "Congratulations! You received 200 flow for changing your decor.",
            200));
        _quests.push(new Quest(
            "buyFurni",
            "furniBought",
            "Buy Furniture",
            "<p class='title'>Buy Furniture!</p>" +
            "<p class='summary'>This is the paragraph where we summarize for the user what this step is about and motivate them to proceed.</p>" +
            "<p class='details'>" +
            "<br><li>Choose <b><i>Catalog -> Furniture</i></b> to find something you like.<br>" +
            "<br><li>Here is the second instruction line!<br>" +
            "<br><li>You can put in a third line too, of course." +
            "</p>",
            
            "We now have furniture to install in our room.",
            0));
        _quests.push(new Quest(
            "installFurni",
            "furniInstalled",
            "Install your furniture",
            "<p class='title'>Install Your Furniture!</p>" +
            "<p class='summary'>This is the paragraph where we summarize for the user what this step is about and motivate them to proceed.</p>" +
            "<p class='details'>" +
            "<br><li>Choose <b><i>My Stuff -> Furniture</i></b> to browse your furniture.<br>" +
            "<br><li>Clicking <b>Add to Room</b> will place the item in the center of your room.<br>" +
            "</p>",
            "Excellent. You received 150 flow for adding furniture to your room.",
            300));
        _quests.push(new Quest(
            "placeFurni",
            "editorClosed",
            "Place your furniture",
            "<p class='title'>Place Your Furniture!</p>" +
            "<p class='summary'>This is the paragraph where we summarize for the user what this step is about and motivate them to proceed.</p>" +
            "<p class='details'>" +
            "<br><li>Click and drag your furni to place it.<br>" +
            "<br><li>Click the <b>Close</b> box on the Room Editing dialog box to return to your room.<br>" +
            "</p>",
            "Congratulations! You received 150 flow for adjusting the furniture's position.",
            300));
        _quests.push(new Quest(
            "buyAvatar",
            "avatarBought",
            "Buy a new Avatar",
            "<p class='title'>Change Your Avatar!</p>" +
            "<p class='summary'>This is the paragraph where we summarize for the user what this step is about and motivate them to proceed.</p>" +
            "<p class='details'>" +
            "<br><li>Click on <b><i>Catalog -> Avatars</i></b> to browse the selection.<br>" +
            "<br><li>Purchase one you like.<br>" +
            "</p>",
            "Great. We're ready to switch into your new avatar.",
            0));
        _quests.push(new Quest(
            "wearAvatar",
            "avatarInstalled",
            "Wear your new Avatar",
            "<p class='title'>Change Your Avatar!</p>" +
            "<p class='summary'>This is the paragraph where we summarize for the user what this step is about and motivate them to proceed.</p>" +
            "<p class='details'>" +
            "<br><li>Choose <b><i>My Stuff -> Avatars</i></b> to view your avatars.<br>" +
            "<br><li>Click the <b>Wear Avatar</b> button to change your avatar.<br>" +
            "</p>",
            "Congratulations! You received 200 flow for changing your avatar.<br><br>" +
            "This concludes the tutorial. Unfortunately we don't yet know how to turn ourselves off, so you will need to click on the little 'X' to leave us. Good luck out there!<br><br>" +
            "Oh bugger, there's no little 'X' to click on anymore.",
            200));
    }

    protected static var _quests :Array;
}
}
