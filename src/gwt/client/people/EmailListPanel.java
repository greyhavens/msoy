//
// $Id$

package client.people;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.Widgets;
import com.threerings.msoy.web.gwt.EmailContact;

import client.shell.CShell;
import client.ui.MsoyUI;

/**
 * Displays an interface for slurping in addresses from webmail contacts and adding them manually.
 */
public abstract class EmailListPanel extends FlowPanel
{
    public EmailListPanel (final boolean filterMembers)
    {
        setStyleName("emailListPanel");
        setWidth("100%");

        // create our two control sets for getting email addresses
        final ManualControls manual = new ManualControls();
        final WebMailControls webmail = new WebMailControls(
            _msgs.emailImportTitle(), _msgs.emailImport()) {
            protected void handleAddresses (List<EmailContact> addrs) {
                List<EmailContact> members = new ArrayList<EmailContact>();
                for (EmailContact ec : addrs) {
                    // don't add existing members to the to be invited list, those will be handled
                    // by virtue of being on the members list
                    if (!filterMembers || ec.mname == null) {
                        addAddress(ec.name, ec.email);
                    } else {
                        members.add(ec);
                    }
                }
                if (members.size() > 0) {
                    handleExistingMembers(members);
                }
            }
        };
        // entry method, default to webmail
        add(_method = webmail);

        // method toggle
        add(MsoyUI.createActionLabel(_msgs.emailManualTip(), "Toggle", new ClickHandler () {
            public void onClick (ClickEvent event) {
                remove(_method);
                if (_method == webmail) {
                    insert(_method = manual, 0);
                    ((Label)event.getSource()).setText(_msgs.emailImportTip());
                } else {
                    insert(_method = webmail, 0);
                    ((Label)event.getSource()).setText(_msgs.emailManualTip());
                }
            }
        }));

        // the address list
        add(_addressList = new InviteList());
        _addressList.setVisible(false);

        // the from/subject/message UI
        add(_msgbox = new SmartTable());
        addFrom(_msgbox);
        _msgbox.addWidget(_message = MsoyUI.createTextArea("", 80, 4), 2);
        Widgets.setPlaceholderText(_message, _msgs.emailMessage());
        int row = _msgbox.addWidget(MsoyUI.createButton(MsoyUI.SHORT_THIN, _msgs.emailSend(),
                                                        new ClickHandler() {
            public void onClick (ClickEvent event) {
                validateAndSend();
            }
        }), 2);
        _msgbox.getFlexCellFormatter().setHorizontalAlignment(row, 0, HasAlignment.ALIGN_RIGHT);
        _msgbox.setVisible(false);
    }

    public void setDefaultMessage (String message)
    {
        _message.setText(message);
    }

    protected void addFrom (SmartTable table)
    {
        int row = table.addText(_msgs.emailFrom(), 1, "Bold");
        table.getFlexCellFormatter().setWidth(row, 0, "10px"); // squeezy!
        _from = MsoyUI.createTextBox(CShell.creds.name.toString(), InviteUtils.MAX_NAME_LENGTH, 25);
        table.setWidget(row, 1, _from);
    }

    /**
     * Adds the supplied name and address to our address list, making the list and the message UI
     * visible if it's not already.
     */
    protected void addAddress (String name, String email)
    {
        _addressList.addItem(name, email);
        _addressList.setVisible(true);
        _msgbox.setVisible(true);
    }

    /**
     * Sends the invite to all the addresses added so far.
     */
    protected void validateAndSend ()
    {
        String from = _from.getText().trim();
        if (from.length() == 0) {
            MsoyUI.error(_msgs.emailEmptyFromField());
            _from.setFocus(true);
            return;
        }
        String msg = _message.getText().trim();
        if (msg.equals(_msgs.emailMessage())) {
            msg = "";
        }
        handleSend(from, msg, InviteUtils.getValidUniqueAddresses(_addressList));
    }

    /**
     * Called with the members of the caller's address book that are already Whirled members, iff
     * <code>filterMembers</code> was true in our constructor.
     */
    protected void handleExistingMembers (List<EmailContact> addrs)
    {
        // nothing by default
    }

    /**
     * Called when the user has clicked send. We will have validated that they have a non-blank
     * from value. We don't validate that contacts is non-empty nor that message is non-blank.
     */
    protected abstract void handleSend (String from, String message, List<EmailContact> contacts);

    protected class ManualControls extends FlowPanel
    {
        public ManualControls ()
        {
            setWidth("100%");

            SmartTable row = new SmartTable(0, 5);
            row.setWidth("100%");
            row.setText(0, 0, _msgs.emailEnterTitle(), 1, "Bold");
            add(row);

            row = new SmartTable(0, 5);
            row.setWidth("100%");

            int col = 0;
            row.setText(0, col++, _msgs.emailName(), 1);
            _name = MsoyUI.createTextBox("", InviteUtils.MAX_NAME_LENGTH, 25);
            Widgets.setPlaceholderText(_name, _msgs.emailFriendName());
            row.setWidget(0, col++, _name);
            row.setText(0, col++, _msgs.emailAddress(), 1);
            _address = MsoyUI.createTextBox("", InviteUtils.MAX_MAIL_LENGTH, 25);
            Widgets.setPlaceholderText(_address, _msgs.emailFriendEmail());
            row.setWidget(0, col++, _address);
            row.setWidget(0, col++, MsoyUI.createButton(MsoyUI.SHORT_THIN, _msgs.emailAdd(),
                                                        new ClickHandler() {
                public void onClick (ClickEvent event) {
                    InviteUtils.addEmailIfValid(_name, _address, _addressList);
                    _addressList.setVisible(true);
                    _msgbox.setVisible(true);
                }
            }));
            add(row);
        }

        protected TextBox _name;
        protected TextBox _address;
    }

    protected Widget _method;
    protected InviteList _addressList;
    protected SmartTable _msgbox;
    protected TextArea _message;
    protected TextBox _from;

    protected static final PeopleMessages _msgs = GWT.create(PeopleMessages.class);
}
