//
// $Id$

package client.people;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.person.gwt.InvitationResults;
import com.threerings.msoy.person.gwt.InviteService;
import com.threerings.msoy.person.gwt.InviteServiceAsync;
import com.threerings.msoy.web.gwt.EmailContact;
import com.threerings.msoy.web.gwt.Pages;

import client.ui.MsoyUI;
import client.util.MsoyCallback;
import client.util.NoopAsyncCallback;
import client.util.ServiceUtil;

/**
 * Displays a generic "invite your friends to Whirled" interface.
 */
public class WhirledInvitePanel extends InvitePanel
{
    public WhirledInvitePanel ()
    {
        // introduction to sharing
        SmartTable intro = new SmartTable();
        intro.setStyleName("MainHeader");
        intro.setWidget(0, 0, new Image("/images/people/share_header.png"));
        intro.setWidget(0, 1, MsoyUI.createHTML(_msgs.shareIntro(), "MainHeaderText"));
        add(intro);

        // buttons to invoke the various ways to invite
        addMethodButton("Email", new InviteMethodCreator() {
            public Widget create () {
                return new InviteEmailListPanel();
            }
        });
        addMethodButton("IM", new InviteMethodCreator() {
            public Widget create () {
                return new IMPanel(ShareUtil.getAffiliateLandingUrl(Pages.LANDING));
            }
        });
        addMethodButtons();
    }

    protected class InviteEmailListPanel extends EmailListPanel
    {
        protected void handleSend (String from, String msg, final List<EmailContact> addrs) {
            if (addrs.isEmpty()) {
                MsoyUI.info(_msgs.inviteEnterAddresses());
                return;
            }

            String subject = "TODO";
//             String subject = _subject.getText().trim();
//             if (subject.length() < InviteUtils.MIN_SUBJECT_LENGTH) {
//                 MsoyUI.error(_msgs.inviteSubjectTooShort(""+InviteUtils.MIN_SUBJECT_LENGTH));
//                 _subject.setFocus(true);
//                 return;
//             }

//             String msg = _customMessage.getText().trim();
//             if (msg.equals(_msgs.inviteCustom())) {
//                 msg = "";
//             }

            sendEvent("invited", new NoopAsyncCallback());

            _invitesvc.sendInvites(addrs, from, subject, msg, false, new MsoyCallback<InvitationResults>() {
                    public void onSuccess (InvitationResults ir) {
                        _addressList.clear();
                        InviteUtils.showInviteResults(addrs, ir);
                    }
                });
        }
    }

    protected static final InviteServiceAsync _invitesvc = (InviteServiceAsync)
        ServiceUtil.bind(GWT.create(InviteService.class), InviteService.ENTRY_POINT);
}
