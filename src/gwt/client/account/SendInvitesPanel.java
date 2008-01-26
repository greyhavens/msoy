//
// $Id$

package client.account;

import java.util.ArrayList;
import java.util.Iterator;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable.FlexCellFormatter;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.web.data.MemberInvites;
import com.threerings.msoy.web.data.InvitationResults;
import com.threerings.msoy.web.data.Invitation;

import client.shell.Frame;
import client.util.BorderedPopup;
import client.util.FlashClients;
import client.util.MsoyCallback;
import client.util.MsoyUI;

/**
 * Display a UI allowing users to send out the invites that have been granted to them, as well
 * as view pending invites they've sent in the past.
 */
public class SendInvitesPanel extends FlexTable
{
    /** Originally formulated by lambert@nas.nasa.gov. */
    public static final String EMAIL_REGEX = "^([-A-Za-z0-9_.!%+]+@" +
        "[-a-zA-Z0-9]+(\\.[-a-zA-Z0-9]+)*\\.[-a-zA-Z0-9]+)$";

    public SendInvitesPanel ()
    {
        setCellSpacing(10);
        setStyleName("sendInvites");
        Frame.setTitle(CAccount.msgs.sendInvitesTitle(), CAccount.msgs.sendInvitesSubtitle());
        reinit();
    }

    protected void reinit ()
    {
        CAccount.membersvc.getInvitationsStatus(CAccount.ident, new MsoyCallback() {
            public void onSuccess (Object result) {
                init((MemberInvites)result);
            }
        });
    }

    protected void init (final MemberInvites invites)
    {
        _invites = invites;

        int row = 0;
        FlexCellFormatter formatter = getFlexCellFormatter();
        formatter.setStyleName(row, 0, "Header");
        formatter.setColSpan(row, 0, 3);
        setText(row++, 0, CAccount.msgs.sendInvitesSendHeader("" + invites.availableInvitations));

        if (_invites.availableInvitations > 0) {
            formatter.setStyleName(row, 0, "Tip");
            formatter.setColSpan(row, 0, 3);
            setText(row++, 0, CAccount.msgs.sendInvitesSendTip(
                "" + _invites.availableInvitations));

            formatter.setVerticalAlignment(row, 0, HasVerticalAlignment.ALIGN_TOP);
            setText(row, 0, CAccount.msgs.sendInvitesEmailAddresses());
            setWidget(row++, 1, _emailAddresses = new TextArea());
            _emailAddresses.setCharacterWidth(40);
            _emailAddresses.setVisibleLines(4);

            setText(row++, 0, CAccount.msgs.sendInvitesCustomMessage());
            formatter.setColSpan(row, 0, 3);
            setWidget(row++, 0, _customMessage = new TextArea());
            _customMessage.setCharacterWidth(80);
            _customMessage.setVisibleLines(6);
            _customMessage.setText(CAccount.msgs.sendInvitesCustomDefault());
            _anonymous = new CheckBox(CAccount.msgs.sendInvitesAnonymous());
            if (CAccount.isAdmin()) {
                setWidget(row, 1, _anonymous);
            }
            setWidget(row++, 2, new Button(CAccount.msgs.sendInvitesSendEmail(),
                                           new ClickListener() {
                public void onClick (Widget widget) {
                    if ("".equals(_emailAddresses.getText())) {
                        MsoyUI.info(CAccount.msgs.sendInvitesEnterAddresses());
                    } else {
                        checkAndSend();
                    }
                }
            }));

        } else {
            formatter.setStyleName(row, 0, "Tip");
            formatter.setColSpan(row, 0, 3);
            setText(row++, 0, CAccount.msgs.sendInvitesNoneAvailable());
        }

        formatter.setStyleName(row, 0, "Header");
        formatter.setColSpan(row, 0, 3);
        setText(row++, 0, CAccount.msgs.sendInvitesPendingHeader());

        if (_invites.pendingInvitations.isEmpty()) {
            formatter.setStyleName(row, 0, "Tip");
            formatter.setColSpan(row, 0, 3);
            setText(row++, 0, CAccount.msgs.sendInvitesNoPending());

        } else {
            formatter.setColSpan(row, 0, 3);
            setText(row++, 0, CAccount.msgs.sendInvitesPendingTip());

            int prow = 0;
            FlexTable penders = new FlexTable();
            penders.setWidth("100%");
            for (Iterator iter = _invites.pendingInvitations.iterator(); iter.hasNext(); ) {
                Invitation inv = (Invitation)iter.next();
                penders.setText(prow, 0, inv.inviteeEmail);
                penders.setText(prow++, 1, invites.serverUrl + inv.inviteId);
            }
            formatter.setStyleName(row, 0, "Tip");
            formatter.setColSpan(row, 0, 3);
            setWidget(row++, 0, penders);
        }
    }

    protected void checkAndSend ()
    {
        final ArrayList valid = new ArrayList();
        String[] addresses = _emailAddresses.getText().split("\n");
        for (int ii = 0; ii < addresses.length; ii++) {
            addresses[ii] = addresses[ii].trim();
            if (addresses[ii].matches(EMAIL_REGEX)) {
                if (valid.contains(addresses[ii])) {
                    MsoyUI.info(CAccount.msgs.sendInvitesDuplicateAddress(addresses[ii]));
                    break;
                }
                valid.add(addresses[ii]);
            } else {
                MsoyUI.info(CAccount.msgs.sendInvitesInvalidAddress(addresses[ii]));
                break;
            }
        }
        if (valid.size() != addresses.length) {
            return;
        }

        if (!CAccount.isAdmin() && (valid.size() > _invites.availableInvitations)) {
            MsoyUI.error(CAccount.msgs.sendInvitesTooMany(
                             "" + valid.size(), "" + _invites.availableInvitations));
            return;
        }

        CAccount.membersvc.sendInvites(CAccount.ident, valid, _customMessage.getText(),
                                       _anonymous.isChecked(), new MsoyCallback() {
            public void onSuccess (Object result) {
                FlashClients.tutorialEvent("friendInvited");
                new ResultsPopup(valid, (InvitationResults)result).show();
            }
        });
    }

    protected class ResultsPopup extends BorderedPopup
    {
        public ResultsPopup (ArrayList addrs, InvitationResults invRes)
        {
            VerticalPanel top = new VerticalPanel();
            top.setStyleName("sendInvitesResultsPopup");
            top.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
            top.add(MsoyUI.createLabel(CAccount.msgs.sendInvitesResults(), "ResultsHeader"));

            FlexTable contents = new FlexTable();
            contents.setCellSpacing(10);
            top.add(contents);

            int row = 0;
            for (int ii = 0; ii < invRes.results.length; ii++) {
                String addr = (String)addrs.get(ii);
                if (invRes.results[ii] == InvitationResults.SUCCESS) { // null == null
                    contents.setText(row++, 0, CAccount.msgs.sendInvitesResultsSuccessful(addr));
                } else if (invRes.results[ii].startsWith("e.")) {
                    contents.setText(row++, 0, CAccount.msgs.sendInvitesResultsFailed(
                                         addr, CAccount.serverError(invRes.results[ii])));
                } else {
                    contents.setText(row++, 0, CAccount.msgs.sendInvitesResultsFailed(
                                         addr, invRes.results[ii]));
                }
            }

            contents.getFlexCellFormatter().setHorizontalAlignment(
                row, 0, HasHorizontalAlignment.ALIGN_RIGHT);
            contents.setWidget(row++, 0, new Button(CAccount.cmsgs.dismiss(), new ClickListener() {
                public void onClick (Widget widget) {
                    hide();
                }
            }));

            setWidget(top);
        }
    }

    protected TextArea _emailAddresses;
    protected TextArea _customMessage;
    protected CheckBox _anonymous;
    protected MemberInvites _invites;
}
