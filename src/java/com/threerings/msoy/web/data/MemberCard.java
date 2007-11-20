//
// $Id$

package com.threerings.msoy.web.data;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.person.data.Profile;

/**
 * Contains a member's name and profile picture.
 */
public class MemberCard
    implements IsSerializable
{
    /** The member's display name and id. */
    public MemberName name;

    /** The member's profile photo (or the default). */
    public MediaDesc photo = Profile.DEFAULT_PHOTO;
}
