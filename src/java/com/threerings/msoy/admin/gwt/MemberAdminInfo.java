//
// $Id$

package com.threerings.msoy.admin.gwt;

import java.util.Date;
import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.web.gwt.WebCreds;

/**
 * Provides information about a member to admins.
 */
public class MemberAdminInfo
    implements IsSerializable
{
    /** This member's name and id. */
    public MemberName name;

    /** This member's account name. */
    public String accountName;

    /** Is this member's email address validated? */
    public boolean validated;

    /** This member's perma name. */
    public String permaName;

    /** This member's role. */
    public WebCreds.Role role;

    /** This member's flow balance. */
    public int flow;

    /** This member's accumulated flow balance. */
    public int accFlow;

    /** This member's gold balance. */
    public int gold;

    /** This member's session count. */
    public int sessions;

    /** This member's total number of session minutes. */
    public int sessionMinutes;

    /** This time of this member's last session. */
    public Date lastSession;

    /** The member that is this player's affiliate, if any. */
    public MemberName affiliate;

    /** The total number of members that are affiliates of this member. */
    public int affiliateOfCount;

    /** The names of some members that are affiliates of this member. */
    public List<MemberName> affiliateOf;

    /** True if this member is a charity. */
    public boolean charity;

    /** True if this member is a core charity.  Can only be true if isCharity is also true. */
    public boolean coreCharity;

    /** Description of the charity as shown to other members. */
    public String charityDescription;
}
