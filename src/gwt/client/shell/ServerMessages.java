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
    public String server_error ();
    public String access_denied ();

    public String server_closed ();
    public String no_registrations ();
    public String invite_already_redeemed ();

    public String no_such_user ();
    public String invalid_password ();
    public String session_expired ();
    public String invalid_email ();
    public String duplicate_email ();
    public String duplicate_permaname ();
    public String version_mismatch ();

    public String no_such_item ();
    public String item_in_use ();
    public String item_listed ();

    public String hit_sales_limit ();
    public String insufficient_flow ();
    public String insufficient_gold ();

    public String no_such_project ();
    public String project_name_exists ();

    public String opted_out ();
    public String already_registered ();
    public String already_invited ();

    public String group_name_in_use ();

    public String invalid_group ();
    public String invalid_thread ();
    public String invalid_message ();

    public String bad_username_pass ();
    public String unsupported_webmail ();
    public String user_input_required ();
    public String max_webmail_attempts ();
}
