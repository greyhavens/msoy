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
    public List invalidAddresses;

    /** A list of the email addresses that we know failed to receive the invitation */
    public List failedAddresses;

    /** A list of the email addresses that we think we successfully sent an invitation to */
    public List successfulAddresses;
}
