//
// $Id$

package com.threerings.msoy.room.data;

import com.threerings.msoy.game.data.GameSummary;

import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.all.MemberName;

/**
 * Contains published information about a member in a scene.
 */
public class MemberInfo extends ActorInfo
{
    public MemberInfo (MemberObject memobj, boolean useStaticImage)
    {
        super(memobj, null, null); // we'll fill these in later

        // configure our media and scale
        if (useStaticImage) {
            _media = Avatar.getStaticImageAvatarMedia();
            _scale = 1f;
            _ident = new ItemIdent(Item.OCCUPANT, memobj.getOid());
            _staticAvatar = true;
        } else if (memobj.avatar != null) {
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

        // note our current game
        _game = memobj.game;
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
     * Returns information on a game this user is currently lobbying or playing.
     */
    public GameSummary getGameSummary ()
    {
        return _game;
    }

    /**
     * Return the scale that should be used for the media.
     */
    public float getScale ()
    {
        return _scale;
    }

    protected float _scale;
    protected GameSummary _game;
    protected boolean _staticAvatar;
}
