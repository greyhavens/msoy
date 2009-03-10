//
// $Id$

package client.people;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.msoy.web.gwt.EmailContact;

import client.shell.CShell;
import client.ui.BorderedPopup;
import client.ui.DefaultTextListener;
import client.ui.MsoyUI;

/**
 * Displays an interface for slurping in addresses from webmail contacts and adding them manually.
 */
public abstract class EmailListPanel extends SmartTable
{
    /**
     * Creates a new email panel.
     * TODO: support a more advanced address list that shows whether each of your contacts is
     * a non-member, a member non-friend or a friend.
     */
    public EmailListPanel ()
    {
        setStyleName("emailListPanel");
        setWidth("100%");

        _addressList = new InviteList();

        // create our two control sets for getting email addresses
        final WebMailControls webmail = new WebMailControls(_addressList);
        final ManualControls manual = new ManualControls(_addressList);

        int row = 0;

        // entry method, default to webmail
        setWidget(row, 0, webmail, 2, null);

        // mark the coordinates of the method widget, just for clarity
        final int methodRow = row++;
        final int methodColumn = 0;

        // the address list
        setWidget(row++, 0, _addressList, 2, null);

        // method toggle
        Label toggle = MsoyUI.createActionLabel(
            _msgs.emailManualTip(), "Toggle", new ClickListener () {
            public void onClick (Widget sender) {
                if (getWidget(methodRow, methodColumn) == webmail) {
                    setWidget(methodRow, methodColumn, manual, 2, null);
                    ((Label)sender).setText(_msgs.emailImportTip());
                } else {
                    setWidget(methodRow, methodColumn, webmail, 2, null);
                    ((Label)sender).setText(_msgs.emailManualTip());
                }
            }
        });
        setWidget(row++, 0, toggle, 2, "Toggle");

        // from
        setText(row, 0, _msgs.emailFrom(), 1, "Bold");
        _from = MsoyUI.createTextBox(CShell.creds.name.toString(), InviteUtils.MAX_NAME_LENGTH, 25);
        setWidget(row++, 1, _from);

        // message
        setText(row, 0, _msgs.emailMessage(), 1, "Bold");
        setText(row++, 1, _msgs.emailOptional(), 1, "labelparen");
        setWidget(row++, 0, _message = MsoyUI.createTextArea("", 80, 4), 2, null);

        setWidget(row, 0, MsoyUI.createButton("shortThin", _msgs.emailSend(), new ClickListener() {
            public void onClick (Widget sender) {
                validateAndSend();
            }
        }), 2, null);
        getFlexCellFormatter().setHorizontalAlignment(row++, 0, HasHorizontalAlignment.ALIGN_RIGHT);
    }

    public void setDefaultMessage (String message)
    {
        _message.setText(message);
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
        handleSend(from, _message.getText().trim(),
                   InviteUtils.getValidUniqueAddresses(_addressList));
    }

    /**
     * Called when the user has clicked send. We will have validated that they have a non-blank
     * from value. We don't validate that contacts is non-empty nor that message is non-blank.
     */
    protected abstract void handleSend (String from, String message, List<EmailContact> contacts);

    protected static class WebMailControls extends FlowPanel
    {
        public WebMailControls (InviteList addressList)
        {
            setWidth("100%");

            // labels line
            SmartTable row = new SmartTable(0, 5);
            row.setWidth("100%");

            int col = 0;
            row.setText(0, col++, _msgs.emailImportTitle(), 1, "Bold");
            row.setWidget(0, col++, new Image(
                "/images/people/invite/webmail_providers_small_horizontal.png"));
            Widget showSupported = MsoyUI.createActionLabel(
                _msgs.emailSupported(), "ImportSupportLink", new ClickListener() {
                public void onClick (Widget widget) {
                    new BorderedPopup(true) { /*constructor*/ {
                        setWidget(MsoyUI.createHTML(_msgs.emailWebmails(), "emailSupported"));
                    }}.show();
                }
            });
            row.setWidget(0, col, showSupported);
            row.getFlexCellFormatter().setHorizontalAlignment(
                0, col++, HasHorizontalAlignment.ALIGN_RIGHT);
            add(row);

            // account entry line
            row = new SmartTable(0, 5);
            row.setWidth("100%");

            col = 0;
            row.setText(0, col++, _msgs.emailAccount(), 1, "Small");
            TextBox account = MsoyUI.createTextBox("", InviteUtils.MAX_MAIL_LENGTH, 25);
            DefaultTextListener.configure(account, _msgs.emailWebAddress());
            row.setWidget(0, col++, account);
            row.setText(0, col++, _msgs.emailPassword(), 1, "Small");
            TextBox password = new PasswordTextBox();
            row.setWidget(0, col++, password);

            PushButton doimp = MsoyUI.createButton(MsoyUI.SHORT_THIN, _msgs.emailImport(), null);
            new InviteUtils.WebmailImporter(doimp, account, password, addressList, false);
            row.setWidget(0, col++, doimp);
            add(row);

            // privacy soother
            row = new SmartTable(0, 5);
            row.setWidth("100%");
            row.setText(0, 0, _msgs.emailCaveat(), 1, "Privacy");
            add(row);
        }
    }

    protected static class ManualControls extends FlowPanel
    {
        public ManualControls (InviteList addressList)
        {
            _list = addressList;

            setWidth("100%");

            SmartTable row = new SmartTable(0, 5);
            row.setWidth("100%");
            row.setText(0, 0, _msgs.emailEnterTitle(), 1, "Bold");
            add(row);

            row = new SmartTable(0, 5);
            row.setWidth("100%");

            int col = 0;
            row.setText(0, col++, _msgs.emailName(), 1, "Small");
            _name = MsoyUI.createTextBox("", InviteUtils.MAX_NAME_LENGTH, 25);
            DefaultTextListener.configure(_name, _msgs.emailFriendName());
            row.setWidget(0, col++, _name);
            row.setText(0, col++, _msgs.emailAddress(), 1, "Small");
            _address = MsoyUI.createTextBox("", InviteUtils.MAX_MAIL_LENGTH, 25);
            DefaultTextListener.configure(_address, _msgs.emailFriendEmail());
            row.setWidget(0, col++, _address);
            row.setWidget(0, col++, MsoyUI.createButton(MsoyUI.SHORT_THIN, _msgs.emailAdd(),
                                                        new ClickListener() {
                public void onClick (Widget sender) {
                    InviteUtils.addEmailIfValid(_name, _address, _list);
                }
            }));
            add(row);
        }

        protected InviteList _list;
        protected TextBox _name;
        protected TextBox _address;
    }

    protected InviteList _addressList;
    protected TextArea _message;
    protected TextBox _from;

    protected static final PeopleMessages _msgs = GWT.create(PeopleMessages.class);
}
