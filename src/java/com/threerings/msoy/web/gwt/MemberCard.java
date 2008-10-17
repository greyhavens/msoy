//
// $Id$

package com.threerings.msoy.web.gwt;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.StaticMediaDesc;

/**
 * Contains a member's name, profile picture and other bits.
 */
public class MemberCard
    implements IsSerializable
{
    /** A base class for all types of member status. */
    public static abstract class Status
        implements IsSerializable
    {
        /** Used by {@link MemberCard#compare}. */
        public abstract int getSortOrder ();
    }

    /** The status of a member who is currently in a scene. */
    public static class InScene extends Status
    {
        /** The id of the scene occupied by this member. */
        public int sceneId;

        /** The name of the scene occupied by this member. */
        public String sceneName;

        public int getSortOrder () {
            return 2;
        }
    }

    /** The status of a member who is currently playing a game. */
    public static class InGame extends Status
    {
        /** The id of the game being played by this member. */
        public int gameId;

        /** The name of the game being played by this member. */
        public String gameName;

        public int getSortOrder () {
            return 1;
        }
    }

    /** The status of a member who is currently playing an AVR game. */
    public static class InAVRGame extends InGame
    {
        /** The id of the scene occupied by this member. */
        public int sceneId;
    }

    /** The status of a member who is not online. */
    public static class NotOnline extends Status
    {
        /** The date on which this member was last logged onto Whirled. */
        public long lastLogon;

        public int getSortOrder () {
            return 0;
        }
    }

    /** The default profile photo. */
    public static final MediaDesc DEFAULT_PHOTO =
        new StaticMediaDesc(MediaDesc.IMAGE_PNG, "photo", "profile_photo",
                            // we know that we're 50x60
                            MediaDesc.HALF_VERTICALLY_CONSTRAINED);

    /**
     * Compares two status records based on potential user interset. People in rooms are first (and
     * compare equally to other people in rooms to allow for a secondary sort key), people in games
     * are second and people not online sort in order of most recently logged on.
     */
    public static int compare (Status one, Status two)
    {
        int oso = one.getSortOrder(), tso = two.getSortOrder();
        if (oso == tso) {
            if (one instanceof NotOnline) {
                long lastLogon1 = ((NotOnline)one).lastLogon;
                long lastLogon2 = ((NotOnline)two).lastLogon;
                if (lastLogon1 < lastLogon2) {
                    return 1;
                } else if (lastLogon1 > lastLogon2) {
                    return -1;
                }
            }
            return 0;

        } else {
            return tso - oso;
        }
    }

    /** The member's display name and id. */
    public MemberName name;

    /** The member's profile photo (or the default). */
    public MediaDesc photo = DEFAULT_PHOTO;

    /** The member's headline, status, whatever you want to call it. */
    public String headline;

    /** This member's current status. */
    public Status status;

    /** This member's level. */
    public int level;

    /** Whether or not this member is the requester's friend. */
    public boolean isFriend;
}
