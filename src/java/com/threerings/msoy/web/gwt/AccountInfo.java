//
// $Id$

package com.threerings.msoy.web.gwt;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Contains all account information not already contained in WebCreds.
 */
public class AccountInfo implements IsSerializable
{
    /** The user's real name.  Used for searching only. */
    public String realName = "";

    /** Whether or not to send email upon receipt of Whirled mail. */
    public boolean emailWhirledMail;

    /** Whether or not to send announcement email. */
    public boolean emailAnnouncements;
}
