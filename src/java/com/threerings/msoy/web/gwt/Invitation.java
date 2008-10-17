//
// $Id$

package com.threerings.msoy.web.gwt;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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

    /**
     * Creates and initializes an instance from supplied {@link #flatten}ed string.
     */
    public static Invitation unflatten (Iterator<String> data)
    {
        if (data == null) {
            return null;
        }

        Invitation invite = new Invitation();
        invite.inviteId = data.next();
        invite.inviteeEmail = data.next();
        invite.inviter = new MemberName(data.next(), Integer.valueOf(data.next()));
        return invite;
    }

    /**
     * Flattens this instance into a string that can be passed between JavaScript apps.
     */
    public List<String> flatten ()
    {
        List<String> data = new ArrayList<String>();
        data.add(inviteId);
        data.add(inviteeEmail);
        data.add(inviter.toString());
        data.add(String.valueOf(inviter.getMemberId()));
        return data;
    }

}
