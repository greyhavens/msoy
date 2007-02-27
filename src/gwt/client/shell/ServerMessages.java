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
    public String unknown_user ();
    public String invalid_password ();
    public String session_expired ();
    public String invalid_email ();
    public String no_such_project ();
}
