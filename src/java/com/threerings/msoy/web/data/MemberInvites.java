//
// $Id$

package com.threerings.msoy.web.data;

import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;

/** 
 * Contains the number of invites aviable for a user, along with the list of pending invites. 
 */
public class MemberInvites
    implements IsSerializable
{
    /** The number of invites available. */
    public int availableInvitations;

    /** The list of pending Invitations. */
    public List pendingInvitations;

    /** The server host string that should be prepended on the invite id */
    public String serverUrl;
}
