//
// $Id$

package client.shell;

import java.util.Iterator;
import java.util.ArrayList;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlexTable.FlexCellFormatter;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.Widget;

import client.util.BorderedDialog;
import client.util.AlertPopup;

import com.threerings.msoy.web.data.MemberInvites;

/**
 * Display a dialog allowing users to send out the invites that have been granted to them, as well
 * as view pending invites they've sent in the past. 
 */
public class SendInvitesDialog extends BorderedDialog
{
    public SendInvitesDialog (MemberInvites invites)
    {
        _header.add(createTitleLabel(CShell.cmsgs.sendInvitesTitle(), null));

        _invites = invites;

        FlexTable contents = (FlexTable)_contents;
        FlexCellFormatter formatter = contents.getFlexCellFormatter();
        contents.setCellSpacing(10);
        contents.setStyleName("sendInvites");

        int row = 0;
        
        formatter.setStyleName(row, 0, "Header");
        formatter.setColSpan(row, 0, 3);
        contents.setText(row++, 0, CShell.cmsgs.sendInvitesSendHeader());    
        if (_invites.availableInvitations > 0) {
            formatter.setStyleName(row, 0, "Tip");
            formatter.setColSpan(row, 0, 3);
            contents.setText(row++, 0, CShell.cmsgs.sendInvitesSendTip( 
                "" + _invites.availableInvitations));

            formatter.setVerticalAlignment(row, 0, HasVerticalAlignment.ALIGN_TOP);
            contents.setText(row, 0, CShell.cmsgs.sendInvitesEmailAddresses());
            contents.setWidget(row++, 1, _emailAddresses = new TextArea());
            _emailAddresses.setCharacterWidth(40);
            _emailAddresses.setVisibleLines(4);

            contents.setText(row++, 0, CShell.cmsgs.sendInvitesCustomMessage());
            formatter.setColSpan(row, 0, 3);
            contents.setWidget(row++, 0, _customMessage = new TextArea());
            _customMessage.setCharacterWidth(80);
            _customMessage.setVisibleLines(6);
            _customMessage.setText(CShell.cmsgs.sendInvitesCustomDefault());
            contents.setWidget(row++, 2, new Button(CShell.cmsgs.sendInvitesSendEmail(), 
                new ClickListener() {
                    public void onClick (Widget widget) {
                        checkAndSend();
                    }
                }));
        } else {
            formatter.setStyleName(row, 0, "Tip");
            formatter.setColSpan(row, 0, 3);
            contents.setText(row++, 0, CShell.cmsgs.sendInvitesNoneAvailable());
        }

        formatter.setStyleName(row, 0, "Header");
        formatter.setColSpan(row, 0, 3);
        contents.setText(row++, 0, CShell.cmsgs.sendInvitesPendingHeader());
        if (!_invites.pendingInvitations.isEmpty()) {
            formatter.setStyleName(row, 0, "Tip");
            formatter.setColSpan(row, 0, 3);
            contents.setText(row++, 0, CShell.cmsgs.sendInvitesPendingTip());
            
            Iterator pendingIter = _invites.pendingInvitations.iterator();
            while (pendingIter.hasNext()) {
                formatter.setStyleName(row, 0, "Pending");
                formatter.setColSpan(row, 0, 3);
                contents.setText(row++, 0, (String)pendingIter.next());
            }
        } else {
            formatter.setStyleName(row, 0, "Tip");
            formatter.setColSpan(row, 0, 3);
            contents.setText(row++, 0, CShell.cmsgs.sendInvitesNoPending());
        }

        _footer.add(new Button(CShell.cmsgs.dismiss(), new ClickListener() {
            public void onClick (Widget widget) {
                hide();
            }
        }));
    }

    protected void checkAndSend () 
    {
        ArrayList validAddresses = new ArrayList();
        String addresses[] = _emailAddresses.getText().split("\n");
        for (int ii = 0; ii < addresses.length; ii++) {
            if (addresses[ii].matches(EMAIL_REGEX)) {
                if (validAddresses.contains(addresses[ii])) {
                    (new AlertPopup(CShell.cmsgs.sendInvitesDuplicateAddress(addresses[ii]))).
                        alert();
                    break;
                }
                validAddresses.add(addresses[ii]);
            } else {
                (new AlertPopup(CShell.cmsgs.sendInvitesInvalidAddress(addresses[ii]))).alert();
                break;
            }
        }

        if (validAddresses.size() == addresses.length) {
            if (validAddresses.size() > _invites.availableInvitations) {
                (new AlertPopup(CShell.cmsgs.sendInvitesTooMany( "" + validAddresses.size(), 
                    "" + _invites.availableInvitations))).alert();
            } else {
                CShell.membersvc.sendInvites(CShell.creds, validAddresses, _customMessage.getText(),
                    new AsyncCallback () {
                        public void onSuccess (Object result) {
                            (new AlertPopup((String)result)).alert();
                        }
                        public void onFailure (Throwable cause) {
                            (new AlertPopup(CShell.serverError(cause))).alert();
                        }
                    });
            }
        }
    }

    // @Override // from BorderedDialog
    protected Widget createContents ()
    {
        return new FlexTable();
    }

    /** Originally formulated by lambert@nas.nasa.gov. */
    protected static final String EMAIL_REGEX = "^([-A-Za-z0-9_.!%+]+@" +
        "[-a-zA-Z0-9]+(\\.[-a-zA-Z0-9]+)*\\.[-a-zA-Z0-9]+)$";

    protected TextArea _emailAddresses;
    protected TextArea _customMessage;
    protected MemberInvites _invites;
}
