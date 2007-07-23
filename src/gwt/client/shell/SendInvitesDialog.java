//
// $Id$

package client.shell;

import java.util.ArrayList;
import java.util.Iterator;
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

import com.threerings.msoy.web.data.MemberInvites;
import com.threerings.msoy.web.data.InvitationResults;
import com.threerings.msoy.web.data.Invitation;

import client.util.BorderedDialog;
import client.util.BorderedPopup;
import client.util.MsoyUI;

/**
 * Display a dialog allowing users to send out the invites that have been granted to them, as well
 * as view pending invites they've sent in the past. 
 */
public class SendInvitesDialog extends BorderedDialog
{
    /** Originally formulated by lambert@nas.nasa.gov. */
    public static final String EMAIL_REGEX = "^([-A-Za-z0-9_.!%+]+@" +
        "[-a-zA-Z0-9]+(\\.[-a-zA-Z0-9]+)*\\.[-a-zA-Z0-9]+)$";

    public SendInvitesDialog (final MemberInvites invites)
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
                            MsoyUI.info(CShell.cmsgs.sendInvitesEnterAddresses());
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
                final Invitation inv = (Invitation)pendingIter.next();
                Label invLabel = new Label(inv.inviteeEmail);
                invLabel.setStyleName("Pending");
                invLabel.addClickListener(new ClickListener() {
                    public void onClick (Widget sender) {
                        MsoyUI.info(invites.serverUrl + inv.inviteId);
                    }
                });
                contents.setWidget(row, col++, invLabel);
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
        final ArrayList validAddresses = new ArrayList();
        String addresses[] = _emailAddresses.getText().split("\n");
        for (int ii = 0; ii < addresses.length; ii++) {
            if (addresses[ii].matches(EMAIL_REGEX)) {
                if (validAddresses.contains(addresses[ii])) {
                    MsoyUI.info(CShell.cmsgs.sendInvitesDuplicateAddress(addresses[ii]));
                    break;
                }
                validAddresses.add(addresses[ii]);
            } else {
                MsoyUI.info(CShell.cmsgs.sendInvitesInvalidAddress(addresses[ii]));
                break;
            }
        }

        if (validAddresses.size() == addresses.length) {
            if (validAddresses.size() > _invites.availableInvitations) {
                MsoyUI.error(CShell.cmsgs.sendInvitesTooMany(
                                 "" + validAddresses.size(), 
                                 "" + _invites.availableInvitations));

            } else {
                CShell.membersvc.sendInvites(CShell.ident, validAddresses, _customMessage.getText(),
                    new AsyncCallback () {
                        public void onSuccess (Object result) {
                            new ResultsPopup(validAddresses, (InvitationResults)result).show();
                            SendInvitesDialog.this.hide();
                        }
                        public void onFailure (Throwable cause) {
                            MsoyUI.error(CShell.serverError(cause));
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
        public ResultsPopup (ArrayList addrs, InvitationResults invRes)
        {
            VerticalPanel top = new VerticalPanel();
            top.setStyleName("sendInvitesResultsPopup");
            top.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
            Label title = new Label(CShell.cmsgs.sendInvitesResults());
            title.setStyleName("ResultsHeader");
            top.add(title);

            FlexTable contents = new FlexTable();
            contents.setCellSpacing(10);
            top.add(contents);

            int row = 0;
            for (int ii = 0; ii < invRes.results.length; ii++) {
                String addr = (String)addrs.get(ii);
                if (invRes.results[ii] == InvitationResults.SUCCESS) { // null == null
                    contents.setText(row++, 0, CShell.cmsgs.sendInvitesResultsSuccessful(addr));
                } else if (invRes.results[ii].startsWith("e.")) { 
                    contents.setText(row++, 0, CShell.cmsgs.sendInvitesResultsFailed(
                                         addr, CShell.serverError(invRes.results[ii])));
                } else {
                    contents.setText(row++, 0, CShell.cmsgs.sendInvitesResultsFailed(
                                         addr, invRes.results[ii]));
                }
            }

            contents.getFlexCellFormatter().setHorizontalAlignment(
                row, 0, HasHorizontalAlignment.ALIGN_RIGHT);
            contents.setWidget(row++, 0, new Button(CShell.cmsgs.dismiss(), new ClickListener() {
                public void onClick (Widget widget) {
                    hide();
                }
            }));

            setWidget(top);
        }
    }

    protected TextArea _emailAddresses;
    protected TextArea _customMessage;
    protected MemberInvites _invites;
}
