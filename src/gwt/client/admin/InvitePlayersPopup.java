//
// $Id$

package client.admin;

import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Widget;

import client.util.BorderedDialog;

/**
 * Displays an interface for inviting players (creating them an account and sending them an invite
 * email).
 */
public class InvitePlayersPopup extends BorderedDialog
{
    public InvitePlayersPopup ()
    {
    }

    // @Override // from BorderedDialog
    protected Widget createContents ()
    {
        return new FlexTable();
    }
}
