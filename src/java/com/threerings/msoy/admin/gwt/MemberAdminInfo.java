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

    /** This member's humanity rating. */
    public int humanity;

    /** The member that invited this player, if any. */
    public MemberName inviter;

    /**
     * The names of members that this member invited.
     */
    public List<MemberName> invitees;
}
