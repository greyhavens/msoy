//
// $Id$

package client.shell;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Widget;

import client.util.BorderedDialog;

/**
 * Display a dialog allowing users to send out the invite that have been granted to them, as well
 * as view pending invites they've sent in the past. 
 */
public class SendInvitesDialog extends BorderedDialog
{
    public SendInvitesDialog ()
    {
        _header.add(createTitleLabel(CShell.cmsgs.sendInvitesTitle(), null));

        FlexTable contents = (FlexTable)_contents;
        contents.setCellSpacing(10);
        contents.setStyleName("sendInvites");

        _footer.add(new Button(CShell.cmsgs.dismiss(), new ClickListener() {
            public void onClick (Widget widget) {
                hide();
            }
        }));
    }

    // @Override // from BorderedDialog
    protected Widget createContents ()
    {
        return new FlexTable();
    }
}
