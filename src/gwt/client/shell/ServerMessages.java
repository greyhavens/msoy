//
// $Id$

package client.shell;

import com.google.gwt.i18n.client.ConstantsWithLookup;

/**
 * Defines all translation messages returned by the server.
 */
public interface ServerMessages extends ConstantsWithLookup
{
    String internal_error ();
    String server_error ();
    String access_denied ();

    String server_closed ();
    String no_registrations ();
    String invite_already_redeemed ();
    String failed_captcha ();

    String no_such_user ();
    String invalid_password ();
    String session_expired ();
    String invalid_email ();
    String duplicate_email ();
    String duplicate_permaname ();
    String version_mismatch ();

    String no_such_item ();
    String item_in_use ();
    String item_listed ();

    String hit_sales_limit ();
    String insufficient_flow ();
    String insufficient_gold ();
    String list_super_item ();
    String cost_updated ();
    
    String no_such_project ();
    String project_name_exists ();

    String opted_out ();
    String already_registered ();
    String already_invited ();
    String already_friend ();

    String group_name_in_use ();

    String invalid_group ();
    String invalid_thread ();
    String invalid_message ();

    String bad_username_pass ();
    String unsupported_webmail ();
    String user_input_required ();
    String max_webmail_attempts ();

    String ab_test_duplicate_name ();
}
