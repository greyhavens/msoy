//
// $Id$

package client.mail;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.group.gwt.GroupService;
import com.threerings.msoy.group.gwt.GroupServiceAsync;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.mail.gwt.GroupInvitePayload;
import com.threerings.msoy.mail.gwt.MailPayload;
import com.threerings.msoy.mail.gwt.PresentPayload;
import com.threerings.msoy.stuff.gwt.StuffService;
import com.threerings.msoy.stuff.gwt.StuffServiceAsync;
import com.threerings.msoy.web.client.MemberService;
import com.threerings.msoy.web.client.MemberServiceAsync;
import com.threerings.msoy.web.data.MemberCard;

import client.shell.Pages;
import client.ui.MsoyUI;
import client.ui.ThumbBox;
import client.util.Link;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

/**
 * Provides an interface for starting a new conversation with another member.
 */
public class ComposePanel extends FlowPanel
{
    public ComposePanel ()
    {
        setStyleName("compose");
        addStyleName("pagedGrid"); // for our header and footer

        SmartTable header = new SmartTable("Header", 0, 0);
        header.setWidth("100%");
        header.setHTML(0, 0, "&nbsp;", 1, "TopLeft");
        header.setText(0, 1, CMail.msgs.composeTitle(), 1, "Middle");
        header.getFlexCellFormatter().addStyleName(0, 1, "WriteTo");
        header.setHTML(0, 2, "&nbsp;", 1, "TopRight");
        add(header);

        _contents = new SmartTable("Contents", 0, 5);
        _contents.setWidget(0, 0, new ThumbBox(MemberCard.DEFAULT_PHOTO, null));
        _contents.getFlexCellFormatter().setRowSpan(0, 0, 5);
        _contents.getFlexCellFormatter().setVerticalAlignment(0, 0, HasAlignment.ALIGN_TOP);

        _contents.setText(0, 1, CMail.msgs.composeTo(), 1, "Label");
        _contents.setWidget(0, 2, _friendBox = new ListBox());
        _friendBox.addChangeListener(new ChangeListener() {
            public void onChange (Widget sender) {
                int idx = _friendBox.getSelectedIndex();
                if (idx > 0) {
                    setRecipient(_friends.get(idx-1), false);
                }
            }
        });

        _contents.setText(1, 0, CMail.msgs.composeSubject(), 1, "Label");
        _contents.setWidget(1, 1, _subject = new TextBox());
        _subject.setWidth("390px");

        _contents.setText(2, 0, CMail.msgs.composeMessage(), 1, "Label");
        _contents.getFlexCellFormatter().setVerticalAlignment(2, 0, HasAlignment.ALIGN_TOP);
        _contents.setWidget(2, 1, _body = new TextArea());
        _body.setVisibleLines(10);
        _body.setWidth("390px");

        _send = new Button(CMail.msgs.composeSend());
        _send.setEnabled(false);
        new StartConvoCallback(_send, _subject, _body) {
            public boolean gotResult (Void result) {
                MsoyUI.info(CMail.msgs.composeSent(_recipient.name.toString()));
                // if we just mailed an item as a gift, we can't go back to the item detail page
                // because we no longer have access to it, so go to the STUFF page instead
                if (_payload instanceof PresentPayload) {
                    Link.go(Pages.STUFF, ""+((PresentPayload)_payload).ident.type);
                } else {
                    History.back();
                }
                return false;
            }
            protected int getRecipientId () {
                return _recipient.name.getMemberId();
            }
            protected MailPayload getPayload () {
                return _payload;
            }
        };
        Button discard = new Button(CMail.msgs.composeDiscard(), new ClickListener() {
            public void onClick (Widget sender) {
                History.back();
            }
        });
        _contents.setWidget(4, 1, MsoyUI.createButtonPair(_send, discard));

        add(_contents);

        SmartTable footer = new SmartTable("Footer", 0, 0);
        footer.setWidth("100%");
        footer.setHTML(0, 0, "&nbsp;", 1, "BottomLeft");
        footer.setHTML(0, 1, "&nbsp;", 1, "Middle");
        footer.getFlexCellFormatter().addStyleName(0, 1, "Subject");
        footer.setHTML(0, 2, "&nbsp;", 1, "BottomRight");
        add(footer);

        // start our focus in the subject field
        _subject.setFocus(true);
    }

    public void setRecipientId (int recipientId)
    {
        _membersvc.getMemberCard(recipientId, new MsoyCallback<MemberCard>() {
            public void onSuccess (MemberCard result) {
                if (result != null) {
                    setRecipient(result, true);
                }
            }
        });
    }

    public void setGiftItem (byte type, int itemId)
    {
        _stuffsvc.loadItem(CMail.ident, new ItemIdent(type, itemId),
            new MsoyCallback<Item>() {
                public void onSuccess (Item result) {
                    PresentPayload payload = new PresentPayload(
                        result.getIdent(), result.name, result.getThumbnailMedia());
                    _contents.setText(3, 0, CMail.msgs.composeAttachment(), 1, "Label");
                    _contents.getFlexCellFormatter().setVerticalAlignment(
                        3, 0, HasAlignment.ALIGN_TOP);
                    _contents.setWidget(3, 1, new ThumbBox(payload.thumbMedia, null));
                    _payload = payload;
                }
            });
    }

    public void setGroupInviteId (int groupId)
    {
        _groupsvc.getGroupInfo(CMail.ident, groupId, new MsoyCallback<GroupService.GroupInfo>() {
            public void onSuccess (GroupService.GroupInfo result) {
                _contents.setText(3, 0, CMail.msgs.composeGroupInvite(), 1, "Label");
                _contents.setText(3, 1, CMail.msgs.composeGroupDeets("" + result.name));
                _payload = new GroupInvitePayload(result.name.getGroupId(), false);
            }
        });
    }

    protected void onLoad ()
    {
        super.onLoad();

        // TODO: replace this with a magical auto-completing search box
        if (_friendBox.isAttached()) {
            _membersvc.loadFriends(CMail.ident, CMail.getMemberId(),
                new MsoyCallback<MemberService.FriendsResult>() {
                    public void onSuccess (MemberService.FriendsResult result) {
                        _friends = result.friends;
                        _friendBox.addItem("Select...");
                        Collections.sort(_friends, new Comparator<MemberCard>() {
                            public int compare (MemberCard one, MemberCard two) {
                                return MemberName.compareNames(one.name, two.name);
                            }
                        });
                        for (MemberCard friend : _friends) {
                            _friendBox.addItem("" + friend.name);
                        }
                    }
                });
        }
    }

    protected void setRecipient (MemberCard recipient, boolean clearBox)
    {
        _recipient = recipient;
        _contents.setWidget(0, 0, new ThumbBox(recipient.photo, null));
        if (clearBox) {
            _contents.setText(0, 2, recipient.name.toString());
        }
        _send.setEnabled(true);
    }

    protected SmartTable _contents;
    protected MemberCard _recipient;
    protected MailPayload _payload;

    protected ListBox _friendBox;
    protected List<MemberCard> _friends;

    protected TextBox _subject;
    protected TextArea _body;
    protected Button _send;

    protected static final MemberServiceAsync _membersvc = (MemberServiceAsync)
        ServiceUtil.bind(GWT.create(MemberService.class), MemberService.ENTRY_POINT);
    protected static final StuffServiceAsync _stuffsvc = (StuffServiceAsync)
        ServiceUtil.bind(GWT.create(StuffService.class), StuffService.ENTRY_POINT);
    protected static final GroupServiceAsync _groupsvc = (GroupServiceAsync)
        ServiceUtil.bind(GWT.create(GroupService.class), GroupService.ENTRY_POINT);
}
