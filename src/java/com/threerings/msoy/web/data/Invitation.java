//
// $Id$

package com.threerings.msoy.web.data;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.data.all.MemberName;

public class Invitation
    implements IsSerializable
{
    /** The invite Id */
    public String inviteId;

    /** The email address this invitation was sent to */
    public String inviteeEmail;

    /** The Member that sent this invitation */
    public MemberName inviter;
}
