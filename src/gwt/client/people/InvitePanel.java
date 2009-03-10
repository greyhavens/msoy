//
// $Id$

package client.people;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

import client.ui.MsoyUI;

/**
 * Display a UI allowing users to send out invitations to Whirled.
 */
public abstract class InvitePanel extends FlowPanel
{
    public InvitePanel ()
    {
        setStyleName("invitePanel");

        _buttons = new HorizontalPanel();
        _buttons.setWidth("100%");
        _buttons.setHorizontalAlignment(HorizontalPanel.ALIGN_CENTER);
    }

    protected void addMethodButton (String label, final InviteMethodCreator creator)
    {
        _buttons.add(MsoyUI.createButton(MsoyUI.LONG_THICK, label, new ClickListener() {
            public void onClick (Widget sender) {
                setMethod(creator.create()); //new SendURLPanel();
            }
        }));
    }

    protected void addMethodButtons ()
    {
        add(WidgetUtil.makeShim(10, 10));
        add(_buttons);
        add(WidgetUtil.makeShim(10, 10));
        _methodRow = getWidgetCount();
    }

    protected void setMethod (Widget panel)
    {
        while (getWidgetCount() > _methodRow) {
            remove(_methodRow);
        }
        if (panel != null) {
            add(panel);
        }
    }

    protected void sendEvent (String name, AsyncCallback<Void> callback)
    {
//         // only send one event per instance of this
//         if (_sendEvents) {
//             _membersvc.trackTestAction(CShell.frame.getVisitorInfo(), name,
//                                        "2008 12 find friends on registration", callback);
//             _sendEvents = false;
//         }
    }

    /** Allows various invite methods to be hooked up to click listeners. */
    protected static interface InviteMethodCreator
    {
        /** Creates the widget that will display this invite method. */
        Widget create ();
    }

    /**
     * Invite method consisting of a text area to copy a URL from.
     */
    protected static class IMPanel extends SmartTable
    {
        public IMPanel (String shareURL)
        {
            setStyleName("IM");
            setWidth("100%");
            setText(0, 0, _msgs.inviteIMCopy(), 1, "Bold");
            setText(1, 0, _msgs.inviteIMTip(), 1, null);
            TextBox link = new TextBox();
            link.setText(shareURL);
            link.setReadOnly(true);
            MsoyUI.selectAllOnFocus(link);
            setWidget(2, 0, link);
        }
    }

//         if (justRegistered) {
//             SmartTable table = new SmartTable(5, 0);
//             table.setStyleName("FindFriends");
//             table.setWidth("100%");
//             table.setText(1, 0, _msgs.justRegInviteBanner(), 2, "Title");

//             // callback to redirect to #world-h
//             final AsyncCallback<Void> goHome = new AsyncCallback<Void>() {
//                 public void onFailure (Throwable caught) {
//                     Link.go(Pages.WORLD, "h");
//                 }
//                 public void onSuccess (Void result) {
//                     Link.go(Pages.WORLD, "h");
//                 }
//             };
//             // add a link that sends an event, and then redirects after the result comes back;
//             // this prevents the (presumed) synchronous redirect from cancelling the rpc
//             // TODO: also send an event if the user navigates away from the page
//             Label skip = MsoyUI.createActionLabel(_msgs.inviteSkipButton(), new ClickListener() {
//                 public void onClick (Widget sender) {
//                     sendEvent("skipped", goHome);
//                 }
//             });
//             table.setWidget(1, 1, skip, 1, "skip");
//             box.add(table);
//             box.add(WidgetUtil.makeShim(10, 10));
//         }

//         // Add a name/e-mail and import webmail section
//         SmartTable input = new SmartTable(0, 5);
//         int row = 0;
//         if (!justRegistered) {
//             input.setText(row++, 0, _msgs.inviteManualTitle(), 3, null);
//             input.setWidget(row, 0, _friendName = MsoyUI.createTextBox(
//                 "", InviteUtils.MAX_NAME_LENGTH, 0));
//             DefaultTextListener.configure(_friendName, _msgs.inviteFriendName());
//             _friendEmail = MsoyUI.createTextBox("", InviteUtils.MAX_MAIL_LENGTH, 0);
//             _friendEmail.addKeyboardListener(new EnterClickAdapter(addEmail));
//             DefaultTextListener.configure(_friendEmail, _msgs.inviteFriendEmail());
//             input.setWidget(row, 1, _friendEmail, 2, null);
//             input.setWidget(row++, 2, new Button(_msgs.inviteAdd(), addEmail));
//         }

//         Widget showSupported = MsoyUI.createActionLabel(_msgs.inviteSupported(),
//             "ImportSupportLink", new ClickListener() {
//                 public void onClick (Widget widget) {
//                     BorderedPopup popup = new BorderedPopup(true);
//                     popup.setWidget(MsoyUI.createHTML(_msgs.inviteSupportedList(),
//                                                       "importSupportList"));
//                     popup.show();
//                 }
//             });

//         if (justRegistered) {
//             input.setText(row++, 0, _msgs.justRegInviteGrabber(), 4, null);
//             input.setWidget(row++, 0, showSupported, 4, null);
//         } else {
//             input.setText(row, 0, _msgs.inviteGrabber(), 3, null);
//             input.setWidget(row++, 1, showSupported);
//         }

//         input.setWidget(row, 0, _webAddress = MsoyUI.createTextBox(
//             "", InviteUtils.MAX_MAIL_LENGTH, 0));
//         DefaultTextListener.configure(_webAddress, _msgs.inviteWebAddress());
//         input.setText(row, 1, _msgs.inviteWebPassword());
//         input.setWidget(row, 2, _webPassword = new PasswordTextBox());
//         Button webImport = new Button(_msgs.inviteWebImport());
//         input.setWidget(row++, 3, webImport);
//         input.setText(row++, 0, _msgs.inviteNote(), 4, "Tip");
//         box.add(input);

//         // Shows the people that will be mailed
//         box.add(WidgetUtil.makeShim(10, 10));
//         box.add(_emailList = new InviteList());

//         // now that we have all of our components, create our importer
//         new InviteUtils.WebmailImporter (webImport, _webAddress, _webPassword, _emailList, true) {
//             // only add non-members to our list
//             @Override protected boolean shouldAddToList (EmailContact contact) {
//                 return contact.mname == null;
//             }
//             // offer to make friends with the ones on the list who are not our friends already
//             @Override protected void handleLeftovers (List<EmailContact> leftovers) {
//                 if (leftovers.size() > 0) {
//                     webmailResults(leftovers);
//                 }
//             }
//         };

//         // From, subject and custom message box
//         SmartTable customs = new SmartTable(0, 5);
//         customs.setWidth("100%");
//         customs.setText(0, 0, _msgs.inviteFrom(), 1, "Title");
//         customs.getFlexCellFormatter().setWidth(0, 0, "10px");
//         String ourName = CShell.creds.name.toString();
//         _fromName = MsoyUI.createTextBox(ourName, InviteUtils.MAX_NAME_LENGTH, 0);
//         customs.setWidget(0, 1, _fromName);
//         customs.setText(1, 0, _msgs.inviteSubject(), 1, "Title");
//         customs.getFlexCellFormatter().setWidth(1, 0, "10px");
//         _subject = MsoyUI.createTextBox(null, InviteUtils.MAX_SUBJECT_LENGTH, 0);
//         _subject.setWidth("100%");
//         customs.setWidget(1, 1, _subject);
//         _customMessage = MsoyUI.createTextArea("", -1, 3);
//         customs.setWidget(2, 0, _customMessage, 2, null);
//         _customMessage.setWidth("100%");
//         DefaultTextListener.configure(_customMessage, _msgs.inviteCustom());
//         box.add(WidgetUtil.makeShim(10, 10));
//         box.add(customs);

//         // Not currently used
//         _anonymous = new CheckBox(_msgs.inviteAnonymous());

//         // Invite tip and button
//         box.add(WidgetUtil.makeShim(10, 10));
//         SmartTable buttons = new SmartTable(0, 0);
//         buttons.setWidth("100%");
//         buttons.setText(0, 0, _msgs.inviteMessage(), 1, "Tip");
//         buttons.setWidget(0, 1, MsoyUI.createButton(MsoyUI.LONG_THIN, _msgs.inviteButton(),
//             new ClickListener() {
//                 public void onClick (Widget widget) {
//                     if (_emailList.getItems().isEmpty()) {
//                         MsoyUI.info(_msgs.inviteEnterAddresses());
//                     } else {
//                         checkAndSend();
//                     }
//                 }
//             }));

//         box.add(buttons);
//         add(box);

//         // Shows pending invitations
//         _penders = new SmartTable(0, 5);
//         _penders.setText(0, 0, _msgs.invitePendingHeader(), 3, "Header");
//         _penders.setText(1, 0, _msgs.invitePendingTip(), 3, "Tip");
//         _penders.setText(2, 0, _msgs.inviteNoPending());
//         add(_penders);

//         _invitesvc.getInvitationsStatus(new MsoyCallback<MemberInvites>() {
//             public void onSuccess (MemberInvites invites) {
//                 gotStatus(invites);
//             }
//         });
//     }

//     public void setMessage (String text)
//     {
//         _customMessage.setText(text);
//     }

//     protected void addEmail ()
//     {
//         InviteUtils.addEmailIfValid(_friendName, _friendEmail, _emailList);
//     }

//     protected void gotStatus (MemberInvites invites)
//     {
//         _invites = invites;
//         addPendingInvites(_invites.pendingInvitations);
//     }

//     protected void addPendingInvites (List<Invitation> penders)
//     {
//         int prow = (_penders.getRowCount() == 2 || _penders.getCellCount(2) == 1) ?
//             2 : _penders.getRowCount();
//         for (int ii = 0; ii < penders.size(); ii++) {
//             final int frow = prow++;
//             final Invitation inv = penders.get(ii);
//             _penders.setWidget(frow, 0,
//                     MsoyUI.createActionImage("/images/profile/remove.png", new ClickListener() {
//                 public void onClick (Widget widget) {
//                     removeInvite(inv);
//                 }
//             }));
//             _penders.setText(frow, 1, inv.inviteeEmail);
//             _penders.setText(frow, 2, _invites.serverUrl + inv.inviteId);
//         }
//     }

//     protected void removeInvite (final Invitation inv)
//     {
//         _invitesvc.removeInvitation(inv.inviteId, new MsoyCallback<Void>() {
//             public void onSuccess (Void result) {
//                 for (int ii = 2, nn = _penders.getRowCount(); ii < nn; ii++) {
//                     if (inv.inviteeEmail.equals(_penders.getText(ii, 1))) {
//                         _penders.removeRow(ii);
//                         break;
//                     }
//                 }
//             }
//         });
//     }

//     protected void checkAndSend ()
//     {
//         final List<EmailContact> invited = InviteUtils.getValidUniqueAddresses(_emailList);

//         boolean anon = _anonymous.isChecked();
//         String from = _fromName.getText().trim();
//         if (!anon && from.length() == 0) {
//             MsoyUI.error(_msgs.inviteEmptyFromField());
//             _fromName.setFocus(true);
//             return;
//         }
//         String subject = _subject.getText().trim();
//         if (subject.length() < InviteUtils.MIN_SUBJECT_LENGTH) {
//             MsoyUI.error(
//                 _msgs.inviteSubjectTooShort(String.valueOf(InviteUtils.MIN_SUBJECT_LENGTH)));
//             _subject.setFocus(true);
//             return;
//         }
//         String msg = _customMessage.getText().trim();
//         if (msg.equals(_msgs.inviteCustom())) {
//             msg = "";
//         }

//         sendEvent("invited", new NoopAsyncCallback());

//         _invitesvc.sendInvites(invited, from, subject, msg, anon,
//             new MsoyCallback<InvitationResults>() {
//             public void onSuccess (InvitationResults ir) {
//                 addPendingInvites(ir.pendingInvitations);
//                 _emailList.clear();
//                 InviteUtils.showInviteResults(invited, ir);
//             }
//         });
//     }

//     protected void sendEvent (String name, AsyncCallback<Void> callback)
//     {
//         // only send one event per instance of this
//         if (_sendEvents) {
//             _membersvc.trackTestAction(CShell.frame.getVisitorInfo(), name,
//                                        "2008 12 find friends on registration", callback);
//             _sendEvents = false;
//         }
//     }

//     protected void webmailResults (List<EmailContact> contacts)
//     {
//         InviteUtils.ResultsPopup rp = new InviteUtils.ResultsPopup(_msgs.webmailResults());
//         int row = 0;
//         SmartTable contents = rp.getContents();

//         contents.setText(row++, 0, _msgs.inviteResultsMembers());
//         for (EmailContact ec : contacts) {
//             contents.setText(row, 0, _msgs.inviteMember(ec.name, ec.email));
//             ClickListener onClick = new FriendInviter(ec.mname, "InvitePanel");
//             contents.setWidget(row, 1, MsoyUI.createActionImage(
//                         "/images/profile/addfriend.png", onClick));
//             contents.setWidget(row++, 2, MsoyUI.createActionLabel(
//                         _msgs.mlAddFriend(), onClick));
//         }

//         rp.show();
//     }

//     protected MemberInvites _invites;

//     protected TextBox _fromName;
//     protected TextBox _subject;
//     protected TextArea _customMessage;
//     protected CheckBox _anonymous;
//     protected SmartTable _penders;
//     protected TextBox _webAddress;
//     protected PasswordTextBox _webPassword;

//     protected TextBox _friendName;
//     protected TextBox _friendEmail;
//     protected InviteList _emailList;

//     protected boolean _sendEvents;

//     protected static final PeopleMessages _msgs = GWT.create(PeopleMessages.class);
//     protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
//     protected static final InviteServiceAsync _invitesvc = (InviteServiceAsync)
//         ServiceUtil.bind(GWT.create(InviteService.class), InviteService.ENTRY_POINT);
//     protected static final WebMemberServiceAsync _membersvc = (WebMemberServiceAsync)
//         ServiceUtil.bind(GWT.create(WebMemberService.class), WebMemberService.ENTRY_POINT);

    /** The row where the invite method is. */
    protected int _methodRow;

    /** The buttons for the various invite methods. */
    protected HorizontalPanel _buttons;

    protected static final PeopleMessages _msgs = GWT.create(PeopleMessages.class);
//     protected static final WebMemberServiceAsync _membersvc = (WebMemberServiceAsync)
//         ServiceUtil.bind(GWT.create(WebMemberService.class), WebMemberService.ENTRY_POINT);
}
