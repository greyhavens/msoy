//
// $Id$

package client.msgs;

import client.shell.CShell;

import com.threerings.msoy.group.gwt.GroupServiceAsync;
import com.threerings.msoy.person.gwt.MailServiceAsync;
import com.threerings.msoy.person.gwt.ProfileServiceAsync;

/**
 * Extends {@link CShell} and provides mail-message-specific services.
 */
public class CMsgs extends CShell
{
    /** Provides profile-related services. */
    public static ProfileServiceAsync profilesvc;

    /** Provides group-related services. */
    public static GroupServiceAsync groupsvc;

    /** Provides mail-related services. */
    public static MailServiceAsync mailsvc;

    /** Messages used by the msgs interfaces. */
    public static MsgsMessages mmsgs;
}
