//
// $Id$

package client.msgs;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.ServiceDefTarget;

import com.threerings.msoy.web.client.GroupService;
import com.threerings.msoy.web.client.GroupServiceAsync;
import com.threerings.msoy.web.client.MailService;
import com.threerings.msoy.web.client.MailServiceAsync;
import com.threerings.msoy.web.client.PersonService;
import com.threerings.msoy.web.client.PersonServiceAsync;
import com.threerings.msoy.web.client.ProfileService;
import com.threerings.msoy.web.client.ProfileServiceAsync;

import client.shell.MsoyEntryPoint;

/**
 * Configures our {@link MsgsContext} for msgs-derived pages.
 */
public abstract class MsgsEntryPoint extends MsoyEntryPoint
{
    // @Override // from MsoyEntryPoint
    protected void initContext ()
    {
        super.initContext();
        MsgsContext mctx = (MsgsContext)_gctx;

        // wire up our remote services
        mctx.profilesvc = (ProfileServiceAsync)GWT.create(ProfileService.class);
        ((ServiceDefTarget)mctx.profilesvc).setServiceEntryPoint("/profilesvc");
        mctx.personsvc = (PersonServiceAsync)GWT.create(PersonService.class);
        ((ServiceDefTarget)mctx.personsvc).setServiceEntryPoint("/personsvc");
        mctx.mailsvc = (MailServiceAsync)GWT.create(MailService.class);
        ((ServiceDefTarget)mctx.mailsvc).setServiceEntryPoint("/mailsvc");
        mctx.groupsvc = (GroupServiceAsync)GWT.create(GroupService.class);
        ((ServiceDefTarget)mctx.groupsvc).setServiceEntryPoint("/groupsvc");

        // load up our translation dictionaries
        mctx.mmsgs = (MsgsMessages)GWT.create(MsgsMessages.class);
    }
}
