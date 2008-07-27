//
// $Id$

package com.threerings.msoy.game.gwt;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.web.data.MemberCard;

/**
 * Contains information on a player's rating in a game.
 */
public class PlayerRating implements IsSerializable
{
    /** The member's display name and id. */
    public MemberName name;

    /** The member's profile photo (or the default). */
    public MediaDesc photo = MemberCard.DEFAULT_PHOTO;

    /** This member's rating in the game in question. */
    public int rating;
}
