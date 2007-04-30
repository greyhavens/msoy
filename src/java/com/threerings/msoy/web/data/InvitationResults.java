//
// $Id$

package com.threerings.msoy.web.data;

import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;

/** 
 * Contains result information on the invitations that were requested.
 */
public class InvitationResults
    implements IsSerializable
{
    /** A list of the email addresses that the server determined were invalid */
    public List invalid;

    /** A list of the email addresses that we know failed to receive the invitation */
    public List failed;

    /** A list of the email addresses that were already registered with whirled */
    public List alreadyRegistered;

    /** A list of the email addresses that were already invited by this user */
    public List alreadyInvited;

    /** A list of the email addresses that we think we successfully sent an invitation to */
    public List successful;
}
