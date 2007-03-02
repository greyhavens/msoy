//
// $Id$

package client.shell;

import com.google.gwt.i18n.client.ConstantsWithLookup;

/**
 * Defines all translation messages returned by the server.
 */
public interface ServerMessages extends ConstantsWithLookup
{
    public String internal_error ();
    public String access_denied ();

    public String server_closed ();
    public String no_registrations ();

    public String no_such_user ();
    public String invalid_password ();
    public String session_expired ();
    public String invalid_email ();
    public String duplicate_email ();
    public String duplicate_permaname ();

    public String insufficient_flow ();
    public String insufficient_gold ();

    public String no_such_project ();
}
