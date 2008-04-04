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
import com.google.gwt.user.client.ui.FocusListener;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.EnterClickAdapter;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;
import com.threerings.msoy.web.data.EmailContact;
import com.threerings.msoy.web.data.MemberInvites;
import com.threerings.msoy.web.data.InvitationResults;
import com.threerings.msoy.web.data.Invitation;

import client.util.BorderedPopup;
import client.util.ClickCallback;
import client.util.DefaultTextListener;
import client.util.MsoyCallback;
import client.util.MsoyUI;
import client.util.RoundBox;

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

        RoundBox box = new RoundBox(RoundBox.DARK_BLUE);
        ClickListener addEmail = new ClickListener() {
            public void onClick (Widget sender) {
                addEmail();
            }
        };

        // Add a name/e-mail and import webmail section
        SmartTable input = new SmartTable(0, 5);
        int row = 0;
        input.setText(row++, 0, CPeople.msgs.inviteManualTitle(), 3, null);
        input.setWidget(row, 0, _friendName = MsoyUI.createTextBox("", MAX_WEBMAIL_LENGTH, 0));
        DefaultTextListener.configure(_friendName, CPeople.msgs.inviteFriendName());
        _friendEmail = MsoyUI.createTextBox("", MAX_WEBMAIL_LENGTH, 0);
        _friendEmail.addKeyboardListener(new EnterClickAdapter(addEmail));
        DefaultTextListener.configure(_friendEmail, CPeople.msgs.inviteFriendEmail());
        input.setWidget(row, 1, _friendEmail, 2, null);
        input.setWidget(row++, 2, new Button(CPeople.msgs.inviteAdd(), addEmail));

        input.setText(row, 0, CPeople.msgs.inviteGrabber(), 3, null);
        ClickListener showSupported = new ClickListener() {
            public void onClick (Widget widget) {
                BorderedPopup popup = new BorderedPopup(true);
                popup.setWidget(MsoyUI.createHTML(CPeople.msgs.inviteSupportedList(),
                                                  "importSupportList"));
                popup.show();
            }
        };
        input.setWidget(row++, 1, MsoyUI.createActionLabel(CPeople.msgs.inviteSupported(),
                                                           "ImportSupportLink", showSupported));

        input.setWidget(row, 0, _webAddress = MsoyUI.createTextBox("", MAX_WEBMAIL_LENGTH, 0));
        DefaultTextListener.configure(_webAddress, CPeople.msgs.inviteWebAddress());
        input.setText(row, 1, CPeople.msgs.inviteWebPassword());
        input.setWidget(row, 2, _webPassword = new PasswordTextBox());
        _webImport = new Button(CPeople.msgs.inviteWebImport());
        new ClickCallback(_webImport) {
            public boolean callService () {
                if ("".equals(_webAddress.getText())) {
                    MsoyUI.info(CPeople.msgs.inviteEnterWebAddress());
                    return false;
                }
                if ("".equals(_webPassword.getText())) {
                    MsoyUI.info(CPeople.msgs.inviteEnterWebPassword());
                    return false;
                }
                CPeople.profilesvc.getWebMailAddresses(
                    CPeople.ident, _webAddress.getText(), _webPassword.getText(), this);
                return true;
            }
            public boolean gotResult (Object result) {
                List addresses = (List)result;
                for (int ii = 0; ii < addresses.size(); ii++) {
                    EmailContact ec = (EmailContact)addresses.get(ii);
                    _emailList.addItem(ec.name, ec.email);
                }
                _webAddress.setText(CPeople.msgs.inviteWebAddress());
                _webPassword.setText("");
                return true;
            }
        };
        input.setWidget(row++, 3, _webImport);
        input.setText(row++, 0, CPeople.msgs.inviteNote(), 4, "Tip");
        box.add(input);

        // Shows the people that will be mailed
        box.add(WidgetUtil.makeShim(10, 10));
        box.add(_emailList = new InviteList());

        // From and custom message box
        SmartTable from = new SmartTable(0, 5);
        from.setText(0, 0, CPeople.msgs.inviteFrom(), 1, "Title");
        from.getFlexCellFormatter().setWidth(0, 0, "10px");
        _fromName = MsoyUI.createTextBox(CPeople.creds.name.toString(), MAX_FROM_LEN, 0);
        from.setWidget(0, 1, _fromName);
        _customMessage = MsoyUI.createTextArea("", -1, 3);
        from.setWidget(1, 0, _customMessage, 2, null);
        _customMessage.setWidth("600px");
        DefaultTextListener.configure(_customMessage, CPeople.msgs.inviteCustom());
        box.add(WidgetUtil.makeShim(10, 10));
        box.add(from);

        // Not currently used
        _anonymous = new CheckBox(CPeople.msgs.inviteAnonymous());

        // Invite tip and button
        box.add(WidgetUtil.makeShim(10, 10));
        SmartTable buttons = new SmartTable(0, 0);
        buttons.setWidth("100%");
        buttons.setText(0, 0, CPeople.msgs.inviteMessage(), 1, "Tip");
        buttons.setWidget(0, 1, MsoyUI.createButton(MsoyUI.LONG_THIN, CPeople.msgs.inviteButton(),
                    new ClickListener() {
            public void onClick (Widget widget) {
                if (_emailList.getItems().isEmpty()) {
                    MsoyUI.info(CPeople.msgs.inviteEnterAddresses());
                } else {
                    checkAndSend();
                }
            }
        }));
        box.add(buttons);
        add(box);

        // Shows pending invitations
        _penders = new SmartTable(0, 5);
        _penders.setText(0, 0, CPeople.msgs.invitePendingHeader(), 3, "Header");
        _penders.setText(1, 0, CPeople.msgs.invitePendingTip(), 3, "Tip");
        _penders.setText(2, 0, CPeople.msgs.inviteNoPending());
        add(_penders);

        CPeople.membersvc.getInvitationsStatus(CPeople.ident, new MsoyCallback() {
            public void onSuccess (Object result) {
                gotStatus((MemberInvites)result);
            }
        });
    }

    protected void addEmail ()
    {
        if ("".equals(_friendEmail.getText())) {
            return;

        } else if (!_friendEmail.getText().matches(EMAIL_REGEX)) {
            MsoyUI.info(CPeople.msgs.inviteInvalidEmail());

        } else {
            _emailList.addItem(_friendName.getText(), _friendEmail.getText());
            _friendName.setText(CPeople.msgs.inviteFriendName());
            _friendEmail.setText(CPeople.msgs.inviteFriendEmail());
        }
    }

    protected void gotStatus (MemberInvites invites)
    {
        _invites = invites;
        addPendingInvites(_invites.pendingInvitations);
    }

    protected void addPendingInvites  (List penders)
    {
        int prow = (_penders.getCellCount(2) == 1) ? 2 : _penders.getRowCount();
        for (int ii = 0; ii < penders.size(); ii++) {
            final int frow = prow++;
            final Invitation inv = (Invitation)penders.get(ii);
            _penders.setWidget(
                frow, 0, MsoyUI.createActionImage("/images/profile/remove.png", new ClickListener() {
                public void onClick (Widget widget) {
                    removeInvite(inv.inviteId, frow);
                }
            }));
            _penders.setText(frow, 1, inv.inviteeEmail);
            _penders.setText(frow, 2, _invites.serverUrl + inv.inviteId);
        }
    }

    protected void removeInvite (String inviteId, final int frow)
    {
        CPeople.membersvc.removeInvitation(CPeople.ident, inviteId, new MsoyCallback() {
            public void onSuccess (Object result) {
                _penders.removeRow(frow);
            }
        });
    }

    protected void checkAndSend ()
    {
        final List invited = new ArrayList();
        Set accepted = new HashSet();
        ArrayList contacts = _emailList.getItems();
        for (int ii = 0; ii < contacts.size(); ii++) {
            EmailContact contact = (EmailContact)contacts.get(ii);
            if (!contact.email.matches(EMAIL_REGEX)) {
                MsoyUI.error(CPeople.msgs.inviteInvalidAddress(contact.email));
                return;
            }
            String laddr = contact.email.toLowerCase();
            if (!accepted.contains(laddr)) {
                accepted.add(laddr);
                invited.add(contact);
            }
        }

        String from = _fromName.getText().trim(), msg = _customMessage.getText().trim();
        if (msg.equals(CPeople.msgs.inviteCustom())) {
            msg = "";
        }
        boolean anon = _anonymous.isChecked();
        CPeople.membersvc.sendInvites(CPeople.ident, invited, from, msg, anon, new MsoyCallback() {
            public void onSuccess (Object result) {
                InvitationResults ir = (InvitationResults)result;
                addPendingInvites(ir.pendingInvitations);
                new ResultsPopup(invited, ir).show();
                _emailList.clear();
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
                EmailContact ec = (EmailContact)addrs.get(ii);
                if (invRes.results[ii] == InvitationResults.SUCCESS) { // null == null
                    contents.setText(row++, 0, CPeople.msgs.inviteResultsSuccessful(ec.email));
                } else if (invRes.results[ii].startsWith("e.")) {
                    contents.setText(row++, 0, CPeople.msgs.inviteResultsFailed(
                                         ec.email, CPeople.serverError(invRes.results[ii])));
                } else {
                    contents.setText(row++, 0, CPeople.msgs.inviteResultsFailed(
                                         ec.email, invRes.results[ii]));
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

    protected class InviteList extends FlexTable
    {
        public InviteList ()
        {
            super();
            setStyleName("InviteList");
            setCellSpacing(0);
            setText(0, 0, CPeople.msgs.inviteListName());
            getFlexCellFormatter().setWidth(0, 0, "190px");
            getFlexCellFormatter().setStyleName(0, 0, "Header");
            setText(0, 1, CPeople.msgs.inviteListRemove());
            getFlexCellFormatter().setWidth(0, 1, "40px");
            getFlexCellFormatter().setStyleName(0, 1, "Header");
            setText(0, 2, CPeople.msgs.inviteListEmail());
            getFlexCellFormatter().setWidth(0, 2, "340px");
            getFlexCellFormatter().setStyleName(0, 2, "Header");

            _listTable = new SmartTable("InviteListTable", 0, 0);

            ScrollPanel scroll = new ScrollPanel(_listTable);
            scroll.setStyleName("Scroll");
            setWidget(1, 0, scroll);
            getFlexCellFormatter().setColSpan(1, 0, 3);
        }

        public ArrayList getItems ()
        {
            return _items;
        }

        public void clear ()
        {
            _items.clear();
            for (int ii = _listTable.getRowCount() - 1; ii > 1; ii++) {
                _listTable.removeRow(ii);
            }
        }

        public void addItem (String name, String email)
        {
            EmailContact ec = new EmailContact();
            ec.name = name.trim();
            ec.email = email.trim();
            _items.add(ec);
            final int row = _listTable.getRowCount();
            _listTable.setText(row, 0, name);
            _listTable.getFlexCellFormatter().setWidth(row, 0, "190px");
            setRemove(row);
            _listTable.setText(row, 2, email);
        }

        public void removeItem (int row)
        {
            _listTable.removeRow(row);
            for (int ii = row; ii < _listTable.getRowCount(); ii++) {
                setRemove(ii);
            }
        }

        public void setRemove (final int row)
        {
            _listTable.setWidget(row, 1, MsoyUI.createActionImage(
                        "/images/profile/remove.png", new ClickListener() {
                public void onClick (Widget widget) {
                    removeItem(row);
                }
            }));
            _listTable.getFlexCellFormatter().setWidth(row, 1, "40px");
        }

        protected ArrayList _items = new ArrayList();
        protected FlexTable _listTable;
    }

    protected MemberInvites _invites;

    protected TextBox _fromName;
    protected TextArea _customMessage;
    protected CheckBox _anonymous;
    protected SmartTable _penders;
    protected TextBox _webAddress;
    protected PasswordTextBox _webPassword;
    protected Button _webImport;

    protected TextBox _friendName;
    protected TextBox _friendEmail;
    protected InviteList _emailList;

    protected static final int MAX_FROM_LEN = 40;
    protected static final int MAX_WEBMAIL_LENGTH = 30;
}
