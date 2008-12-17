//
// $Id$

package com.threerings.msoy.web.gwt;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Represents credentials from an external authentication source (Facebook Connect, Open Social,
 * etc.).
 *
 * <p> Note: if you create a new external credentials source, you must add a pattern that matches
 * your placeholder email address to MailSender.isPlaceholderAddress().
 *
 * @see ExternalAuther
 */
public abstract class ExternalCreds
    implements IsSerializable
{
    /** Returns the authentication source from whence came these creds. */
    public abstract ExternalAuther getAuthSource ();

    /** Returns the unique identifier for this user supplied by the external source. */
    public abstract String getUserId ();

    /** Returns a placeholder email address to use when auto-creating an account for this user. */
    public abstract String getPlaceholderAddress ();

    @Override // from Object
    public String toString ()
    {
        return getAuthSource().toString();
    }
}
