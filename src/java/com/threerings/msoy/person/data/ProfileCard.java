//
// $Id$

package com.threerings.msoy.person.data;

import com.threerings.msoy.web.data.MemberCard;

/**
 * Contains a member's name, photo and headline.
 */
public class ProfileCard extends MemberCard
{
    /** The member's headline, status, whatever you want to call it. */
    public String headline;

    /** The date on which this member was last logged onto Whirled. */
    public long lastLogon;
}
