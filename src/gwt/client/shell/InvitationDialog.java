//
// $Id$

package client.shell;

import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.web.data.Invitation;

import client.util.BorderedDialog;

public class InvitationDialog extends BorderedDialog 
{
    public InvitationDialog (Invitation invite) 
    {
        _header.add(createTitleLabel(CShell.cmsgs.inviteTitle(), null));
    }

    // @Override // from BorderedDialog
    public Widget createContents ()
    {
        return new FlexTable();
    }

}
