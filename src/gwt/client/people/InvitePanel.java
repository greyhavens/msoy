//
// $Id$

package client.people;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
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

import com.threerings.msoy.person.gwt.InvitationResults;
import com.threerings.msoy.person.gwt.InviteService;
import com.threerings.msoy.person.gwt.InviteServiceAsync;
import com.threerings.msoy.person.gwt.MemberInvites;
import com.threerings.msoy.web.gwt.EmailContact;
import com.threerings.msoy.web.gwt.Invitation;
import com.threerings.msoy.web.gwt.Pages;
import com.threerings.msoy.web.gwt.WebMemberService;
import com.threerings.msoy.web.gwt.WebMemberServiceAsync;

import client.shell.CShell;
import client.shell.ShellMessages;
import client.ui.BorderedDialog;
import client.ui.BorderedPopup;
import client.ui.DefaultTextListener;
import client.ui.MsoyUI;
import client.ui.RoundBox;
import client.util.ClickCallback;
import client.util.Link;
import client.util.MsoyCallback;
import client.util.NoopAsyncCallback;
import client.util.ServiceUtil;

/**
 * Display a UI allowing users to send out the invites that have been granted to them, as well as
 * view pending invites they've sent in the past.
 */
public class InvitePanel extends VerticalPanel
{
    public InvitePanel (boolean justRegistered)
    {
        _sendEvents = justRegistered;

        setSpacing(10);
        setStyleName("invite");

        add(MsoyUI.createHTML(_msgs.inviteBanner(), "Banner"));

        RoundBox box = new RoundBox(RoundBox.DARK_BLUE);
        ClickListener addEmail = new ClickListener() {
            public void onClick (Widget sender) {
                addEmail();
            }
        };

        // Add a name/e-mail and import webmail section
        SmartTable input = new SmartTable(0, 5);
        int row = 0;
        input.setText(row++, 0, _msgs.inviteManualTitle(), 3, null);
        input.setWidget(row, 0, _friendName = MsoyUI.createTextBox("", MAX_NAME_LENGTH, 0));
        DefaultTextListener.configure(_friendName, _msgs.inviteFriendName());
        _friendEmail = MsoyUI.createTextBox("", MAX_WEBMAIL_LENGTH, 0);
        _friendEmail.addKeyboardListener(new EnterClickAdapter(addEmail));
        DefaultTextListener.configure(_friendEmail, _msgs.inviteFriendEmail());
        input.setWidget(row, 1, _friendEmail, 2, null);
        input.setWidget(row++, 2, new Button(_msgs.inviteAdd(), addEmail));

        input.setText(row, 0, _msgs.inviteGrabber(), 3, null);
        ClickListener showSupported = new ClickListener() {
            public void onClick (Widget widget) {
                BorderedPopup popup = new BorderedPopup(true);
                popup.setWidget(MsoyUI.createHTML(_msgs.inviteSupportedList(),
                                                  "importSupportList"));
                popup.show();
            }
        };
        input.setWidget(row++, 1, MsoyUI.createActionLabel(_msgs.inviteSupported(),
                                                           "ImportSupportLink", showSupported));

        input.setWidget(row, 0, _webAddress = MsoyUI.createTextBox("", MAX_WEBMAIL_LENGTH, 0));
        DefaultTextListener.configure(_webAddress, _msgs.inviteWebAddress());
        input.setText(row, 1, _msgs.inviteWebPassword());
        input.setWidget(row, 2, _webPassword = new PasswordTextBox());
        Button webImport = new Button(_msgs.inviteWebImport());
        new ClickCallback<List<EmailContact>>(webImport) {
            String webAddress;
            @Override protected boolean callService () {
                if ("".equals(_webAddress.getText())) {
                    MsoyUI.info(_msgs.inviteEnterWebAddress());
                    return false;
                }
                if ("".equals(_webPassword.getText())) {
                    MsoyUI.info(_msgs.inviteEnterWebPassword());
                    return false;
                }
                _invitesvc.getWebMailAddresses(
                    webAddress = _webAddress.getText(), _webPassword.getText(), this);
                return true;
            }
            @Override protected boolean gotResult (List<EmailContact> addresses) {
                if (addresses.size() == 0) {
                    MsoyUI.info(_msgs.inviteNoContacts(webAddress));
                    return true;
                }
                int added = 0;
                for (EmailContact ec : addresses) {
                    if (ec.mname == null) {
                        if (_emailList.addItem(ec.name, ec.email)) {
                            added++;
                        }
                    }
                }
                _webAddress.setText(_msgs.inviteWebAddress());
                _webPassword.setText("");
                webmailResults(addresses);
                if (added == 0) {
                    MsoyUI.info(_msgs.inviteNoNewNonMembers(webAddress));
                }
                return true;
            }
        };
        input.setWidget(row++, 3, webImport);
        input.setText(row++, 0, _msgs.inviteNote(), 4, "Tip");
        box.add(input);

        // Shows the people that will be mailed
        box.add(WidgetUtil.makeShim(10, 10));
        box.add(_emailList = new InviteList());

        // From and custom message box
        SmartTable from = new SmartTable(0, 5);
        from.setText(0, 0, _msgs.inviteFrom(), 1, "Title");
        from.getFlexCellFormatter().setWidth(0, 0, "10px");
        _fromName = MsoyUI.createTextBox(CShell.creds.name.toString(), MAX_NAME_LENGTH, 0);
        from.setWidget(0, 1, _fromName);
        _customMessage = MsoyUI.createTextArea("", -1, 3);
        from.setWidget(1, 0, _customMessage, 2, null);
        _customMessage.setWidth("600px");
        DefaultTextListener.configure(_customMessage, _msgs.inviteCustom());
        box.add(WidgetUtil.makeShim(10, 10));
        box.add(from);

        // Not currently used
        _anonymous = new CheckBox(_msgs.inviteAnonymous());

        // Invite tip and button
        box.add(WidgetUtil.makeShim(10, 10));
        SmartTable buttons = new SmartTable(0, 0);
        buttons.setWidth("100%");
        buttons.setText(0, 0, _msgs.inviteMessage(), 1, "Tip");
        buttons.setWidget(0, 1, MsoyUI.createButton(MsoyUI.LONG_THIN, _msgs.inviteButton(),
                    new ClickListener() {
            public void onClick (Widget widget) {
                if (_emailList.getItems().isEmpty()) {
                    MsoyUI.info(_msgs.inviteEnterAddresses());
                } else {
                    checkAndSend();
                }
            }
        }));
        if (justRegistered) {
            // callback to redirect to #world-h
            final AsyncCallback<Void> goHome = new AsyncCallback<Void>() {
                public void onFailure (Throwable caught) {
                    go();
                }

                public void onSuccess (Void result) {
                    go();
                }

                void go () {
                    Link.go(Pages.WORLD, "h");
                }
            };
            // add a link that sends an event, and then redirects after the result comes back;
            // this prevents the (presumed) synchronous redirect from cancelling the rpc
            // TODO: also send an event if the user navigates away from the page
            Label skip = MsoyUI.createActionLabel(
                _msgs.inviteSkipButton(), new ClickListener() {
                    public void onClick (Widget sender) {
                        sendEvent("skipped", goHome);
                    }
                });
            buttons.setWidget(1, 1, skip, 1, "skip");
        }
        box.add(buttons);
        add(box);

        // Shows pending invitations
        _penders = new SmartTable(0, 5);
        _penders.setText(0, 0, _msgs.invitePendingHeader(), 3, "Header");
        _penders.setText(1, 0, _msgs.invitePendingTip(), 3, "Tip");
        _penders.setText(2, 0, _msgs.inviteNoPending());
        add(_penders);

        _invitesvc.getInvitationsStatus(new MsoyCallback<MemberInvites>() {
            public void onSuccess (MemberInvites invites) {
                gotStatus(invites);
            }
        });
    }

    protected void addEmail ()
    {
        if ("".equals(_friendEmail.getText())) {
            return;

        } else if (!_friendEmail.getText().matches(MsoyUI.EMAIL_REGEX)) {
            MsoyUI.info(_msgs.inviteInvalidEmail());

        } else {
            _emailList.addItem(_friendName.getText(), _friendEmail.getText());
            _friendName.setText(_msgs.inviteFriendName());
            _friendEmail.setText(_msgs.inviteFriendEmail());
        }
    }

    protected void gotStatus (MemberInvites invites)
    {
        _invites = invites;
        addPendingInvites(_invites.pendingInvitations);
    }

    protected void addPendingInvites (List<Invitation> penders)
    {
        int prow = (_penders.getRowCount() == 2 || _penders.getCellCount(2) == 1) ?
            2 : _penders.getRowCount();
        for (int ii = 0; ii < penders.size(); ii++) {
            final int frow = prow++;
            final Invitation inv = penders.get(ii);
            _penders.setWidget(frow, 0,
                    MsoyUI.createActionImage("/images/profile/remove.png", new ClickListener() {
                public void onClick (Widget widget) {
                    removeInvite(inv);
                }
            }));
            _penders.setText(frow, 1, inv.inviteeEmail);
            _penders.setText(frow, 2, _invites.serverUrl + inv.inviteId);
        }
    }

    protected void removeInvite (final Invitation inv)
    {
        _invitesvc.removeInvitation(inv.inviteId, new MsoyCallback<Void>() {
            public void onSuccess (Void result) {
                for (int ii = 2, nn = _penders.getRowCount(); ii < nn; ii++) {
                    if (inv.inviteeEmail.equals(_penders.getText(ii, 1))) {
                        _penders.removeRow(ii);
                        break;
                    }
                }
            }
        });
    }

    protected void checkAndSend ()
    {
        final List<EmailContact> invited = new ArrayList<EmailContact>();
        Set<String> accepted = new HashSet<String>();
        for (EmailContact contact : _emailList.getItems()) {
            if (!contact.email.matches(MsoyUI.EMAIL_REGEX)) {
                MsoyUI.error(_msgs.inviteInvalidAddress(contact.email));
                return;
            }
            String laddr = contact.email.toLowerCase();
            if (!accepted.contains(laddr)) {
                accepted.add(laddr);
                invited.add(contact);
            }
        }

        boolean anon = _anonymous.isChecked();
        String from = _fromName.getText().trim();
        if (!anon && from.length() == 0) {
            MsoyUI.error(_msgs.inviteEmptyFromField());
            _fromName.setFocus(true);
            return;
        }
        String msg = _customMessage.getText().trim();
        if (msg.equals(_msgs.inviteCustom())) {
            msg = "";
        }

        sendEvent("invited", new NoopAsyncCallback());

        _invitesvc.sendInvites(invited, from, msg, anon, new MsoyCallback<InvitationResults>() {
            public void onSuccess (InvitationResults ir) {
                addPendingInvites(ir.pendingInvitations);
                _emailList.clear();
                inviteResults(invited, ir);
            }
        });
    }

    protected void sendEvent (String name, AsyncCallback<Void> callback)
    {
        // only send one event per instance of this
        if (_sendEvents) {
            _membersvc.trackTestAction(
                CShell.visitor, name, "2008 11 force invite on registration", callback);
            _sendEvents = false;
        }
    }

    protected void inviteResults (List<EmailContact> addrs, InvitationResults invRes)
    {
        ResultsPopup rp = new ResultsPopup(_msgs.inviteResults());
        int row = 0;
        boolean success = false;
        SmartTable contents = rp.getContents();

        for (int ii = 0; ii < invRes.results.length; ii++) {
            if (invRes.results[ii] == InvitationResults.SUCCESS) { // null == null;
                EmailContact ec = addrs.get(ii);
                if (!success) {
                    contents.setText(row++, 0, _msgs.inviteResultsSuccessful());
                    success = true;
                }
                contents.setText(row++, 0, _msgs.inviteMember(ec.name, ec.email), 3, null);
            }
        }
        if (success) {
            contents.setWidget(row++, 0, WidgetUtil.makeShim(10, 10));
        }

        boolean members = false;
        for (int ii = 0; ii < invRes.results.length; ii++) {
            if (invRes.names[ii] != null) {
                EmailContact ec = addrs.get(ii);
                if (!members) {
                    contents.setText(row++, 0, _msgs.inviteResultsMembers());
                    members = true;
                }
                contents.setText(row, 0, _msgs.inviteMember(ec.name, ec.email));
                ClickListener onClick = new FriendInviter(invRes.names[ii], "InvitePanel");
                contents.setWidget(row, 1, MsoyUI.createActionImage(
                            "/images/profile/addfriend.png", onClick));
                contents.setWidget(row++, 2, MsoyUI.createActionLabel(
                            _msgs.mlAddFriend(), onClick));
            }
        }
        if (members) {
            contents.setWidget(row++, 0, WidgetUtil.makeShim(10, 10));
        }

        boolean failed = false;
        for (int ii = 0; ii < invRes.results.length; ii++) {
            if (invRes.results[ii] == InvitationResults.SUCCESS || invRes.names[ii] != null) {
                continue;
            }
            if (!failed) {
                contents.setText(row++, 0, _msgs.inviteResultsFailed());
                failed = true;
            }
            EmailContact ec = addrs.get(ii);
            String name = _msgs.inviteMember(ec.name, ec.email);
            String result = invRes.results[ii].startsWith("e.") ?
                _msgs.inviteResultsNote(name, CShell.serverError(invRes.results[ii])) :
                _msgs.inviteResultsNote(name, invRes.results[ii]);
            contents.setText(row++, 0, result, 3, null);
        }
        rp.show();
    }

    protected void webmailResults (List<EmailContact> contacts)
    {
        ResultsPopup rp = new ResultsPopup(_msgs.webmailResults());
        boolean showResults = false;
        int row = 0;
        SmartTable contents = rp.getContents();

        contents.setText(row++, 0, _msgs.inviteResultsMembers());
        for (EmailContact ec : contacts) {
            if (ec.mname != null) {
                showResults = true;
                contents.setText(row, 0, _msgs.inviteMember(ec.name, ec.email));
                ClickListener onClick = new FriendInviter(ec.mname, "InvitePanel");
                contents.setWidget(row, 1, MsoyUI.createActionImage(
                            "/images/profile/addfriend.png", onClick));
                contents.setWidget(row++, 2, MsoyUI.createActionLabel(
                            _msgs.mlAddFriend(), onClick));
            }
        }

        if (showResults) {
            rp.show();
        }
    }

    protected class ResultsPopup extends BorderedDialog
    {
        public ResultsPopup (String title)
        {
            addStyleName("sendInvitesResultsPopup");
            setHeaderTitle(title);

            _contents = new SmartTable();
            _contents.setCellSpacing(3);
            ScrollPanel scroll = new ScrollPanel(_contents);
            scroll.setStyleName("ScrollPanel");
            setContents(scroll);

            addButton(new Button(_cmsgs.dismiss(), new ClickListener() {
                public void onClick (Widget widget) {
                    hide();
                }
            }));
        }

        public SmartTable getContents ()
        {
            return _contents;
        }

        protected SmartTable _contents;
    }

    protected class InviteList extends FlexTable
    {
        public InviteList ()
        {
            super();
            setStyleName("InviteList");
            setCellSpacing(0);
            setText(0, 0, _msgs.inviteListName());
            getFlexCellFormatter().setWidth(0, 0, "190px");
            getFlexCellFormatter().setStyleName(0, 0, "Header");
            setText(0, 1, _msgs.inviteListRemove());
            getFlexCellFormatter().setWidth(0, 1, "40px");
            getFlexCellFormatter().setStyleName(0, 1, "Header");
            setText(0, 2, _msgs.inviteListEmail());
            getFlexCellFormatter().setWidth(0, 2, "364px");
            getFlexCellFormatter().setStyleName(0, 2, "Header");

            _listTable = new SmartTable("InviteListTable", 0, 0);

            ScrollPanel scroll = new ScrollPanel(_listTable);
            scroll.setStyleName("Scroll");
            setWidget(1, 0, scroll);
            getFlexCellFormatter().setColSpan(1, 0, 3);
        }

        public List<EmailContact> getItems ()
        {
            return _items;
        }

        public void clear ()
        {
            _items.clear();
            for (int ii = _listTable.getRowCount() - 1; ii >= 0; ii--) {
                _listTable.removeRow(ii);
            }
        }

        public boolean addItem (String name, String email)
        {
            EmailContact ec = new EmailContact();
            ec.name = name.trim();
            ec.email = email.trim();
            if (_items.contains(ec)) {
                return false;
            }
            _items.add(ec);
            final int row = _listTable.getRowCount();
            _listTable.setText(row, 0, name);
            _listTable.getFlexCellFormatter().setWidth(row, 0, "190px");
            setRemove(row);
            _listTable.setText(row, 2, email);
            return true;
        }

        public void removeItem (int row)
        {
            _items.remove(row);
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

        protected List<EmailContact> _items = new ArrayList<EmailContact>();
        protected FlexTable _listTable;
    }

    protected MemberInvites _invites;

    protected TextBox _fromName;
    protected TextArea _customMessage;
    protected CheckBox _anonymous;
    protected SmartTable _penders;
    protected TextBox _webAddress;
    protected PasswordTextBox _webPassword;

    protected TextBox _friendName;
    protected TextBox _friendEmail;
    protected InviteList _emailList;

    protected boolean _sendEvents;
    
    protected static final PeopleMessages _msgs = GWT.create(PeopleMessages.class);
    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
    protected static final InviteServiceAsync _invitesvc = (InviteServiceAsync)
        ServiceUtil.bind(GWT.create(InviteService.class), InviteService.ENTRY_POINT);
    protected static final WebMemberServiceAsync _membersvc = (WebMemberServiceAsync)
        ServiceUtil.bind(GWT.create(WebMemberService.class), WebMemberService.ENTRY_POINT);

    protected static final int MAX_NAME_LENGTH = 80;
    protected static final int MAX_WEBMAIL_LENGTH = 200;
}
