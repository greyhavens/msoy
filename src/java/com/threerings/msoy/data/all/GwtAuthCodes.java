//
// $Id$

package com.threerings.msoy.data.all;

/**
 * Specific auth codes for the MetaSOY server exposed to GWT code.
 */
public interface GwtAuthCodes
{
    /** A message bundle used during client authorization. */
    public static final String AUTH_MSGS = "logon";

    /** A code indicating that the user does not have access to the requested service. */
    public static final String ACCESS_DENIED = "m.access_denied";

    /** A code indicating that the user has been banned from the server. */
    public static final String BANNED = "m.banned";

    /** A code indicating that the user has been temporarily banned from the server. */
    public static final String TEMP_BANNED = "m.temp_banned";

    /** A code indicating that the machine is tainted and no new accounts will be able to logon
     * from it. */
    public static final String MACHINE_TAINTED = "m.machine_tainted";

    /** A code indicating a bounced check or reversed payment. */
    public static final String DEADBEAT = "m.deadbeat";

    /** A code indicating that the server is closed to non-admins. */
    public static final String SERVER_CLOSED = "m.server_closed";

    /** A code indicating that the client version is out of date. */
    public static final String VERSION_MISMATCH = "m.version_mismatch";

    /** A code indicating that the client has a newer version of the code than the server which
     * generally means we're in the middle of updating the game. */
    public static final String NEWER_VERSION = "m.newer_version";

    /** A code indicating that the username (email address) requested by the client is invalid. */
    public static final String INVALID_EMAIL = "m.invalid_email";

    /** A code indicating that the username (email address) requested by the client is in use. */
    public static final String DUPLICATE_EMAIL = "m.duplicate_email";

    /** A code indicating that the permaname requested by the client is in use. */
    public static final String DUPLICATE_PERMANAME = "m.duplicate_permaname";

    /** A code indicating that we are not currently allowing new registrations. */
    public static final String NO_REGISTRATIONS = "m.no_registrations";

    /** A code indicating that we are not currently allowing guest users. */
    public static final String NO_GUESTS = "m.no_guests";

    /** A code indicating that a client's web session has expired. */
    public static final String SESSION_EXPIRED = "m.session_expired";

    /** A code indicating that an invitation has already been redeemed. */
    public static final String INVITE_ALREADY_REDEEMED = "m.invite_already_redeemed";

    /** A code indication that the user failed the captcha check. */
    public static final String FAILED_CAPTCHA = "m.failed_captcha";
}
