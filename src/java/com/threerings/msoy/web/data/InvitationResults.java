//
// $Id$

package com.threerings.msoy.web.data;

import com.google.gwt.user.client.rpc.IsSerializable;

/** 
 * Contains result information on the invitations that were requested.
 */
public class InvitationResults
    implements IsSerializable
{
    /** The invitation was successful. */
    public static final String SUCCESS = null;

    /** The invitee email was invalid. */
    public static final String INVALID_EMAIL = "e.invalid_email";

    /** The invitee has opted out of communications from Whirled. */
    public static final String OPTED_OUT = "e.opted_out";

    /** The invitee has already registered. */
    public static final String ALREADY_REGISTERED = "e.already_registered";

    /** The invitee has already been invited by the inviter. */
    public static final String ALREADY_INVITED = "e.already_invited";

    /** The results for each address sent in the request. */
    public String[] results;
}
