//
// $Id$

package com.threerings.msoy.person.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.web.gwt.Invitation;

/**
 * Contains the number of invites aviable for a user, along with the list of pending invites.
 */
public class MemberInvites
    implements IsSerializable
{
    /** The number of invites available. */
    public int availableInvitations;

    /** The list of pending invitations. */
    public List<Invitation> pendingInvitations;

    /** The server host string that should be prepended to the invite id */
    public String serverUrl;
}
