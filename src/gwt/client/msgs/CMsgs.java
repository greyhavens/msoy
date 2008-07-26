//
// $Id$

package client.msgs;

import client.shell.CShell;

import com.threerings.msoy.web.client.GroupServiceAsync;
import com.threerings.msoy.web.client.IssueServiceAsync;
import com.threerings.msoy.web.client.MailServiceAsync;
import com.threerings.msoy.web.client.ProfileServiceAsync;

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

    /** Provides issue-related services. */
    public static IssueServiceAsync issuesvc;

    /** Messages used by the msgs interfaces. */
    public static MsgsMessages mmsgs;
}
