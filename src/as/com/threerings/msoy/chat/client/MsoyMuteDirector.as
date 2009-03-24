//
// $Id$

package com.threerings.msoy.chat.client {

import com.threerings.crowd.chat.client.MuteDirector;

import com.threerings.util.Name;

import com.threerings.msoy.client.MemberService;
import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.room.data.PetName;

/**
 * Extend the basic MuteDirector with pet-aware functionality:
 * - if a member is muted, their pets are too.
 */
public class MsoyMuteDirector extends MuteDirector
{
    public function MsoyMuteDirector (ctx :MsoyContext)
    {
        super(ctx);
    }

    /**
     * Configure the intial set of muted memberIds, arriving from the server.
     */
    public function setMutedMemberIds (memberIds :Array /* of int */) :void
    {
        for each (var id :int in memberIds) {
            // call super to avoid sending off service requests!
            super.setMuted(new MemberName("", id), true);
        }
    }

    /**
     * Find out specifically if a pet is muted due to the owner being muted, because
     * calling isMuted() with a PetName will return true for either.
     */
    public function isOwnerMuted (name :PetName) :Boolean
    {
        return isMuted(new MemberName("", name.getOwnerId()));
    }

    // from MuteDirector
    override public function isMuted (name :Name) :Boolean
    {
        return super.isMuted(name) ||
            ((name is PetName) && isOwnerMuted(PetName(name)));
    }

    // from MuteDirector
    override public function setMuted (name :Name, mute :Boolean) :void
    {
        super.setMuted(name, mute);

        // take care of persisting to the server
        if (name is MemberName) {
            (_ctx.getClient().requireService(MemberService) as MemberService).setMuted(
                _ctx.getClient(), MemberName(name).getMemberId(), mute,
                MsoyContext(_ctx).listener());
        }
    }
}
}
