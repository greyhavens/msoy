//
// $Id$

package client.people;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.util.ServiceUtil;

import com.threerings.msoy.person.gwt.InviteService;
import com.threerings.msoy.person.gwt.InviteServiceAsync;
import com.threerings.msoy.web.gwt.EmailContact;

import client.ui.BorderedPopup;
import client.ui.DefaultTextListener;
import client.ui.MsoyUI;
import client.util.ClickCallback;

/**
 * Displays an interface for entering a webmail username and password and importing contacts
 * therefrom.
 */
public abstract class WebMailControls extends FlowPanel
{
    public WebMailControls (String title, String action)
    {
        setStyleName("WebMailControls");
        setWidth("100%");

        // labels line
        SmartTable row = new SmartTable(0, 5);
        row.setWidth("100%");

        int col = 0;
        row.setText(0, col++, title, 1, "Bold");
        row.setWidget(0, col++, new Image(PROVIDERS_IMG));
        Widget showSupported = MsoyUI.createActionLabel(
            _msgs.emailSupported(), null, new ClickHandler() {
            public void onClick (ClickEvent event) {
                new BorderedPopup(true) { /*constructor*/ {
                    setWidget(MsoyUI.createHTML(_msgs.emailWebmails(), "emailSupported"));
                }}.show();
            }
        });
        row.setWidget(0, col, showSupported);
        row.getFlexCellFormatter().setHorizontalAlignment(0, col++, HasAlignment.ALIGN_RIGHT);
        add(row);

        // account entry line
        row = new SmartTable(0, 5);
        row.setWidth("100%");

        col = 0;
        row.setText(0, col++, _msgs.emailAccount(), 1, null);
        final TextBox account = MsoyUI.createTextBox("", InviteUtils.MAX_MAIL_LENGTH, 25);
        DefaultTextListener.configure(account, _msgs.emailWebAddress());
        row.setWidget(0, col++, account);
        row.setText(0, col++, _msgs.emailPassword(), 1, null);
        final TextBox password = new PasswordTextBox();
        row.setWidget(0, col++, password);

        PushButton doimp = MsoyUI.createButton(MsoyUI.SHORT_THIN, action, null);
        row.setWidget(0, col++, doimp);

        // privacy soother
        row.setText(1, 0, _msgs.emailCaveat(), row.getCellCount(0), null);
        add(row);

        // wire up the import button
        new ClickCallback<List<EmailContact>>(doimp) {
            @Override protected boolean callService () {
                if ("".equals(account.getText())) {
                    MsoyUI.info(_msgs.inviteEnterWebAddress());
                    return false;
                }
                if ("".equals(password.getText())) {
                    MsoyUI.info(_msgs.inviteEnterWebPassword());
                    return false;
                }
                _invitesvc.getWebMailAddresses(
                    _sentAddress = account.getText(), password.getText(), this);
                return true;
            }

            @Override protected boolean gotResult (List<EmailContact> addresses) {
                account.setText(_msgs.emailWebAddress());
                password.setText("");
                if (addresses.size() == 0) {
                    MsoyUI.info(_msgs.inviteNoContacts(_sentAddress));
                } else {
                    handleAddresses(addresses);
                }
                return true;
            }

            protected String _sentAddress;
        };
    }

    /**
     * Processes contacts that were obtained from the webmail account.
     */
    protected abstract void handleAddresses (List<EmailContact> addresses);

    protected static final String PROVIDERS_IMG =
        "/images/people/invite/webmail_providers_small_horizontal.png";

    protected static final PeopleMessages _msgs = GWT.create(PeopleMessages.class);
    protected static final InviteServiceAsync _invitesvc = (InviteServiceAsync)
        ServiceUtil.bind(GWT.create(InviteService.class), InviteService.ENTRY_POINT);
}
