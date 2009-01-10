//
// $Id$

package com.threerings.msoy.room.data;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyBodyObject;
import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.game.data.GameSummary;

import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;

/**
 * Contains published information about a member in a scene.
 */
public class MemberInfo extends ActorInfo
{
    /** Used to update our avatar info when that changes. */
    public static class AvatarUpdater implements Updater<MemberInfo>
    {
        public AvatarUpdater (MemberObject mobj) {
            _mobj = mobj;
        }
        public boolean update (MemberInfo info) {
            info.updateMedia(_mobj);
            return true;
        }
        protected MemberObject _mobj;
    }

    public MemberInfo (MemberObject memobj)
    {
        super(memobj); // we'll fill these in later

        // note our current game
        updateGameSummary(memobj);

        // configure our managerness
        updateIsManager(memobj);

        updatePartyId(memobj.partyId);
    }

    /** Used for unserialization. */
    public MemberInfo ()
    {
    }

    /**
     * Get the member id for this user, or 0 if they're a guest.
     */
    public int getMemberId ()
    {
        return ((MemberName) username).getMemberId();
    }

    /**
     * Return true if we represent a guest.
     */
    public boolean isGuest ()
    {
        return ((MemberName) username).isGuest();
    }

    /**
     * Get this player's partyId.
     */
    public int getPartyId ()
    {
        return _partyId;
    }

    /**
     * Tests if this member is able to manage the room.
     * Note that this is not a definitive check, but rather one that can be used by clients
     * to check other occupants. The value is computed at the time the occupant enters the
     * room, and is not recomputed even if the room ownership changes. The server should
     * continue to do definitive checks where it matters.
     */
    public boolean isManager ()
    {
        return (_flags & MANAGER) != 0;
    }

    /**
     * Updates our room manager status.
     */
    public void updateIsManager (MemberObject memobj)
    {
        RoomLocal local = memobj.getLocal(RoomLocal.class);
        if (local != null && local.isManager(memobj)) {
            _flags |= MANAGER;
        } else {
            _flags &= ~MANAGER;
        }
    }

    public boolean updatePartyId (int partyId)
    {
        if (partyId != _partyId) {
            _partyId = partyId;
            return true;
        }
        return false;
    }

    /**
     * Returns information on a game this user is currently lobbying or playing.
     */
    public GameSummary getGameSummary ()
    {
        return _game;
    }

    /**
     * Updates our game summary information.
     */
    public void updateGameSummary (MemberObject memobj)
    {
        _game = memobj.game;
    }

    /**
     * Return the scale that should be used for the media.
     */
    public float getScale ()
    {
        return _scale;
    }

    @Override // from ActorInfo
    protected void useStaticMedia ()
    {
        _media = Avatar.getStaticImageAvatarMedia();
        _ident = new ItemIdent(Item.OCCUPANT, getBodyOid());
        _scale = 1f;
    }

    @Override // from ActorInfo
    protected void useDynamicMedia (MsoyBodyObject body)
    {
        MemberObject memobj = (MemberObject)body;
        if (memobj.avatar != null) {
            _media = memobj.avatar.avatarMedia;
            _ident = memobj.avatar.getIdent();
            _scale = memobj.avatar.scale;
        } else {
            _media = memobj.isGuest() ?
                Avatar.getDefaultGuestAvatarMedia() : Avatar.getDefaultMemberAvatarMedia();
            _ident = new ItemIdent(Item.OCCUPANT, getBodyOid());
            _scale = 1f;
        }
        _state = memobj.actorState;
    }

    @Override // from SimpleStreamableObject
    protected void toString (StringBuilder buf)
    {
        super.toString(buf);
        buf.append(", scale=").append(_scale).append(", game=").append(_game);
        buf.append(", party=").append(_partyId);
    }

    protected float _scale;
    protected GameSummary _game;
    protected int _partyId;
}
