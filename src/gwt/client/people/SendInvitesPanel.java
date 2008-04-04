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
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.EnterClickAdapter;
import com.threerings.gwt.ui.WidgetUtil;
import com.threerings.msoy.web.data.EmailContact;
import com.threerings.msoy.web.data.MemberInvites;
import com.threerings.msoy.web.data.InvitationResults;
import com.threerings.msoy.web.data.Invitation;

import client.util.BorderedPopup;
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

        final RoundBox box = new RoundBox(RoundBox.DARK_BLUE);

        // Add a name/e-mail section
        FlexTable addTable = new FlexTable();
        addTable.setStyleName("Section");
        addTable.setText(0, 0, CPeople.msgs.inviteFriendName());
        addTable.setWidget(1, 0, _friendName = MsoyUI.createTextBox("", 0, MAX_WEBMAIL_LENGTH));
        addTable.setText(0, 1, CPeople.msgs.inviteFriendEmail());
        addTable.setWidget(1, 1, _friendEmail = MsoyUI.createTextBox("", 0, MAX_WEBMAIL_LENGTH));
        _friendEmail.addKeyboardListener(new EnterClickAdapter(new ClickListener() {
            public void onClick (Widget sender) {
                addEmail();
            }
        }));
        addTable.setWidget(1, 2, new Button(CPeople.msgs.inviteAdd(), new ClickListener() {
            public void onClick (Widget widget) {
                addEmail();
            }
        }));
        addTable.getColumnFormatter().setWidth(0, "40%");
        addTable.getColumnFormatter().setWidth(1, "40%");
        addTable.getColumnFormatter().setWidth(2, "20%");
        box.add(addTable);

        box.add(_emailList = new InviteList());

        // Import e-mail addresses section
        addTable = new FlexTable();
        addTable.setStyleName("Section");
        addTable.setText(0, 0, CPeople.msgs.inviteGrabber());
        addTable.getFlexCellFormatter().setColSpan(0, 0, 2);
        addTable.setWidget(0, 1, MsoyUI.createActionLabel(CPeople.msgs.inviteSupported(),
                    "importSupportLink", new ClickListener() {
            public void onClick (Widget widget) {
                BorderedPopup popup = new BorderedPopup(true);
                popup.setWidget(MsoyUI.createHTML(
                        CPeople.msgs.inviteSupportedList(), "importSupportList"));
                popup.show();
            }
        }));
        addTable.setText(1, 0, CPeople.msgs.inviteWebAddress());
        addTable.setWidget(2, 0, _webAddress = MsoyUI.createTextBox("", 0, MAX_WEBMAIL_LENGTH));
        addTable.setText(1, 1, CPeople.msgs.inviteWebPassword());
        addTable.setWidget(2, 1, _webPassword = new PasswordTextBox());
        _webPassword.setWidth(MAX_PASSWORD_LENGTH);
        addTable.setWidget(2, 2, _webImport = new Button(
                    CPeople.msgs.inviteWebImport(), new ClickListener() {
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
        }));
        addTable.getColumnFormatter().setWidth(0, "40%");
        addTable.getColumnFormatter().setWidth(1, "40%");
        addTable.getColumnFormatter().setWidth(2, "20%");
        box.add(addTable);

        // From box
        box.add(MsoyUI.createLabel(CPeople.msgs.inviteFrom(), "Title"));
        box.add(_fromName = MsoyUI.createTextBox(
                    CPeople.creds.name.toString(), MAX_FROM_LEN, MAX_FROM_LEN));
        _fromName.setStyleName("Action");

        // Custom message box
        box.add(MsoyUI.createLabel(CPeople.msgs.inviteCustom(), "Title"));
        box.add(MsoyUI.createLabel(CPeople.msgs.inviteOptional(), "Tip"));
        box.add(_customMessage = MsoyUI.createTextArea("", 80, 6));
        _customMessage.setStyleName("Action");

        // Invite button
        _anonymous = new CheckBox(CPeople.msgs.inviteAnonymous());
        addTable = new FlexTable();
        addTable.setStyleName("Action");
        addTable.setText(0, 0, CPeople.msgs.inviteMessage());
        addTable.setWidget(0, 1, MsoyUI.createButton(MsoyUI.LONG_THIN, CPeople.msgs.inviteButton(),
                    new ClickListener() {
            public void onClick (Widget widget) {
                if (_emailList.getItems().isEmpty()) {
                    MsoyUI.info(CPeople.msgs.inviteEnterAddresses());
                } else {
                    checkAndSend();
                }
            }
        }));
        addTable.getColumnFormatter().setWidth(0, "80%");
        addTable.getColumnFormatter().setWidth(1, "20%");

        box.add(addTable);

        CPeople.membersvc.getInvitationsStatus(CPeople.ident, new MsoyCallback() {
            public void onSuccess (Object result) {
                gotStatus((MemberInvites)result, box);
            }
        });

        add(box);

        add(MsoyUI.createLabel(CPeople.msgs.inviteFooter(), "Footer"));
    }

    protected void addEmail ()
    {
        if ("".equals(_friendEmail.getText())) {
            return;
        } else if (!_friendEmail.getText().matches(EMAIL_REGEX)) {
            MsoyUI.info(CPeople.msgs.inviteInvalidEmail());
        } else {
            _emailList.addItem(_friendName.getText(), _friendEmail.getText());
            _friendName.setText("");
            _friendEmail.setText("");
            _friendName.setFocus(true);
        }
    }

    protected void gotStatus (MemberInvites invites, RoundBox box)
    {
        _invites = invites;

        setHorizontalAlignment(ALIGN_LEFT);
        box.add(MsoyUI.createLabel(CPeople.msgs.invitePendingHeader(), "Header"));

        box.add(MsoyUI.createLabel(CPeople.msgs.invitePendingTip(), "Action"));

        int prow = 0;
        _penders = new FlexTable();
        _penders.setWidth("100%");
        _penders.setText(0, 0, CPeople.msgs.inviteNoPending());
        addPendingInvites(_invites.pendingInvitations);
        box.add(_penders);
    }

    protected void addPendingInvites  (List penders)
    {
        int prow = (_penders.getCellCount(0) == 1) ? 0 : _penders.getRowCount();
        for (int ii = 0; ii < penders.size(); ii++) {
            final int frow = prow++;
            final Invitation inv = (Invitation)penders.get(ii);
            _penders.setText(frow, 0, inv.inviteeEmail);
            _penders.setText(frow, 1, _invites.serverUrl + inv.inviteId);
            _penders.setWidget(frow, 2, MsoyUI.createActionLabel(CPeople.msgs.inviteRemove(),
                        null, new ClickListener() {
                public void onClick (Widget widget) {
                    CPeople.membersvc.removeInvitation(CPeople.ident, inv.inviteId,
                        new MsoyCallback() {
                        public void onSuccess (Object result) {
                            _penders.setText(frow, 0, "");
                            _penders.setText(frow, 1, "");
                            _penders.setText(frow, 2, "");
                        }
                    });
                }
            }));
        }
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

    protected void getWebAddresses ()
    {
        CPeople.profilesvc.getWebMailAddresses(
                CPeople.ident, _webAddress.getText(), _webPassword.getText(),
                new MsoyCallback() {
            public void onSuccess (Object result) {
                List addresses = (List)result;
                for (int ii = 0; ii < addresses.size(); ii++) {
                    EmailContact ec = (EmailContact)addresses.get(ii);
                    _emailList.addItem(ec.name, ec.email);
                }
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
            setStyleName("inviteList");
            setCellSpacing(0);
            setText(0, 0, CPeople.msgs.inviteListName());
            getFlexCellFormatter().setWidth(0, 0, "200px");
            getFlexCellFormatter().setStyleName(0, 0, "Header");
            setText(0, 1, CPeople.msgs.inviteListRemove());
            getFlexCellFormatter().setWidth(0, 1, "50px");
            getFlexCellFormatter().setStyleName(0, 1, "Header");
            setText(0, 2, CPeople.msgs.inviteListEmail());
            getFlexCellFormatter().setWidth(0, 2, "350px");
            getFlexCellFormatter().setStyleName(0, 2, "Header");

            _listTable = new FlexTable();
            _listTable.setStyleName("inviteListTable");

            ScrollPanel scroll = new ScrollPanel(_listTable);
            scroll.setStyleName("Scroll");
            setWidget(1, 0, scroll);
            getFlexCellFormatter().setColSpan(1, 0, 3);
            getFlexCellFormatter().setWidth(1, 0, "600px");
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
            _listTable.getFlexCellFormatter().setWidth(row, 0, "200px");
            setRemove(row);
            _listTable.setText(row, 2, email);
            _listTable.getFlexCellFormatter().setWidth(row, 2, "*");
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
            _listTable.getFlexCellFormatter().setWidth(row, 1, "50px");
        }

        protected ArrayList _items = new ArrayList();
        protected FlexTable _listTable;
    }

    protected MemberInvites _invites;

    protected TextBox _fromName;
    protected TextArea _customMessage;
    protected CheckBox _anonymous;
    protected FlexTable _penders;
    protected TextBox _webAddress;
    protected PasswordTextBox _webPassword;
    protected Button _webImport;

    protected TextBox _friendName;
    protected TextBox _friendEmail;
    protected InviteList _emailList;

    protected static final int MAX_FROM_LEN = 40;
    protected static final int MAX_WEBMAIL_LENGTH = 30;
    protected static final String MAX_PASSWORD_LENGTH = "200px";
}
