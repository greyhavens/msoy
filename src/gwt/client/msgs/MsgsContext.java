//
// $Id$

package client.msgs;

import com.threerings.msoy.web.client.GroupServiceAsync;
import com.threerings.msoy.web.client.MailServiceAsync;
import com.threerings.msoy.web.client.PersonServiceAsync;
import com.threerings.msoy.web.client.ProfileServiceAsync;

import client.shell.ShellContext;

/**
 * Extends {@link ShellContext} and provides mail-message-specific services.
 */
public class MsgsContext extends ShellContext
{
    /** Provides profile-related services. */
    public ProfileServiceAsync profilesvc;

    /** Provides group-related services. */
    public GroupServiceAsync groupsvc;

    /** Provides mail-related services. */
    public MailServiceAsync mailsvc;

    /** Provides person-related services. */
    public PersonServiceAsync personsvc;

    /** Messages used by the msgs interfaces. */
    public MsgsMessages mmsgs;
}
