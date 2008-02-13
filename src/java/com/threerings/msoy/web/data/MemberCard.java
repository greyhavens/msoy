//
// $Id$

package com.threerings.msoy.web.data;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.person.data.Profile;

/**
 * Contains a member's name, profile picture and other bits.
 */
public class MemberCard
    implements IsSerializable
{
    /** The member's display name and id. */
    public MemberName name;

    /** The member's profile photo (or the default). */
    public MediaDesc photo = Profile.DEFAULT_PHOTO;

    /** The member's headline, status, whatever you want to call it. */
    public String headline;

    /** The date on which this member was last logged onto Whirled. */
    public long lastLogon;
}
