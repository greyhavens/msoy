//
// $Id$

package client.account;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.web.client.WebMemberService;
import com.threerings.msoy.web.client.WebMemberServiceAsync;
import com.threerings.msoy.web.data.Invitation;

import client.shell.Pages;
import client.util.Link;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

public class OptOutPanel extends FlexTable
{
    public OptOutPanel (String inviteId)
    {
        setCellSpacing(10);
        setStyleName("invitation"); // mimic the styles on InvitationPanel
        _membersvc.getInvitation(inviteId, false, new MsoyCallback<Invitation>() {
            public void onSuccess (Invitation invite) {
                init(invite);
            }
        });
    }

    protected void init (final Invitation invite)
    {
        int row = 0;
        getFlexCellFormatter().setStyleName(row, 0, "Header");
        setText(row++, 0, _msgs.optOutIntro(invite.inviteeEmail));

        getFlexCellFormatter().setColSpan(row, 0, 2);
        getFlexCellFormatter().setStyleName(row, 0, "Body");
        setText(row++, 0, _msgs.optOutBody1());

        HorizontalPanel footer = new HorizontalPanel();
        footer.add(new Button(_msgs.optOutAccept(), new ClickListener() {
            public void onClick (Widget widget) {
                _membersvc.optOut(invite.inviteId, new MsoyCallback<Void>() {
                    public void onSuccess (Void result) {
                        clear();
                        setText(1, 0, _msgs.optOutSuccessful(invite.inviteeEmail));
                    }
                });
            }
        }));
        footer.add(WidgetUtil.makeShim(10, 10));
        footer.add(new Button(_msgs.optOutReject(), new ClickListener() {
            public void onClick (Widget widget) {
                Link.go(Pages.ME, "");
            }
        }));

        getFlexCellFormatter().setColSpan(row, 0, 2);
        setWidget(row++, 0, footer);
    }

    protected static final AccountMessages _msgs = GWT.create(AccountMessages.class);
    protected static final WebMemberServiceAsync _membersvc = (WebMemberServiceAsync)
        ServiceUtil.bind(GWT.create(WebMemberService.class), WebMemberService.ENTRY_POINT);
}
