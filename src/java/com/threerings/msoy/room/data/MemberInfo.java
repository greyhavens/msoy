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

        // configure our managerness
        updateIsManager(memobj);
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
     * Updates our avatar information. This may result in a static avatar being used if the room
     * occupied by the member requests it.
     */
    public void updateAvatar (MemberObject memobj)
    {
        RoomLocal local = memobj.getLocal(RoomLocal.class);
        if (local != null && local.useStaticMedia(memobj)) {
            useStaticMedia();

        } else {
            if (memobj.avatar != null) {
                useDynamicMedia(memobj.avatar.avatarMedia, memobj.avatar.getIdent());
                _scale = memobj.avatar.scale;
            } else {
                MediaDesc desc = memobj.isGuest() ?
                    Avatar.getDefaultGuestAvatarMedia() : Avatar.getDefaultMemberAvatarMedia();
                useDynamicMedia(desc, new ItemIdent(Item.OCCUPANT, memobj.getOid()));
                _scale = 1f;
            }
            _state = memobj.actorState;
        }
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
