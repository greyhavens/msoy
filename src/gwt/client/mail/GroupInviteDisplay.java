//
// $Id$

package client.mail;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.InlineLabel;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.group.data.all.GroupMembership.Rank;
import com.threerings.msoy.group.gwt.GroupService;
import com.threerings.msoy.group.gwt.GroupServiceAsync;
import com.threerings.msoy.mail.gwt.GroupInvitePayload;

import client.ui.MsoyUI;
import client.util.ClickCallback;
import client.util.InfoCallback;

/**
 * Displays a group invitation payload.
 */
public class GroupInviteDisplay extends MailPayloadDisplay
{
    @Override // from MailPayloadDisplay
    public Widget widgetForRecipient ()
    {
        return new DisplayWidget(_invitePayload.responded == false);
    }

    @Override // from MailPayloadDisplay
    protected void didInit ()
    {
        // no sanity checks: if anything breaks here, it's already a disaster
        _invitePayload = (GroupInvitePayload) _message.payload;
    }

    protected class DisplayWidget extends FlowPanel
    {
        public DisplayWidget (boolean enabled)
        {
            super();
            _enabled = enabled;
            setStyleName("groupInvitation");
            refreshUI();
        }

        protected void refreshUI ()
        {
            _groupsvc.getGroupInfo(
                _invitePayload.groupId, new InfoCallback<GroupService.GroupInfo>() {
                public void onSuccess (GroupService.GroupInfo result) {
                    _info = result;
                    buildUI();
                }
            });
        }

        protected void buildUI ()
        {
            clear();
            if (_enabled && _info.rank != Rank.NON_MEMBER) {
                add(MsoyUI.createLabel(_msgs.groupAlreadyMember(""+_info.name), null));
                return;
            }

            add(new InlineLabel(_msgs.groupInvitation(""+_info.name), true, false, true));
            add(WidgetUtil.makeShim(5, 5));
            Button joinButton = new Button(_msgs.groupBtnJoin());
            joinButton.addStyleName("JoinButton");
            joinButton.setEnabled(_enabled);
            new ClickCallback<Void>(joinButton) {
                @Override protected boolean callService () {
                    _groupsvc.joinGroup(_invitePayload.groupId, this);
                    return true;
                }
                @Override protected boolean gotResult (Void result) {
                    _invitePayload.responded = true;
                    updateState(_invitePayload, new InfoCallback.NOOP<Void>());
                    refreshUI();
                    return true;
                }
            };
            add(joinButton);
        }

        protected boolean _enabled;
        protected GroupService.GroupInfo _info;
    }

    protected GroupInvitePayload _invitePayload;

    protected static final MailMessages _msgs = GWT.create(MailMessages.class);
    protected static final GroupServiceAsync _groupsvc = GWT.create(GroupService.class);
}
