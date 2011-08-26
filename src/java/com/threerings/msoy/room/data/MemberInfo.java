//
// $Id$

package com.threerings.msoy.room.data;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyBodyObject;
import com.threerings.msoy.data.MsoyTokenRing;
import com.threerings.msoy.data.MsoyUserOccupantInfo;
import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.game.data.GameSummary;

import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.item.data.all.ItemIdent;

import com.threerings.msoy.item.data.all.MsoyItemType;
import com.threerings.msoy.party.data.PartyOccupantInfo;

/**
 * Contains published information about a member in a scene.
 */
@com.threerings.util.ActionScript(omit=true)
public class MemberInfo extends ActorInfo
    implements MsoyUserOccupantInfo, PartyOccupantInfo
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

        // configure our various bits
        updateGameSummary(memobj);
        updateIsManager(memobj);
        updatePartyId(memobj.partyId);
        updateIsAway(memobj);
        updateTokens(memobj.tokens);
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
        return ((MemberName) username).getId();
    }

    // from PartyOccupantInfo
    public int getPartyId ()
    {
        return _partyId;
    }

    /**
     * Returns true if this member is away, false otherwise.
     */
    public boolean isAway ()
    {
        return (_flags & AWAY) != 0;
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

    // from MsoyUserOccupantInfo
    public boolean isSubscriber ()
    {
        return (_flags & SUBSCRIBER) != 0;
    }

    /**
     * Updates our away status.
     */
    public void updateIsAway (MemberObject memobj)
    {
        if (memobj.isAway()) {
            _flags |= AWAY;
        } else {
            _flags &= ~AWAY;
        }
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

    // from MsoyUserOccupantInfo
    public boolean updateTokens (MsoyTokenRing tokens)
    {
        boolean changed = false;
        if (isSubscriber() != tokens.isSubscriber()) {
            _flags ^= SUBSCRIBER;
            changed = true;
        }
        return changed;
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

    // from PartyOccupantInfo
    public boolean updatePartyId (int partyId)
    {
        if (partyId != _partyId) {
            _partyId = partyId;
            return true;
        }
        return false;
    }

    @Override // from ActorInfo
    protected void useStaticMedia ()
    {
        _media = Avatar.getStaticImageAvatarMedia();
        _ident = new ItemIdent(MsoyItemType.OCCUPANT, getBodyOid());
        _scale = 1f;
    }

    @Override // from ActorInfo
    protected void useDynamicMedia (MsoyBodyObject body)
    {
        MemberObject memobj = (MemberObject)body;
        configureAvatar(memobj.avatar, (memobj.isViewer() || memobj.isPermaguest()));
        if (memobj.avatar != null) {
            _media = memobj.avatar.avatarMedia;
            _ident = memobj.avatar.getIdent();
            _scale = memobj.avatar.scale;
        } else {
            _media = (memobj.isViewer() || memobj.isPermaguest()) ?
                Avatar.getDefaultGuestAvatarMedia() : Avatar.getDefaultMemberAvatarMedia();
            _ident = new ItemIdent(MsoyItemType.OCCUPANT, getBodyOid());
            _scale = 1f;
        }
        _state = memobj.getActorState();
    }

    // TODO: presently exposed to allow for puppets
    public void configureAvatar (Avatar avatar, boolean guestDefault)
    {
        if (avatar != null) {
            _media = avatar.avatarMedia;
            _ident = avatar.getIdent();
            _scale = avatar.scale;
        } else {
            _media = guestDefault ?
                Avatar.getDefaultGuestAvatarMedia() : Avatar.getDefaultMemberAvatarMedia();
            _ident = new ItemIdent(MsoyItemType.OCCUPANT, getBodyOid());
            _scale = 1f;
        }
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
