//
// $Id$

package com.threerings.msoy.world.client {

import flash.events.MouseEvent;

import com.threerings.util.CommandEvent;

import com.threerings.flash.MenuUtil;

import com.threerings.crowd.chat.data.ChatMessage;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.WorldContext;
import com.threerings.msoy.client.MsoyController;
import com.threerings.msoy.data.ActorInfo;

import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.ItemIdent;
import com.threerings.msoy.item.web.MediaDesc;

import com.threerings.msoy.world.data.MsoyLocation;
import com.threerings.msoy.world.data.MsoyScene;
import com.threerings.msoy.world.data.WorldMemberInfo;

/**
 * Displays a member's avatar in the virtual world.
 */
public class AvatarSprite extends ActorSprite
{
    /**
     * Creates an avatar sprite for the supplied occupant.
     */
    public function AvatarSprite (occInfo :ActorInfo)
    {
        super(occInfo);
    }

//    /**
//     * Returns the style of chat bubble to use for this occupant.
//     */
//    public function getChatStyle () :int
//    {
//        return (_occInfo as WorldMemberInfo).chatStyle;
//    }
//
//    /**
//     * Returns the style with which to pop up a chat bubble for this avatar.
//     */
//    public function getChatPopStyle () :int
//    {
//        return (_occInfo as WorldMemberInfo).chatPopStyle;
//    }

    override public function getHoverColor () :uint
    {
        return AVATAR_HOVER;
    }

    override protected function createBackend () :EntityBackend
    {
        return new AvatarBackend();
    }

    /**
     * Get a list of the names of special actions that this avatar supports.
     */
    public function getAvatarActions () :Array
    {
        var arr :Array = (callUserCode("getActions_v1") as Array);
        if (arr == null) {
            arr = [];
        }
        // TODO: filter returned array to ensure it contains Strings?
        return arr;
    }

    /**
     * Get a list of the names of the states that this avatar may be in.
     */
    public function getAvatarStates () :Array
    {
        var arr :Array = (callUserCode("getStates_v1") as Array);
        if (arr == null) {
            arr = [];
        }
        // TODO: filter the returned array, ensure strings, max 64 chars
        return arr;
    }

    override public function messageReceived (name :String, arg :Object, isAction :Boolean) :void
    {
        super.messageReceived(name, arg, isAction);

        // TODO: remove someday
        // TEMP: dispatch an old-style avatar action notification
        // Deprecated 2007-03-13
        if (isAction) {
            callUserCode("action_v1", name); // no arg
        }
    }

    /**
     * Informs the avatar that the player it represents just spoke.
     */
    public function performAvatarSpoke () :void
    {
        callUserCode("avatarSpoke_v1");
    }

    override public function hasAction () :Boolean
    {
        return true;
    }

    override public function mouseClick (event :MouseEvent) :void
    {
        // let's just post a command to our controller
        CommandEvent.dispatch(this, RoomController.AVATAR_CLICKED, this);
    }

    override public function toString () :String
    {
        return "AvatarSprite[" + _occInfo.username + " (oid=" + _occInfo.bodyOid + ")]";
    }
}
}
