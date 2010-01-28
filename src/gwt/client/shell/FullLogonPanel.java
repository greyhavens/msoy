//
// $Id$

package client.shell;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.HasAlignment;


import client.shell.LogonPanel;
import client.ui.MsoyUI;

/**
 * Extends the simple logon panel with external authentication stuffs. See
 * {@link FBLogonPanel} for restrictions.
 */
public class FullLogonPanel extends LogonPanel
{
    public FullLogonPanel ()
    {
        super(Mode.HORIZ, MsoyUI.createButton(MsoyUI.MEDIUM_THIN, _msgs.logonLogon(), null));

        // add the interface for logging in via Facebook connect
        setText(1, 0, _msgs.logonFacebook(), 1);
        getFlexCellFormatter().setHorizontalAlignment(1, 0, HasAlignment.ALIGN_RIGHT);

        setWidget(1, 1, new FBLogonPanel());
    }

    protected static final ShellMessages _msgs = GWT.create(ShellMessages.class);
}
