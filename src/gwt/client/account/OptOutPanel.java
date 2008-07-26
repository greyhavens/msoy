//
// $Id$

package client.account;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.WidgetUtil;
import com.threerings.msoy.web.data.Invitation;

import client.shell.Application;
import client.shell.Page;
import client.util.Link;
import client.util.MsoyCallback;

public class OptOutPanel extends FlexTable
{
    public OptOutPanel (String inviteId)
    {
        setCellSpacing(10);
        setStyleName("invitation"); // mimic the styles on InvitationPanel
        CAccount.membersvc.getInvitation(inviteId, false, new MsoyCallback<Invitation>() {
            public void onSuccess (Invitation invite) {
                init(invite);
            }
        });
    }

    protected void init (final Invitation invite)
    {
        int row = 0;
        getFlexCellFormatter().setStyleName(row, 0, "Header");
        setText(row++, 0, CAccount.msgs.optOutIntro(invite.inviteeEmail));

        getFlexCellFormatter().setColSpan(row, 0, 2);
        getFlexCellFormatter().setStyleName(row, 0, "Body");
        setText(row++, 0, CAccount.msgs.optOutBody1());

        HorizontalPanel footer = new HorizontalPanel();
        footer.add(new Button(CAccount.msgs.optOutAccept(), new ClickListener() {
            public void onClick (Widget widget) {
                CAccount.membersvc.optOut(invite.inviteId, new MsoyCallback<Void>() {
                    public void onSuccess (Void result) {
                        clear();
                        setText(1, 0, CAccount.msgs.optOutSuccessful(invite.inviteeEmail));
                    }
                });
            }
        }));
        footer.add(WidgetUtil.makeShim(10, 10));
        footer.add(new Button(CAccount.msgs.optOutReject(), new ClickListener() {
            public void onClick (Widget widget) {
                Link.go(Page.ME, "");
            }
        }));

        getFlexCellFormatter().setColSpan(row, 0, 2);
        setWidget(row++, 0, footer);
    }
}
