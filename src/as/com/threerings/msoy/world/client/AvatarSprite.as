//
// $Id$

package com.threerings.msoy.world.client {

import com.threerings.util.CommandEvent;

import com.threerings.flash.MenuUtil;

import com.threerings.crowd.chat.data.ChatMessage;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.WorldContext;
import com.threerings.msoy.client.MsoyController;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.all.MediaDesc;

import com.threerings.msoy.world.data.MemberInfo;
import com.threerings.msoy.world.data.MsoyLocation;
import com.threerings.msoy.world.data.MsoyScene;

/**
 * Displays a member's avatar in the virtual world.
 */
public class AvatarSprite extends ActorSprite
{
    /**
     * Creates an avatar sprite for the supplied occupant.
     */
    public function AvatarSprite (occInfo :MemberInfo)
    {
        super(occInfo);
    }

    override public function getDesc () :String
    {
        return "m.avatar";
    }

    override public function getHoverColor () :uint
    {
        return AVATAR_HOVER;
    }

    /**
     * Get a list of the names of special actions that this avatar supports.
     */
    public function getAvatarActions () :Array
    {
        return validateActionsOrStates(callUserCode("getActions_v1") as Array);
    }

    /**
     * Get a list of the names of the states that this avatar may be in.
     */
    public function getAvatarStates () :Array
    {
        return validateActionsOrStates(callUserCode("getStates_v1") as Array);
    }

    /**
     * Get our preferred y value for positioning.
     */
    public function getPreferredY () :int
    {
        return int(Math.round(_scale * _preferredY));
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

    override public function toString () :String
    {
        return "AvatarSprite[" + _occInfo.username + " (oid=" + _occInfo.bodyOid + ")]";
    }

    override protected function postClickAction () :void
    {
        CommandEvent.dispatch(this, RoomController.AVATAR_CLICKED, this);
    }

    override protected function createBackend () :EntityBackend
    {
        _preferredY = 0;
        return new AvatarBackend();
    }

    /**
     * Verify that the actions or states received from usercode are not wacky.
     *
     * @return the cleaned Array, which may be empty things didn't check out.
     */
    protected function validateActionsOrStates (vals :Array) :Array
    {
        if (vals == null) {
            return [];
        }
        // If there are duplicates, non-strings, or strings.length > 64, then the
        // user has bypassed the checks in their Control and we just discard everything.
        for (var ii :int = 0; ii < vals.length; ii++) {
            if (!validateUserData(vals[ii], null)) {
                return [];
            }
            // reject duplicates
            for (var jj :int = 0; jj < ii; jj++) {
                if (vals[jj] === vals[ii]) {
                    return [];
                }
            }
        }
        // everything checks out...
        return vals;
    }

    /**
     * Routed from usercode by our backend.
     */
    internal function setPreferredYFromUser (prefY :int) :void
    {
        _preferredY = prefY;
    }

    /** The preferred y value, in pixels, when a user selects a location. */
    protected var _preferredY :int
}
}
