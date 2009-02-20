package client.people;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import client.ui.MsoyUI;
import client.util.ClickCallback;
import client.util.ServiceUtil;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.SourcesClickEvents;
import com.google.gwt.user.client.ui.TextBox;
import com.threerings.msoy.person.gwt.InviteService;
import com.threerings.msoy.person.gwt.InviteServiceAsync;
import com.threerings.msoy.web.gwt.EmailContact;

/**
 * Performs various and sundry tasks related to invitations. I.e. code pulled out of InvitePanel
 * for sharing with GameInvitePanel.
 */
public class InviteUtils
{
    /** Maximum length for names. */
    public static final int MAX_NAME_LENGTH = 80;

    /** Maximum length for email addresses. */
    protected static final int MAX_MAIL_LENGTH = 200;

    /**
     * Adds a name and email address to the invite list if both are valid. Reports errors to the
     * user and resets the text fields to default values on success.
     */
    public static void addEmailIfValid (TextBox name, TextBox email, InviteList list)
    {
        if ("".equals(email.getText())) {
            return;

        } else if (!email.getText().matches(MsoyUI.EMAIL_REGEX)) {
            MsoyUI.info(_msgs.inviteInvalidEmail());

        } else if (name.getText().length() == 0 ||
                name.getText().equals(_msgs.inviteFriendName())) {
            MsoyUI.info(_msgs.inviteInvalidName());

        } else {
            list.addItem(name.getText(), email.getText());
            name.setText(_msgs.inviteFriendName());
            email.setText(_msgs.inviteFriendEmail());
        }
    }

    /**
     * Checks the list for duplicates and invalid addresses and returns either a null list if
     * the invites should not be sent, or the filtered list of contacts to invite.
     */
    public static List<EmailContact> getValidUniqueAddresses (InviteList list)
    {
        List<EmailContact> invited = new ArrayList<EmailContact>();
        Set<String> accepted = new HashSet<String>();
        for (EmailContact contact : list.getItems()) {
            if (!contact.email.matches(MsoyUI.EMAIL_REGEX)) {
                MsoyUI.error(_msgs.inviteInvalidAddress(contact.email));
                return null;
            }
            String laddr = contact.email.toLowerCase();
            if (!accepted.contains(laddr)) {
                accepted.add(laddr);
                invited.add(contact);
            }
        }
        return invited;
    }
    
    /**
     * Handles the standard bits of importing webmail addresses and delegates the optional bits.
     */
    public static class WebmailImporter extends ClickCallback<List<EmailContact>>
    {
        public WebmailImporter (
            SourcesClickEvents trigger, TextBox address, TextBox password, InviteList list,
            boolean skipFriends)
        {
            super(trigger);
            _address = address;
            _password = password;
            _list = list;
            _skipFriends = skipFriends;
        }

        @Override protected boolean callService ()
        {
            if ("".equals(_address.getText())) {
                MsoyUI.info(_msgs.inviteEnterWebAddress());
                return false;
            }
            if ("".equals(_address.getText())) {
                MsoyUI.info(_msgs.inviteEnterWebPassword());
                return false;
            }
            _invitesvc.getWebMailAddresses(
                _sentAddress = _address.getText(), _password.getText(), _skipFriends, this);
            return true;
        }

        @Override protected boolean gotResult (List<EmailContact> addresses)
        {
            if (addresses.size() == 0) {
                MsoyUI.info(_msgs.inviteNoContacts(_sentAddress));
                return true;
            }
            List<EmailContact> leftovers = new ArrayList<EmailContact>();
            int added = 0;
            for (EmailContact ec : addresses) {
                if (shouldAddToList(ec)) {
                    if (_list.addItem(ec.name, ec.email)) {
                        added++;
                    }
                } else {
                    leftovers.add(ec);
                }
            }
            _address.setText(_msgs.inviteWebAddress());
            _password.setText("");
            handleLeftovers(leftovers);
            if (added == 0) {
                MsoyUI.info(_msgs.inviteNoNewNonMembers(_sentAddress));
            }
            return true;
        }

        /**
         * Returns true if the given contact meets our criteria and should be added to the list.
         * By default, everything is added to the list.
         */
        protected boolean shouldAddToList (EmailContact contact)
        {
            return true;
        }

        /**
         * Processes contacts that were not added to the list. By default, this does nothing.
         */
        protected void handleLeftovers (List<EmailContact> addresses)
        {
        }

        protected TextBox _address;
        protected TextBox _password;
        protected String _sentAddress;
        protected InviteList _list;
        protected boolean _skipFriends;
    }

    protected static final PeopleMessages _msgs = GWT.create(PeopleMessages.class);
    protected static final InviteServiceAsync _invitesvc = (InviteServiceAsync)
        ServiceUtil.bind(GWT.create(InviteService.class), InviteService.ENTRY_POINT);
}
