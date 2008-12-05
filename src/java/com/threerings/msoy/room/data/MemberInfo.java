//
// $Id$

package com.threerings.msoy.room.data;

import com.threerings.msoy.game.data.GameSummary;

import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.MemberName;

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
            info.updateAvatar(_mobj);
            return true;
        }
        protected MemberObject _mobj;
    }

    public MemberInfo (MemberObject memobj)
    {
        super(memobj, null, null); // we'll fill these in later

        // configure our media and scale
        updateAvatar(memobj);

        // note our current game
        updateGameSummary(memobj);
    }

    /** Used for unserialization. */
    public MemberInfo ()
    {
    }

    @Override // from ActorInfo
    public void useStaticMedia ()
    {
        super.useStaticMedia();
        _scale = 1f;
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
     * Sets whether this member can manage the room.
     */
    public void setIsManager (boolean isManager)
    {
        if (isManager) {
            _flags |= MANAGER;
        } else {
            _flags &= ~MANAGER;
        }
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

    /**
     * Updates our avatar information.
     */
    public void updateAvatar (MemberObject memobj)
    {
        if (memobj.avatar != null) {
            _media = memobj.avatar.avatarMedia;
            _scale = memobj.avatar.scale;
            _ident = memobj.avatar.getIdent();
        } else if (memobj.isGuest()) {
            _media = Avatar.getDefaultGuestAvatarMedia();
            _scale = 1f;
            _ident = new ItemIdent(Item.OCCUPANT, memobj.getOid());
        } else {
            _media = Avatar.getDefaultMemberAvatarMedia();
            _scale = 1f;
            _ident = new ItemIdent(Item.OCCUPANT, memobj.getOid());
        }
        _state = memobj.actorState;
    }

    // from ActorInfo
    protected MediaDesc getStaticMedia ()
    {
        return Avatar.getStaticImageAvatarMedia();
    }

    @Override // from SimpleStreamableObject
    protected void toString (StringBuilder buf)
    {
        super.toString(buf);
        buf.append(", scale=").append(_scale).append(", game=").append(_game);
    }

    protected float _scale;
    protected GameSummary _game;
}
