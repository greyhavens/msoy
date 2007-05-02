//
// $Id$

package client.shell;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlexTable.FlexCellFormatter;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import client.util.BorderedDialog;
import client.util.BorderedPopup;
import client.util.AlertPopup;
import client.util.InfoPopup;

import com.threerings.msoy.web.data.MemberInvites;
import com.threerings.msoy.web.data.InvitationResults;

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
        contents.setText(row++, 0, CShell.cmsgs.sendInvitesSendHeader(
            "" + invites.availableInvitations));    
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
                        if ("".equals(_emailAddresses.getText())) {
                            (new AlertPopup(CShell.cmsgs.sendInvitesEnterAddresses())).alert();
                        } else {
                            checkAndSend();
                        }
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
            
            int col = 0;

            Iterator pendingIter = _invites.pendingInvitations.iterator();
            while (pendingIter.hasNext()) {
                formatter.setStyleName(row, col, "Pending");
                contents.setText(row, col++, (String)pendingIter.next());
                if (col == 3) {
                    row++;
                    col = 0;
                }
            }
            if (col != 0) {
                row++;
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
                            (new ResultsPopup((InvitationResults)result)).show();
                            SendInvitesDialog.this.hide();
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

    protected class ResultsPopup extends BorderedPopup 
    {
        public ResultsPopup (InvitationResults invRes)
        {
            VerticalPanel top = new VerticalPanel();
            top.setStyleName("sendInvitesResultsPopup");
            top.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
            Label title = new Label(CShell.cmsgs.sendInvitesResults());
            title.setStyleName("ResultsHeader");
            top.add(title);

            FlexTable contents = new FlexTable();
            contents.setCellSpacing(10);
            FlexCellFormatter formatter = contents.getFlexCellFormatter();
            top.add(contents);

            int row = 0;

            List[] lists = { invRes.successful, invRes.alreadyRegistered, invRes.alreadyInvited,
                invRes.optedOut, invRes.failed, invRes.invalid };
            for (int ii = 0; ii < lists.length; ii++) {
                if (lists[ii] != null && lists[ii].size() > 0) {
                    formatter.setStyleName(row, 0, "Header");
                    formatter.setColSpan(row, 0, 3);
                    switch(ii) {
                    case 0: 
                        contents.setText(row++, 0, CShell.cmsgs.sendInvitesResultsSuccessful("" + 
                            lists[ii].size())); 
                        break;
                    case 1:
                        contents.setText(row++, 0, CShell.cmsgs.sendInvitesResultsAlreadyRegistered(
                            "" + lists[ii].size()));
                        break;
                    case 2:
                        contents.setText(row++, 0, CShell.cmsgs.sendInvitesResultsAlreadyInvited(
                            "" + lists[ii].size()));
                        break;
                    case 3:
                        contents.setText(row++, 0, CShell.cmsgs.sendInvitesResultsOptedOut(
                            "" + lists[ii].size()));
                        break;
                    case 4:
                        contents.setText(row++, 0, CShell.cmsgs.sendInvitesResultsFailed("" +
                            lists[ii].size()));
                        break;
                    case 5:
                        contents.setText(row++, 0, CShell.cmsgs.sendInvitesResultsInvalid("" +
                            lists[ii].size()));
                        break;
                    }

                    Iterator iter = lists[ii].iterator();
                    int col = 0;
                    while (iter.hasNext()) {
                        formatter.setStyleName(row, col, "Email");
                        contents.setText(row, col++, (String)iter.next());
                        if (col == 3) {
                            row++;
                            col = 0;
                        }
                    }
                    if (col != 0) {
                        row++;
                    }
                }
            }

            formatter.setHorizontalAlignment(row, 2, HasHorizontalAlignment.ALIGN_RIGHT);
            contents.setWidget(row++, 2, new Button(CShell.cmsgs.dismiss(), new ClickListener() {
                public void onClick (Widget widget) {
                    hide();
                }
            }));

            setWidget(top);
        }
    }

    /** Originally formulated by lambert@nas.nasa.gov. */
    protected static final String EMAIL_REGEX = "^([-A-Za-z0-9_.!%+]+@" +
        "[-a-zA-Z0-9]+(\\.[-a-zA-Z0-9]+)*\\.[-a-zA-Z0-9]+)$";

    protected TextArea _emailAddresses;
    protected TextArea _customMessage;
    protected MemberInvites _invites;
}
