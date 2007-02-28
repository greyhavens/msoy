//
// $Id$

package com.threerings.msoy.world.client {

import flash.events.MouseEvent;

import com.threerings.util.CommandEvent;

import com.threerings.flash.MenuUtil;

import com.threerings.crowd.chat.data.ChatMessage;

import com.threerings.msoy.client.ContextMenuProvider;
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
    implements ContextMenuProvider
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
     * Have this avatar perform an action.
     */
    public function performAvatarAction (actionName :String) :void
    {
        callUserCode("action_v1", actionName);
    }

    /**
     * Informs the avatar that the player it represents just spoke.
     */
    public function performAvatarSpoke () :void
    {
        callUserCode("avatarSpoke_v1");
    }

    // from ContextMenuProvider
    public function populateContextMenu (menuItems :Array) :void
    {
        var minfo :WorldMemberInfo = (_occInfo as WorldMemberInfo);
        menuItems.unshift(MenuUtil.createControllerMenuItem(
            Msgs.GENERAL.get("b.view_member"), MsoyController.VIEW_MEMBER,
            minfo.getMemberId(), false, !minfo.isGuest()));
        var ident :ItemIdent = minfo.getItemIdent();
        menuItems.unshift(MenuUtil.createControllerMenuItem(
            Msgs.GENERAL.get("b.view_item"), MsoyController.VIEW_ITEM,
            ident, false, (ident.type == Item.AVATAR)));
    }

    override public function isInteractive () :Boolean
    {
        return true;
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
