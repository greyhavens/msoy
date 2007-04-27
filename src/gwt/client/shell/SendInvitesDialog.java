//
// $Id$

package client.shell;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlexTable.FlexCellFormatter;
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
        FlexCellFormatter formatter = contents.getFlexCellFormatter();
        contents.setCellSpacing(10);
        contents.setStyleName("sendInvites");

        int row = 0;
        
        // TODO: only show send if we have invites available - otherwise show an explanatory message
        formatter.setStyleName(row, 0, "Header");
        formatter.setColSpan(row, 0, 3);
        contents.setText(row++, 0, CShell.cmsgs.sendInvitesSendHeader());

        formatter.setStyleName(row, 0, "Tip");
        formatter.setColSpan(row, 0, 3);
        contents.setText(row++, 0, CShell.cmsgs.sendInvitesSendTip("0" /* TODO */));

        // TODO: only show pending if we do, in fact, have pending invites
        formatter.setStyleName(row, 0, "Header");
        formatter.setColSpan(row, 0, 3);
        contents.setText(row++, 0, CShell.cmsgs.sendInvitesPendingHeader());

        formatter.setStyleName(row, 0, "Tip");
        formatter.setColSpan(row, 0, 3);
        contents.setText(row++, 0, CShell.cmsgs.sendInvitesPendingTip());

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
