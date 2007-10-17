//
// $Id$

package tutorial {

public class Quest
{
    public var questId :String;
    public var trigger :String;
    public var status :String;
    public var intro :String;
    public var summary :String;
    public var payout :int;

    public function Quest (questId :String, trigger :String, status :String, intro :String,
                           summary :String, extro :String, payout :uint)
    {
        this.questId = questId;
        this.trigger = trigger;
        this.status = status;
        this.intro = intro;
        this.summary = summary;
        this.payout = payout;
    }

    public function getQuestId () :String
    {
        return questId;
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
            "Edit your Profile",
            "Fill out your profile and receive 500 flow.",
            "Choose Me -> My Profile to see your Whirled Profile page.<br>" +
            "Click Edit to make changes.",
            "Congratulations! You updated your profile and received 500 flow.",
            500));
        _quests.push(new Quest(
            "buyDecor",
            "decorBought",
            "Buy new Decor",
            "Your room's background image is known as decor. Let's go shopping.",
            "Choose Catalog -> Decor for a selection of new room settings. Buy one you like.",
            null, 0));
        _quests.push(new Quest(
            "installDecor",
            "decorInstalled",
            "Change Decor",
            "Now we need to install your new decor.",
            "Choose My Stuff -> Decor to see the decor you own.<br>" +
            "Apply your new decor by clicking the 'Add to Room' button.<br>" +
            "Click the close box to return to your room.",
            "Congratulations! You received 200 flow for changing your decor.",
            200));
        _quests.push(new Quest(
            "buyFurni",
            "furniBought",
            "Buy Furniture",
            "Furniture adds depth and personality to a room.<br><br>Choose Catalog -> Furniture to find something you like.",
            "Choose Catalog -> Furniture to find something you like.",
            null, 0));
        _quests.push(new Quest(
            "installFurni",
            "furniInstalled",
            "Install your furniture",
            "Choose My Stuff -> Furniture to browse your furniture.<br><br>Clicking 'Add to Room' will place the item in the center of your room.",
            "Choose My Stuff -> Furniture to browse your furniture. Clicking 'Add to Room' will place the item in the center of your room.",
            "Excellent. You received 150 flow for adding furniture to your room.",
            300));
        _quests.push(new Quest(
            "placeFurni",
            "editorClosed",
            "Place your furniture",
            "Click and drag your furni to place it.  Click the Close box on the Room Editing dialog box to return to your room.",
            "Click and drag your furni to place it.  Click the Close box on the Room Editing dialog box to return to your room.",
            "Congratulations! You received 150 flow for positioning the furniture just right.",
            300));
        _quests.push(new Quest(
            "buyAvatar",
            "avatarBought",
            "Buy a new Avatar",
            "Find a new face. Click on Catalog -> Avatars to browse the selection. Purchase one you like.",
            "Click on Catalog -> Avatars to browse the selection. Purchase one you like.",
            null, 0));
        _quests.push(new Quest(
            "wearAvatar",
            "avatarInstalled",
            "Wear your new Avatar",
            "Choose My Stuff -> Avatars to view your avatars. Click the \"Wear Avatar\" button to change your avatar.",
            "Choose My Stuff -> Avatars to view your avatars. Click the \"Wear Avatar\" button to change your avatar.",
            "Congratulations! You received 200 flow for changing your avatar.",
            200));

        // TODO: dump friendInvited?
    }

    protected static var _quests :Array;
}
}
