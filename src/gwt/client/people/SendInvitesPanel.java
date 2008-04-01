//
// $Id$

package client.people;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable.FlexCellFormatter;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.WidgetUtil;
import com.threerings.msoy.web.data.MemberInvites;
import com.threerings.msoy.web.data.InvitationResults;
import com.threerings.msoy.web.data.Invitation;

import client.util.BorderedPopup;
import client.util.MsoyCallback;
import client.util.MsoyUI;
import java.util.List;

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

        FlowPanel box = new FlowPanel();
        box.add(_emailAddresses = MsoyUI.createTextArea("", 40, 4));
        String tip = CPeople.msgs.inviteSendTip();
        box.add(MsoyUI.createLabel(tip, "tipLabel"));

        FlowPanel grabber = new FlowPanel();
        grabber.add(MsoyUI.createLabel(CPeople.msgs.inviteGrabber(), "nowrapLabel"));
        grabber.add(MsoyUI.createLabel(CPeople.msgs.inviteWebAddress(), "tipLabel"));
        grabber.add(_webAddress = MsoyUI.createTextBox("", 0, MAX_WEBMAIL_LENGTH));
        grabber.add(MsoyUI.createLabel(CPeople.msgs.inviteWebPassword(), "tipLabel"));
        grabber.add(_webPassword = new PasswordTextBox());
        _webPassword.setWidth("" + MAX_WEBMAIL_LENGTH);
        _webImport = new Button(CPeople.msgs.inviteWebImport(), new ClickListener() {
            public void onClick (Widget widget) {
                if ("".equals(_webAddress.getText())) {
                    MsoyUI.info(CPeople.msgs.inviteEnterWebAddress());
                } else if ("".equals(_webPassword.getText())) {
                    MsoyUI.info(CPeople.msgs.inviteEnterWebPassword());
                } else {
                    _webImport.setEnabled(false);
                    getWebAddresses();
                }
            }
        });
        grabber.add(_webImport);

        add(makeRow(CPeople.msgs.inviteAddresses(), makeRow(box, grabber)));

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

        CPeople.membersvc.getInvitationsStatus(CPeople.ident, new MsoyCallback() {
            public void onSuccess (Object result) {
                gotStatus((MemberInvites)result);
            }
        });
    }

    protected void gotStatus (MemberInvites invites)
    {
        _invites = invites;

        setHorizontalAlignment(ALIGN_LEFT);
        add(MsoyUI.createLabel(CPeople.msgs.invitePendingHeader(), "Header"));

        add(new Label(CPeople.msgs.invitePendingTip()));

        int prow = 0;
        _penders = new FlexTable();
        _penders.setStyleName("tipLabel");
        _penders.setWidth("100%");
        _penders.setText(0, 0, CPeople.msgs.inviteNoPending());
        addPendingInvites(_invites.pendingInvitations);
        add(_penders);
    }

    protected void addPendingInvites  (List penders)
    {
        int prow = (_penders.getCellCount(0) == 1) ? 0 : _penders.getRowCount();
        for (int ii = 0; ii < penders.size(); ii++) {
            Invitation inv = (Invitation)penders.get(ii);
            _penders.setText(prow, 0, inv.inviteeEmail);
            _penders.setText(prow++, 1, _invites.serverUrl + inv.inviteId);
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
        final List invited = new ArrayList();
        Set accepted = new HashSet();
        String[] addresses = _emailAddresses.getText().split("\n");
        for (int ii = 0; ii < addresses.length; ii++) {
            addresses[ii] = addresses[ii].trim();
            if (!addresses[ii].matches(EMAIL_REGEX)) {
                MsoyUI.error(CPeople.msgs.inviteInvalidAddress(addresses[ii]));
                return;
            }
            String laddr = addresses[ii].toLowerCase();
            if (!accepted.contains(laddr)) {
                accepted.add(laddr);
                invited.add(addresses[ii]);
            }
        }

        String from = _fromName.getText().trim(), msg = _customMessage.getText().trim();
        boolean anon = _anonymous.isChecked();
        CPeople.membersvc.sendInvites(CPeople.ident, invited, from, msg, anon, new MsoyCallback() {
            public void onSuccess (Object result) {
                InvitationResults ir = (InvitationResults)result;
                addPendingInvites(ir.pendingInvitations);
                new ResultsPopup(invited, ir).show();
            }
        });
    }

    protected void getWebAddresses ()
    {
        CPeople.profilesvc.getWebMailAddresses(
                CPeople.ident, _webAddress.getText(), _webPassword.getText(),
                new MsoyCallback() {
            public void onSuccess (Object result) {
                List addresses = (List)result;
                String abox = _emailAddresses.getText();
                if (abox.length() > 0 && !abox.endsWith("\n")) {
                    abox += "\n";
                }
                for (int ii = 0, nn = addresses.size(); ii < nn; ii++) {
                    if (ii > 0) {
                        abox += "\n";
                    }
                    abox += (String)addresses.get(ii);
                }
                _emailAddresses.setText(abox);
                _webImport.setEnabled(true);
                _webAddress.setText("");
                _webPassword.setText("");
            }
            public void onFailure (Throwable cause) {
                MsoyUI.error(CPeople.serverError(cause));
                _webImport.setEnabled(true);
            }
        });
    }

    protected class ResultsPopup extends BorderedPopup
    {
        public ResultsPopup (List addrs, InvitationResults invRes)
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

    protected MemberInvites _invites;

    protected TextArea _emailAddresses;
    protected TextBox _fromName;
    protected TextArea _customMessage;
    protected CheckBox _anonymous;
    protected FlexTable _penders;
    protected TextBox _webAddress;
    protected PasswordTextBox _webPassword;
    protected Button _webImport;

    protected static final int MAX_FROM_LEN = 40;
    protected static final int MAX_WEBMAIL_LENGTH = 30;
}
