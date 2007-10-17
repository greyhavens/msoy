//
// $Id$

package com.threerings.msoy.web.data;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.person.data.Profile;

/**
 * Contains for a particular member the id, display name, profile photo and anything else that we
 * want to display when displaying a bunch of members.
 */
public class MemberCard
    implements IsSerializable
{
    /** The member's display name and id. */
    public MemberName name;

    /** The member's profile photo (or the default). */
    public MediaDesc photo = Profile.DEFAULT_PHOTO;

    /** The member's blurb */
    public String headline;
}
