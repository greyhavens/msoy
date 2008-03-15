//
// $Id$

package client.people;

import java.util.ArrayList;
import java.util.Iterator;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable.FlexCellFormatter;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.WidgetUtil;
import com.threerings.msoy.web.data.MemberInvites;
import com.threerings.msoy.web.data.InvitationResults;
import com.threerings.msoy.web.data.Invitation;

import client.util.BorderedPopup;
import client.util.FlashClients;
import client.util.MsoyCallback;
import client.util.MsoyUI;

/**
 * Display a UI allowing users to send out the invites that have been granted to them, as well
 * as view pending invites they've sent in the past.
 */
public class SendInvitesPanel extends VerticalPanel
{
    /** Originally formulated by lambert@nas.nasa.gov. */
    public static final String EMAIL_REGEX = "^([-A-Za-z0-9_.!%+]+@" +
        "[-a-zA-Z0-9]+(\\.[-a-zA-Z0-9]+)*\\.[-a-zA-Z0-9]+)$";

    public SendInvitesPanel ()
    {
        setSpacing(10);
        setStyleName("sendInvites");
        reinit();
    }

    protected void reinit ()
    {
        CPeople.membersvc.getInvitationsStatus(CPeople.ident, new MsoyCallback() {
            public void onSuccess (Object result) {
                init((MemberInvites)result);
            }
        });
    }

    protected void init (final MemberInvites invites)
    {
        clear();
        _invites = invites;

        String header = CPeople.msgs.inviteSendHeader("" + invites.availableInvitations);
        add(MsoyUI.createLabel(header, "Header"));

        if (_invites.availableInvitations > 0) {
            FlowPanel box = new FlowPanel();
            box.add(_emailAddresses = MsoyUI.createTextArea("", 40, 4));
            String tip = CPeople.msgs.inviteSendTip("" + _invites.availableInvitations);
            box.add(MsoyUI.createLabel(tip, "tipLabel"));
            add(makeRow(CPeople.msgs.inviteAddresses(), box));

            add(makeRow(CPeople.msgs.inviteFrom(), _fromName = MsoyUI.createTextBox(
                            CPeople.creds.name.toString(), MAX_FROM_LEN, MAX_FROM_LEN)));

            add(MsoyUI.createLabel(CPeople.msgs.inviteCustomTip(), "tipLabel"));
            add(_customMessage = MsoyUI.createTextArea("", 80, 6));

            setHorizontalAlignment(ALIGN_RIGHT);
            _anonymous = new CheckBox(CPeople.msgs.inviteAnonymous());
            Button send = new Button(CPeople.msgs.inviteSendEmail(), new ClickListener() {
                public void onClick (Widget widget) {
                    if ("".equals(_emailAddresses.getText())) {
                        MsoyUI.info(CPeople.msgs.inviteEnterAddresses());
                    } else {
                        checkAndSend();
                    }
                }
            });
            if (CPeople.isAdmin()) {
                add(makeRow(_anonymous, send));
            } else {
                add(send);
            }

        } else {
            add(MsoyUI.createLabel(CPeople.msgs.inviteNoneAvailable(), "tipLabel"));
        }

        setHorizontalAlignment(ALIGN_LEFT);
        add(MsoyUI.createLabel(CPeople.msgs.invitePendingHeader(), "Header"));

        if (_invites.pendingInvitations.isEmpty()) {
            add(new Label(CPeople.msgs.inviteNoPending()));

        } else {
            add(new Label(CPeople.msgs.invitePendingTip()));

            int prow = 0;
            FlexTable penders = new FlexTable();
            penders.setStyleName("tipLabel");
            penders.setWidth("100%");
            for (Iterator iter = _invites.pendingInvitations.iterator(); iter.hasNext(); ) {
                Invitation inv = (Invitation)iter.next();
                penders.setText(prow, 0, inv.inviteeEmail);
                penders.setText(prow++, 1, invites.serverUrl + inv.inviteId);
            }
            add(penders);
        }
    }

    protected HorizontalPanel makeRow (String label, Widget right)
    {
        return makeRow(MsoyUI.createLabel(label, "rightLabel"), right);
    }

    protected HorizontalPanel makeRow (Widget label, Widget right)
    {
        HorizontalPanel row = new HorizontalPanel();
        row.add(label);
        row.add(WidgetUtil.makeShim(10, 10));
        row.add(right);
        return row;
    }

    protected void checkAndSend ()
    {
        final ArrayList valid = new ArrayList();
        String[] addresses = _emailAddresses.getText().split("\n");
        for (int ii = 0; ii < addresses.length; ii++) {
            addresses[ii] = addresses[ii].trim();
            if (addresses[ii].matches(EMAIL_REGEX)) {
                if (valid.contains(addresses[ii])) {
                    MsoyUI.info(CPeople.msgs.inviteDuplicateAddress(addresses[ii]));
                    break;
                }
                valid.add(addresses[ii]);
            } else {
                MsoyUI.info(CPeople.msgs.inviteInvalidAddress(addresses[ii]));
                break;
            }
        }
        if (valid.size() != addresses.length) {
            return;
        }

        if (!CPeople.isAdmin() && (valid.size() > _invites.availableInvitations)) {
            MsoyUI.error(CPeople.msgs.inviteTooMany(
                             "" + valid.size(), "" + _invites.availableInvitations));
            return;
        }

        String from = _fromName.getText().trim(), msg = _customMessage.getText().trim();
        boolean anon = _anonymous.isChecked();
        CPeople.membersvc.sendInvites(CPeople.ident, valid, from, msg, anon, new MsoyCallback() {
            public void onSuccess (Object result) {
                FlashClients.tutorialEvent("friendInvited");
                reinit();
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
            top.setHorizontalAlignment(VerticalPanel.ALIGN_CENTER);
            top.add(MsoyUI.createLabel(CPeople.msgs.inviteResults(), "ResultsHeader"));

            FlexTable contents = new FlexTable();
            contents.setCellSpacing(10);
            top.add(contents);

            int row = 0;
            for (int ii = 0; ii < invRes.results.length; ii++) {
                String addr = (String)addrs.get(ii);
                if (invRes.results[ii] == InvitationResults.SUCCESS) { // null == null
                    contents.setText(row++, 0, CPeople.msgs.inviteResultsSuccessful(addr));
                } else if (invRes.results[ii].startsWith("e.")) {
                    contents.setText(row++, 0, CPeople.msgs.inviteResultsFailed(
                                         addr, CPeople.serverError(invRes.results[ii])));
                } else {
                    contents.setText(row++, 0, CPeople.msgs.inviteResultsFailed(
                                         addr, invRes.results[ii]));
                }
            }

            contents.getFlexCellFormatter().setHorizontalAlignment(
                row, 0, VerticalPanel.ALIGN_RIGHT);
            contents.setWidget(row++, 0, new Button(CPeople.cmsgs.dismiss(), new ClickListener() {
                public void onClick (Widget widget) {
                    hide();
                }
            }));

            setWidget(top);
        }
    }

    protected TextArea _emailAddresses;
    protected TextBox _fromName;
    protected TextArea _customMessage;
    protected CheckBox _anonymous;
    protected MemberInvites _invites;

    protected static final int MAX_FROM_LEN = 40;
}
