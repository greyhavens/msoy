//
// $Id$

package com.threerings.msoy.mail.server;

import com.threerings.msoy.web.gwt.ServiceCodes;

/**
 * Codes and constants relating to mail services.
 */
public interface MailCodes extends ServiceCodes
{
    /** An error reported when a user complains about a conversation that he has previously
     * complained about.*/
    public static final String COMPLAINT_ALREADY_REGISTERED =
        "e.conversation_complaint_already_registered";
}
