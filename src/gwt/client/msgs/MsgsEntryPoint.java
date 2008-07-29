//
// $Id$

package client.msgs;

import com.google.gwt.core.client.GWT;

import client.shell.Page;

/**
 * Configures our {@link CMsgs} for msgs-derived pages.
 */
public abstract class MsgsEntryPoint extends Page
{
    @Override // from Page
    protected void initContext ()
    {
        super.initContext();

        // load up our translation dictionaries
        CMsgs.mmsgs = (MsgsMessages)GWT.create(MsgsMessages.class);
    }
}
